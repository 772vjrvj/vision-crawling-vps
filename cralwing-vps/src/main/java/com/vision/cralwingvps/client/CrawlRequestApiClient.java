package com.vision.cralwingvps.client;

import com.vision.cralwingvps.dto.CrawlPlaceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.IDN;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class CrawlRequestApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${crawl.place.domain}")
    private String baseDomain;

    public List<CrawlPlaceDto> fetchBatchTasks(String type) {
        try {
            // 1. URI에 쿼리 파라미터 추가
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseDomain + "/user/place/rest/select-currentrank")
                    .queryParam("type", type);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String finalUrl = builder.toUriString();
            log.info("📡 요청 URL: {}", finalUrl);

            ResponseEntity<List<CrawlPlaceDto>> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("✅ 응답 수신 성공: {}건", response.getBody().size());
                return response.getBody();
            } else {
                log.warn("⚠ 응답 상태 코드: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ 요청 실패: {}", e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    public void bulkUpdateCrawlStatus(List<Integer> placeIds, String status) {
        String url = "https://your-server.com/api/place/bulk-update-status";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Mozilla/5.0");

        Map<String, Object> body = Map.of(
                "placeIds", placeIds,
                "status", status
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("✅ {}건 상태 일괄 업데이트 완료: {}", placeIds.size(), status);
        } catch (Exception e) {
            log.error("❌ 상태 일괄 업데이트 실패: {}", e.getMessage(), e);
        }
    }

    public void sendCrawlingResult(CrawlPlaceDto task, String status, String message) {
        String url = "https://your-server.com/api/place/" + task.getNo() + "/crawling-result";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Mozilla/5.0");

        Map<String, Object> body = Map.of(
                "status", status,
                "message", message,
                "worker", ""
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
            log.info("✅ 결과 업데이트 완료 → {}", task.getNo());
        } catch (Exception e) {
            log.error("❌ 결과 업데이트 실패: {}", e.getMessage(), e);
        }
    }

}
