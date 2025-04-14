package com.vision.cralwingvps.dispatcher;

import com.vision.cralwingvps.client.CrawlRequestApiClient;
import com.vision.cralwingvps.dto.CrawlPlaceDto;
import com.vision.cralwingvps.queue.WorkerQueueManager;
import com.vision.cralwingvps.util.SeleniumUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class DispatcherService {

    private final WorkerQueueManager queueManager;
    private final CrawlRequestApiClient apiClient;

    @PostConstruct
    public void init() {
        log.info("🧹 DispatcherService 초기화 중... 브라우저 프로세스 종료 시도");
        SeleniumUtil.killBrowserProcesses();
    }


//    @Scheduled(fixedDelay = 2500)
    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
//    @Scheduled(initialDelay = 1000, fixedDelay = 2500)
    public void pollServer2ForTasks() {
        LocalTime now = LocalTime.now();
        // ❌ 01:00 ~ 04:00 중지
//        if (now.isAfter(LocalTime.of(1, 0)) && now.isBefore(LocalTime.of(4, 0))) return;
        // ❌ 04:00 ~ 08:00 중지 (04시 배치만 처리됨)
//        if (now.isAfter(LocalTime.of(4, 0)) && now.isBefore(LocalTime.of(8, 0))) return;

        List<CrawlPlaceDto> tasks = apiClient.fetchBatchTasks();
        for (int i = 0; i < tasks.size(); i++) {
//            if(i >= 5) break;
            CrawlPlaceDto task = tasks.get(i);
//            if (task.getCurrentRank() != 1 && task.getCurrentRank() > 100 && task.getCurrentRank() != 301 && task.getCurrentRank() != 999){
//                queueManager.dispatch(task);
//            }
            if (task.getCurrentRank() != 1){
                queueManager.dispatch(task);
            }
        }

        log.info("끝");


        // ✅ 나머지 시간대 (00:00 ~ 01:00, 08:00 ~ 24:00) → polling 실행
//        if (now.isBefore(LocalTime.of(1, 0)) || now.isAfter(LocalTime.of(8, 0))) {
//            List<CrawlPlaceDto> tasks = apiClient.fetchBatchTasks();
//            if (tasks.isEmpty()) return;
//            List<Integer> ids = tasks.stream()
//                    .map(CrawlPlaceDto::getNo)
//                    .collect(Collectors.toList());
//            apiClient.bulkUpdateCrawlStatus(ids, "START");
//            for (CrawlPlaceDto task : tasks) {
//                queueManager.dispatch(task);
//            }
//        }
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void batchFetchFromApiAt4am() {
        List<CrawlPlaceDto> tasks = apiClient.fetchBatchTasks();
        for (CrawlPlaceDto task : tasks) {
            queueManager.dispatch(task);
        }
    }
}