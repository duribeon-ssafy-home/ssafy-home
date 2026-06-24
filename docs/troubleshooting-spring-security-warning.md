# 트러블 슈팅: Spring Boot 시작 시 Security 경고

## 증상

서버를 시작할 때마다 아래 경고가 출력됨.

```
WARN --- [main] .s.s.UserDetailsServiceAutoConfiguration :

Using generated security password: 4f3e2a1b-xxxx-xxxx-xxxx-xxxxxxxxxxxx

This generated password is for development use only. Your security configuration must be updated before running your application in production.
```

---

## 분석 과정

### 1단계: 경고 발생 원인 파악

`UserDetailsServiceAutoConfiguration`은 Spring Boot가 Security 의존성을 발견했을 때, `UserDetailsService` 빈이 없으면 자동으로 기본 유저를 생성하는 Auto-Configuration이다.

```
spring-boot-starter-security 존재
  → UserDetailsServiceAutoConfiguration 활성화
  → 기본 InMemoryUserDetailsManager 생성 시도
  → 랜덤 패스워드 발급 + 경고 출력
```

이 프로젝트는 JWT 기반 인증을 직접 구현하고 있어 `UserDetailsService`를 별도로 정의하지 않았다. Spring Boot는 이 경우를 "아직 설정이 없다"고 판단해 기본 유저를 만들려고 시도한 것.

### 2단계: 해결 방법 탐색

세 가지 방법이 있다.

| 방법 | 방식 | 적합 여부 |
|------|------|-----------|
| A. `application.yml`에 username/password 직접 지정 | 기본 유저를 명시적으로 설정 | ❌ JWT 프로젝트에선 의미 없음 |
| B. `UserDetailsService` 빈 직접 구현 | Auto-Configuration 조건 충족 | ⚠️ 불필요한 코드 추가 |
| C. Auto-Configuration 자체를 exclude | 해당 클래스를 아예 제외 | ✅ 의도가 명확하고 깔끔함 |

### 3단계: C 방법 선택 이유

JWT 인증 환경에서는 세션 기반 `UserDetailsService` 자체가 불필요하다. 불필요한 Auto-Configuration을 exclude 하는 것이 Spring Boot의 설계 의도에 맞고, 코드 추가 없이 한 줄로 해결된다.

---

## 해결

```java
// SsafyHomeApplication.java

import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SsafyHomeApplication {
    public static void main(String[] args) {
        SpringApplication.run(SsafyHomeApplication.class, args);
    }
}
```

---

## 면접에서 말할 수 있는 포인트

> "서버 시작 시 `Using generated security password` 경고가 계속 출력됐습니다. JWT 기반 인증을 사용하면서 `UserDetailsService`를 정의하지 않았는데, Spring Boot가 이를 설정 누락으로 판단해 자동으로 기본 유저를 만들려 했던 것입니다."

> "세션 기반 인증 빈을 억지로 만들거나 application.yml에 더미 계정을 넣는 대신, 해당 Auto-Configuration 자체를 `exclude` 했습니다. JWT 환경에서는 이 Auto-Configuration이 처음부터 필요 없기 때문입니다."

> "이 경험을 통해 Spring Boot의 조건부 Auto-Configuration 동작 방식을 이해하게 됐습니다. `@ConditionalOnMissingBean`으로 특정 빈이 없을 때만 활성화되는 방식입니다."

---

## 관련 개념

- `@SpringBootApplication` = `@EnableAutoConfiguration` + `@ComponentScan` + `@SpringBootConfiguration`
- `exclude` 속성: 특정 Auto-Configuration 클래스를 Bean 등록 대상에서 제외
- `@ConditionalOnMissingBean`: 조건부 등록의 핵심 — 빈이 없을 때만 등록
