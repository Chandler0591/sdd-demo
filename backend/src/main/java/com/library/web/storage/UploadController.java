package com.library.web.storage;

import com.library.service.storage.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传接口：上架现场照片（本地目录存储，返回相对路径）。
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final FileStorageService fileStorageService;

    public UploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /** 上传上架现场照片 */
    @PostMapping("/photo")
    public Map<String, String> uploadPhoto(@RequestParam("file") MultipartFile file) {
        return Map.of("photoPath", fileStorageService.store(file));
    }
}
