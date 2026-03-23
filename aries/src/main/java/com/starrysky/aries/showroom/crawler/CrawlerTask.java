//package com.starrysky.aries.showroom.crawler;
//
//import com.starrysky.aries.entity.BikeModel;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import us.codecraft.webmagic.Spider;
//import us.codecraft.webmagic.pipeline.Pipeline;
//
///**
// * 定时任务，每周爬取一次最新车型
// *
// * @author adi
// * @version 1.0
// * @since 2026/2/12 14:07
// */
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class CrawlerTask {
//    private final PlaywrightDownloader playwrightDownloader;
//    private final BikeModelProcessor bikeModelProcessor;
//
//    /**
//     * 定时任务：每周自动扫描最新车型
//     */
//    @Scheduled(cron = "0 0 2 * * TUE")
//    public void runCrawler() {
//        Spider.create(bikeModelProcessor)
//                .addUrl("http://www.biketo.com/product/list-2-p1.html") // 公路车分类
//                .setDownloader(playwrightDownloader)
//                .addPipeline(createPipeline())
//                .thread(5)
//                .run();
//    }
//
//    private Pipeline createPipeline() {
//        return (resultItems, task) -> {
//            // 在这里实现最终的数据清洗与保存逻辑
//            // 检查数据库是否存在 existsByBrandAndModelAndSize
//            // 如果不存在则 save
//            log.info(task.getSite().getDomain());
//            resultItems.getAll().forEach((index, item) -> {
//                log.info("已持久化新车: {}--{}", index, item);
//            });
//            log.info("持久化引擎正在同步数据...");
//        };
//    }
//}
