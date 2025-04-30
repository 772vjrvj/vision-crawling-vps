CREATE TABLE BATCH_CRAWL_LOG (
                                 LOG_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 BATCH_TYPE VARCHAR(20) NOT NULL,
                                 TOTAL_COUNT INT,
                                 SUCCESS_COUNT INT,
                                 FAIL_COUNT INT,
                                 EQ_COUNT INT,
                                 DF_COUNT INT,
                                 STARTED_AT DATETIME,
                                 ENDED_AT DATETIME,
                                 STATUS VARCHAR(20),
                                 MESSAGE TEXT
);

CREATE TABLE PLACE_LOG (
                           PLACE_LOG_NO BIGINT AUTO_INCREMENT PRIMARY KEY,
                           LOG_ID BIGINT,
                           NO INT,
                           REG_DT VARCHAR(255) NOT NULL,
                           BUSINESS_NAME VARCHAR(500) NOT NULL,
                           PLACE_NUMBER INT NOT NULL,
                           KEYWORD VARCHAR(500),
                           CATEGORY VARCHAR(100),
                           INITIAL_RANK INT,
                           HIGHEST_RANK INT,
                           RECENT_RANK INT,
                           CURRENT_RANK INT,
                           EMP_NAME VARCHAR(100),
                           BLOG_REVIEWS INT,
                           VISITOR_REVIEWS INT,
                           ADVERTISEMENT VARCHAR(100),
                           RANK_CHK_DT VARCHAR(100),
                           DELETED_YN CHAR(1),
                           EMP_ID VARCHAR(100),
                           HIGHEST_DT VARCHAR(100),
                           CRAWL_YN CHAR(1),
                           CORRECT_YN CHAR(1),
                           STARTED_AT DATETIME,
                           ENDED_AT DATETIME
);



SELECT * FROM PLACE_LOG
