package com.vision.cralwingvps.service;

import com.vision.cralwingvps.dto.CrawlPlaceDto;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
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
            searchInput.sendKeys(keyword);
            Thread.sleep(500);

            // Enter 입력하여 검색 실행
            searchInput.sendKeys(Keys.ENTER);
            Thread.sleep(2000); // 검색 결과 로딩 대기

            WebElement scrollableDiv = switchToIframeAndGetScrollContainer(wait);
            if (scrollableDiv == null) {
                log.warn("❌ iframe 전환 실패 키워드: {}, 상호명 : {}", keyword, businessName);
                driver.switchTo().defaultContent();
                return task.getCurrentRank();
            }

            List<String> businessNames = new ArrayList<>();
            int pageNum = 1;

            do {
                int rank = extractBusinessRank(driver, scrollableDiv, businessNames, businessName);
                if (rank > 0) return rank;

                scrollToBottom(driver, scrollableDiv);

                rank = extractBusinessRank(driver, scrollableDiv, businessNames, businessName);
                if (rank > 0) return rank;

            } while (goToNextPage(driver, pageNum++));

            log.warn("⚠ '{}'을(를) 찾지 못했습니다. 꼴등 처리.", businessName);
            driver.switchTo().defaultContent();
            return businessNames.size() + 1;

        } catch (Exception e) {
            log.error("❌ [ERROR] 스크롤 중 오류: {}", e.getMessage());
            return task.getCurrentRank();
        }
    }

    private static WebElement switchToIframeAndGetScrollContainer(WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("searchIframe")));
            return wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("div#_pcmap_list_scroll_container")));
        } catch (TimeoutException e) {
            log.warn("❌ iframe 또는 스크롤 컨테이너 로딩 실패");
            return null;
        } catch (Exception e) {
            log.error("❌ iframe 전환 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }

    private static void scrollToBottom(WebDriver driver, WebElement scrollableDiv) throws InterruptedException {
        Actions actions = new Actions(driver);
        actions.moveToElement(scrollableDiv).perform();
        Thread.sleep(1000);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollTop = arguments[0].scrollHeight;", scrollableDiv);
        Thread.sleep(500);
    }

    private static int extractBusinessRank(WebDriver driver, WebElement scrollableDiv, List<String> businessNames, String targetName) {
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

                if (name.equals(targetName)) {
                    int rank = businessNames.indexOf(targetName) + 1;
                    log.info("✅ '{}'의 위치: {}번째", targetName, rank);
                    driver.switchTo().defaultContent();
                    return rank;
                }

            } catch (Exception ignored) {
                log.info("⛔ 순위 조회중 에러 발생");
            }
        }
        return 0;
    }

    private static boolean goToNextPage(WebDriver driver, int currentPage) {
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
                log.info("📄 다음 페이지로 이동: {}", currentPage + 1);
                Thread.sleep(2000);
                return true;
            } else {
                WebElement nextGroup = driver.findElement(By.cssSelector("div.zRM9F > a.eUTV2[aria-disabled='false']:last-child"));
                js.executeScript("arguments[0].click();", nextGroup);
                log.info("📄 다음 그룹 페이지로 이동");
                Thread.sleep(2000);
                return true;
            }
        } catch (Exception e) {
            log.info("⛔ 다음 페이지 없음");
            return false;
        }
    }
}