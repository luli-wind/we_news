package com.course.newsplatform.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client for Juhe News API (https://www.juhe.cn/docs/api/id/235).
 * Free tier: 50 requests/day.
 */
@Slf4j
@Component
public class JuheNewsClient {

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public JuheNewsClient(
            @Value("${juhe.api.key}") String apiKey,
            @Value("${juhe.api.base-url}") String baseUrl,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    /**
     * Fetch news list from Juhe.
     * @param type news category: top, guonei, guoji, yule, tiyu, junshi, keji, caijing, youxi, qiche, jiankang
     * @param page page number (1-based)
     * @param pageSize items per page (max 30)
     */
    public JuheNewsListResponse getNewsList(String type, int page, int pageSize) {
        try {
            String url = baseUrl + "/index?key=" + apiKey
                    + "&type=" + encode(type)
                    + "&page=" + page
                    + "&page_size=" + Math.min(pageSize, 30)
                    + "&is_filter=1";
            String json = get(url);
            return objectMapper.readValue(json, JuheNewsListResponse.class);
        } catch (Exception e) {
            log.warn("Juhe news list failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch full news content by unique key.
     */
    public JuheNewsContentResponse getNewsContent(String uniqueKey) {
        try {
            String url = baseUrl + "/content?key=" + apiKey + "&uniquekey=" + encode(uniqueKey);
            String json = get(url);
            return objectMapper.readValue(json, JuheNewsContentResponse.class);
        } catch (Exception e) {
            log.warn("Juhe news content failed for {}: {}", uniqueKey, e.getMessage());
            return null;
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && !apiKey.equals("YOUR_JUHE_API_KEY");
    }

    private String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "Mozilla/5.0 (compatible; WeNewsBot/1.0)")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    // --- DTOs ---

    @Data
    public static class JuheNewsListResponse {
        @JsonProperty("error_code")
        private int errorCode;
        private String reason;
        private JuheNewsListResult result;

        public boolean isSuccess() { return errorCode == 0 && result != null && result.getData() != null; }
    }

    @Data
    public static class JuheNewsListResult {
        private String stat;
        private List<JuheNewsItem> data;
    }

    @Data
    public static class JuheNewsItem {
        private String uniquekey;
        private String title;
        private String date;
        private String category;
        @JsonProperty("author_name")
        private String authorName;
        private String url;
        @JsonProperty("thumbnail_pic_s")
        private String thumbnailPicS;
        @JsonProperty("is_content")
        private String isContent;

        public boolean hasContent() { return "1".equals(isContent); }
    }

    @Data
    public static class JuheNewsContentResponse {
        @JsonProperty("error_code")
        private int errorCode;
        private String reason;
        private JuheNewsContentResult result;

        public boolean isSuccess() { return errorCode == 0 && result != null; }
    }

    @Data
    public static class JuheNewsContentResult {
        private String uniquekey;
        private String content;
        private JuheNewsDetail detail;
        private List<Map<String, String>> pics;
    }

    @Data
    public static class JuheNewsDetail {
        private String title;
        private String date;
        private String category;
        @JsonProperty("author_name")
        private String authorName;
        private String url;
        @JsonProperty("thumbnail_pic_s")
        private String thumbnailPicS;
    }
}
