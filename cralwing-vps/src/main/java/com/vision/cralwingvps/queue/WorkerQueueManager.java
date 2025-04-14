package com.vision.cralwingvps.queue;

import com.vision.cralwingvps.dto.CrawlPlaceDto;
import com.vision.cralwingvps.worker.BraveWorker;
import com.vision.cralwingvps.worker.ChromeWorker;
import com.vision.cralwingvps.worker.FirefoxWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
public class WorkerQueueManager {

    private final Map<String, BlockingQueue<CrawlPlaceDto>> workerQueues = new HashMap<>();
    private final List<String> workerIds = List.of("chrome", "brave", "firefox");
    private int roundRobinIndex = 0;

    public WorkerQueueManager() {
        for (String id : workerIds) {
            workerQueues.put(id, new LinkedBlockingQueue<>());
        }
        startWorkers();
    }

    public void dispatch(CrawlPlaceDto task) {
        String target = getSmartWorkerId();
        boolean offer = workerQueues.get(target).offer(task);
        if (offer){
            log.info("📦 작업 {} → {} 워커에 분배됨 (큐 길이: {})", task.getBusinessName(), target, workerQueues.get(target).size());
        }
    }

    private String getSmartWorkerId() {
        int minSize = Integer.MAX_VALUE;
        List<String> candidates = new ArrayList<>();

        for (String id : workerIds) {
            int size = workerQueues.get(id).size();

            if (size < minSize) {
                minSize = size;
                candidates.clear();
                candidates.add(id);
            } else if (size == minSize) {
                candidates.add(id);
            }
        }

        if (candidates.size() == workerIds.size()) {
            String id = workerIds.get(roundRobinIndex);
            roundRobinIndex = (roundRobinIndex + 1) % workerIds.size();
            return id;
        }

        return candidates.get(0);
    }

    public BlockingQueue<CrawlPlaceDto> getQueue(String workerId) {
        return workerQueues.get(workerId);
    }

    private void startWorkers() {
        new Thread(new ChromeWorker(getQueue("chrome"))).start();
        new Thread(new FirefoxWorker(getQueue("firefox"))).start();
        new Thread(new BraveWorker(getQueue("brave"))).start();
    }
}
