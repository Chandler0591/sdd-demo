package com.library.service.book;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.book.RecognizedBookInfo;
import com.library.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通义千问视觉（Qwen-VL）书籍识别实现：调用 DashScope OpenAI 兼容接口，
 * 以 JSON 返回书名 / 作者 / 出版社 / 年份 / ISBN（识别不到留空，前端可修正后录入）。
 * 通过配置 library.recognizer.provider=qwen 装配；
 * API Key 经环境变量 DASHSCOPE_API_KEY 注入，不写入配置文件。
 */
@Service
@ConditionalOnProperty(name = "library.recognizer.provider", havingValue = "qwen")
public class QwenBookInfoRecognizer implements BookInfoRecognizer {

    /** 识别提示词：只输出 JSON，ISBN 仅接受清晰可见的编号，不得编造。 */
    static final String PROMPT = """
            你是图书编目助手，从书籍封面照片中提取图书信息。只输出一个 JSON 对象，不要输出任何其他文字或 markdown 代码块，格式如下：
            {"title":"书名","author":"作者","publisher":"出版社","publishYear":2020,"isbn":"ISBN"}
            要求：
            1. title 必填，识别不到输出空字符串；
            2. author、publisher 识别不到输出空字符串；
            3. publishYear 必须是 4 位数字，识别不到输出 null；
            4. ISBN 只接受照片上清晰可见的以 978 或 979 开头的数字串（封面通常没有 ISBN，看不到就输出空字符串，绝对不要编造）；
            5. 不要输出 markdown 代码块或多余说明。
            """;

    /** 匹配被 markdown 代码块包裹的 JSON。 */
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*(\\{.*})\\s*```", Pattern.DOTALL);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String endpoint;

    public QwenBookInfoRecognizer(ObjectMapper objectMapper,
                                  @Value("${library.recognizer.qwen.api-key:}") String apiKey,
                                  @Value("${library.recognizer.qwen.model:qwen3.6-flash}") String model,
                                  @Value("${library.recognizer.qwen.endpoint:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}") String endpoint) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public RecognizedBookInfo recognize(byte[] photoBytes) {
        if (photoBytes == null || photoBytes.length == 0) {
            throw new BusinessException("无法从照片识别出书籍信息，请重新拍照或手工录入");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("未配置通义千问 API Key（环境变量 DASHSCOPE_API_KEY），请先配置，或改为手工录入");
        }
        return parse(callQwen(photoBytes));
    }

    /** 调用 DashScope OpenAI 兼容接口，返回模型输出的文本内容。 */
    private String callQwen(byte[] photoBytes) {
        Map<String, Object> body = Map.of(
                "model", model,
                // 关掉思考模式：图书信息提取无需推理链，省 token 降延迟
                "enable_thinking", false,
                "messages", List.of(
                        Map.of("role", "system", "content", PROMPT),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "image_url", "image_url",
                                        Map.of("url", "data:" + detectImageMime(photoBytes) + ";base64,"
                                                + Base64.getEncoder().encodeToString(photoBytes))),
                                Map.of("type", "text", "text", "请识别这张书籍封面照片，按系统要求返回 JSON。")
                        ))
                ));
        try {
            JsonNode resp = restClient.post()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String content = resp == null ? null : resp.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new BusinessException("千问未返回识别结果，请重试或手工录入");
            }
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("千问识别调用失败：" + rootMessage(e) + "，请重试或手工录入");
        }
    }

    /** 解析模型返回的 JSON（容忍 markdown 代码块包裹），识别不到书名的视为识别失败。 */
    RecognizedBookInfo parse(String content) {
        Matcher matcher = JSON_BLOCK.matcher(content);
        String json = matcher.find() ? matcher.group(1) : content;
        try {
            JsonNode node = objectMapper.readTree(json);
            RecognizedBookInfo info = new RecognizedBookInfo();
            info.setTitle(text(node, "title"));
            info.setAuthor(text(node, "author"));
            info.setPublisher(text(node, "publisher"));
            JsonNode year = node.get("publishYear");
            info.setPublishYear(year != null && year.isInt() && year.asInt() >= 1000 && year.asInt() <= 2100 ? year.asInt() : null);
            info.setIsbn(text(node, "isbn"));
            if (isBlank(info.getTitle())) {
                throw new BusinessException("未能识别出书名，请重新拍照或手工录入");
            }
            return info;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("识别结果解析失败，请重试或手工录入");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String s = value.asText(null);
        return isBlank(s) ? null : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 图片 MIME 探测：PNG 魔数，其余按 JPEG 处理（小程序压缩图均为这两类）。 */
    private static String detectImageMime(byte[] bytes) {
        if (bytes.length > 4 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        return "image/jpeg";
    }

    /** 取最底层异常消息（截断，避免过长），不暴露请求细节。 */
    private static String rootMessage(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = current.getClass().getSimpleName();
        }
        return message.length() > 100 ? message.substring(0, 100) + "..." : message;
    }
}
