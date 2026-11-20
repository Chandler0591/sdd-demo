package com.library.service.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.book.RecognizedBookInfo;
import com.library.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 千问识别结果解析单测：纯 JSON / markdown 包裹 / 字段缺失 / 无书名失败。
 */
class QwenBookInfoRecognizerTest {

    private QwenBookInfoRecognizer recognizer;

    @BeforeEach
    void setUp() {
        recognizer = new QwenBookInfoRecognizer(new ObjectMapper(), "test-key", "qwen-vl-max",
                "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
    }

    @Test
    void parsePlainJson_extractsAllFields() {
        RecognizedBookInfo info = recognizer.parse(
                "{\"title\":\"万历十五年\",\"author\":\"黄仁宇\",\"publisher\":\"中华书局\",\"publishYear\":2006,\"isbn\":\"9787101056732\"}");
        assertEquals("万历十五年", info.getTitle());
        assertEquals("黄仁宇", info.getAuthor());
        assertEquals("中华书局", info.getPublisher());
        assertEquals(2006, info.getPublishYear());
        assertEquals("9787101056732", info.getIsbn());
    }

    @Test
    void parseMarkdownWrappedJson_isTolerated() {
        RecognizedBookInfo info = recognizer.parse(
                "```json\n{\"title\":\"老人与海\",\"author\":\"海明威\",\"publisher\":\"上海译文出版社\",\"publishYear\":null,\"isbn\":\"\"}\n```");
        assertEquals("老人与海", info.getTitle());
        assertNull(info.getPublishYear());
        assertNull(info.getIsbn());
    }

    @Test
    void parseMissingFields_leavesThemNull() {
        RecognizedBookInfo info = recognizer.parse("{\"title\":\"只有书名\",\"publishYear\":\"2020\"}");
        assertEquals("只有书名", info.getTitle());
        assertNull(info.getAuthor());
        assertNull(info.getPublisher());
        // 年份为字符串或非法值时置空
        assertNull(info.getPublishYear());
        assertNull(info.getIsbn());
    }

    @Test
    void parseEmptyTitle_throwsBusinessException() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> recognizer.parse("{\"title\":\"\",\"author\":\"某作者\"}"));
        assertTrue(e.getMessage().contains("书名"));
    }

    @Test
    void parseInvalidJson_throwsBusinessException() {
        assertThrows(BusinessException.class, () -> recognizer.parse("这不是 JSON"));
    }
}
