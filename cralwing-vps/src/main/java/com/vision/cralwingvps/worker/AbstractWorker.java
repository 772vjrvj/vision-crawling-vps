package com.vision.cralwingvps.worker;

import com.vision.cralwingvps.dto.CrawlPlaceDto;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;

@Slf4j
public abstract class AbstractWorker implements Runnable {

    protected final String workerId;
    protected final BlockingQueue<CrawlPlaceDto> queue;

    public AbstractWorker(String workerId, BlockingQueue<CrawlPlaceDto> queue) {
        this.workerId = workerId;
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            log.info("🛠 [{}] 워커 초기화 시작", workerId);
            if (!init()) {
                log.error("❌ [{}] 워커 초기화 실패, 실행 중단", workerId);
                return;
            }
            log.info("✅ [{}] 워커 초기화 완료", workerId);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    CrawlPlaceDto task = queue.take();
                    logReceived(task);
                    process(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("⚠ [{}] 워커 인터럽트 발생 - 종료 요청 감지", workerId);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("❌ [{}] 워커 처리 중 예외 발생: {}", workerId, e.getMessage(), e);
        } finally {
            destroy();
        }
    }

    /**
     * 큐에 들어온 작업 로그 출력
     */
    protected void logReceived(CrawlPlaceDto task) {
        log.info("📥 [{}] 작업 수신: {} ({})", workerId, task.getBusinessName(), task.getPlaceNumber());
    }

    /**
     * ✅ 워커 최초 실행 시 딱 한 번 실행됨
     * - 예: WebDriver 초기화, 기본 페이지 로딩 등
     */
    protected abstract boolean init();

    /**
     * 🔁 각 작업 처리 로직 (큐에서 하나씩 꺼내 수행)
     */
    protected abstract void process(CrawlPlaceDto task);

    /**
     * 🧹 종료 시 자원 정리 (예: WebDriver quit 등)
     */
    protected void destroy() {
        log.info("🧹 [{}] 워커 종료 정리 완료", workerId);
    }
}
