//package com.starrysky.aries.showroom.crawler;
//
//import com.starrysky.aries.entity.BikeModel;
//import lombok.extern.slf4j.Slf4j;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import org.springframework.stereotype.Component;
//import us.codecraft.webmagic.Page;
//import us.codecraft.webmagic.Site;
//import us.codecraft.webmagic.Spider;
//import us.codecraft.webmagic.processor.PageProcessor;
//import us.codecraft.webmagic.selector.Html;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * 自动抓取：BikeModelProcessor
// * 爬取美骑网的车型信息
// *
// * @author adi
// * @version 1.0
// * @since 2026/2/12 14:04
// */
//@Component
//@Slf4j
//public class BikeModelProcessor implements PageProcessor {
//    private final Site site = Site.me()
//            .setRetryTimes(3)
//            .setSleepTime(3000)
//            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
//
//    // 生物力学中文术语映射
//    private static final Map<String, String> TERMS = new HashMap<>();
//
//    static {
//        TERMS.put("堆高", "stack");
//        TERMS.put("Stack", "stack");
//        TERMS.put("前伸量", "reach");
//        TERMS.put("Reach", "reach");
//        TERMS.put("立管角度", "sta");
//        TERMS.put("STA", "sta");
//        TERMS.put("头管角度", "hta");
//        TERMS.put("HTA", "hta");
//    }
//
//    @Override
//    public void process(Page page) {
//        // --- 逻辑 1：处理列表页 (Discovery) ---
//        if (page.getUrl().get().contains("a=lists")) {
//            // 获取产品列表中的所有 a 标签链接
//            List<String> detailLinks = page.getHtml().css(".product_list_main ul li a").links().all();
//            page.addTargetRequests(detailLinks);
//
//            // 获取分页链接 (如果有)
//            List<String> nextPages = page.getHtml().css(".pages a").links().all();
//            page.addTargetRequests(nextPages);
//            return;
//        }
//
//        // --- 逻辑 2：处理详情页 (Extraction) ---
//        Html html = page.getHtml();
//        String modelName = html.xpath("//div[@class='pro_info_title']/h1/text()").get();
//        if (modelName == null) return;
//
//        String rawImageUrl = html.xpath("//div[@class='pro_info_img']//img/@src").get();
//        String absImageUrl = rawImageUrl.startsWith("http") ? rawImageUrl : "https://www.merida.cn" + rawImageUrl;
//
//        // 定位几何参数表
//        Element tableElement = html.getDocument().select("table").first();
//        // 注意：美利达官网几何表有时没有特定class，我们取详情页中的第一个 table
//        if (tableElement == null) return;
//
//        Elements trs = tableElement.select("tr");
//        if (trs.size() < 2) return;
//
//        // 解析第一行：获取尺码列表 (e.g. 44, 47, 50...)
//        Elements sizeTds = trs.get(0).select("td");
//        List<BikeModel> sizeModels = new ArrayList<>();
//        for (int j = 1; j < sizeTds.size(); j++) {
//            BikeModel m = new BikeModel();
//            m.setModel(modelName);
//            m.setSize(sizeTds.get(j).text().trim());
//            m.setImageUrl(absImageUrl);
//            m.setSourceUrl(page.getUrl().get());
//            m.setBikeType(1); // 默认设为公路，实际可根据 URL 参数 catid 判断
//            sizeModels.add(m);
//        }
//
//        // 遍历剩余行：解析具体参数
//        for (int i = 1; i < trs.size(); i++) {
//            Elements tds = trs.get(i).select("td");
//            if (tds.isEmpty()) continue;
//
//            String label = tds.get(0).text().trim().toUpperCase(); // 参数名，如 "STACK"
//
//            for (int j = 1; j < tds.size(); j++) {
//                if (j > sizeModels.size()) break;
//                String value = tds.get(j).text().replaceAll("[^0-9.]", "");
//                if (value.isEmpty()) continue;
//
//                BikeModel current = sizeModels.get(j - 1);
//                mapProperty(current, label, value);
//            }
//        }
//
//        // --- 逻辑 3：打印结果 (测试版不存库) ---
//        System.out.println("\n>>> 发现战车型号: " + modelName + " (共解析出 " + sizeModels.size() + " 个尺码)");
//        for (BikeModel m : sizeModels) {
//            System.out.println("   尺码 " + m.getSize() + " -> " + m);
//        }
//
//        // 传递给下一步处理 (虽然本版不存库)
//        page.putField("models", sizeModels);
//    }
//
//    /**
//     * 将美利达官网的参数名映射到我们的模型字段
//     */
//    private void mapProperty(BikeModel m, String label, String value) {
//        try {
//            double val = Double.parseDouble(value);
//            if (label.contains("STACK")) m.setStack((int) val);
//            else if (label.contains("REACH")) m.setReach((int) val);
//            else if (label.contains("STA") || label.contains("立管角度")) m.setSeatTubeAngle(val);
//            else if (label.contains("HTA") || label.contains("头管角度")) m.setHeadTubeAngle(val);
//            else if (label.contains("HT") || label.contains("头管高度")) m.setHeadTubeLength((int) val);
//            else if (label.contains("TT") || label.contains("上管长度")) m.setTopTubeLength((int) val);
//        } catch (Exception ignored) {
//        }
//    }
//
//    @Override
//    public Site getSite() {
//        return site;
//    }
//
//    public static void main(String[] args) {
//        System.out.println("--- 启动美利达战车展厅爬虫测试 ---");
//        Spider.create(new BikeModelProcessor())
//                // 初始 URL：列表页
//                .addUrl("https://www.merida.cn/index.php?m=content&c=index&a=lists&catid=164")
//                .thread(5)
//                .run();
//    }
//
//}
