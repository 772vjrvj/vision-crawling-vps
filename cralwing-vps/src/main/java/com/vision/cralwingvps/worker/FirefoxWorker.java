package com.vision.cralwingvps.worker;

import com.vision.cralwingvps.client.CrawlRequestApiClient;
import com.vision.cralwingvps.dto.CrawlPlaceDto;
import com.vision.cralwingvps.service.NaverMapService;
import com.vision.cralwingvps.util.SeleniumUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class FirefoxWorker extends AbstractWorker {

    private WebDriver driver;
    private final CrawlRequestApiClient apiClient = new CrawlRequestApiClient();

    public FirefoxWorker(BlockingQueue<CrawlPlaceDto> queue) {
        super("firefox", queue);
    }

    @Override
    protected boolean init() {
        log.info("🟡 [{}] FirefoxDriver 초기화 시도 중", workerId);
        try {
            driver = SeleniumUtil.getUserFirefoxDriver();
            if (driver == null) return false;

            driver.get("https://map.naver.com");
            Thread.sleep(2000); // 초기 로딩 대기
            log.info("🟢 [{}] FirefoxDriver 초기화 및 NaverMap 로딩 완료", workerId);
            return true;
        } catch (Exception e) {
            log.error("❌ [{}] Firefox 초기화 실패: {}", workerId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected void process(CrawlPlaceDto task) {
        log.info("🔍 [{}] 크롤링 시작 → {}", workerId, task.getBusinessName());
        try {
            int rank = NaverMapService.scrollToFindBusiness(driver, task);
            task.setCurrentRank(rank);
            task.setDeamonCrawlStatus("DONE");
//            apiClient.sendCrawlingResult(task, "DONE", "성공");
            log.info("✅ [{}] 크롤링 성공: {} → {}위", workerId, task.getBusinessName(), rank);
        } catch (Exception e) {
            log.error("❌ [{}] 크롤링 실패: {}", workerId, e.getMessage(), e);
            task.setDeamonCrawlStatus("FAIL");
//            apiClient.sendCrawlingResult(task, "FAIL", e.getMessage());
        }
    }

    @Override
    protected void destroy() {
        if (driver != null) {
            try {
                driver.quit();
                log.info("🧹 [{}] FirefoxDriver 정상 종료", workerId);
            } catch (Exception e) {
                log.warn("⚠ [{}] FirefoxDriver 종료 중 오류: {}", workerId, e.getMessage());
            }
        }
    }
}
