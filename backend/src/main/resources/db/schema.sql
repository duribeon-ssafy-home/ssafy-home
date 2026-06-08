-- =============================================
-- SSAFY HOME Database Schema
-- =============================================

CREATE DATABASE IF NOT EXISTS ssafy_home
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ssafy_home;

-- =============================================
-- 1. USERS
-- role  : BUYER | AGENT | ADMIN
-- status: ACTIVE | SUSPENDED | WITHDRAWN
-- =============================================
CREATE TABLE IF NOT EXISTS users (
    user_id    BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(100) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    nickname   VARCHAR(50)  NULL,
    phone      VARCHAR(20)  NULL,
    role       VARCHAR(10)  NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 2. PROPERTIES
-- rent_type  : JEONSE | MONTHLY
-- room_type  : ONE_ROOM | TWO_ROOM | OFFICETEL | APARTMENT
-- data_source: AGENT | PUBLIC
-- status     : PENDING | APPROVED | REJECTED | HIDDEN | DELETED
-- latitude/longitude: 전처리(Kakao Geocoding API) 후 채워짐
-- =============================================
CREATE TABLE IF NOT EXISTS properties (
    property_id  BIGINT        NOT NULL AUTO_INCREMENT,
    owner_id     BIGINT        NULL,                      -- PUBLIC 데이터는 NULL
    title        VARCHAR(255)  NULL,
    address      VARCHAR(255)  NOT NULL,
    road_address VARCHAR(255)  NULL,
    sido         VARCHAR(50)   NOT NULL,
    gugun        VARCHAR(50)   NOT NULL,
    dong         VARCHAR(50)   NOT NULL,
    latitude     DECIMAL(10,7) NULL,                      -- 전처리 후 채워짐
    longitude    DECIMAL(10,7) NULL,                      -- 전처리 후 채워짐
    rent_type    VARCHAR(10)   NULL,
    room_type    VARCHAR(20)   NULL,
    deposit      BIGINT        NULL,                      -- 보증금 (만원)
    monthly_rent INT           NULL,                      -- 월세 (만원), MONTHLY일 때만
    area         DECIMAL(6,2)  NULL,                      -- 전용면적 m²
    floor        SMALLINT      NULL,                      -- 층수
    build_year   SMALLINT      NULL,                      -- 건축년도
    data_source  VARCHAR(20)   NOT NULL,
    deal_date    DATE          NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (property_id),
    CONSTRAINT fk_properties_owner
        FOREIGN KEY (owner_id) REFERENCES users (user_id) ON DELETE SET NULL,
    INDEX idx_properties_region (sido, gugun, dong),
    INDEX idx_properties_type   (rent_type, room_type),
    INDEX idx_properties_status (status),
    INDEX idx_properties_price  (deposit, monthly_rent)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 3. PROPERTY_IMAGES
-- =============================================
CREATE TABLE IF NOT EXISTS property_images (
    image_id     BIGINT       NOT NULL AUTO_INCREMENT,
    property_id  BIGINT       NOT NULL,
    image_url    VARCHAR(500) NOT NULL,
    is_thumbnail BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order   INT          NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (image_id),
    CONSTRAINT fk_property_images_property
        FOREIGN KEY (property_id) REFERENCES properties (property_id) ON DELETE CASCADE,
    INDEX idx_property_images_property (property_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 4. FAVORITES
-- =============================================
CREATE TABLE IF NOT EXISTS favorites (
    favorite_id BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    property_id BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (favorite_id),
    UNIQUE KEY uq_favorites (user_id, property_id),
    CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_property
        FOREIGN KEY (property_id) REFERENCES properties (property_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 5. LIFESTYLE_RESULTS
-- lifestyle_type: 3개 축 조합으로 분류한 8가지 생활 성향 유형
-- =============================================
CREATE TABLE IF NOT EXISTS lifestyle_results (
    lifestyle_result_id       BIGINT        NOT NULL AUTO_INCREMENT,
    user_id                   BIGINT        NOT NULL,
    lifestyle_type            VARCHAR(50)   NOT NULL,
    living_convenience_score  INT           NOT NULL DEFAULT 0,
    cost_sensitivity_score    INT           NOT NULL DEFAULT 0,
    home_quality_score        INT           NOT NULL DEFAULT 0,
    facility_score_min        INT           NULL,
    facility_count_min        INT           NULL,
    monthly_rent_max          INT           NULL,
    deposit_max               BIGINT        NULL,
    area_min                  DECIMAL(6,2)  NULL,
    build_year_min            INT           NULL,
    created_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (lifestyle_result_id),
    CONSTRAINT fk_lifestyle_results_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    INDEX idx_lifestyle_results_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 6. PROPERTY_RECOMMEND_SCORES
-- MVP에서는 실시간 계산 후 반환이 기본 동작.
-- 이 테이블은 선택적 캐시 레이어 — 다른 테이블이 FK 참조하지 않으므로
-- 성능 이슈 없을 시 DROP 가능.
-- =============================================
CREATE TABLE IF NOT EXISTS property_recommend_scores (
    recommend_score_id BIGINT   NOT NULL AUTO_INCREMENT,
    user_id            BIGINT   NOT NULL,
    property_id        BIGINT   NOT NULL,
    total_score        INT      NOT NULL DEFAULT 0,
    distance_score     INT      NOT NULL DEFAULT 0,
    station_score      INT      NOT NULL DEFAULT 0,
    facility_score     INT      NOT NULL DEFAULT 0,
    lifestyle_score    INT      NOT NULL DEFAULT 0,
    calculated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (recommend_score_id),
    UNIQUE KEY uq_recommend_scores (user_id, property_id),
    CONSTRAINT fk_recommend_scores_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_recommend_scores_property
        FOREIGN KEY (property_id) REFERENCES properties (property_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 7. AREA_FACILITY_COUNTS
-- 동(dong) 단위 주변 시설 카운트 (카카오 로컬 API 전처리 결과)
-- center_lat/lng: 해당 동 소속 매물들의 위경도 평균값
-- 동네 추천 기능에서 동 단위 시설 환경 비교에 사용
-- =============================================
CREATE TABLE IF NOT EXISTS area_facility_counts (
    area_id                BIGINT        NOT NULL AUTO_INCREMENT,
    sido                   VARCHAR(50)   NOT NULL,
    gugun                  VARCHAR(50)   NOT NULL,
    dong                   VARCHAR(50)   NOT NULL,
    center_lat             DECIMAL(10,7) NOT NULL,
    center_lng             DECIMAL(10,7) NOT NULL,
    subway_count_500m      INT           NULL,
    mart_count_1km         INT           NULL,
    convenience_count_500m INT           NULL,
    hospital_count_1km     INT           NULL,
    pharmacy_count_500m    INT           NULL,
    cafe_count_500m        INT           NULL,
    restaurant_count_500m  INT           NULL,
    calculated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (area_id),
    UNIQUE KEY uq_area (sido, gugun, dong)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 8. PROPERTY_RISK_SCORES
-- risk_label: SAFE | CAUTION | DANGER | UNKNOWN
-- property_id UNIQUE → PROPERTIES 1:1 관계
-- =============================================
CREATE TABLE IF NOT EXISTS property_risk_scores (
    risk_score_id    BIGINT        NOT NULL AUTO_INCREMENT,
    property_id      BIGINT        NOT NULL,
    risk_label       VARCHAR(10)   NOT NULL DEFAULT 'UNKNOWN',
    risk_score       INT           NOT NULL DEFAULT 0,
    market_price_avg DECIMAL(15,2) NULL,
    price_gap_rate   DECIMAL(5,2)  NULL,
    report_count     INT           NOT NULL DEFAULT 0,
    calculated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (risk_score_id),
    UNIQUE KEY uq_risk_scores_property (property_id),
    CONSTRAINT fk_risk_scores_property
        FOREIGN KEY (property_id) REFERENCES properties (property_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 8. REPORTS
-- reason: FAKE_LISTING | PRICE_MISMATCH | PHOTO_MISMATCH | NO_CONTACT | FRAUD_SUSPECTED | ETC
-- status: PENDING | RESOLVED | CAUTION | DANGER | HIDDEN
-- 같은 사용자가 같은 매물을 중복 신고할 수 없다.
-- =============================================
CREATE TABLE IF NOT EXISTS reports (
    report_id    BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    property_id  BIGINT      NOT NULL,
    reason       VARCHAR(30) NOT NULL,
    content      TEXT        NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME    NULL,
    PRIMARY KEY (report_id),
    UNIQUE KEY uq_reports (user_id, property_id),
    CONSTRAINT fk_reports_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_property
        FOREIGN KEY (property_id) REFERENCES properties (property_id) ON DELETE CASCADE,
    INDEX idx_reports_property (property_id),
    INDEX idx_reports_status   (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 9. BLACKLISTS
-- status: ACTIVE | RELEASED
-- =============================================
CREATE TABLE IF NOT EXISTS blacklists (
    blacklist_id BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    reason       VARCHAR(255) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at  DATETIME     NULL,
    PRIMARY KEY (blacklist_id),
    CONSTRAINT fk_blacklists_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    INDEX idx_blacklists_user   (user_id),
    INDEX idx_blacklists_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
