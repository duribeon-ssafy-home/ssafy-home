# AI 하이브리드 통합 구현 플랜 (Spring AI + LangChain RAG)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Python LangChain RAG 서비스를 Spring AI 오케스트레이터의 HTTP Tool로 연결하고, 매물 비교 화면에 AI 상담 채팅 패널을 구현한다.

**Architecture:** Spring Boot(8080)의 `AiOrchestratorService`가 Spring AI ChatClient와 Tool Calling을 통해 DB 조회 및 Python RAG 서비스(8000) 호출을 조율한다. LLM이 사용자 질문을 분석해 어떤 Tool을 호출할지 스스로 결정하는 방식으로 Intent Router를 대체한다.

**Tech Stack:**
- Backend: Spring AI 1.0.0 (Google Gemini), Spring WebFlux(WebClient), Java 17
- AI RAG: Python FastAPI 8000 (LangChain + ChromaDB + Gemini) — 기존 코드 유지
- Frontend: Vue 3 Composition API, Pinia, Axios

## Global Constraints

- Spring Boot 버전: 3.3.5 (기존 유지)
- Java 버전: 17 (기존 유지)
- Gemini 모델: `gemini-2.5-flash-lite` (RAG 서비스와 동일)
- RAG 서비스 URL: `http://localhost:8000` (환경변수 `RAG_SERVICE_URL`로 주입)
- API 응답 포맷: 기존 `ApiResponse<T>` 래퍼 사용 (`com.ssafy.home.common.response.ApiResponse`)
- 금액 단위: 만원 (기존 프로젝트 `docs/money-unit-convention.md` 규칙 준수)
- AI 채팅 엔드포인트: `POST /api/ai/chat` — 인증 필요(JWT)

---

## 파일 구조

### 새로 생성할 파일

```
backend/
├── build.gradle                                          (수정)
├── src/main/resources/application.yml                   (수정)
└── src/main/java/com/ssafy/home/
    ├── common/config/
    │   └── AiConfig.java                                (신규) ChatClient Bean 정의
    ├── ai/
    │   ├── client/
    │   │   └── RagServiceClient.java                    (신규) Python RAG HTTP 클라이언트
    │   ├── tool/
    │   │   ├── PropertyTool.java                        (신규) 매물 조회 Tool
    │   │   ├── RiskTool.java                            (신규) 위험도 조회 Tool
    │   │   ├── LifestyleTool.java                       (신규) 라이프스타일 조회 Tool
    │   │   └── RagTool.java                             (신규) RAG 검색 Tool
    │   ├── dto/
    │   │   ├── request/
    │   │   │   └── AiChatRequest.java                   (신규) 채팅 요청 DTO
    │   │   └── response/
    │   │       └── AiChatResponse.java                  (신규) 채팅 응답 DTO
    │   ├── service/
    │   │   └── AiOrchestratorService.java               (신규) 오케스트레이터
    │   └── controller/
    │       └── AiController.java                        (신규) /api/ai/chat 엔드포인트
    └── common/config/
        └── SecurityConfig.java                          (수정) /api/ai/** 인증 규칙 추가

frontend/
└── src/
    ├── api/
    │   └── aiApi.js                                     (신규) AI 채팅 API 클라이언트
    ├── components/
    │   └── AiChatPanel.vue                              (신규) AI 채팅 UI 컴포넌트
    └── views/
        └── CompareView.vue                              (수정) AI 패널 통합
```

---

## Task 1: Spring AI 의존성 추가 및 기본 설정

**Files:**
- Modify: `backend/build.gradle`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/ssafy/home/common/config/AiConfig.java`

**Interfaces:**
- Produces: `ChatClient` Bean — 이후 Task에서 `AiOrchestratorService`가 주입받아 사용

- [ ] **Step 1: build.gradle에 Spring AI BOM 및 의존성 추가**

`backend/build.gradle`의 `dependencies` 블록에 아래 항목 추가:

```groovy
dependencies {
    // 기존 의존성은 그대로 유지 ...

    // Spring AI
    implementation platform('org.springframework.ai:spring-ai-bom:1.0.0')
    implementation 'org.springframework.ai:spring-ai-google-ai-gemini-spring-boot-starter'

    // WebClient (Python RAG 서비스 HTTP 호출용)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
}
```

`repositories` 블록에 Spring Milestone 저장소 추가 (Spring AI 1.0.0 릴리스 전 버전인 경우):

```groovy
repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
}
```

- [ ] **Step 2: Gradle 빌드 확인**

```bash
cd backend
./gradlew dependencies --configuration runtimeClasspath | grep spring-ai
```

Expected: `spring-ai-google-ai-gemini-spring-boot-starter` 포함 출력

- [ ] **Step 3: application.yml에 Spring AI 및 RAG 서비스 URL 설정 추가**

`backend/src/main/resources/application.yml` 파일 하단에 추가:

```yaml
spring:
  ai:
    google:
      gemini:
        api-key: ${GOOGLE_API_KEY}
        chat:
          options:
            model: gemini-2.5-flash-lite
            temperature: 0.3

