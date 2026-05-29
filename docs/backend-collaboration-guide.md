# 백엔드 협업 시 알아둘 점

## 1. 공통 응답 형식 사용

모든 API 응답은 `ApiResponse<T>`로 감싸서 반환한다.

### 성공 응답

```java
return ApiResponse.success("요청이 성공했습니다.", data);
```

응답 형태:

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

### 실패 응답

실패 응답은 직접 만들기보다 예외를 던지고 전역 핸들러에서 처리되도록 한다.

```java
throw new BusinessException(ErrorCode.USER_NOT_FOUND);
```

응답 형태:

```json
{
  "success": false,
  "message": "사용자를 찾을 수 없습니다.",
  "errorCode": "USER_NOT_FOUND"
}
```

## 2. 예외 처리는 전역 핸들러 사용

비즈니스 예외는 `BusinessException` + `ErrorCode` 조합으로 처리한다.

```java
if (user == null) {
    throw new BusinessException(ErrorCode.USER_NOT_FOUND);
}
```

컨트롤러나 서비스에서 직접 `ResponseEntity`로 에러 응답을 만들지 않는다.

전역 예외 핸들러가 담당하는 것:

| 대상 | 설명 |
| --- | --- |
| `BusinessException` | 서비스에서 발생한 비즈니스 예외 |
| Validation 실패 | `@Valid` 검증 실패 |
| 공통 실패 응답 변환 | `success`, `message`, `errorCode` 형식으로 응답 |

## 3. 에러 코드는 `ErrorCode`에 추가

새로운 실패 케이스가 필요하면 문자열을 직접 쓰지 말고 `ErrorCode` enum에 추가한다.

```java
INVALID_ROLE(HttpStatus.BAD_REQUEST, "허용되지 않은 역할입니다.")
```

서비스에서는 이렇게 사용한다.

```java
throw new BusinessException(ErrorCode.INVALID_ROLE);
```

## 4. Validation 사용

요청 DTO에는 가능한 한 validation annotation을 붙인다.

```java
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
```

필수값, 이메일 형식 등 단순 검증은 DTO에서 처리한다.

역할별 조건처럼 비즈니스 규칙이 필요한 검증은 서비스에서 처리한다.

예:

- AGENT 가입 시 전화번호 필수
- BUYER -> AGENT 변경만 허용
- ADMIN 일반 회원가입 불가

## 5. 인증 사용자 조회

현재 로그인한 사용자 ID가 필요하면 `CurrentUser`를 사용한다.

```java
Long userId = currentUser.getId();
```

컨트롤러에서 토큰을 직접 파싱하지 않는다.

JWT 파싱과 SecurityContext 저장은 필터가 담당한다.

## 6. Swagger 문서 작성

API를 추가하거나 수정할 때는 Swagger 설명도 함께 작성한다.

컨트롤러에는 `@Tag`를 붙인다.

```java
@Tag(name = "사용자 정보", description = "내 사용자 정보 조회, 수정, 계정 상태 변경, 역할 변경 API")
```

각 API 메서드에는 `@Operation`을 붙인다.

```java
@Operation(
        summary = "내 정보 조회",
        description = "현재 로그인한 사용자의 상세 프로필 정보를 조회합니다."
)
```

Swagger UI만 봐도 API 목적을 이해할 수 있게 작성한다.

확인 경로:

```text
/swagger-ui.html
```

## 7. 테스트 코드 작성

기능을 추가하거나 수정하면 테스트 코드도 같이 작성한다.

권장 테스트 범위:

- 정상 요청 성공
- 필수값 누락
- 권한/역할 제한
- 잘못된 상태값
- 예외 발생 시 `errorCode` 응답 확인

예:

```java
.andExpect(jsonPath("$.success").value(false))
.andExpect(jsonPath("$.errorCode").value("PHONE_NUMBER_REQUIRED"));
```

테스트는 가능한 한 API 흐름 기준으로 작성한다.

예:

- 회원가입
- 로그인
- 토큰 발급
- 인증 필요한 API 호출
- 상태/역할 변경

## 8. PR 전 확인 사항

PR 올리기 전에 아래를 확인한다.

- 새 API에 Swagger `@Tag`, `@Operation`이 작성되었는지
- 응답이 `ApiResponse` 형식인지
- 에러 응답이 `BusinessException` + `ErrorCode` 기반인지
- 요청 DTO에 validation이 들어갔는지
- 테스트 코드가 추가되었는지
- 테스트를 실행했는지
- Swagger UI에서 API가 의도대로 보이는지

테스트 실행:

```bash
./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew.bat test
```

## 9. PR 작성 시 포함할 내용

PR 설명에는 다음을 적는다.

- 추가/수정한 API 목록
- 주요 비즈니스 규칙
- 추가한 예외 코드
- 테스트한 내용
- Swagger 확인 여부
- 특별히 리뷰어가 봐야 할 부분
