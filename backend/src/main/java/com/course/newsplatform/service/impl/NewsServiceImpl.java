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
import com.course.newsplatform.entity.User;
import com.course.newsplatform.enums.ContentStatus;
import com.course.newsplatform.mapper.NewsMapper;
import com.course.newsplatform.mapper.NewsMediaMapper;
import com.course.newsplatform.mapper.UserMapper;
import com.course.newsplatform.client.JuheNewsClient;
import com.course.newsplatform.service.FileStorageService;
import com.course.newsplatform.service.LogService;
import com.course.newsplatform.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private static final int DEFAULT_LIMIT_PER_FEED = 30;
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
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;
    private final JuheNewsClient juheNewsClient;
    private final LogService logService;

    @Override
    public PageResponse<News> page(NewsQueryRequest request, boolean includeAllStatus) {
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            wrapper.like(News::getTitle, request.getKeyword());
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
        populateAuthorNames(page.getRecords());
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
        if (news.getAuthorId() != null) {
            User author = userMapper.selectById(news.getAuthorId());
            if (author != null) {
                news.setAuthorName(author.getNickname());
            }
        }
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
    public Map<String, Object> repairImages() {
        List<News> all = newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, ContentStatus.PUBLISHED.name()));
        int fixed = 0;
        int skipped = 0;
        for (News news : all) {
            String cover = news.getCoverUrl();
            if (cover != null && !cover.isBlank() && !cover.startsWith("/uploads/")) {
                String resolved = cover;
                if (cover.startsWith("/")) {
                    resolved = tryResolveRelative(cover, news);
                }
                String local = downloadCoverImage(resolved);
                if (local != null) {
                    news.setCoverUrl(local);
                    newsMapper.updateById(news);
                    fixed++;
                } else {
                    skipped++;
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("fixed", fixed);
        result.put("skipped", skipped);
        logService.operation("news", "repair_images", "fixed=" + fixed + " skipped=" + skipped);
        return result;
    }

    private static final int JUHE_DAILY_LIMIT = 50;

    @Override
    public NewsSyncResult syncJuheNews() {
        if (!juheNewsClient.isConfigured()) {
            throw new BizException("聚合数据 API Key 未配置");
        }

        String[] categories = {"top", "guonei", "guoji", "keji", "caijing"};
        NewsSyncResult result = new NewsSyncResult();
        int imported = 0, skipped = 0, failed = 0, apiCalls = 0;

        // Phase 1: collect all new items from all categories (1 call per category for list)
        record Candidate(JuheNewsClient.JuheNewsItem item, String cat) {}
        List<Candidate> candidates = new ArrayList<>();
        for (String cat : categories) {
            if (apiCalls >= JUHE_DAILY_LIMIT) break;
            try {
                JuheNewsClient.JuheNewsListResponse listResp = juheNewsClient.getNewsList(cat, 1, 20);
                apiCalls++;
                if (listResp == null || !listResp.isSuccess()) { failed++; continue; }
                for (JuheNewsClient.JuheNewsItem item : listResp.getResult().getData()) {
                    String hash = sha256(item.getUniquekey());
                    Long exists = newsMapper.selectCount(new LambdaQueryWrapper<News>().eq(News::getOriginHash, hash));
                    if (exists != null && exists > 0) { skipped++; continue; }
                    candidates.add(new Candidate(item, cat));
                }
            } catch (Exception e) { failed++; }
        }

        // Phase 2: fetch full content for candidates until we run out of budget
        for (Candidate c : candidates) {
            if (apiCalls >= JUHE_DAILY_LIMIT) break;
            try {
                JuheNewsClient.JuheNewsItem item = c.item();
                String content = null;
                if (item.hasContent()) {
                    JuheNewsClient.JuheNewsContentResponse contentResp = juheNewsClient.getNewsContent(item.getUniquekey());
                    apiCalls++;
                    if (contentResp != null && contentResp.isSuccess() && contentResp.getResult().getContent() != null) {
                        content = cleanHtmlContent(contentResp.getResult().getContent());
                    }
                }
                // NO fallback — if we can't get full content, skip this article entirely
                if (content == null || content.isBlank()) continue;

                String coverUrl = null;
                if (item.getThumbnailPicS() != null && !item.getThumbnailPicS().isBlank()) {
                    String[] pics = item.getThumbnailPicS().split(",");
                    coverUrl = downloadCoverImage(pics[0].trim());
                    if (coverUrl == null) coverUrl = pics[0].trim();
                }

                News news = new News();
                news.setTitle(item.getTitle());
                news.setSummary(truncate(HtmlUtils.htmlUnescape(content.replaceAll("\\s+", " ")), 300));
                news.setContent(content);
                news.setCategory(catToChinese(c.cat()));
                news.setCoverUrl(coverUrl);
                news.setSourceName(item.getAuthorName());
                news.setSourceUrl(item.getUrl());
                news.setOriginHash(sha256(item.getUniquekey()));
                news.setStatus(ContentStatus.PUBLISHED.name());
                news.setPublishedAt(LocalDateTime.now());
                newsMapper.insert(news);
                imported++;
            } catch (Exception ex) { failed++; }
        }

        result.setImported(imported);
        result.setSkipped(skipped);
        result.setFailed(failed);
        logService.operation("news", "sync-juhe",
                "imported=" + imported + " skipped=" + skipped + " failed=" + failed + " apiCalls=" + apiCalls);
        return result;
    }

    private String cleanHtmlContent(String html) {
        if (html == null || html.isBlank()) return "";
        try {
            org.jsoup.nodes.Document doc = Jsoup.parse(html);
            doc.select("script, style, a").remove();
            return cleanArticleBody(doc.body());
        } catch (Exception e) {
            return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        }
    }

    private String catToChinese(String cat) {
        return switch (cat) {
            case "top" -> "推荐"; case "guonei" -> "国内"; case "guoji" -> "国际";
            case "keji" -> "科技"; case "caijing" -> "财经"; case "tiyu" -> "体育";
            case "yule" -> "娱乐"; case "junshi" -> "军事"; case "jiankang" -> "健康";
            case "qiche" -> "汽车"; case "youxi" -> "游戏";
            default -> cat;
        };
    }

    @Override
    public Map<String, Object> enrichContent() {
        List<News> all = newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, ContentStatus.PUBLISHED.name()));
        int enriched = 0;
        int skipped = 0;
        for (News news : all) {
            // Skip news that already have long content (already enriched or scraped)
            if (news.getContent() != null && news.getContent().length() > 600) {
                skipped++;
                continue;
            }
            ScrapeResult scraped = scrapeArticle(news.getSourceUrl());
            if (scraped != null && scraped.text().length() > 150) {
                news.setContent(scraped.text());
                // Also try to get cover from article if missing
                if ((news.getCoverUrl() == null || news.getCoverUrl().isBlank())
                        && scraped.mainImageUrl() != null) {
                    String local = downloadCoverImage(scraped.mainImageUrl());
                    if (local != null) {
                        news.setCoverUrl(local);
                    }
                }
                newsMapper.updateById(news);
                enriched++;
            } else {
                skipped++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("enriched", enriched);
        result.put("skipped", skipped);
        logService.operation("news", "enrich_content", "enriched=" + enriched + " skipped=" + skipped);
        return result;
    }

    private String tryResolveRelative(String path, News news) {
        // If we have source_url, extract domain from it
        if (news.getSourceUrl() != null && !news.getSourceUrl().isBlank()) {
            try {
                URI uri = URI.create(news.getSourceUrl());
                return uri.getScheme() + "://" + uri.getHost() + path;
            } catch (Exception e) { /* fall through */ }
        }
        // Try known RSS source domains
        for (DomesticFeed feed : allDomesticFeeds()) {
            try {
                URI uri = URI.create(feed.url());
                return uri.getScheme() + "://" + uri.getHost() + path;
            } catch (Exception e) { /* try next */ }
        }
        return path;
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
                // 中国新闻网 (chinanews.com) — 稳定
                new DomesticFeed("chinanews_top", "中国新闻网", "要闻", "http://www.chinanews.com.cn/rss/importnews.xml"),
                new DomesticFeed("chinanews_society", "中国新闻网", "社会", "http://www.chinanews.com.cn/rss/society.xml"),
                new DomesticFeed("chinanews_finance", "中国新闻网", "财经", "http://www.chinanews.com.cn/rss/finance.xml"),
                new DomesticFeed("chinanews_sports", "中国新闻网", "体育", "http://www.chinanews.com.cn/rss/sports.xml"),
                new DomesticFeed("chinanews_culture", "中国新闻网", "文化", "http://www.chinanews.com.cn/rss/culture.xml"),
                new DomesticFeed("chinanews_world", "中国新闻网", "国际", "http://www.chinanews.com.cn/rss/world.xml"),
                new DomesticFeed("chinanews_tech", "中国新闻网", "科技", "http://www.chinanews.com.cn/rss/tech.xml"),
                // 人民网 (people.com.cn) — 稳定
                new DomesticFeed("people_society", "人民网", "社会", "http://www.people.com.cn/rss/society.xml"),
                new DomesticFeed("people_finance", "人民网", "财经", "http://www.people.com.cn/rss/finance.xml"),
                new DomesticFeed("people_world", "人民网", "国际", "http://www.people.com.cn/rss/world.xml"),
                new DomesticFeed("people_culture", "人民网", "文化", "http://www.people.com.cn/rss/culture.xml"),
                new DomesticFeed("people_tech", "人民网", "科技", "http://www.people.com.cn/rss/scitech.xml"),
                // 新华网 (xinhuanet.com) — 核心频道
                new DomesticFeed("xinhuanet_politics", "新华网", "时政", "http://www.xinhuanet.com/politics/news_politics.xml"),
                new DomesticFeed("xinhuanet_world", "新华网", "国际", "http://www.xinhuanet.com/world/news_world.xml"),
                new DomesticFeed("xinhuanet_finance", "新华网", "财经", "http://www.xinhuanet.com/fortune/news_fortune.xml"),
                new DomesticFeed("xinhuanet_tech", "新华网", "科技", "http://www.xinhuanet.com/tech/news_tech.xml"),
                new DomesticFeed("xinhuanet_culture", "新华网", "文化", "http://www.xinhuanet.com/culture/news_culture.xml")
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

        String summary = truncate(cleanText(item.description()), 300);
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

        String baseUrl = sourceUrl != null ? sourceUrl : feed.url();
        String coverUrl = resolveRelativeUrl(blankToNull(item.coverUrl()), baseUrl);
        List<String> imageUrls = extractImageUrls(item.description(), baseUrl);
        if (coverUrl == null && !imageUrls.isEmpty()) {
            coverUrl = imageUrls.get(0);
        }

        String localCoverUrl = downloadCoverImage(coverUrl);

        News news = new News();
        news.setTitle(title);
        news.setSummary(summary);
        news.setContent(buildContent(item.description(), sourceUrl, title));
        news.setCategory(category);
        news.setCoverUrl(localCoverUrl != null ? localCoverUrl : coverUrl);
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
                String localUrl = downloadCoverImage(imageUrls.get(i));
                NewsMedia media = new NewsMedia();
                media.setNewsId(news.getId());
                media.setMediaType("IMAGE");
                media.setUrl(localUrl != null ? localUrl : imageUrls.get(i));
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
            String link = nodeText(element, "link");
            result.add(new RssItem(
                    nodeText(element, "title"),
                    nodeText(element, "description"),
                    link,
                    nodeText(element, "pubDate"),
                    nodeText(element, "category"),
                    extractCoverUrl(element, link)
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

    private String downloadCoverImage(String externalUrl) {
        if (externalUrl == null || externalUrl.isBlank()) return null;
        if (externalUrl.startsWith("/uploads/")) return externalUrl;
        try {
            return fileStorageService.downloadImage(externalUrl);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveRelativeUrl(String url, String baseUrl) {
        if (url == null || url.isBlank()) return null;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        if (url.startsWith("//")) return "https:" + url;
        if (url.startsWith("/") && baseUrl != null) {
            try {
                URI base = URI.create(baseUrl);
                return base.getScheme() + "://" + base.getHost() + url;
            } catch (Exception e) {
                return url;
            }
        }
        return url;
    }

    private List<String> extractImageUrls(String html, String baseUrl) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        Matcher matcher = IMG_SRC_PATTERN.matcher(html);
        while (matcher.find()) {
            String url = matcher.group(1).trim();
            if (!url.isBlank()) {
                urls.add(resolveRelativeUrl(HtmlUtils.htmlUnescape(url), baseUrl));
            }
        }
        return urls;
    }

    private String extractCoverUrl(Element itemElement, String linkUrl) {
        NodeList enclosures = itemElement.getElementsByTagName("enclosure");
        if (enclosures.getLength() > 0 && enclosures.item(0) instanceof Element enclosure) {
            String url = enclosure.getAttribute("url");
            if (url != null && !url.isBlank()) {
                return resolveRelativeUrl(url.trim(), linkUrl);
            }
        }

        NodeList mediaContentNodes = itemElement.getElementsByTagName("media:content");
        if (mediaContentNodes.getLength() > 0 && mediaContentNodes.item(0) instanceof Element mediaContent) {
            String url = mediaContent.getAttribute("url");
            if (url != null && !url.isBlank()) {
                return resolveRelativeUrl(url.trim(), linkUrl);
            }
        }

        String description = nodeText(itemElement, "description");
        Matcher matcher = IMG_SRC_PATTERN.matcher(description);
        if (matcher.find()) {
            return resolveRelativeUrl(matcher.group(1).trim(), linkUrl);
        }

        return null;
    }

    /** Build rich content from RSS description (no article scraping during sync). */
    private String buildContent(String rssDescription, String sourceUrl, String fallbackTitle) {
        // Use Jsoup to clean RSS description into well-formatted paragraphs
        String desc = cleanToParagraphs(rssDescription);
        if (desc != null && !desc.isBlank()) {
            StringBuilder sb = new StringBuilder(desc);
            if (sourceUrl != null && !sourceUrl.isBlank()) {
                sb.append("\n\n原文链接：").append(sourceUrl);
            }
            return sb.toString();
        }
        return fallbackTitle;
    }

    /** Article scraping result: text content + optional main image URL. */
    private record ScrapeResult(String text, String mainImageUrl) {}

    /** Try to fetch and extract the main article content and image from a URL. */
    private ScrapeResult scrapeArticle(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            org.jsoup.nodes.Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; WeNewsBot/1.0)")
                    .timeout(8000)
                    .get();

            // Remove known noise first
            String[] noiseSelectors = {
                "script", "style", "nav", "iframe", "noscript",
                ".nav", ".header", ".footer", ".sidebar", ".menu",
                ".ad", ".advertisement", ".adv", ".banner",
                ".related", ".recommend", ".hot-news", ".hotnews",
                ".share", ".shared", ".social",
                ".breadcrumb", ".breadcrumbs", ".crumbs",
                ".tags", ".tag", ".keywords",
                ".copyright", ".copyright-info", ".disclaimer",
                ".editor", ".editor-info", ".reporter",
                ".comment", ".comments", ".comment-list",
                ".page-nav", ".pagination",
                ".toolbar", ".tools",
                "[class*=side]", "[class*=footer]", "[class*=header]"
            };
            for (String sel : noiseSelectors) {
                doc.select(sel).remove();
            }

            // Try specific article content selectors
            String[] contentSelectors = {
                "#artibody", ".TRS_Editor", ".Custom_UnionStyle",
                "article", "[class*=article-body]", "[class*=article-content]",
                ".article-content", ".article-body", ".article",
                "#article", ".post_body", ".entry-content",
                "#content", ".content", ".main-content",
                ".news-content", ".text-content", ".text"
            };

            org.jsoup.nodes.Element articleBody = null;
            for (String sel : contentSelectors) {
                Elements elements = doc.select(sel);
                if (!elements.isEmpty()) {
                    // Pick the one with most paragraph text (exclude short sidebars)
                    int bestScore = 0;
                    for (org.jsoup.nodes.Element el : elements) {
                        int pCount = el.select("p").size();
                        int textLen = el.text().length();
                        if (pCount >= 2 && textLen > bestScore) {
                            bestScore = textLen;
                            articleBody = el;
                        }
                    }
                    if (articleBody != null && articleBody.text().length() > 100) break;
                }
            }

            // Fallback: collect meaningful paragraphs from body
            if (articleBody == null) {
                Elements allP = doc.select("p");
                if (allP.isEmpty()) return null;
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (org.jsoup.nodes.Element p : allP) {
                    org.jsoup.nodes.Element parent = p.parent();
                    if (parent != null) {
                        String parentTag = parent.tagName().toLowerCase();
                        String parentClass = parent.className().toLowerCase();
                        if (parentClass.contains("nav") || parentClass.contains("side")
                                || parentClass.contains("foot") || parentClass.contains("head")
                                || parentClass.contains("ad") || parentClass.contains("menu")) {
                            continue;
                        }
                    }
                    String text = p.text().trim();
                    if (text.length() > 15) {
                        if (sb.length() > 0) sb.append("\n\n");
                        sb.append(text);
                        count++;
                    }
                }
                if (count >= 2 && sb.length() > 150) {
                    return new ScrapeResult(sb.toString(), null);
                }
                return null;
            }

            // Extract main image from article body (for cover)
            String mainImage = null;
            Elements imgs = articleBody.select("img");
            for (org.jsoup.nodes.Element img : imgs) {
                String src = img.absUrl("src");
                if (src == null || src.isBlank()) continue;
                // Skip tiny icons, tracking pixels, etc.
                String width = img.attr("width");
                String height = img.attr("height");
                if (!width.isBlank() && !height.isBlank()) {
                    try {
                        if (Integer.parseInt(width) < 100 || Integer.parseInt(height) < 100) continue;
                    } catch (NumberFormatException e) { /* keep */ }
                }
                // Skip obvious non-content images
                String classes = img.className().toLowerCase();
                String alt = img.attr("alt").toLowerCase();
                if (classes.contains("icon") || classes.contains("logo")
                        || classes.contains("avatar") || classes.contains("qr")
                        || alt.contains("二维码") || alt.contains("logo")) continue;
                mainImage = src;
                break;
            }

            // Clean the article body into paragraphs
            String text = cleanArticleBody(articleBody);
            if (text == null || text.length() < 80) return null;
            return new ScrapeResult(text, mainImage);

        } catch (Exception e) {
            return null;
        }
    }

    /** Clean article body element to pure paragraph text. */
    private String cleanArticleBody(org.jsoup.nodes.Element body) {
        // Remove any remaining noise elements inside the article
        body.select("script, style, iframe, noscript, nav, form").remove();
        body.select("[class*=ad], [class*=share], [class*=comment], " +
                "[class*=related], [class*=recommend], [class*=editor], " +
                "[class*=copyright], [class*=breadcrumb], [class*=tag], " +
                "[class*=toolbar], [class*=video]").remove();
        // Remove empty elements and short text links
        body.select("a").forEach(a -> {
            if (a.text().trim().length() < 4 && a.select("img").isEmpty()) a.remove();
        });

        StringBuilder sb = new StringBuilder();
        Elements children = body.children();
        for (org.jsoup.nodes.Element child : children) {
            if (child.tagName().equals("img") || child.tagName().equals("figure")
                    || child.tagName().equals("video")) continue;

            String text = child.text().trim();
            if (text.isEmpty()) continue;

            // Filter known boilerplate patterns
            if (isBoilerplate(text)) continue;

            // Filter repeated/similar text (common in some news layouts)
            if (sb.length() > 0 && text.length() < 30 && sb.toString().contains(text)) continue;

            if (sb.length() > 0) sb.append("\n\n");
            sb.append(text);
        }

        return sb.length() > 80 ? sb.toString() : body.text().trim();
    }

    private boolean isBoilerplate(String text) {
        if (text.length() < 8) return true;
        return text.startsWith("原标题：") || text.startsWith("责任编辑")
                || text.startsWith("作者：") || text.startsWith("来源：")
                || text.startsWith("编辑：") || text.startsWith("记者")
                || text.startsWith("【责任编辑") || text.startsWith("（原标题")
                || text.contains("版权声明") || text.contains("转载")
                || text.contains("更多精彩内容") || text.contains("阅读原文")
                || text.contains("扫描二维码") || text.contains("关注微信")
                || text.equals("相关新闻") || text.equals("推荐阅读")
                || text.startsWith("【") && text.length() < 40;
    }

    /** Convert HTML description into clean paragraph text. */
    private String cleanToParagraphs(String html) {
        if (html == null || html.isBlank()) return "";
        try {
            org.jsoup.nodes.Document doc = Jsoup.parse(html);
            // Remove links, scripts, styles
            doc.select("a, script, style").remove();
            Elements paragraphs = doc.select("p, br, div");
            if (paragraphs.isEmpty()) {
                return cleanText(html);
            }
            StringBuilder sb = new StringBuilder();
            String text = doc.text();
            if (text != null && !text.isBlank()) {
                // Split by common delimiters to create paragraph structure
                String[] parts = text.split("(?<=[。！？])");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.length() > 4) {
                        if (sb.length() > 0) sb.append("\n\n");
                        sb.append(trimmed);
                    }
                }
            }
            return sb.length() > 0 ? sb.toString() : cleanText(html);
        } catch (Exception e) {
            return cleanText(html);
        }
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

    private void populateAuthorNames(List<News> newsList) {
        if (newsList == null || newsList.isEmpty()) return;
        Set<Long> authorIds = newsList.stream()
                .map(News::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (authorIds.isEmpty()) return;
        Map<Long, String> nameMap = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, authorIds))
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : "用户"));
        newsList.forEach(n -> {
            if (n.getAuthorId() != null && nameMap.containsKey(n.getAuthorId())) {
                n.setAuthorName(nameMap.get(n.getAuthorId()));
            }
        });
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
