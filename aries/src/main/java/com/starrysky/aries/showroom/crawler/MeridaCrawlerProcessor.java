//package com.starrysky.aries.showroom.crawler;
//
//import com.starrysky.aries.entity.BikeModel;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import us.codecraft.webmagic.Page;
//import us.codecraft.webmagic.Site;
//import us.codecraft.webmagic.Spider;
//import us.codecraft.webmagic.processor.PageProcessor;
//import us.codecraft.webmagic.selector.Html;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * TODO
// *
// * @author adi
// * @version 1.0
// * @since 2026/2/13 12:21
// */
//public class MeridaCrawlerProcessor implements PageProcessor {
//
//    private Site site = Site.me()
//            .setRetryTimes(3)
//            .setSleepTime(2000)
//            .setCharset("UTF-8")
//            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");
//
//    @Override
//    public void process(Page page) {
//        // --- 逻辑 1：发现阶段 (列表页) ---
//        // 兼容不同的列表页链接特征
//        if (page.getUrl().get().contains("a=lists")) {
//            System.out.println(">>> 正在扫描列表页: " + page.getUrl());
//
//            // 尝试更宽泛的选择器：直接寻找所有产品详情链接 (通常包含 a=show)
//            List<String> allLinks = page.getHtml().xpath("//a[contains(@href, 'a=show')]/@href").all();
//
//            if (allLinks == null || allLinks.isEmpty()) {
//                System.err.println("!!! 未能提取到任何详情链接，请检查列表页选择器。");
//                // 打印前1000个字符看看HTML到底长什么样
//                System.out.println("DEBUG HTML: " + page.getHtml().get().substring(0, Math.min(1000, page.getHtml().get().length())));
//                return;
//            }
//
//            // 过滤并添加请求
//            for (String link : allLinks) {
//                // 确保补全域名
//                String absLink = link.startsWith("http") ? link : "https://www.merida.cn/" + link;
//                page.addTargetRequest(absLink);
//            }
//            System.out.println(">>> 已成功提取并添加 " + allLinks.size() + " 个战车详情请求到队列。");
//            return;
//        }
//
//        // --- 逻辑 2：提取阶段 (详情页) ---
//        System.out.println(">>> 正在解析详情页: " + page.getUrl());
//        Html html = page.getHtml();
//
//        // 获取型号名称
//        String modelName = html.xpath("//div[contains(@class,'pro_info_title')]/h1/text()").get();
//        if (modelName == null) {
//            // 尝试另一种结构
//            modelName = html.xpath("//h1/text()").get();
//        }
//
//        if (modelName == null) {
//            System.err.println("!!! 无法解析型号名称: " + page.getUrl());
//            return;
//        }
//
//        // 定位几何表格 - 采用更激进的查找策略
//        // 寻找页面中包含 "REACH" 或 "STACK" 或 "尺寸" 字样的 table
//        Element tableElement = null;
//        Elements tables = html.getDocument().select("table");
//        for (Element t : tables) {
//            String tableText = t.text().toUpperCase();
//            if (tableText.contains("REACH") || tableText.contains("STACK") || tableText.contains("几何")) {
//                tableElement = t;
//                break;
//            }
//        }
//
//        if (tableElement == null) {
//            System.err.println("!!! 未找到几何表: " + modelName);
//            return;
//        }
//
//        Elements trs = tableElement.select("tr");
//        if (trs.size() < 2) return;
//
//        // 解析尺码 (第一行)
//        Elements sizeTds = trs.get(0).select("td, th");
//        List<BikeModel> sizeModels = new ArrayList<>();
//        for (int j = 1; j < sizeTds.size(); j++) {
//            String sizeStr = sizeTds.get(j).text().trim();
//            if (sizeStr.isEmpty()) continue;
//
//            BikeModel m = new BikeModel();
//            m.setModel(modelName);
//            m.setSize(sizeStr);
//            m.setSourceUrl(page.getUrl().get());
//            sizeModels.add(m);
//        }
//
//        // 解析参数
//        for (int i = 1; i < trs.size(); i++) {
//            Elements tds = trs.get(i).select("td");
//            if (tds.isEmpty()) continue;
//            String label = tds.get(0).text().trim().toUpperCase();
//
//            for (int j = 1; j < tds.size(); j++) {
//                if (j > sizeModels.size()) break;
//                String value = tds.get(j).text().replaceAll("[^0-9.]", "");
//                if (value.isEmpty()) continue;
//                mapProperty(sizeModels.get(j - 1), label, value);
//            }
//        }
//
//        System.out.println("\n[解析成功] " + modelName + " (" + sizeModels.size() + "个尺码)");
//        sizeModels.forEach(m -> System.out.println("  - " + m));
//    }
//
//    private void mapProperty(BikeModel m, String label, String value) {
//        try {
//            double val = Double.parseDouble(value);
//            if (label.contains("STACK")) m.setStack((int) val);
//            else if (label.contains("REACH")) m.setReach((int) val);
//            else if (label.contains("STA") || label.contains("立管角度")) m.setSeatTubeAngle(val);
//            else if (label.contains("HTA") || label.contains("头管角度")) m.setHeadTubeAngle(val);
//        } catch (Exception ignored) {}
//    }
//
//    @Override
//    public Site getSite() { return site; }
//
//    public static void main(String[] args) {
//        Spider.create(new MeridaCrawlerProcessor())
//                // 换一个包含几何表的分类测试
//                .addUrl("https://www.merida.cn/index.php?m=content&c=index&a=lists&catid=162")
//                .thread(1) // 测试时先用单线程，方便看日志
//                .run();
//    }
//}
