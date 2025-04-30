package com.vision.batchcrawler.service;

import com.vision.batchcrawler.dto.PlaceDto;
import com.vision.batchcrawler.entity.BatchCrawlLogEntity;
import com.vision.batchcrawler.entity.PlaceLogEntity;
import com.vision.batchcrawler.repository.BatchCrawlLogRepository;
import com.vision.batchcrawler.repository.PlaceLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j  // SLF4J 로깅을 사용하기 위한 어노테이션
public class BatchCrawlService {

    private final BatchApiService batchApiService;
    private final BatchCrawlLogRepository batchCrawlLogRepository;
    private final PlaceLogRepository placeLogRepository;

    private static final int RESTART_INTERVAL = 50;
    private final List<PlaceDto> failList = new ArrayList<>();
    private final List<PlaceDto> successList = new ArrayList<>();
    private int eqCnt = 0;
    private int dfCnt = 0;

    public void batchCrawlMain() throws InterruptedException {
        crawlWithRetry("one", "1위");
        crawlWithRetry("last", "301위");
        crawlWithRetry("none", "999위");
    }

    private void crawlWithRetry(String type, String label) throws InterruptedException {
        log.info("✅ {} 시작 ====================", label);
        List<PlaceDto> placeDtos = batchApiService.fetchDataFromMainServer(type);
        crawlAndRetry(type,placeDtos);
        log.info("✅ {} 끝 ====================", label);
    }

    private BatchCrawlLogEntity insertBatchLog(String type, int total, int success, int fail, int eq, int diff,
                                LocalDateTime start, LocalDateTime end, String status, String message) {
        BatchCrawlLogEntity log = new BatchCrawlLogEntity();
        log.setBatchType(type);
        log.setTotalCount(total);
        log.setSuccessCount(success);
        log.setFailCount(fail);
        log.setEqCount(eq);
        log.setDfCount(diff);
        log.setStartedAt(start);
        log.setEndedAt(end);
        log.setStatus(status);
        log.setMessage(message);
        return batchCrawlLogRepository.save(log);
    }

    private PlaceLogEntity toEntity(PlaceDto dto, long logId, LocalDateTime startedAt, LocalDateTime endedAt) {
        PlaceLogEntity entity = new PlaceLogEntity();
        entity.setNo(dto.getNo());
        entity.setLogId(logId);
        entity.setRegDt(dto.getRegDt());
        entity.setBusinessName(dto.getBusinessName());
        entity.setPlaceNumber(dto.getPlaceNumber());
        entity.setKeyword(dto.getKeyword());
        entity.setCategory(dto.getCategory());
        entity.setInitialRank(dto.getInitialRank());
        entity.setHighestRank(dto.getHighestRank());
        entity.setRecentRank(dto.getRecentRank());
        entity.setCurrentRank(dto.getCurrentRank());
        entity.setEmpName(dto.getEmpName());
        entity.setBlogReviews(dto.getBlogReviews());
        entity.setVisitorReviews(dto.getVisitorReviews());
        entity.setAdvertisement(dto.getAdvertisement());
        entity.setRankChkDt(dto.getRankChkDt());
        entity.setDeletedYn(dto.getDeletedYn());
        entity.setEmpId(dto.getEmpId());
        entity.setHighestDt(dto.getHighestDt());
        entity.setCrawlYn(dto.getCrawlYn());
        entity.setCorrectYn(dto.getCorrectYn());
        entity.setStartedAt(startedAt);
        entity.setEndedAt(endedAt);
        return entity;
    }


    private void crawlAndRetry(String type, List<PlaceDto> placeDtos) throws InterruptedException {
        // 1차 실행
        LocalDateTime firstStart = LocalDateTime.now();
        naverCralwing(placeDtos);
        LocalDateTime firstEnd = LocalDateTime.now();

        BatchCrawlLogEntity firstEntity = insertBatchLog(type, placeDtos.size(), successList.size(), failList.size(), eqCnt, dfCnt, firstStart, firstEnd, "SUCCESS", null);

        if (!failList.isEmpty()) {
            List<PlaceLogEntity> logs = failList.stream()
                    .map(dto -> toEntity(dto, firstEntity.getLogId(), firstStart, firstEnd))
                    .toList();
            placeLogRepository.saveAll(logs);
        }

        List<PlaceDto> copyFailList = new ArrayList<>(failList);
        resetTrackingLists();

        // 2차 실행
        LocalDateTime secondStart = LocalDateTime.now();
        naverCralwing(copyFailList);
        LocalDateTime secondEnd = LocalDateTime.now();

        BatchCrawlLogEntity secondEntity = insertBatchLog(type, copyFailList.size(), successList.size(), failList.size(), eqCnt, dfCnt, secondStart, secondEnd, "SUCCESS", null);

        if (!failList.isEmpty()) {
            List<PlaceLogEntity> logs = failList.stream()
                    .map(dto -> toEntity(dto, secondEntity.getLogId(), secondStart, secondEnd))
                    .toList();
            placeLogRepository.saveAll(logs);
        }

        Thread.sleep(60000);
    }

