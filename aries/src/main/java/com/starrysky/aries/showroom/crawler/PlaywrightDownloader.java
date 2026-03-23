//package com.starrysky.aries.showroom.crawler;
//
//import com.microsoft.playwright.Browser;
//import com.microsoft.playwright.BrowserContext;
//import com.microsoft.playwright.BrowserType;
//import com.microsoft.playwright.Playwright;
//import com.microsoft.playwright.options.LoadState;
//import jakarta.annotation.Resource;
//import org.springframework.stereotype.Component;
//import us.codecraft.webmagic.Page;
//import us.codecraft.webmagic.Request;
//import us.codecraft.webmagic.Task;
//import us.codecraft.webmagic.downloader.Downloader;
//import us.codecraft.webmagic.selector.PlainText;
//
///**
// * 利用Playwright渲染动态JS内容
// *
// * @author adi
// * @version 1.0
// * @since 2026/2/12 13:59
// */
//@Component
//public class PlaywrightDownloader implements Downloader {
//    private final Browser browser;
//    public PlaywrightDownloader() {
//        Playwright playwright = Playwright.create();
//        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
//    }
//    @Override
//    public Page download(Request request, Task task) {
//        try (BrowserContext context = browser.newContext()) {
//            com.microsoft.playwright.Page playwrightPage = context.newPage();
//            playwrightPage.navigate(request.getUrl());
//            // 等待页面核心几何表加载 (根据美骑网或品牌官网特征调整)
//            playwrightPage.waitForLoadState(LoadState.NETWORKIDLE);
//
//            String content = playwrightPage.content();
//            Page page = new Page();
//            page.setRawText(content);
//            page.setUrl(new PlainText(request.getUrl()));
//            page.setRequest(request);
//            page.setStatusCode(200);
//            return page;
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    @Override
//    public void setThread(int i) {
//
//    }
//}
