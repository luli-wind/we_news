package com.course.newsplatform.service.impl;

import com.course.newsplatform.common.BizException;
import com.course.newsplatform.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "mp4", "mov", "avi");

    private final Path rootPath;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

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

    @Override
    public String downloadImage(String externalUrl) {
        if (externalUrl == null || externalUrl.isBlank()) return null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(externalUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Mozilla/5.0 (compatible; WeNewsBot/1.0)")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return null;

            String ext = inferImageExt(externalUrl, response);
            String targetName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = rootPath.resolve(targetName).normalize();
            Files.copy(response.body(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + targetName;
        } catch (Exception e) {
            return null;
        }
    }

    private String inferImageExt(String url, HttpResponse<?> response) {
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (contentType.contains("png")) return "png";
        if (contentType.contains("gif")) return "gif";
        if (contentType.contains("webp")) return "webp";
        if (contentType.contains("jpeg") || contentType.contains("jpg")) return "jpg";
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".png")) return "png";
        if (lower.contains(".gif")) return "gif";
        if (lower.contains(".webp")) return "webp";
        return "jpg";
    }
}
