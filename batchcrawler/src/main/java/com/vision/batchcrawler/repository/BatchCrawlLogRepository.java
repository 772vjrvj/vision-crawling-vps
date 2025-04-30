package com.vision.batchcrawler.repository;

import com.vision.batchcrawler.entity.BatchCrawlLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchCrawlLogRepository extends JpaRepository<BatchCrawlLogEntity, Long> {
}