    private void resetTrackingLists() {
        failList.clear();
        successList.clear();
        eqCnt = 0;
        dfCnt = 0;
    }


    public WebDriver setupChromeDriver() {
        // 크롬 실행 옵션 설정
        ChromeOptions options = new ChromeOptions();

        // 최신 헤드리스 모드 사용 (Chrome 109 이상부터 추천)
        options.addArguments("--headless=new");

        // 충분한 뷰포트 크기 설정 (반응형 레이아웃 대응)
        options.addArguments("--window-size=1920,1080");

        // GPU 비활성화 (서버 환경 등에서 성능 및 오류 예방)
        options.addArguments("--disable-gpu");

        // 샌드박스 비활성화 (보안 격리 기능 끔, 컨테이너 환경에서 필요)
        options.addArguments("--no-sandbox");

        // /dev/shm 용량 부족 방지 (Docker 환경에서 자주 발생)
        options.addArguments("--disable-dev-shm-usage");

        // 자동화 탐지 방지 (navigator.webdriver 등 감지 차단 목적)
        options.addArguments("--disable-blink-features=AutomationControlled");

        // "Chrome is being controlled by automated test software" 메시지 제거
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        // Chrome의 기본 자동화 확장 제거
        options.setExperimentalOption("useAutomationExtension", false);

        // 사용자 에이전트 위조 (정상 사용자 브라우저처럼 보이게)
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // 커스텀 크롬 바이너리 경로 설정 (Docker 또는 리눅스 서버용)
//        options.setBinary("/usr/bin/chromium");

        // 크롬드라이버 경로 설정 및 서비스 생성
//        ChromeDriverService service = new ChromeDriverService.Builder()
//                .usingDriverExecutable(new java.io.File("/usr/bin/chromedriver"))  // 커스텀 드라이버
//                .usingAnyFreePort()  // 사용 가능한 포트 자동 할당
//                .build();

        // 크롬 드라이버 생성
//        ChromeDriver driver = new ChromeDriver(service, options);
        ChromeDriver driver = new ChromeDriver(options);

        // navigator.webdriver = undefined 설정으로 봇 탐지 우회
        driver.executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});"
        );

        // 창 위치 (좌측 상단) 및 크기 설정 (렌더링 및 스크린샷 오류 방지)
        driver.manage().window().setPosition(new org.openqa.selenium.Point(0, 0));
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(1000, 1000));

        return driver;
    }






    public boolean inputSearchKeyword(WebDriver driver, String keyword) {
        try {
            // iframe 밖으로 나가기
            driver.switchTo().defaultContent();

            // 검색 입력창 요소 기다리기 (최대 10초)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement searchInput = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.className("input_search"))
            );

            // 입력창 클릭 및 초기화
            searchInput.click();
            searchInput.clear();
            searchInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));  // 전체 선택
            searchInput.sendKeys(Keys.DELETE);  // 삭제

            Thread.sleep(250);  // 0.3초 대기

            // 키워드 입력
            searchInput.sendKeys(keyword);
            Thread.sleep(250);  // 0.5초 대기

            // Enter 입력
            searchInput.sendKeys(Keys.ENTER);
            Thread.sleep(2500);  // 2.5초 대기

            return true;

        } catch (Exception e) {
            log.error("❌ 검색어 입력 중 예외 발생");
            return false;
        }
    }


    private boolean waitForIframeAndSwitch(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("searchIframe")));
            return true;
        } catch (Exception e) {
            log.error("iframe 전환 실패");
            return false;
        }
    }


    public Integer realTimeRank(WebElement scrollableDiv, List<String> businessNames, String targetName) {
        List<WebElement> liElements;
        try {
            liElements = scrollableDiv.findElements(By.cssSelector("ul > li"));
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            log.error("❌ ⚠ li 요소 탐색 실패");
            return -1;
        } catch (Exception e) {
            log.error("❌ 예기치 못한 에러 발생");
            return -1;
        }

        for (int index = 0; index < liElements.size(); index++) {
            WebElement li = liElements.get(index);
            try {
                List<WebElement> adElements = li.findElements(By.cssSelector("span.place_blind"));
                boolean isAd = adElements.stream()
                        .anyMatch(ad -> "광고".equals(ad.getText().trim()));
                if (isAd) continue;

                // 상호명 추출
                WebElement bluelinkDiv = li.findElement(By.className("place_bluelink"));
                List<WebElement> spanElements = bluelinkDiv.findElements(By.tagName("span"));
                WebElement nameElement = !spanElements.isEmpty() ? spanElements.get(0) : null;

                if (nameElement != null) {
                    String businessName = nameElement.getText().trim();
                    if (!businessName.isEmpty() && !businessNames.contains(businessName)) {
                        businessNames.add(businessName);
                    }
                }

                if (businessNames.contains(targetName)) {
                    return businessNames.indexOf(targetName) + 1;
                }

            } catch (Exception e) {
                log.warn("❌ real_time_rank 에러 (index: {})", index);
            }
        }

        return 0;  // 못 찾은 경우
    }


    public Integer scrollSlowlyToBottom(WebDriver driver, PlaceDto obj) throws InterruptedException {
        try {
            driver.switchTo().defaultContent();
        } catch (Exception e) {
            log.error("❌ defaultContent 전환 실패");
            return -1;
        }

        if (!waitForIframeAndSwitch(driver)) {
            log.error("❌ iframe 로딩 실패");
            return -1;
        }

        String targetName = obj.getBusinessName().trim();
        List<String> businessNames = new ArrayList<>();
        int pageNum = 1;

        WebElement scrollableDiv;
        while (true) {
            log.info("✅ 현재 페이지 {}.", pageNum);
            try {
                // 상호명 목록 리스트 컨테이너 가져오기
                scrollableDiv = new WebDriverWait(driver, Duration.ofSeconds(4))
                        .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div#_pcmap_list_scroll_container")));
            } catch (TimeoutException e) {
                log.error("❌ 타임아웃 에러");

                try {
                    WebElement noResult = driver.findElement(By.className("FYvSc"));
                    if ("조건에 맞는 업체가 없습니다.".equals(noResult.getText())) {
                        log.warn("조건에 맞는 업체가 없습니다.");
                        log.warn("❌ '{}'의 위치: 999 번째", targetName);
                        return 999;
                    }
                } catch (Exception ex) {
                    log.error("❌ '{}'의 위치: 999 번째 에러", targetName);
                }
                return -1;
            }

            try {
                new Actions(driver).moveToElement(scrollableDiv).perform();
            } catch (Exception e) {
                log.error("❌ move_to_element 에러");
                return -1;
            }


            Integer result1 = realTimeRank(scrollableDiv, businessNames, targetName);
            if (result1 == -1) {
                log.error("❌ 순위조회 실패");
                return -1;
            } else if (result1 != 0) {
                return result1;
            }

            try {
                while (true) {
                    for (int i = 0; i < 7; i++) {
                        try {
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollTop += 250;", scrollableDiv);
                        } catch (Exception e) {
                            log.error("❌ 스크롤 증가 중 에러");
                        }
                        Thread.sleep(200);
                    }
                    Thread.sleep(1000);

                    Long currentScroll = (Long) ((JavascriptExecutor) driver)
                            .executeScript("return arguments[0].scrollTop;", scrollableDiv);
                    Long maxScroll = (Long) ((JavascriptExecutor) driver)
                            .executeScript("return arguments[0].scrollHeight - arguments[0].clientHeight;", scrollableDiv);

                    if (currentScroll != null && maxScroll != null && currentScroll >= maxScroll - 5) {
                        log.info("✅ 스크롤이 끝까지 내려졌습니다.");
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("❌ 전체 스크롤 처리 중 예외 발생");
                return -1;
            }

            Integer result2 = realTimeRank(scrollableDiv, businessNames, targetName);
            if (result2 == -1) {
                log.error("❌ 순위조회 실패");
                return -1;
            } else if (result2 != 0) {
                return result2;
            }

            List<WebElement> pages;
            try {
                pages = driver.findElements(By.cssSelector("div.zRM9F > a.mBN2s"));
            } catch (Exception e) {
                log.error("❌ 페이지 리스트 로딩 실패");
                return -1;
            }

            int currentPageIndex = -1;
            for (int idx = 0; idx < pages.size(); idx++) {
                try {
                    if (pages.get(idx).getAttribute("class").contains("qxokY")) {
                        currentPageIndex = idx;
                        break;
                    }
                } catch (Exception e) {
                    log.error("❌ 페이지 클래스 확인 실패 (index {})", idx);
                    return -1;
                }
            }

            if (currentPageIndex + 1 < pages.size()) {
                try {
                    WebElement nextPage = pages.get(currentPageIndex + 1);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextPage);
                    Thread.sleep(2000);
                    pageNum++;
                } catch (Exception e) {
                    log.error("❌ 다음 페이지 클릭 실패");
                    return -1;
                }
            } else {
                log.info("✅ 마지막 페이지에 도달했습니다.");
                break;
            }
        }

        return businessNames.size() + 1;
    }

    // 크롤링 및 데이터 업데이트 처리
    public void naverCralwing(List<PlaceDto> placeDtos) throws InterruptedException {
        if (placeDtos != null && !placeDtos.isEmpty()) {
            WebDriver driver = setupChromeDriver();

            driver.get("https://map.naver.com");

            // 예: 특정 요소가 나타날 때까지 최대 5초 기다림
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("root")));

            for (int i = 0; i < placeDtos.size(); i++) {
                PlaceDto obj = placeDtos.get(i);

                if(i == 2){
                    break;
                }

                if (i % RESTART_INTERVAL == 0 && i != 0) {
                    driver.quit();
                    driver = setupChromeDriver();  // 드라이버 재시작
                    driver.get("https://map.naver.com");

                    WebDriverWait reWait = new WebDriverWait(driver, Duration.ofSeconds(2));
                    reWait.until(ExpectedConditions.presenceOfElementLocated(By.id("root")));
                    log.info("{} ■ 드라이버 리셋 ========================", java.time.LocalDateTime.now());
                }

                if("N".equals(obj.getCrawlYn())) continue;

                log.info("■ 현재 위치 {}/{} , 최초현재 순위 {} ========================", i + 1, placeDtos.size(), obj.getCurrentRank());
                log.info("🔍 검색 키워드: {}, 상호명: {}", obj.getKeyword(), obj.getBusinessName());

                if (!inputSearchKeyword(driver, obj.getKeyword())) continue;

                Integer currentRank = scrollSlowlyToBottom(driver, obj);

                if (currentRank == -1){
                    obj.setCrawlSuccessYn("N");
                    failList.add(obj);
                    continue;
                }

                boolean rs = Objects.equals(obj.getCurrentRank(), currentRank);
                String rsText = rs ? "같음" : "다름";

                if (rs) {
                    eqCnt++;
                } else {
                    dfCnt++;
                }

                obj.setRecentRank(obj.getCurrentRank());
                obj.setRankChkDt(LocalDateTime.now().toString());

                if ("N".equals(obj.getCorrectYn()) && !Objects.equals(obj.getCurrentRank(), currentRank)) {
                    obj.setCorrectYn("Y");
                    obj.setHighestRank(currentRank);
                    obj.setInitialRank(currentRank);
                    obj.setHighestDt(LocalDateTime.now().toString());
                } else if (obj.getHighestRank() > currentRank) {
                    obj.setHighestRank(currentRank);
                    obj.setHighestDt(LocalDateTime.now().toString());
                }

                obj.setCurrentRank(currentRank);
                obj.setCrawlSuccessYn("Y");
                successList.add(obj);

                log.info("■ 끝 현재 위치 : {}/{} | 현재 순위: {} | 차이: {}", i + 1, placeDtos.size(), obj.getCurrentRank(), rsText);
            }

            log.info("작업완료(처음 수): {}", placeDtos.size());
            log.info("작업완료(성공 수): {}", successList.size());
            log.info("작업완료(같은 수): {}", eqCnt);
            log.info("작업완료(다른 수): {}", dfCnt);
            log.info("작업완료(실패 수): {}", failList.size());

            batchApiService.updateMainServerList(successList);

            // 드라이버 종료
            driver.quit();
        }
    }

}
