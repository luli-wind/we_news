package com.course.newsplatform.service.impl;

import com.course.newsplatform.common.BizException;
import com.course.newsplatform.dto.NewsSyncRequest;
import com.course.newsplatform.dto.NewsSyncResult;
import com.course.newsplatform.entity.News;
import com.course.newsplatform.mapper.NewsMapper;
import com.course.newsplatform.service.LogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsServiceImplSyncTest {

    @Test
    void syncDomesticNews_shouldImportAndSkipDuplicate() {
        NewsMapper newsMapper = mock(NewsMapper.class);
        LogService logService = mock(LogService.class);
        when(newsMapper.selectCount(any())).thenReturn(0L, 1L);

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <item>
                      <title>Test A</title>
                      <description><![CDATA[<p>Hello <b>RSS</b></p>]]></description>
                      <link>https://example.com/a</link>
                      <pubDate>Thu, 16 Apr 2026 10:00:00 +0800</pubDate>
                      <category>社会</category>
                    </item>
                    <item>
                      <title>Test A Duplicate</title>
                      <description>dup</description>
                      <link>https://example.com/a</link>
                      <pubDate>Thu, 16 Apr 2026 09:00:00 +0800</pubDate>
                      <category>社会</category>
                    </item>
                  </channel>
                </rss>
                """;

        NewsServiceImpl service = new StubbedNewsService(newsMapper, logService, xml);
        NewsSyncResult result = service.syncDomesticNews(null);

        assertEquals(1, result.getImported());
        assertEquals(1, result.getSkipped());
        assertEquals(0, result.getFailed());
        assertEquals(1, result.getDetails().size());

        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);
        verify(newsMapper, times(1)).insert(captor.capture());
        News saved = captor.getValue();
        assertEquals("人民网", saved.getSourceName());
        assertEquals("https://example.com/a", saved.getSourceUrl());
        assertEquals("PUBLISHED", saved.getStatus());
        assertFalse(saved.getSummary().contains("<b>"));
        assertNotNull(saved.getPublishedAt());
    }

    @Test
    void syncDomesticNews_shouldFailOnUnsupportedSourceKey() {
        NewsMapper newsMapper = mock(NewsMapper.class);
        LogService logService = mock(LogService.class);
        NewsServiceImpl service = new StubbedNewsService(newsMapper, logService, "<rss version=\"2.0\"><channel/></rss>");

        NewsSyncRequest request = new NewsSyncRequest();
        request.setSources(List.of("unknown_source"));

        assertThrows(BizException.class, () -> service.syncDomesticNews(request));
    }

    private static class StubbedNewsService extends NewsServiceImpl {

        private final String xml;

        StubbedNewsService(NewsMapper newsMapper, LogService logService, String xml) {
            super(newsMapper, logService);
            this.xml = xml;
        }

        @Override
        protected List<DomesticFeed> allDomesticFeeds() {
            return List.of(new DomesticFeed("people_society", "人民网", "社会", "http://mock.local/rss.xml"));
        }

        @Override
        protected byte[] fetchFeedContent(String url) {
            return xml.getBytes(StandardCharsets.UTF_8);
        }
    }
}
