# 추천 정렬 성능 개선기 — Caffeine 캐시 도입과 DB 인덱스 최적화

> **적용 범위**: 추천순 정렬 API (`GET /api/recommendations`)
> **개선 결과**: 총 응답시간 2.031s → 0.911s **(−55.2%)**

---

## 1. 문제 인식

추천순 정렬을 선택하면 다른 정렬(최신순, 가격순)보다 체감상 응답이 눈에 띄게 느렸다. 원인을 특정하기 위해 `org.springframework.util.StopWatch`를 `RecommendationService.recommend()` 내부에 삽입해 단계별 실행 시간을 직접 측정했다.

```java
StopWatch sw = new StopWatch("추천 정렬");

sw.start("라이프스타일 조회");   // ...
sw.start("후보 매물 전체 조회"); // propertyRepository.findAll(spec)
sw.start("시설 정보 조회");      // buildFacilityMap(candidates)
sw.start("채점 및 정렬");        // 점수 계산 + 정렬
sw.start("페이지 변환");         // PageImpl 생성

log.info("[추천 정렬] 후보 {}건 | 총 {}s\n{}", ...);
```

**측정 결과 (초기, 캐시 미적용)**

```
[추천 정렬] 후보 72041건 | 총 3.885s
  라이프스타일 조회              0.045s
  후보 매물 전체 조회            1.237s   ← 병목 ②
  시설 정보 조회 (N+1 구간)      2.505s   ← 병목 ①
  채점 및 정렬                   0.060s
  페이지 변환                    0.039s
```

두 가지 병목이 명확히 드러났다.

- **병목 ①** `시설 정보 조회 (N+1 구간)`: 전체 시간의 64% 차지
- **병목 ②** `후보 매물 전체 조회`: 후보 72,000건 조회에 1.2s

---

## 2. 원인 분석

### 2-1. N+1 문제의 두 가지 유형

이 프로젝트에서는 N+1 문제가 두 가지 다른 형태로 나타났다.

