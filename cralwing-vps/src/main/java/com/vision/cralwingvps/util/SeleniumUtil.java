package com.vision.cralwingvps.util;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SeleniumUtil {

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
            String userProfilePath;

            switch (browserType) {
                case "chrome" -> {
                    browserPath = "C:/Program Files/Google/Chrome/Application/chrome.exe";
                    driverPath = "D:/driver/chrome/chromedriver-win64/chromedriver.exe";
                    // ✅ 실제 Chrome 사용자 프로필 경로
                    userProfilePath = "C:/Users/772vj/AppData/Local/Google/Chrome/User Data";
                }
                case "brave" -> {
                    browserPath = "C:/Program Files/BraveSoftware/Brave-Browser/Application/brave.exe";
                    driverPath = "D:/driver/brave/chromedriver-win64/chromedriver.exe";
                    // ✅ 실제 Brave 사용자 프로필 경로
                    userProfilePath = "C:/Users/772vj/AppData/Local/BraveSoftware/Brave-Browser/User Data";
                }
                default -> throw new IllegalArgumentException("지원하지 않는 브라우저: " + browserType);
            }

            System.setProperty("webdriver.chrome.driver", driverPath);
            options.setBinary(new File(browserPath));
            options.addArguments("--start-maximized");

            // 자동화 감지 우회 옵션
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--remote-allow-origins=*");
            options.setExperimentalOption("useAutomationExtension", false);
            options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));

            // ✅ 실제 사용자 프로필 경로 지정
            options.addArguments("user-data-dir=" + userProfilePath);

            ChromeDriver driver = new ChromeDriver(ChromeDriverService.createDefaultService(), options);

            // navigator.webdriver 우회
            String script = "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });";
            Map<String, Object> params = new HashMap<>();
            params.put("source", script);
            driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", params);

            log.info("✅ {} WebDriver (실제 사용자 프로필) 생성 완료", browserType);
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

            // ✅ 실제 로그인된 Firefox 사용자 프로필 경로
            String realProfilePath = "C:/Users/772vj/AppData/Roaming/Mozilla/Firefox/Profiles/4vbvqpsl.default-release";

            options.addArguments("-profile");
            options.addArguments(realProfilePath);

            FirefoxDriver driver = new FirefoxDriver(options);

            // navigator.webdriver 감추기
            driver.get("about:blank");
            driver.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

            log.info("✅ Firefox WebDriver (실제 사용자 프로필) 생성 완료");
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
