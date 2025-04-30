package com.vision.batchcrawler.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BatchCrawlLogDto {
    private String batchType;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Integer eqCount;
    private Integer dfCount;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String status;
    private String message;
}
