package com.library.service.storage;

import com.library.exception.BusinessException;
import com.library.id.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

/**
 * 上架照片本地存储：文件写入本地目录，数据库只存相对路径。
 * 接口化封装，后续可平滑切换为对象存储（MinIO / OSS）。
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");

    /** 照片存储目录（配置项 library.upload-dir，相对运行目录） */
    @Value("${library.upload-dir:uploads/shelving/}")
    private String uploadDir;

    private final SnowflakeIdGenerator idGenerator;

    public FileStorageService(SnowflakeIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * 保存上传照片，返回 Web 访问路径（如 /uploads/xxxx.jpg，经 /uploads/** 静态映射到上传目录）。
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的照片");
        }
        String original = file.getOriginalFilename();
        String extension = "";
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 jpg / jpeg / png 格式照片");
        }

        String filename = idGenerator.nextId() + extension;
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = dir.resolve(filename).normalize();
        try {
            Files.createDirectories(dir);
            file.transferTo(target);
        } catch (IOException e) {
            // 上传失败清理已写入的临时文件，避免孤儿文件
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // 清理失败不影响主流程，孤儿文件清理留作后续 change
            }
            throw new BusinessException("照片保存失败，请重试");
        }
        // 返回固定 Web 前缀 + 文件名：/uploads/** 由 WebConfig 静态映射到本地上传目录，
        // 请求 /uploads/xxx.png 时仅文件名拼接到映射目录，避免目录段重复导致 404
        return "/uploads/" + filename;
    }
}
