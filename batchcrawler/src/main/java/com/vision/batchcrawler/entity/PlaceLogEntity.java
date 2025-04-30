package com.vision.batchcrawler.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "PLACE_LOG")
public class PlaceLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PLACE_LOG_NO", nullable = false)
    private Long placeLogNo;  // ✅ 새로운 PK

    @Column(name = "LOG_ID")
    private Long logId;

    @Column(name = "NO")
    private Integer no;  // 원본 PLACE의 키 (PK 아님)

    @Column(name = "REG_DT", nullable = false)
    private String regDt;

    @Column(name = "BUSINESS_NAME", length = 500, nullable = false)
    private String businessName;

    @Column(name = "PLACE_NUMBER", nullable = false)
    private Integer placeNumber;

    @Column(name = "KEYWORD", length = 500)
    private String keyword;

    @Column(name = "CATEGORY", length = 100)
    private String category;

    @Column(name = "INITIAL_RANK")
    private Integer initialRank;

    @Column(name = "HIGHEST_RANK")
    private Integer highestRank;

    @Column(name = "RECENT_RANK")
    private Integer recentRank;

    @Column(name = "CURRENT_RANK")
    private Integer currentRank;

    @Column(name = "EMP_NAME", length = 100)
    private String empName;

    @Column(name = "BLOG_REVIEWS")
    private Integer blogReviews;

    @Column(name = "VISITOR_REVIEWS")
    private Integer visitorReviews;

    @Column(name = "ADVERTISEMENT", length = 100)
    private String advertisement;

    @Column(name = "RANK_CHK_DT", length = 100)
    private String rankChkDt;

    @Column(name = "DELETED_YN", length = 1)
    private String deletedYn;

    @Column(name = "EMP_ID", length = 100)
    private String empId;

    @Column(name = "HIGHEST_DT", length = 100)
    private String highestDt;

    @Column(name = "CRAWL_YN", length = 1)
    private String crawlYn;

    @Column(name = "CORRECT_YN", length = 1)
    private String correctYn;

    @Column(name = "STARTED_AT")
    private LocalDateTime startedAt;

    @Column(name = "ENDED_AT")
    private LocalDateTime endedAt;
}
