package com.vision.batchcrawler.service;

import com.vision.batchcrawler.dto.PlaceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class BatchApiService {

    private final RestTemplate restTemplate;

    // 메인 서버 URL을 application.yml에서 주입 받음
    @Value("${main.server.select-url}")
    private String SELECT_URL;

    @Value("${main.server.update-url}")
    private String UPDATE_URL;

    public BatchApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // 메인 서버에서 데이터를 가져오는 메서드
    public List<PlaceDto> fetchDataFromMainServer(String paramType) {
        try {
            // 요청 URL에 쿼리 파라미터 추가
            String urlWithParams = SELECT_URL + "?type=" + paramType;

            // API 호출하여 데이터를 가져옴
            ResponseEntity<List<PlaceDto>> response = restTemplate.exchange(
                    urlWithParams,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<List<PlaceDto>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                List<PlaceDto> data = response.getBody();

                if (data != null && !data.isEmpty()) {
                    log.info("Fetched {} records from main server with paramType={}", data.size(), paramType);
                    return data;
                } else {
                    log.warn("No data received from main server. paramType={}", paramType);
                }
            } else {
                log.error("Failed to fetch data from main server. HTTP Status: {}, paramType={}", response.getStatusCode(), paramType);
            }

        } catch (HttpStatusCodeException e) {
            log.error("Error calling main server: {} - {}, paramType={}", e.getStatusCode(), e.getResponseBodyAsString(), paramType, e);
        } catch (Exception e) {
            log.error("An unexpected error occurred while fetching data from main server. paramType={}", paramType, e);
        }
        return Collections.emptyList();
    }
    // 단일 PlaceDto 객체를 받아서 리스트로 만들어서 메인 서버에 데이터를 업데이트하는 메서드
    public void updateMainServerOne(PlaceDto placeDto) {
        try {
            // 단일 PlaceDto 객체를 리스트로 감싸서 리스트로 변환
            List<PlaceDto> placeDtoList = List.of(placeDto);  // 단일 객체를 리스트로 변환

            // POST 요청으로 리스트를 전송
            ResponseEntity<Integer> response = restTemplate.postForEntity(UPDATE_URL, placeDtoList, Integer.class);

            // 응답에서 성공한 갯수를 받아옴
            if (response.getStatusCode() == HttpStatus.OK) {
                Integer successCount = response.getBody();  // 성공한 갯수

                if (successCount != null) {
                    log.info("Successfully updated {} records to the main server.", successCount);
                } else {
                    log.warn("No successful update count returned from the main server.");
                }
            } else {
                log.error("Failed to update main server. HTTP Status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to update main server with place data", e);
        }
    }

    public void updateMainServerList(List<PlaceDto> placeDtoList) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<PlaceDto>> requestEntity = new HttpEntity<>(placeDtoList, headers);

            ResponseEntity<Integer> response = restTemplate.exchange(
                    UPDATE_URL,
                    HttpMethod.PUT,
                    requestEntity,
                    Integer.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Integer successCount = response.getBody();

                if (successCount != null) {
                    log.info("✅ 서버에 {}건 업데이트 완료", successCount);
                } else {
                    log.warn("⚠ 서버 응답에 성공 건수가 없습니다.");
                }
            } else {
                log.error("❌ 서버 업데이트 실패 - 상태 코드: {}",
                        response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ 서버로 place 리스트 전송 중 예외 발생", e);
        }
    }

}
