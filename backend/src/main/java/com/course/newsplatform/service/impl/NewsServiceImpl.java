package com.course.newsplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.course.newsplatform.common.BizException;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.common.PageUtils;
import com.course.newsplatform.common.SecurityUtils;
import com.course.newsplatform.dto.NewsQueryRequest;
import com.course.newsplatform.dto.NewsSaveRequest;
import com.course.newsplatform.dto.NewsSyncFeedDetail;
import com.course.newsplatform.dto.NewsSyncRequest;
import com.course.newsplatform.dto.NewsSyncResult;
import com.course.newsplatform.entity.News;
import com.course.newsplatform.entity.NewsMedia;
import com.course.newsplatform.enums.ContentStatus;
import com.course.newsplatform.mapper.NewsMapper;
import com.course.newsplatform.mapper.NewsMediaMapper;
import com.course.newsplatform.service.LogService;
import com.course.newsplatform.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private static final int DEFAULT_LIMIT_PER_FEED = 20;
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("(?i)<img[^>]*\\s+src=[\"']([^\"']+)[\"']");
    private static final List<DateTimeFormatter> DATETIME_FORMATTERS = List.of(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("EEE,dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_DATE_TIME
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private volatile Boolean schemaSupportsSourceFields;

    private final NewsMapper newsMapper;
    private final NewsMediaMapper newsMediaMapper;
    private final LogService logService;

    @Override
    public PageResponse<News> page(NewsQueryRequest request, boolean includeAllStatus) {
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(News::getTitle, request.getKeyword()).or().like(News::getSummary, request.getKeyword()));
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            wrapper.eq(News::getCategory, request.getCategory());
        } else if (request.getExcludeCategory() != null && !request.getExcludeCategory().isBlank()) {
            wrapper.ne(News::getCategory, request.getExcludeCategory());
        }
        if (includeAllStatus) {
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                wrapper.eq(News::getStatus, request.getStatus());
            }
        } else {
            wrapper.eq(News::getStatus, ContentStatus.PUBLISHED.name());
        }
        wrapper.orderByDesc(News::getPublishedAt).orderByDesc(News::getCreatedAt);

        Page<News> page = newsMapper.selectPage(new Page<>(request.getPage(), request.getPageSize()), wrapper);
        return PageUtils.toPageResponse(page);
    }

    @Override
    public News detail(Long id, boolean includeUnpublished) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BizException("news not found");
        }
        if (!includeUnpublished && !ContentStatus.PUBLISHED.name().equals(news.getStatus())) {
            throw new BizException("news is not published");
        }
        List<NewsMedia> media = newsMediaMapper.selectList(
                new LambdaQueryWrapper<NewsMedia>()
                        .eq(NewsMedia::getNewsId, id)
                        .orderByAsc(NewsMedia::getSortOrder));
        news.setMedia(media);
        return news;
    }

    @Override
    public Long create(NewsSaveRequest request) {
        News news = new News();
        news.setTitle(request.getTitle());
        news.setSummary(request.getSummary());
        news.setContent(request.getContent());
        news.setCategory(request.getCategory());
        news.setCoverUrl(request.getCoverUrl());
        news.setStatus(normalizeStatus(request.getStatus()));
        news.setAuthorId(SecurityUtils.currentUserId());
        if (ContentStatus.PUBLISHED.name().equals(news.getStatus())) {
            news.setPublishedAt(LocalDateTime.now());
        }
        newsMapper.insert(news);
        logService.operation("news", "create", "create news: " + news.getTitle());
        return news.getId();
    }

    @Override
    public void update(Long id, NewsSaveRequest request) {
        News news = detail(id, true);
        news.setTitle(request.getTitle());
        news.setSummary(request.getSummary());
        news.setContent(request.getContent());
        news.setCategory(request.getCategory());
        news.setCoverUrl(request.getCoverUrl());
        String status = normalizeStatus(request.getStatus());
        news.setStatus(status);
        if (ContentStatus.PUBLISHED.name().equals(status) && news.getPublishedAt() == null) {
            news.setPublishedAt(LocalDateTime.now());
        }
        newsMapper.updateById(news);
        logService.operation("news", "update", "update news: " + id);
    }

    @Override
    public void delete(Long id) {
        if (newsMapper.deleteById(id) == 0) {
            throw new BizException("news not found");
        }
        logService.operation("news", "delete", "delete news: " + id);
    }

    @Override
    public long count() {
        return newsMapper.selectCount(new LambdaQueryWrapper<>());
    }

    @Override
    public NewsSyncResult syncDomesticNews(NewsSyncRequest request) {
        int limitPerFeed = request != null && request.getLimitPerFeed() != null
                ? request.getLimitPerFeed()
                : DEFAULT_LIMIT_PER_FEED;
        List<DomesticFeed> feeds = resolveFeeds(request != null ? request.getSources() : null);

        NewsSyncResult result = new NewsSyncResult();
        for (DomesticFeed feed : feeds) {
            NewsSyncFeedDetail detail = syncSingleFeed(feed, limitPerFeed);
            result.getDetails().add(detail);
            result.setImported(result.getImported() + detail.getImported());
            result.setSkipped(result.getSkipped() + detail.getSkipped());
            result.setFailed(result.getFailed() + detail.getFailed());
        }

        logService.operation(
                "news",
                "sync-domestic",
                "sync completed imported=%d skipped=%d failed=%d".formatted(
                        result.getImported(), result.getSkipped(), result.getFailed())
        );

        return result;
    }

    protected List<DomesticFeed> allDomesticFeeds() {
        return List.of(
                new DomesticFeed("chinanews_top", "中国新闻网", "要闻", "http://www.chinanews.com.cn/rss/importnews.xml"),
                new DomesticFeed("people_society", "人民网", "社会", "http://www.people.com.cn/rss/society.xml"),
                new DomesticFeed("people_finance", "人民网", "财经", "http://www.people.com.cn/rss/finance.xml"),
                new DomesticFeed("xinhuanet_politics", "新华网", "时政", "http://www.xinhuanet.com/politics/news_politics.xml"),
                new DomesticFeed("xinhuanet_world", "新华网", "国际", "http://www.xinhuanet.com/world/news_world.xml")
        );
    }

    protected byte[] fetchFeedContent(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "Mozilla/5.0 (compatible; WeNewsBot/1.0)")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BizException("fetch feed failed, status=" + response.statusCode() + ", url=" + url);
        }
        return response.body();
    }

    private List<DomesticFeed> resolveFeeds(List<String> sourceKeys) {
        List<DomesticFeed> allFeeds = allDomesticFeeds();
        if (sourceKeys == null || sourceKeys.isEmpty()) {
            return allFeeds;
        }

        Map<String, DomesticFeed> feedMap = allFeeds.stream()
                .collect(Collectors.toMap(DomesticFeed::key, value -> value, (left, right) -> left, LinkedHashMap::new));

        List<DomesticFeed> selected = new ArrayList<>();
        for (String sourceKey : sourceKeys) {
            if (sourceKey == null || sourceKey.isBlank()) {
                continue;
            }
            DomesticFeed feed = feedMap.get(sourceKey.trim());
            if (feed == null) {
                throw new BizException("unsupported source key: " + sourceKey);
            }
            selected.add(feed);
        }

        if (selected.isEmpty()) {
            throw new BizException("no valid source key provided");
        }

        return selected;
    }

    private NewsSyncFeedDetail syncSingleFeed(DomesticFeed feed, int limitPerFeed) {
        NewsSyncFeedDetail detail = new NewsSyncFeedDetail();
        detail.setSource(feed.sourceName());
        detail.setSourceKey(feed.key());
        detail.setFeedUrl(feed.url());
        String firstError = null;

        try {
            byte[] xml = fetchFeedContent(feed.url());
            List<RssItem> items = parseRssItems(xml);
            int max = Math.min(limitPerFeed, items.size());
            for (int i = 0; i < max; i++) {
                try {
                    SyncOutcome outcome = importItem(feed, items.get(i));
                    if (outcome == SyncOutcome.IMPORTED) {
                        detail.setImported(detail.getImported() + 1);
                    } else {
                        detail.setSkipped(detail.getSkipped() + 1);
                    }
                } catch (Exception ex) {
                    detail.setFailed(detail.getFailed() + 1);
                    if (firstError == null) {
                        firstError = ex.getMessage();
                    }
                }
            }
            if (detail.getFailed() > 0 && firstError != null && !firstError.isBlank()) {
                detail.setMessage("partial_failed: " + firstError);
            } else {
                detail.setMessage("ok");
            }
            return detail;
        } catch (Exception ex) {
            detail.setFailed(1);
            detail.setMessage(ex.getMessage());
            return detail;
        }
    }

    private SyncOutcome importItem(DomesticFeed feed, RssItem item) {
        String title = cleanText(item.title());
        if (title.isBlank()) {
            throw new BizException("rss item title is empty");
        }

        String summary = truncate(cleanText(item.description()), 500);
        LocalDateTime publishedAt = parseDate(item.pubDate());
        String sourceUrl = blankToNull(item.link());
        String cleanedCategory = cleanText(item.category());
        String category = !cleanedCategory.isBlank() ? cleanedCategory : feed.defaultCategory();

        String originKey = sourceUrl != null ? sourceUrl : title + "|" + publishedAt;
        String originHash = sha256(originKey);
        boolean supportsSourceFields = schemaSupportsSourceFields();

        Long exists;
        if (supportsSourceFields) {
            exists = newsMapper.selectCount(new LambdaQueryWrapper<News>().eq(News::getOriginHash, originHash));
        } else {
            exists = newsMapper.selectCount(new LambdaQueryWrapper<News>()
                    .eq(News::getTitle, title)
                    .eq(News::getPublishedAt, publishedAt));
        }
        if (exists != null && exists > 0) {
            return SyncOutcome.SKIPPED;
        }

        String coverUrl = blankToNull(item.coverUrl());
        List<String> imageUrls = extractImageUrls(item.description());
        if (coverUrl == null && !imageUrls.isEmpty()) {
            coverUrl = imageUrls.get(0);
        }

        News news = new News();
        news.setTitle(title);
        news.setSummary(summary);
        news.setContent(buildContent(summary, sourceUrl, title));
        news.setCategory(category);
        news.setCoverUrl(coverUrl);
        if (supportsSourceFields) {
            news.setSourceName(feed.sourceName());
            news.setSourceUrl(sourceUrl);
            news.setOriginHash(originHash);
        }
        news.setStatus(ContentStatus.PUBLISHED.name());
        news.setPublishedAt(publishedAt);

        newsMapper.insert(news);

        if (!imageUrls.isEmpty()) {
            for (int i = 0; i < imageUrls.size(); i++) {
                NewsMedia media = new NewsMedia();
                media.setNewsId(news.getId());
                media.setMediaType("IMAGE");
                media.setUrl(imageUrls.get(i));
                media.setSortOrder(i);
                newsMediaMapper.insert(media);
            }
        }

        return SyncOutcome.IMPORTED;
    }

    private List<RssItem> parseRssItems(byte[] xmlBytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        safeSetFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        safeSetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        safeSetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        safeSetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        safeSetFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);
        factory.setNamespaceAware(false);
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception ignore) {
            // ignore
        }

        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));
        NodeList items = document.getElementsByTagName("item");

        List<RssItem> result = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            Element element = (Element) items.item(i);
            result.add(new RssItem(
                    nodeText(element, "title"),
                    nodeText(element, "description"),
                    nodeText(element, "link"),
                    nodeText(element, "pubDate"),
                    nodeText(element, "category"),
                    extractCoverUrl(element)
            ));
        }

        result.sort(Comparator.comparing((RssItem item) -> parseDate(item.pubDate())).reversed());
        return result;
    }

    private void safeSetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignore) {
            // ignore
        }
    }

    private String nodeText(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }
        return Objects.toString(nodes.item(0).getTextContent(), "").trim();
    }

    private List<String> extractImageUrls(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        Matcher matcher = IMG_SRC_PATTERN.matcher(html);
        while (matcher.find()) {
            String url = matcher.group(1).trim();
            if (!url.isBlank()) {
                urls.add(HtmlUtils.htmlUnescape(url));
            }
        }
        return urls;
    }

    private String extractCoverUrl(Element itemElement) {
        NodeList enclosures = itemElement.getElementsByTagName("enclosure");
        if (enclosures.getLength() > 0 && enclosures.item(0) instanceof Element enclosure) {
            String url = enclosure.getAttribute("url");
            if (url != null && !url.isBlank()) {
                return url.trim();
            }
        }

        NodeList mediaContentNodes = itemElement.getElementsByTagName("media:content");
        if (mediaContentNodes.getLength() > 0 && mediaContentNodes.item(0) instanceof Element mediaContent) {
            String url = mediaContent.getAttribute("url");
            if (url != null && !url.isBlank()) {
                return url.trim();
            }
        }

        String description = nodeText(itemElement, "description");
        Matcher matcher = IMG_SRC_PATTERN.matcher(description);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    private String buildContent(String summary, String sourceUrl, String fallbackTitle) {
        StringBuilder content = new StringBuilder();
        if (summary != null && !summary.isBlank()) {
            content.append(summary).append("\n\n");
        }
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            content.append("原文链接：").append(sourceUrl);
        }
        if (content.length() == 0) {
            content.append(fallbackTitle);
        }
        return content.toString();
    }

    private String cleanText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String unescaped = HtmlUtils.htmlUnescape(raw);
        String text = HTML_TAG_PATTERN.matcher(unescaped).replaceAll(" ");
        return text
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String truncate(String source, int maxLength) {
        if (source == null) {
            return null;
        }
        if (source.length() <= maxLength) {
            return source;
        }
        return source.substring(0, maxLength);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private LocalDateTime parseDate(String source) {
        if (source == null || source.isBlank()) {
            return LocalDateTime.now();
        }

        String input = source.trim();
        for (DateTimeFormatter formatter : DATETIME_FORMATTERS) {
            try {
                return ZonedDateTime.parse(input, formatter).toLocalDateTime();
            } catch (DateTimeParseException ignore) {
                // continue
            }

            try {
                return OffsetDateTime.parse(input, formatter).toLocalDateTime();
            } catch (DateTimeParseException ignore) {
                // continue
            }

            try {
                return LocalDateTime.parse(input, formatter);
            } catch (DateTimeParseException ignore) {
                // continue
            }
        }

        return LocalDateTime.now();
    }

    private String sha256(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new BizException("failed to build origin hash");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return ContentStatus.DRAFT.name();
        }
        try {
            return ContentStatus.valueOf(status).name();
        } catch (Exception ex) {
            throw new BizException("invalid status: " + status);
        }
    }

    protected record DomesticFeed(String key, String sourceName, String defaultCategory, String url) {
    }

    private boolean schemaSupportsSourceFields() {
        if (schemaSupportsSourceFields != null) {
            return schemaSupportsSourceFields;
        }
        try {
            newsMapper.selectCount(new LambdaQueryWrapper<News>()
                    .isNotNull(News::getOriginHash)
                    .last("LIMIT 1"));
            schemaSupportsSourceFields = true;
        } catch (Exception ex) {
            if (isUnknownColumnException(ex)) {
                schemaSupportsSourceFields = false;
            } else {
                throw ex;
            }
        }
        return schemaSupportsSourceFields;
    }

    private boolean isUnknownColumnException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("unknown column")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record RssItem(String title, String description, String link, String pubDate, String category,
                           String coverUrl) {
    }

    private enum SyncOutcome {
        IMPORTED,
        SKIPPED
    }
}
