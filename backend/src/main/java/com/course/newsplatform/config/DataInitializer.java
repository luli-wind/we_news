package com.course.newsplatform.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.course.newsplatform.entity.News;
import com.course.newsplatform.entity.Video;
import com.course.newsplatform.enums.ContentStatus;
import com.course.newsplatform.mapper.NewsMapper;
import com.course.newsplatform.mapper.VideoMapper;
import com.course.newsplatform.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final NewsService newsService;
    private final NewsMapper newsMapper;
    private final VideoMapper videoMapper;

    @Override
    public void run(ApplicationArguments args) {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                initNews();
                initVideos();
            } catch (Exception e) {
                log.warn("数据初始化异常: {}", e.getMessage());
            }
        }, "data-init").start();
    }

    private void initNews() {
        long count = newsMapper.selectCount(new LambdaQueryWrapper<>());
        if (count > 0) {
            log.info("新闻表已有 {} 条数据，跳过初始化", count);
            return;
        }
        log.info("新闻表为空，开始从RSS源同步...");
        try {
            var result = newsService.syncDomesticNews(null);
            log.info("RSS同步完成: 导入={} 跳过={} 失败={}",
                    result.getImported(), result.getSkipped(), result.getFailed());
        } catch (Exception e) {
            log.warn("RSS同步失败: {}, 使用备用数据", e.getMessage());
            insertFallbackNews();
        }

        long after = newsMapper.selectCount(new LambdaQueryWrapper<>());
        if (after == 0) {
            log.info("RSS未获取到数据，使用备用数据");
            insertFallbackNews();
        }
    }

    private void insertFallbackNews() {
        String[][] items = {
            {"人工智能如何改变我们的日常生活",
             "从智能助手到自动驾驶，AI技术正以前所未有的速度渗透到我们生活的方方面面。医疗领域，AI辅助诊断准确率已超过资深医生。教育领域，个性化学习系统根据学生习惯自动调整内容。交通领域，自动驾驶有望将事故率降低90%。",
             "科技", "科技日报"},
            {"2024年新能源汽车市场分析与展望",
             "新能源汽车市场持续高速增长。今年第一季度全球销量同比增长超35%，中国市场贡献超60%增量。固态电池技术取得重大突破，能量密度有望达到当前锂离子电池的两倍。充电基础设施加速推进，高速服务区充电覆盖率超90%。",
             "财经", "经济观察报"},
            {"城市绿化与生态环境改善的实践探索",
             "越来越多的城市开始重视生态环境建设。北京实施'留白增绿'战略，新增大量口袋公园。上海建设城市生态廊道，连接公园绿地形成完整生态系统。深圳在屋顶绿化和垂直绿化方面走在全国前列。",
             "社会", "新华网"},
            {"中国传统文化在新时代的传承与创新",
             "国潮文化持续升温。汉服从爱好发展成超百亿产业。故宫、敦煌等机构的文创产品屡屡成为爆款。国风音乐和中国风影视作品广受欢迎。传统文化正以全新面貌回归大众视野。",
             "文化", "人民日报"},
            {"健康饮食：科学搭配让生活更美好",
             "中国营养学会最新膳食指南建议：每天摄入食物种类应达12种以上，每周25种以上。控制油盐糖摄入，每人每天食盐不超过5克。适量摄入优质蛋白质，每周至少吃两次鱼。每天饮水1500-1700毫升。",
             "健康", "健康时报"},
            {"5G技术推动工业互联网加速发展",
             "5G商用进入第四年，工业领域应用加速落地。智能工厂中5G支撑大量传感器实时通信。远程操控方面，低时延使远程挖掘机、远程手术成为可能。5G+AI视觉检测系统可在毫秒级完成产品缺陷识别，准确率高达99.9%。",
             "科技", "工信微报"},
            {"全球气候变化最新研究报告深度解析",
             "IPCC最新报告指出全球平均气温较工业化前已上升约1.2°C。人类活动是导致全球变暖的主要原因。极端天气事件更加频繁和剧烈。报告强调仍有机会将升温控制在1.5°C以内，需要全球共同努力发展可再生能源。",
             "科学", "科学通报"},
            {"数字经济时代的就业变革与机遇",
             "数字经济的发展正在深刻改变就业结构。人工智能、大数据、云计算等领域人才需求旺盛。远程办公、灵活用工等新型就业形态快速发展。终身学习和技能更新成为职场人士的必备能力。",
             "财经", "经济日报"},
        };

        String[] covers = {
            "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&q=60",
            "https://images.unsplash.com/photo-1593941707882-a5bba14938c7?w=800&q=60",
            "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=800&q=60",
            "https://images.unsplash.com/photo-1547981609-4b6bfe67ca0b?w=800&q=60",
            "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=800&q=60",
            "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&q=60",
            "https://images.unsplash.com/photo-1611273426858-450d8e3c9fce?w=800&q=60",
            "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=800&q=60",
        };

        int inserted = 0;
        for (int i = 0; i < items.length; i++) {
            News news = new News();
            news.setTitle(items[i][0]);
            news.setSummary(items[i][1]);
            news.setContent(items[i][1] + "\n\n本文为你带来最新资讯与深度分析。欢迎在评论区分享你的看法。");
            news.setCategory(items[i][2]);
            news.setSourceName(items[i][3]);
            news.setCoverUrl(covers[i]);
            news.setStatus(ContentStatus.PUBLISHED.name());
            news.setPublishedAt(LocalDateTime.now().minusHours(i * 3L));
            newsMapper.insert(news);
            inserted++;
        }
        log.info("已插入 {} 条备用新闻数据", inserted);
    }

    private void initVideos() {
        long count = videoMapper.selectCount(new LambdaQueryWrapper<>());
        if (count > 0) {
            log.info("视频表已有 {} 条数据，跳过初始化", count);
            return;
        }
        log.info("视频表为空，初始化示例视频...");

        String[][] items = {
            {"探索深海奥秘：马里亚纳海沟科考纪实",
             "跟随科考队潜入地球最深处，探索那片神秘而未知的水下世界。",
             "科学"},
            {"五分钟看懂量子计算",
             "量子计算是什么？它将如何改变未来？这支短视频用通俗易懂的方式为你讲解。",
             "科技"},
            {"中国高铁：速度与创新的故事",
             "从追赶者到领跑者，中国高铁的发展史是中国制造崛起的一个缩影。",
             "纪实"},
            {"美食探店：藏在胡同里的老北京味道",
             "走进北京老胡同，探访那些传承数十年的老店，品味最地道的北京小吃。",
             "生活"},
            {"航拍中国：壮美山河从空中看",
             "用无人机镜头记录中国大地的壮丽景色，每一帧都是震撼的视觉盛宴。",
             "自然"},
        };

        String[] videoUrls = {
            "https://www.w3schools.com/html/mov_bbb.mp4",
            "https://www.w3schools.com/html/movie.mp4",
            "https://sample-videos.com/video321/mp4/240/big_buck_bunny_240p_1mb.mp4",
            "https://sample-videos.com/video321/mp4/240/big_buck_bunny_240p_1mb.mp4",
            "https://www.w3schools.com/html/mov_bbb.mp4",
        };

        String[] coverUrls = {
            "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&q=60",
            "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=60",
            "https://images.unsplash.com/photo-1474487548417-781cb71495f3?w=800&q=60",
            "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&q=60",
            "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=60",
        };

        for (int i = 0; i < items.length; i++) {
            Video video = new Video();
            video.setTitle(items[i][0]);
            video.setDescription(items[i][1]);
            video.setUrl(videoUrls[i]);
            video.setCoverUrl(coverUrls[i]);
            video.setCategory(items[i][2]);
            video.setStatus(ContentStatus.PUBLISHED.name());
            video.setPublishedAt(LocalDateTime.now().minusHours(i * 4L));
            videoMapper.insert(video);
        }
        log.info("已插入 {} 条示例视频数据", items.length);
    }
}
