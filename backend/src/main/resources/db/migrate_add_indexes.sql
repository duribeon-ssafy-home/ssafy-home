-- 추천 정렬 쿼리 성능 개선용 복합 인덱스
-- 실행 환경: MySQL에서 직접 실행 (Hibernate DDL로 생성하지 않음)

USE ssafy_home;

-- status + 전체 지역 필터 (sido/gugun/dong 모두 지정 시)
-- prefix 길이: status(20), sido/gugun/dong(50) → 최대 680 bytes (utf8mb4)
-- 실제 컬럼이 VARCHAR(255)이므로 prefix 필수
ALTER TABLE properties
    ADD INDEX idx_properties_status_region (status(20), sido(50), gugun(50), dong(50));

-- status + dong 필터 (dong만 지정 시, ex: "하단동" 검색 후 추천정렬)
ALTER TABLE properties
    ADD INDEX idx_properties_status_dong (status(20), dong(50));
