package com.vision.cralwingvps.worker;

import com.vision.cralwingvps.client.CrawlRequestApiClient;
import com.vision.cralwingvps.dto.CrawlPlaceDto;
import com.vision.cralwingvps.service.NaverMapService;
import com.vision.cralwingvps.util.SeleniumUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class ChromeWorker extends AbstractWorker {

    private WebDriver driver;
    private final CrawlRequestApiClient apiClient = new CrawlRequestApiClient();

    public ChromeWorker(BlockingQueue<CrawlPlaceDto> queue) {
        super("chrome", queue);
    }

    @Override
    protected boolean init() {
        log.info("🟡 [{}] ChromeDriver 초기화 시도 중", workerId);
        try {
            driver = SeleniumUtil.getUserChromeDriver();
            if (driver == null) return false;

            driver.get("https://map.naver.com");
            Thread.sleep(2000); // 초기 로딩 대기
            log.info("🟢 [{}] ChromeDriver 초기화 및 NaverMap 로딩 완료", workerId);
            return true;
        } catch (Exception e) {
            log.error("❌ [{}] 초기화 중 오류 발생: {}", workerId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected void process(CrawlPlaceDto task) {
        log.info("✅ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, 크롤링 시작", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank());
        try {
            int beforeCurRank = task.getCurrentRank();
            int rank = NaverMapService.scrollToFindBusiness(driver, task);
            task.setCurrentRank(rank);
            task.setDeamonCrawlStatus("DONE");
//            apiClient.sendCrawlingResult(task, "DONE", "성공");
            log.info("✅ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {} → 크롤링 순위 {}, 크롤링 성공", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), beforeCurRank, rank);
        } catch (Exception e) {
            log.error("❌ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, 크롤링 실패", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank());
            task.setDeamonCrawlStatus("FAIL");
//            apiClient.sendCrawlingResult(task, "FAIL", e.getMessage());
        }
    }

    @Override
    protected void destroy() {
        if (driver != null) {
            try {
                driver.quit();
                log.info("🧹 [{}] ChromeDriver 정상 종료", workerId);
            } catch (Exception e) {
                log.warn("⚠ [{}] ChromeDriver 종료 중 오류: {}", workerId, e.getMessage());
            }
        }
    }
}
