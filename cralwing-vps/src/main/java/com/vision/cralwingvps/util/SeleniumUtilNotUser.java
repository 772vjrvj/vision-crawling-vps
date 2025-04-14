package com.vision.cralwingvps.util;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class SeleniumUtilNotUser {

    public static WebDriver getUserChromeDriver() {
        return createChromeLikeDriver("chrome");
    }

    public static WebDriver getUserBraveDriver() {
        return createChromeLikeDriver("brave");
    }

    public static WebDriver getUserFirefoxDriver() {
        return createFirefoxDriver();
    }

    private static WebDriver createChromeLikeDriver(String browserType) {
        try {
            ChromeOptions options = new ChromeOptions();

            String browserPath;
            String driverPath;

            switch (browserType) {
                case "chrome" -> {
                    browserPath = "C:/Program Files/Google/Chrome/Application/chrome.exe";
                    driverPath = "D:/driver/chrome/chromedriver-win64/chromedriver.exe";
                }
                case "brave" -> {
                    browserPath = "C:/Program Files/BraveSoftware/Brave-Browser/Application/brave.exe";
                    driverPath = "D:/driver/brave/chromedriver-win64/chromedriver.exe";
                }
                default -> throw new IllegalArgumentException("지원하지 않는 브라우저: " + browserType);
            }

            System.setProperty("webdriver.chrome.driver", driverPath);
            options.setBinary(new File(browserPath));
            options.addArguments("--start-maximized");

            // 자동화 감지 우회용 추가 옵션
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--remote-allow-origins=*");

            // 자동 확장 로딩 비활성화
            options.setExperimentalOption("useAutomationExtension", false);
            options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));

            // 프로필 디렉토리 생성
            File tempDir = Files.createTempDirectory(browserType + "-profile-" + UUID.randomUUID()).toFile();
            options.addArguments("user-data-dir=" + tempDir.getAbsolutePath());
            log.info("🧪 [{}] user-data-dir 경로: {}", browserType, tempDir.getAbsolutePath());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    FileUtils.deleteDirectory(tempDir);
                    log.info("🧹 {} 임시 프로필 디렉토리 삭제 완료: {}", browserType, tempDir.getAbsolutePath());
                } catch (IOException ex) {
                    log.warn("⚠ {} 임시 디렉토리 삭제 실패: {}", browserType, ex.getMessage());
                }
            }));

            // WebDriver 생성
            ChromeDriver driver = new ChromeDriver(ChromeDriverService.createDefaultService(), options);

            // navigator.webdriver = false 설정
            String script = "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });";
            Map<String, Object> params = new HashMap<>();
            params.put("source", script);
            driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", params);

            log.info("✅ {} WebDriver 생성 완료", browserType);
            return driver;

        } catch (Exception e) {
            log.error("❌ {} WebDriver 생성 실패: {}", browserType, e.getMessage(), e);
            return null;
        }
    }

    private static WebDriver createFirefoxDriver() {
        try {
            WebDriverManager.firefoxdriver().setup();

            FirefoxOptions options = new FirefoxOptions();
            options.setBinary("C:/Program Files/Mozilla Firefox/firefox.exe");

            // 임시 프로필 생성
            File tempDir = Files.createTempDirectory("firefox-profile").toFile();
            options.addArguments("-profile");
            options.addArguments(tempDir.getAbsolutePath());

            // 자동화 감지 우회용 환경 설정
            options.addPreference("dom.webdriver.enabled", false);
            options.addPreference("useAutomationExtension", false);
            options.addPreference("media.navigator.enabled", false);
            options.addPreference("media.peerconnection.enabled", false);
            options.addPreference("privacy.resistFingerprinting", true); // 지문방지

            log.info("✅ Firefox 임시 프로필 경로 적용: {}", tempDir.getAbsolutePath());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    FileUtils.deleteDirectory(tempDir);
                    log.info("🧹 Firefox 임시 프로필 디렉토리 삭제 완료: {}", tempDir.getAbsolutePath());
                } catch (IOException ex) {
                    log.warn("⚠ 임시 디렉토리 삭제 실패: {}", ex.getMessage());
                }
            }));

            FirefoxDriver driver = new FirefoxDriver(options);

            // navigator.webdriver 우회: 페이지에 스크립트 삽입
            driver.get("about:blank");
            ((FirefoxDriver) driver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

            log.info("✅ Firefox WebDriver 생성 완료");
            return driver;
        } catch (Exception e) {
            log.error("❌ FirefoxDriver 생성 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    public static void killBrowserProcesses() {
        String[] browserProcesses = {"chrome", "brave", "firefox"};

        for (String browser : browserProcesses) {
            try {
                String cmd = "taskkill /F /IM " + browser + ".exe /T";
                Process process = Runtime.getRuntime().exec(cmd);
                process.waitFor();
                log.info("✅ {} 프로세스 종료 완료", browser);
            } catch (Exception e) {
                log.warn("⚠ {} 프로세스 종료 실패 (이미 종료되었거나 존재하지 않을 수 있음): {}", browser, e.getMessage());
            }
        }
    }
}