**유형 A — JPA 지연로딩 N+1** (PR #72에서 해결)

JPA의 `@OneToMany` 연관관계에서 부모 엔티티를 조회한 뒤, 자식 엔티티를 참조할 때마다 추가 쿼리가 발생하는 패턴이다. `JOIN FETCH`나 `@BatchSize`로 해결한다.

```sql
-- 부모 조회 1번
SELECT * FROM properties WHERE ...;

-- 자식(이미지) 조회 N번
SELECT * FROM property_images WHERE property_id = 1;
SELECT * FROM property_images WHERE property_id = 2;
-- ...
```

**유형 B — 명시적 루프 N+1** (이번 개선 대상)

`buildFacilityMap()`에서 후보 매물 목록을 순회하며 고유 행정동마다 `findBySidoAndGugunAndDong()`을 명시적으로 반복 호출하는 패턴이다. JPA 레이어 밖에서 발생하므로 `JOIN FETCH`나 `default_batch_fetch_size`로는 해결되지 않는다.

```java
private Map<String, AreaFacilityCount> buildFacilityMap(List<Property> properties) {
    for (Property property : properties) {
        String key = areaKey(property);
        if (!searchedKeys.add(key)) continue;

        // 고유 동(dong) 수만큼 DB 쿼리 반복 발생
        areaFacilityCountRepository
            .findBySidoAndGugunAndDong(property.getSido(), property.getGugun(), property.getDong())
            .ifPresent(area -> facilityMap.put(key, area));
    }
}
```

후보 72,000건에 걸쳐 수백~수천 개의 고유 행정동이 존재하면, 그 수만큼 SELECT 쿼리가 발생한다.

### 2-2. 풀 테이블 스캔

`findAll(spec)`이 실행하는 쿼리의 WHERE 절은 다음과 같다.

```sql
WHERE status = 'APPROVED'
  AND dong LIKE '하단동%'
```

기존 인덱스 구성:

| 인덱스 | 컬럼 |
|--------|------|
| `idx_properties_region` | `(sido, gugun, dong)` |
| `idx_properties_status` | `(status)` |

`status`와 지역 컬럼이 **별개 인덱스**이기 때문에, 두 조건을 동시에 만족하는 행을 찾을 때 MySQL 옵티마이저는 인덱스 하나만 선택하고 나머지 조건은 행 단위로 필터링한다. 특히 `dong`만 지정되는 경우 `(sido, gugun, dong)` 복합 인덱스는 좌측 선두 컬럼인 `sido`가 없어 활용이 불가능하여 풀 테이블 스캔이 발생한다.

---

## 3. 기술 선정 — Caffeine vs Redis

캐시 도입을 결정한 뒤, 구현 기술로 **Caffeine**과 **Redis** 두 가지를 검토했다.

| 항목 | Caffeine | Redis |
|------|----------|-------|
| 위치 | JVM 내 힙 메모리 | 외부 네트워크 프로세스 |
| 조회 속도 | 나노초 (메모리 직접 접근) | 마이크로초~밀리초 (네트워크 왕복) |
| 운영 비용 | 없음 (라이브러리) | 별도 서버 설치·운영 필요 |
| 서버 재시작 시 | 캐시 초기화 | 데이터 유지 가능 |
| 다중 서버 환경 | 서버마다 별도 캐시 | 공유 캐시 |
| 설정 복잡도 | 낮음 | 높음 (직렬화, 연결 설정 등) |

**Caffeine을 선택한 이유**

캐싱 대상인 `area_facility_counts` 데이터는 다음 특성을 가진다.

- **정적 참조 데이터**: 카카오 로컬 API를 통해 사전 계산된 값으로, 실시간으로 변경되지 않는다.
- **전수 데이터**: 한국 전체 행정동 단위 시설 카운트이며, 데이터 총 건수가 약 3,500개로 고정적이다.

현재 서비스는 **단일 서버** 환경으로, 서버 간 캐시 공유가 필요하지 않다. Redis의 핵심 장점인 네트워크 공유 캐시가 불필요한 상황에서 Redis를 선택하면 오히려 네트워크 홉 비용이 추가되고, 운영 복잡도만 올라간다.

정적 데이터를 단일 서버에서 빠르게 조회하는 상황에서는 JVM 내부 메모리를 직접 활용하는 **Caffeine이 최적 선택**이다. 서버 재시작 시 캐시가 초기화되더라도 첫 번째 호출 이후 곧바로 캐시가 채워지므로 실용적인 문제가 없다.

---

## 4. 구현

### 4-1. 의존성 추가

```groovy
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-cache'
implementation 'com.github.ben-manes.caffeine:caffeine'
```

Spring Boot의 `spring-boot-starter-cache`는 `@Cacheable` 등 캐시 추상화 어노테이션을 제공한다. Caffeine은 실제 캐시 구현체로, 이 두 라이브러리를 함께 사용하면 Spring이 자동으로 Caffeine을 캐시 구현체로 선택한다.

### 4-2. 캐시 설정

```java
@Configuration
@EnableCaching  // 애플리케이션 전체에서 @Cacheable 어노테이션 활성화
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("areaFacility");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(5000)           // 최대 항목 수
                .expireAfterWrite(1, TimeUnit.HOURS)); // 1시간 후 만료
        return manager;
    }
}
```

### 4-3. 캐시 적용

```java
public interface AreaFacilityCountRepository extends JpaRepository<AreaFacilityCount, Long> {

    @Cacheable(value = "areaFacility", key = "#sido + '|' + #gugun + '|' + #dong")
    Optional<AreaFacilityCount> findBySidoAndGugunAndDong(String sido, String gugun, String dong);
}
```

`@Cacheable`은 메서드 호출 전 캐시를 먼저 확인하고, 캐시 미스(miss)인 경우에만 실제 DB 쿼리를 실행한 뒤 결과를 캐시에 저장한다. key는 `sido|gugun|dong` 형태의 문자열로 구성해 행정동 단위로 구분한다.

---

## 5. 시행착오 ① — maximumSize=1000의 함정

### 현상

초기 설정인 `maximumSize=1000`으로 적용 후 측정한 결과, N+1 구간이 예상보다 훨씬 적게 개선됐다.

```
[캐시 적용 후, maximumSize=1000] 안정 구간 평균 (3~10회차)

  시설 정보 조회 (N+1 구간)    0.757s   ← 기대: ~0.001s
```

또한 연속 호출 시 N+1 구간이 **서서히** 줄어드는 패턴이 관찰됐다.

```
1회차: 1.257s
2회차: 1.198s
3회차: 0.924s
4회차: 0.836s
5회차: 0.820s
6회차: 0.671s   ← 이 시점에서 안정화
...
```

캐시 히트가 즉시 이루어진다면 처음부터 0에 가까워야 한다. 서서히 줄어드는 패턴은 **LRU(Least Recently Used) eviction이 지속적으로 발생**하고 있다는 신호였다.

### 원인 분석

Caffeine은 `maximumSize`를 초과하면 오래된 항목을 자동으로 제거(eviction)한다. 문제는 후보 매물 72,000건에 걸쳐 등장하는 **고유 행정동 수가 1,000개를 초과**한다는 것이었다.

```
후보 매물: 72,041건
고유 행정동 수: 1,000개 초과 (한국 전체 행정동 수 ≈ 3,500개)

→ maximumSize=1000으로는 전체 행정동을 캐시에 수용 불가
→ 조회할 때마다 새로운 동이 캐시에 들어오면서 기존 항목이 밀려남
→ 사실상 캐시 효과가 호출마다 부분적으로만 발휘됨
```

### 해결

한국 전체 행정동 수(약 3,500개)를 넉넉히 수용할 수 있도록 `maximumSize=5000`으로 상향했다.

```java
Caffeine.newBuilder()
    .maximumSize(5000)  // 1000 → 5000
    .expireAfterWrite(1, TimeUnit.HOURS)
```

**메모리 영향**: `AreaFacilityCount` 객체 1개당 약 200~300 bytes 수준이므로, 5,000개 기준 최대 **1.5MB** 수준으로 JVM 힙에 미치는 영향은 미미하다.

---

## 6. 시행착오 ② — DB 복합 인덱스 3072 bytes 오류

### 시도한 방법

JPA 엔티티에 `@Table(indexes = ...)` 어노테이션을 추가해 Hibernate `ddl-auto: update`가 서버 재시작 시 자동으로 인덱스를 생성하도록 시도했다.

```java
@Entity
@Table(
    name = "properties",
    indexes = {
        @Index(name = "idx_properties_status_region", columnList = "status, sido, gugun, dong"),
        @Index(name = "idx_properties_status_dong",   columnList = "status, dong"),
    }
)
public class Property { ... }
```

### 오류 발생

```
java.sql.SQLSyntaxErrorException: Specified key was too long; max key length is 3072 bytes
```

### 원인 분석

`@Column(length = ...)` 속성을 지정하지 않으면, Hibernate는 `String` 타입 필드를 기본값인 **`VARCHAR(255)`** 로 관리한다. `schema.sql`에는 `VARCHAR(50)`으로 정의되어 있지만, Hibernate가 `ddl-auto: update`로 컬럼을 다루는 기준은 엔티티 어노테이션이다.

```
MySQL utf8mb4: 문자당 최대 4 bytes
VARCHAR(255) × 4 bytes = 1,020 bytes / 컬럼

(status + sido + gugun + dong) = 1,020 × 4 = 4,080 bytes
→ InnoDB 최대 인덱스 키 길이 3,072 bytes 초과
```

### 해결

컬럼 전체가 아닌 **앞 N자만 인덱싱**하는 prefix index로 전환했다. 한국 행정구역명은 어떤 경우도 50자를 넘지 않으므로, `sido(50)` prefix는 full-column index와 동일한 선택도를 보장한다.

```sql
-- Hibernate DDL 대신 MySQL에서 직접 실행
ALTER TABLE properties
    ADD INDEX idx_properties_status_region (status(20), sido(50), gugun(50), dong(50));
-- 바이트 계산: (20 + 50 + 50 + 50) × 4 = 680 bytes ✓

ALTER TABLE properties
    ADD INDEX idx_properties_status_dong (status(20), dong(50));
-- 바이트 계산: (20 + 50) × 4 = 280 bytes ✓
```

이 과정에서 `@Table(indexes = ...)` 어노테이션을 통한 Hibernate DDL 방식의 한계를 확인했다. 실제 DB 컬럼 정의와 엔티티 어노테이션 사이의 불일치가 예상치 못한 오류를 유발할 수 있으므로, 스키마 변경은 명시적인 SQL 마이그레이션 파일(`migrate_add_indexes.sql`)로 관리하는 것이 더 안전하다.

---

## 7. 최종 성능 비교

StopWatch 기반 실측값 (안정 구간 평균, 2~10회차 기준)

| 단계 | 캐시 전 | Caffeine size=1000 | Caffeine size=5000 |
|------|---:|---:|---:|
| 라이프스타일 조회 | 0.001s | 0.001s | 0.002s |
| 후보 매물 전체 조회 | 0.870s | 0.844s | **0.826s** |
| 시설 정보 조회 (N+1) | **1.112s** | 0.757s | **0.025s** |
| 채점 및 정렬 | 0.041s | 0.044s | 0.052s |
| 페이지 변환 | 0.006s | 0.006s | 0.006s |
| **총** | **2.031s** | 1.650s | **0.911s** |

| 지표 | 개선량 | 개선률 |
|------|---:|---:|
| N+1 구간 | 1.112s → 0.025s | **−97.8%** |
| 총 응답시간 | 2.031s → 0.911s | **−55.2%** |

`size=5000` 적용 후 N+1 구간에 남은 0.025s는 DB 쿼리가 없는 상태에서 발생하는 캐시 Map 조회 및 객체 접근 비용으로, 사실상 제거된 것으로 볼 수 있다.

---

## 8. 회고

### 수치로 검증하는 습관

처음에는 "느리다"는 체감에서 출발했다. StopWatch 측정을 통해 전체 3.8s 중 2.5s가 시설 정보 조회에서 발생한다는 것을 수치로 특정할 수 있었고, 그 덕분에 잘못된 곳을 최적화하는 낭비를 피할 수 있었다. **측정 없는 최적화는 추측일 뿐이다.**

### 캐시 설계 시 데이터 카디널리티를 먼저 파악할 것

`maximumSize=1000` 시행착오의 교훈이다. 캐시를 설계하기 전에 **"이 키는 몇 종류나 존재하는가?"** 를 먼저 파악해야 한다. 캐시 크기가 실제 데이터 카디널리티보다 작으면 지속적인 eviction으로 캐시 효과가 반감된다. 이 경우 행정동 수를 사전에 파악했더라면 처음부터 5000으로 설정할 수 있었다.

### Hibernate DDL과 실제 DB 스키마의 불일치

`@Column(length = ...)` 미지정 문제는 엔티티와 실제 DB 스키마가 서로 다른 기준으로 관리될 때 발생하는 전형적인 불일치다. `ddl-auto: update`는 편리하지만 세부 스키마 제어가 어렵기 때문에, 인덱스·컬럼 타입 같은 민감한 변경은 Flyway나 Liquibase 같은 마이그레이션 도구로 명시적으로 관리하는 것이 장기적으로 안전하다.
