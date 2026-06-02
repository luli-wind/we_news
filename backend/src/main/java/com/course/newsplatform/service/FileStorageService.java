package com.course.newsplatform.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(MultipartFile file);

    /** Download an external image and store it locally. Returns the local URL path (e.g. /uploads/xxx.jpg), or null on failure. */
    String downloadImage(String externalUrl);
}
