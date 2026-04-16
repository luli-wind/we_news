package com.course.newsplatform.service.impl;

import com.course.newsplatform.common.BizException;
import com.course.newsplatform.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "mp4", "mov", "avi");

    private final Path rootPath;

    public LocalFileStorageServiceImpl(@Value("${app.file.local-dir:uploads}") String localDir) throws IOException {
        this.rootPath = Paths.get(localDir).toAbsolutePath().normalize();
        Files.createDirectories(rootPath);
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new BizException("文件类型不合法");
        }
        String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("仅支持图片或视频文件上传");
        }

        String targetName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = rootPath.resolve(targetName).normalize();
        try {
            file.transferTo(target);
            return "/uploads/" + targetName;
        } catch (IOException ex) {
            throw new BizException("文件保存失败: " + ex.getMessage());
        }
    }
}
