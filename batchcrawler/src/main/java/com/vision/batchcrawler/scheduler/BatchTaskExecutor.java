package com.vision.batchcrawler.scheduler;

import com.vision.batchcrawler.service.BatchCrawlService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchTaskExecutor {

    private final BatchCrawlService batchCrawlService;  // 크롤링 및 업데이트 처리 서비스

    @PostConstruct
    public void executeBatchJob() throws InterruptedException {
        log.info("Starting the batch job immediately...");
        batchCrawlService.batchCrawlMain();
        System.exit(0);  // 배치 작업 후 애플리케이션 종료
    }

}