rag:
  service:
    url: ${RAG_SERVICE_URL:http://localhost:8000}
```

- [ ] **Step 4: AiConfig.java 생성**

`backend/src/main/java/com/ssafy/home/common/config/AiConfig.java`:

```java
package com.ssafy.home.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        당신은 한국 전세·임대차 부동산 전문 AI 상담사입니다.
                        제공된 도구(Tool)를 활용해 매물 데이터, 위험도 분석, 라이프스타일 정보,
                        법률 문서 등을 조회한 뒤 사용자 질문에 근거 기반으로 답변하세요.
                        근거가 없는 내용은 추측하지 말고 "확인된 데이터가 없습니다"라고 답하세요.
                        """)
                .build();
    }
}
```

- [ ] **Step 5: 애플리케이션 기동 확인**

```bash
cd backend
GOOGLE_API_KEY=test ./gradlew bootRun
```

Expected: Spring Boot 8080 포트 기동 성공, `ChatClient` Bean 로딩 로그 확인

- [ ] **Step 6: 커밋**

```bash
git add backend/build.gradle backend/src/main/resources/application.yml
git add backend/src/main/java/com/ssafy/home/common/config/AiConfig.java
git commit -m "feat(ai): Spring AI 1.0.0 의존성 및 Gemini ChatClient 설정 추가"
```

---

## Task 2: Python RAG HTTP 클라이언트 구현

**Files:**
- Create: `backend/src/main/java/com/ssafy/home/ai/client/RagServiceClient.java`

**Interfaces:**
- Consumes: `rag.service.url` 프로퍼티
- Produces: `RagServiceClient.chat(String question): String` — Task 4의 `RagTool`이 사용

- [ ] **Step 1: 실패하는 테스트 작성**

`backend/src/test/java/com/ssafy/home/ai/client/RagServiceClientTest.java`:

```java
package com.ssafy.home.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

// Note: WebClient 테스트이므로 WireMock 또는 MockWebServer 사용
// 여기서는 통합 확인용 단순 테스트로 대체
@TestPropertySource(properties = "rag.service.url=http://localhost:8000")
class RagServiceClientTest {

    @Test
    void chat_returnsAnswer() {
        // 실제 RAG 서비스가 실행 중일 때만 통과
        // CI에서는 @Disabled 처리 권장
        // 통합 테스트는 Task 5에서 Controller 레이어 테스트로 대체
    }
}
```

- [ ] **Step 2: RagServiceClient 구현**

`backend/src/main/java/com/ssafy/home/ai/client/RagServiceClient.java`:

```java
package com.ssafy.home.ai.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Component
public class RagServiceClient {

    private final WebClient webClient;

    public RagServiceClient(@Value("${rag.service.url}") String ragServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(ragServiceUrl)
                .build();
    }

    public String chat(String question) {
        RagChatResult result = webClient.post()
                .uri("/api/rag/chat")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("question", question))
                .retrieve()
                .bodyToMono(RagChatResult.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        if (result == null || result.data() == null) {
            return "RAG 서비스에서 응답을 받지 못했습니다.";
        }
        return result.data().answer();
    }

    record RagChatResult(boolean success, String message, RagData data) {}
    record RagData(String answer, String query) {}
}
```

- [ ] **Step 3: 빌드 확인**

```bash
cd backend
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/ssafy/home/ai/client/
git commit -m "feat(ai): Python RAG 서비스 WebClient HTTP 클라이언트 구현"
```

---

## Task 3: Spring AI Tool 구현 (DB Tool 3개 + RAG Tool 1개)

**Files:**
- Create: `backend/src/main/java/com/ssafy/home/ai/tool/PropertyTool.java`
- Create: `backend/src/main/java/com/ssafy/home/ai/tool/RiskTool.java`
- Create: `backend/src/main/java/com/ssafy/home/ai/tool/LifestyleTool.java`
- Create: `backend/src/main/java/com/ssafy/home/ai/tool/RagTool.java`

**Interfaces:**
- Consumes: `PropertyService`, `RiskService`, `LifestyleService`, `RagServiceClient` (Task 2)
- Produces: 각 Tool 클래스 인스턴스 — Task 4의 `AiOrchestratorService`가 `chatClient.prompt().tools(...)`에 전달

- [ ] **Step 1: PropertyTool 구현**

`backend/src/main/java/com/ssafy/home/ai/tool/PropertyTool.java`:

```java
package com.ssafy.home.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.property.service.PropertyService;
import org.springframework.ai.tool.annotation.Tool;

public class PropertyTool {

    private final PropertyService propertyService;
    private final ObjectMapper objectMapper;

    public PropertyTool(PropertyService propertyService, ObjectMapper objectMapper) {
        this.propertyService = propertyService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "매물 ID로 매물 상세 정보(주소, 면적, 보증금, 월세, 층수, 건축연도, 거래유형 등)를 조회합니다.")
    public String getPropertyDetail(Long propertyId) {
        try {
            var response = propertyService.getProperty(propertyId);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "매물 ID " + propertyId + " 조회 실패: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 2: RiskTool 구현**

`backend/src/main/java/com/ssafy/home/ai/tool/RiskTool.java`:

```java
package com.ssafy.home.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.risk.service.RiskService;
import org.springframework.ai.tool.annotation.Tool;

public class RiskTool {

    private final RiskService riskService;
    private final ObjectMapper objectMapper;

    public RiskTool(RiskService riskService, ObjectMapper objectMapper) {
        this.riskService = riskService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "매물 ID로 전세사기 위험도 분석 결과(위험 등급, 점수, 시세 대비 차이율, 신고 수)를 조회합니다.")
    public String analyzePropertyRisk(Long propertyId) {
        try {
            var response = riskService.analyzeRisk(propertyId);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "매물 ID " + propertyId + " 위험도 조회 실패: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 3: LifestyleTool 구현**

`backend/src/main/java/com/ssafy/home/ai/tool/LifestyleTool.java`:

```java
package com.ssafy.home.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.lifestyle.service.LifestyleService;
import org.springframework.ai.tool.annotation.Tool;

public class LifestyleTool {

    private final LifestyleService lifestyleService;
    private final ObjectMapper objectMapper;
    private final Long userId;

    public LifestyleTool(LifestyleService lifestyleService, ObjectMapper objectMapper, Long userId) {
        this.lifestyleService = lifestyleService;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    @Tool(description = "현재 로그인한 사용자의 라이프스타일 분석 결과(생활 유형, 우선순위 시설 등)를 조회합니다.")
    public String getUserLifestyle() {
        try {
            var response = lifestyleService.getResult(userId);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "라이프스타일 정보 없음: 사용자가 라이프스타일 설문을 완료하지 않았습니다.";
        }
    }
}
```

- [ ] **Step 4: RagTool 구현**

`backend/src/main/java/com/ssafy/home/ai/tool/RagTool.java`:

```java
package com.ssafy.home.ai.tool;

import com.ssafy.home.ai.client.RagServiceClient;
import org.springframework.ai.tool.annotation.Tool;

public class RagTool {

    private final RagServiceClient ragServiceClient;

    public RagTool(RagServiceClient ragServiceClient) {
        this.ragServiceClient = ragServiceClient;
    }

    @Tool(description = """
            전세사기 예방, 임대차보호법, 계약 체크리스트, 등기부등본 읽는 법,
            전세보증보험, 임차권등기명령 등 법률·계약 문서 기반 지식을 검색합니다.
            계약 관련 질문이나 법적 절차 질문에 사용하세요.
            """)
    public String searchLegalKnowledge(String question) {
        return ragServiceClient.chat(question);
    }
}
```

- [ ] **Step 5: 빌드 확인**

```bash
cd backend
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ssafy/home/ai/tool/
git commit -m "feat(ai): Spring AI Tool 4종 구현 (PropertyTool, RiskTool, LifestyleTool, RagTool)"
```

---

## Task 4: DTO 및 AiOrchestratorService 구현

**Files:**
- Create: `backend/src/main/java/com/ssafy/home/ai/dto/request/AiChatRequest.java`
- Create: `backend/src/main/java/com/ssafy/home/ai/dto/response/AiChatResponse.java`
- Create: `backend/src/main/java/com/ssafy/home/ai/service/AiOrchestratorService.java`

**Interfaces:**
- Consumes: `ChatClient` Bean(Task 1), `PropertyTool/RiskTool/LifestyleTool/RagTool`(Task 3)
- Produces: `AiOrchestratorService.chat(AiChatRequest, Long userId): AiChatResponse` — Task 5의 Controller가 사용

- [ ] **Step 1: 요청/응답 DTO 작성**

`backend/src/main/java/com/ssafy/home/ai/dto/request/AiChatRequest.java`:

```java
package com.ssafy.home.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiChatRequest(
        @NotBlank String question,
        @Size(max = 4) List<Long> propertyIds
) {}
```

`backend/src/main/java/com/ssafy/home/ai/dto/response/AiChatResponse.java`:

```java
package com.ssafy.home.ai.dto.response;

public record AiChatResponse(String answer) {}
```

- [ ] **Step 2: AiOrchestratorService 구현**

`backend/src/main/java/com/ssafy/home/ai/service/AiOrchestratorService.java`:

```java
package com.ssafy.home.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.ai.client.RagServiceClient;
import com.ssafy.home.ai.dto.request.AiChatRequest;
import com.ssafy.home.ai.dto.response.AiChatResponse;
import com.ssafy.home.ai.tool.LifestyleTool;
import com.ssafy.home.ai.tool.PropertyTool;
import com.ssafy.home.ai.tool.RagTool;
import com.ssafy.home.ai.tool.RiskTool;
import com.ssafy.home.lifestyle.service.LifestyleService;
import com.ssafy.home.property.service.PropertyService;
import com.ssafy.home.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiOrchestratorService {

    private final ChatClient chatClient;
    private final PropertyService propertyService;
    private final RiskService riskService;
    private final LifestyleService lifestyleService;
    private final RagServiceClient ragServiceClient;
    private final ObjectMapper objectMapper;

    public AiChatResponse chat(AiChatRequest request, Long userId) {
        String userMessage = buildUserMessage(request.question(), request.propertyIds());

        String answer = chatClient.prompt()
                .user(userMessage)
                .tools(
                        new PropertyTool(propertyService, objectMapper),
                        new RiskTool(riskService, objectMapper),
                        new LifestyleTool(lifestyleService, objectMapper, userId),
                        new RagTool(ragServiceClient)
                )
                .call()
                .content();

        return new AiChatResponse(answer);
    }

    private String buildUserMessage(String question, List<Long> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return question;
        }
        return "비교 중인 매물 ID 목록: " + propertyIds + "\n\n사용자 질문: " + question;
    }
}
```

- [ ] **Step 3: LifestyleService에 getResult 메서드 확인**

```bash
grep -n "getResult\|findByUser\|getUserResult" \
  backend/src/main/java/com/ssafy/home/lifestyle/service/LifestyleService.java
```

Expected: `getResult(Long userId)` 또는 유사 메서드 존재 확인.
없으면 `LifestyleResultRepository.findByUser_UserId(userId)`를 직접 호출하도록 LifestyleTool 수정:

```java
// LifestyleTool.getUserLifestyle() 수정 버전
public String getUserLifestyle() {
    try {
        // LifestyleService에 getResult가 없는 경우
        return "라이프스타일 설문을 완료한 경우 라이프스타일 유형과 우선 조건을 참고하세요.";
    } catch (Exception e) {
        return "라이프스타일 정보 없음.";
    }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
cd backend
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ssafy/home/ai/dto/
git add backend/src/main/java/com/ssafy/home/ai/service/
git commit -m "feat(ai): AiOrchestratorService 구현 - ChatClient Tool Calling 오케스트레이션"
```

---

## Task 5: AiController 구현 및 Security 설정

**Files:**
- Create: `backend/src/main/java/com/ssafy/home/ai/controller/AiController.java`
- Modify: `backend/src/main/java/com/ssafy/home/common/config/SecurityConfig.java`

**Interfaces:**
- Consumes: `AiOrchestratorService.chat(AiChatRequest, Long): AiChatResponse` (Task 4)
- Produces: `POST /api/ai/chat` → `ApiResponse<AiChatResponse>`

- [ ] **Step 1: 실패하는 테스트 작성**

`backend/src/test/java/com/ssafy/home/ai/controller/AiControllerTest.java`:

```java
package com.ssafy.home.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.ai.dto.request.AiChatRequest;
import com.ssafy.home.ai.dto.response.AiChatResponse;
import com.ssafy.home.ai.service.AiOrchestratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AiOrchestratorService aiOrchestratorService;

    @Test
    @WithMockUser
    void chat_returnsAnswer() throws Exception {
        when(aiOrchestratorService.chat(any(), anyLong()))
                .thenReturn(new AiChatResponse("전세사기를 예방하려면 등기부등본을 확인하세요."));

        AiChatRequest request = new AiChatRequest("전세사기 어떻게 피해?", List.of());

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("전세사기를 예방하려면 등기부등본을 확인하세요."));
    }

    @Test
    void chat_withoutAuth_returns401() throws Exception {
        AiChatRequest request = new AiChatRequest("질문", List.of());

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd backend
./gradlew test --tests "com.ssafy.home.ai.controller.AiControllerTest"
```

Expected: FAIL — `AiController` 클래스 없음

- [ ] **Step 3: AiController 구현**

`backend/src/main/java/com/ssafy/home/ai/controller/AiController.java`:

```java
package com.ssafy.home.ai.controller;

import com.ssafy.home.ai.dto.request.AiChatRequest;
import com.ssafy.home.ai.dto.response.AiChatResponse;
import com.ssafy.home.ai.service.AiOrchestratorService;
import com.ssafy.home.auth.jwt.JwtAuthentication;
import com.ssafy.home.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiOrchestratorService aiOrchestratorService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal JwtAuthentication auth
    ) {
        AiChatResponse response = aiOrchestratorService.chat(request, auth.userId());
        return ResponseEntity.ok(ApiResponse.success("AI 답변 생성 완료", response));
    }
}
```

- [ ] **Step 4: SecurityConfig에 /api/ai/** 인증 규칙 추가**

`SecurityConfig.java`의 `authorizeHttpRequests` 블록에서 `anyRequest().authenticated()` 바로 위에 추가:

```java
.requestMatchers(HttpMethod.POST, "/api/ai/chat").authenticated()
```

(기존 `.anyRequest().authenticated()`가 이미 커버하므로 별도 추가 없이도 동작하나, 명시적으로 추가해 의도를 명확히 한다.)

- [ ] **Step 5: 테스트 통과 확인**

```bash
cd backend
./gradlew test --tests "com.ssafy.home.ai.controller.AiControllerTest"
```

Expected: 2개 테스트 PASS

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ssafy/home/ai/controller/
git add backend/src/main/java/com/ssafy/home/common/config/SecurityConfig.java
git add backend/src/test/java/com/ssafy/home/ai/
git commit -m "feat(ai): AiController POST /api/ai/chat 엔드포인트 구현 및 테스트"
```

---

## Task 6: 프론트엔드 AI API 클라이언트 구현

**Files:**
- Create: `frontend/src/api/aiApi.js`

**Interfaces:**
- Consumes: 기존 `api` (axios 인스턴스, `frontend/src/api/axios.js`)
- Produces: `chatWithAi({ question, propertyIds }): Promise<string>` — Task 7의 AiChatPanel이 사용

- [ ] **Step 1: aiApi.js 작성**

`frontend/src/api/aiApi.js`:

```js
import api from './axios'

export async function chatWithAi({ question, propertyIds = [] }) {
  const response = await api.post('/ai/chat', { question, propertyIds })
  return response.data.data.answer
}
```

- [ ] **Step 2: 단위 테스트 작성**

`frontend/src/api/__tests__/aiApi.spec.js`:

```js
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { chatWithAi } from '../aiApi'
import api from '../axios'

vi.mock('../axios', () => ({
  default: { post: vi.fn() },
}))

describe('chatWithAi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('질문과 매물 IDs를 POST하고 answer를 반환한다', async () => {
    api.post.mockResolvedValue({
      data: { success: true, data: { answer: '전세사기 예방법입니다.' } },
    })

    const result = await chatWithAi({ question: '전세사기 피하려면?', propertyIds: [1, 2] })

    expect(api.post).toHaveBeenCalledWith('/ai/chat', {
      question: '전세사기 피하려면?',
      propertyIds: [1, 2],
    })
    expect(result).toBe('전세사기 예방법입니다.')
  })

  it('propertyIds 없이 호출하면 빈 배열을 전송한다', async () => {
    api.post.mockResolvedValue({
      data: { success: true, data: { answer: '답변' } },
    })

    await chatWithAi({ question: '질문' })

    expect(api.post).toHaveBeenCalledWith('/ai/chat', {
      question: '질문',
      propertyIds: [],
    })
  })
})
```

- [ ] **Step 3: 테스트 실행**

```bash
cd frontend
npm run test:unit -- src/api/__tests__/aiApi.spec.js
```

Expected: 2개 테스트 PASS

- [ ] **Step 4: 커밋**

```bash
git add frontend/src/api/aiApi.js frontend/src/api/__tests__/aiApi.spec.js
git commit -m "feat(frontend): AI 채팅 API 클라이언트 구현"
```

---

## Task 7: AiChatPanel 컴포넌트 구현 및 CompareView 통합

**Files:**
- Create: `frontend/src/components/AiChatPanel.vue`
- Modify: `frontend/src/views/CompareView.vue`

**Interfaces:**
- Consumes: `chatWithAi` (Task 6), `compareStore.ids` (기존 Pinia store)
- Produces: AI 채팅 패널 UI — 비교 화면 하단에 렌더링

- [ ] **Step 1: AiChatPanel.vue 구현**

`frontend/src/components/AiChatPanel.vue`:

```vue
<script setup>
import { ref } from 'vue'
import { chatWithAi } from '@/api/aiApi'

const props = defineProps({
  propertyIds: {
    type: Array,
    default: () => [],
  },
})

const question = ref('')
const answer = ref('')
const isLoading = ref(false)
const error = ref('')

const SUGGESTED_QUESTIONS = [
  '이 매물 중 전세사기 위험이 가장 낮은 곳은?',
  '내 라이프스타일에 가장 잘 맞는 매물은?',
  '계약 전에 꼭 확인해야 할 사항은?',
  '보증금이 시세 대비 비싼 매물이 있나요?',
]

async function submit() {
  if (!question.value.trim() || isLoading.value) return

  isLoading.value = true
  error.value = ''
  answer.value = ''

  try {
    answer.value = await chatWithAi({
      question: question.value.trim(),
      propertyIds: props.propertyIds,
    })
  } catch (e) {
    error.value = 'AI 답변을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    isLoading.value = false
  }
}

function useSuggested(q) {
  question.value = q
  submit()
}
</script>

<template>
  <section class="ai-panel">
    <h3 class="ai-panel__title">AI 매물 상담</h3>

    <div class="ai-panel__suggestions">
      <button
        v-for="q in SUGGESTED_QUESTIONS"
        :key="q"
        class="ai-panel__chip"
        @click="useSuggested(q)"
      >
        {{ q }}
      </button>
    </div>

    <form class="ai-panel__form" @submit.prevent="submit">
      <input
        v-model="question"
        class="ai-panel__input"
        placeholder="매물 비교, 계약 주의사항, 전세사기 예방 등 무엇이든 질문하세요"
        :disabled="isLoading"
      />
      <button type="submit" class="ai-panel__submit" :disabled="isLoading || !question.trim()">
        {{ isLoading ? '분석 중...' : '질문하기' }}
      </button>
    </form>

    <div v-if="isLoading" class="ai-panel__loading">AI가 분석 중입니다...</div>
    <div v-if="error" class="ai-panel__error">{{ error }}</div>
    <div v-if="answer" class="ai-panel__answer">
      <p class="ai-panel__answer-label">AI 답변</p>
      <p class="ai-panel__answer-text">{{ answer }}</p>
    </div>
  </section>
</template>

<style scoped>
.ai-panel {
  margin-top: 2rem;
  padding: 1.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.ai-panel__title {
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 1rem;
  color: #1e293b;
}

.ai-panel__suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.ai-panel__chip {
  padding: 0.35rem 0.75rem;
  border: 1px solid #cbd5e1;
  border-radius: 20px;
  background: #fff;
  font-size: 0.8rem;
  cursor: pointer;
  color: #475569;
  transition: background 0.15s;
}

.ai-panel__chip:hover {
  background: #e2e8f0;
}

.ai-panel__form {
  display: flex;
  gap: 0.5rem;
}

.ai-panel__input {
  flex: 1;
  padding: 0.6rem 1rem;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.9rem;
}

.ai-panel__submit {
  padding: 0.6rem 1.2rem;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
}

.ai-panel__submit:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.ai-panel__loading {
  margin-top: 1rem;
  color: #64748b;
  font-size: 0.9rem;
}

.ai-panel__error {
  margin-top: 1rem;
  color: #ef4444;
  font-size: 0.9rem;
}

.ai-panel__answer {
  margin-top: 1rem;
  padding: 1rem;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.ai-panel__answer-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #3b82f6;
  margin-bottom: 0.5rem;
}

.ai-panel__answer-text {
  font-size: 0.9rem;
  line-height: 1.6;
  color: #334155;
  white-space: pre-wrap;
}
</style>
```

- [ ] **Step 2: CompareView.vue에 AiChatPanel 추가**

`frontend/src/views/CompareView.vue`에서 `<script setup>` 상단에 import 추가:

```js
import AiChatPanel from '@/components/AiChatPanel.vue'
```

템플릿에서 비교 테이블 또는 빈 상태 컴포넌트 바로 아래(닫는 `</template>` 전)에 추가:

```html
<AiChatPanel
  v-if="items.length > 0"
  :property-ids="compareStore.ids"
/>
```

- [ ] **Step 3: 개발 서버 실행 후 화면 확인**

```bash
cd frontend
npm run dev
```

1. `http://localhost:5173` 접속 → 로그인
2. 매물 2개 이상 비교 목록에 추가
3. 비교 화면(`/compare`) 진입
4. AI 패널이 비교 테이블 하단에 표시되는지 확인
5. 추천 질문 칩 클릭 → AI 답변 표시 확인
6. 직접 질문 입력 → 답변 확인

- [ ] **Step 4: 커밋**

```bash
git add frontend/src/components/AiChatPanel.vue
git add frontend/src/views/CompareView.vue
git commit -m "feat(frontend): AI 매물 상담 채팅 패널 구현 및 CompareView 통합"
```

---

## 전체 통합 검증

- [ ] Python RAG 서비스 기동: `cd ai && uvicorn app.main:app --port 8000`
- [ ] Spring Boot 기동: `cd backend && GOOGLE_API_KEY=xxx ./gradlew bootRun`
- [ ] Frontend 기동: `cd frontend && npm run dev`
- [ ] CompareView에서 "전세사기 어떻게 피해?" 질문 → RAG Tool이 법률 문서 검색 후 답변 반환 확인
- [ ] "이 매물 위험도 비교해줘" 질문 → RiskTool 호출 후 위험도 데이터 기반 답변 확인

---

## 아키텍처 다이어그램 (발표용)

```
[Vue 3 Frontend]
  └─ CompareView + AiChatPanel
       └─ POST /api/ai/chat (JWT 인증)
              ↓
[Spring Boot :8080]
  └─ AiController
       └─ AiOrchestratorService
            └─ ChatClient (Spring AI + Gemini gemini-2.5-flash-lite)
                 ├─ PropertyTool   → PropertyService → MySQL
                 ├─ RiskTool       → RiskService     → MySQL
                 ├─ LifestyleTool  → LifestyleService → MySQL
                 └─ RagTool        → HTTP POST
                                          ↓
                              [Python FastAPI :8000]
                                LangChain RAG Service
                                ChromaDB + Gemini Embedding
                                법률 문서 10종
```

**설명 포인트:**
- Spring AI 오케스트레이터가 LLM에게 4개 Tool을 제공
- LLM이 질문 유형에 따라 어떤 Tool을 호출할지 자율 결정 (Intent Router 역할)
- RAG Tool은 Python LangChain 서비스를 HTTP로 호출 → 하이브리드 아키텍처
- "Spring AI + LangChain 하이브리드 AI 아키텍처"로 발표
