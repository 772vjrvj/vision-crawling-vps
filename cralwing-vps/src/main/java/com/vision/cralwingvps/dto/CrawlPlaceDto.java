package com.vision.cralwingvps.dto;

import lombok.Data;

@Data
public class CrawlPlaceDto {
    private Integer no;               // No
    private String regDt;            // 등록일
    private String businessName;     // 상호명
    private Integer placeNumber;      // 플레이스 번호
    private String keyword;          // 키워드
    private String category;         // 카테고리
    private Integer initialRank;      // 최초 순위
    private Integer highestRank;      // 최고 순위
    private Integer recentRank;       // 최근 순위
    private Integer currentRank;      // 현재 순위
    private String empName;          // 담당자
    private Integer blogReviews;      // 블로그 리뷰
    private Integer visitorReviews;   // 방문자 리뷰
    private String advertisement;    // 선광고
    private String rankChkDt;        // 순위조회 일시
    private String highestDt;      // 최고 일시
    private String deletedYn;        // 삭제 여부
    private String empId;        // 담당자아이디
    private String crawlYn;        // 크롤링
    private String correctYn;        // 보정
    private String deamonCrawlStatus;
    private String deamonCrawlDt;
    private Integer index;
    private String queueName;
}
