package com.vision.batchcrawler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "BATCH_CRAWL_LOG")
@Getter
@Setter
public class BatchCrawlLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    private String batchType;

    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Integer eqCount;
    private Integer dfCount;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    private String status;

    @Column(columnDefinition = "TEXT")
    private String message;
}
