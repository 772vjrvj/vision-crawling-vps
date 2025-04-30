package com.vision.batchcrawler.repository;

import com.vision.batchcrawler.entity.PlaceLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceLogRepository extends JpaRepository<PlaceLogEntity, Long> {
}
