package com.vision.cralwingvps.service;

import com.vision.cralwingvps.dto.CrawlPlaceDto;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class NaverMapService {

    public static int scrollToFindBusiness(WebDriver driver, CrawlPlaceDto task) {
        try {
            String businessName = task.getBusinessName();
            String keyword = task.getKeyword();

            driver.switchTo().defaultContent();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("input_search")));

            // 키워드 입력 전 초기화
            searchInput.click();
            searchInput.clear();
            searchInput.sendKeys(Keys.CONTROL + "a");
            searchInput.sendKeys(Keys.DELETE);
            Thread.sleep(300); // 살짝 대기
            
            // 키워드 입력
            searchInput.sendKeys(task.getKeyword());
            Thread.sleep(500);

            // Enter 입력하여 검색 실행
            searchInput.sendKeys(Keys.ENTER);
            Thread.sleep(2000); // 검색 결과 로딩 대기

            WebElement scrollableDiv = switchToIframeAndGetScrollContainer(wait, task);
            if (scrollableDiv == null) {
                log.error("❌ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, iframe 전환 실패 null", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank());
                driver.switchTo().defaultContent();
                return task.getCurrentRank();
            }

            int resolvedRank = findScrollableDivOrHandleNoResult(driver, task);

            if (resolvedRank == 999) {
                driver.switchTo().defaultContent();
                return 999;
            }

            List<String> businessNames = new ArrayList<>();
            int pageNum = 1;

            do {
                int rank = extractBusinessRank(driver, scrollableDiv, businessNames, task);
                if (rank > 0) return rank;

                scrollToBottom(driver, scrollableDiv, task);

                rank = extractBusinessRank(driver, scrollableDiv, businessNames, task);
                if (rank > 0) return rank;

            } while (goToNextPage(driver, pageNum++, task));
            log.warn("⚠ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, 을(를) 찾지 못했습니다. 꼴등 처리.", task.getQueueName(), task.getIndex(), keyword, businessName, task.getCurrentRank());
            driver.switchTo().defaultContent();
            return businessNames.size() + 1;

        } catch (Exception e) {
            log.error("❌ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, [ERROR] 스크롤 중 오류", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getQueueName(), task.getCurrentRank());
            return task.getCurrentRank();
        }
    }

    public static int findScrollableDivOrHandleNoResult(WebDriver driver, CrawlPlaceDto task) {
        try {
            // "조건에 맞는 업체가 없습니다." 메시지가 있는지 확인
            WebElement noResultDiv = driver.findElement(By.className("FYvSc"));
            String message = noResultDiv.getText().trim();

            if ("조건에 맞는 업체가 없습니다.".equals(message)) {
                log.warn("⚠ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, 조건에 맞는 업체가 없습니다. 999 번째", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank());
                return 999;
            }
        } catch (NoSuchElementException e) {
            // 메시지 못 찾은 경우 → 정상 처리 계속
            log.error("⚠ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, 조건에 맞는 업체가 없습니다. 999 요소없음 에러", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank());
        } catch (Exception e) {
            // 예외 발생 시 fallback 반환
            log.error("⚠ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, 조건에 맞는 업체가 없습니다. 999 관련 에러", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank());
        }

        return task.getCurrentRank();  // 기본적으로 계속 진행
    }



    private static WebElement switchToIframeAndGetScrollContainer(WebDriverWait wait, CrawlPlaceDto task) {
        try {
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("searchIframe")));
            return wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("div#_pcmap_list_scroll_container")));
        } catch (TimeoutException e) {
            log.error("❌ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, iframe 또는 스크롤 컨테이너 로딩 실패", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank());
            return null;
        } catch (Exception e) {
            log.error("❌ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, iframe 전환 중 오류", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank());
            return null;
        }
    }

    public static void scrollToBottom(WebDriver driver, WebElement scrollableDiv, CrawlPlaceDto task) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        while (true) {
            // 1. 7번에 걸쳐 조금씩 스크롤 다운
            for (int i = 0; i < 7; i++) {
                js.executeScript("arguments[0].scrollTop += 250;", scrollableDiv);
                Thread.sleep(200);  // 0.2초 대기
            }
            Thread.sleep(1000);  // 1초 대기

            // 2. 현재 스크롤 위치와 최대 스크롤 높이 구하기
            Long currentScroll = (Long) js.executeScript("return arguments[0].scrollTop;", scrollableDiv);
            Long maxScrollHeight = (Long) js.executeScript(
                    "return arguments[0].scrollHeight - arguments[0].clientHeight;", scrollableDiv
            );

            // 3. 거의 끝까지 내렸는지 체크 (여유 오차: 5)
            if (currentScroll >= maxScrollHeight - 5) {
                log.info("✅ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, 스크롤 끝", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank());
                break;
            }
        }
    }

    private static int extractBusinessRank(WebDriver driver, WebElement scrollableDiv, List<String> businessNames, CrawlPlaceDto task) {
        List<WebElement> liElements = scrollableDiv.findElements(By.cssSelector("ul > li"));

        for (WebElement li : liElements) {
            try {
                List<WebElement> adElements = li.findElements(By.cssSelector("span.place_blind"));
                if (adElements.stream().anyMatch(el -> el.getText().trim().equals("광고"))) continue;

                WebElement nameElement = li.findElement(By.cssSelector("div.place_bluelink span"));
                String name = nameElement.getText().trim();

                if (!businessNames.contains(name)) {
                    businessNames.add(name);
                }

                if (name.equals(task.getBusinessName())) {
                    int rank = businessNames.indexOf(task.getBusinessName()) + 1;
                    log.info("✅ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, 위치 : {}", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank(), rank);
                    driver.switchTo().defaultContent();
                    return rank;
                }

            } catch (Exception e) {
                log.error("⛔ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, 순위 조회중 에러 발생", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank());
            }
        }
        return 0;
    }

    private static boolean goToNextPage(WebDriver driver, int currentPage, CrawlPlaceDto task) {
        try {
            List<WebElement> pages = driver.findElements(By.cssSelector("div.zRM9F > a.mBN2s"));
            int currentIndex = -1;

            for (int i = 0; i < pages.size(); i++) {
                if (Objects.requireNonNull(pages.get(i).getAttribute("class")).contains("qxokY")) {

                    currentIndex = i;
                    break;
                }
            }

            JavascriptExecutor js = (JavascriptExecutor) driver;

            if (currentIndex >= 0 && currentIndex + 1 < pages.size()) {
                WebElement nextPage = pages.get(currentIndex + 1);
                js.executeScript("arguments[0].click();", nextPage);
                log.info("📄 [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, 다음 페이지로 이동 : {}", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank(), currentPage + 1);
                Thread.sleep(2000);
                return true;
            } else {
                WebElement nextGroup = driver.findElement(By.cssSelector("div.zRM9F > a.eUTV2[aria-disabled='false']:last-child"));
                js.executeScript("arguments[0].click();", nextGroup);
                log.info("📄 [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, 마지막 페이지로 이동 : {}", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank(), currentPage + 1);
                Thread.sleep(2000);
                return true;
            }
        } catch (Exception e) {
            log.error("⛔ [{}] index : {}, 키워드: {}, 상호명: {}, 기존순위: {}, 다음 페이지 없음", task.getQueueName(), task.getIndex(), task.getKeyword(), task.getBusinessName(), task.getCurrentRank());
            return false;
        }
    }
}