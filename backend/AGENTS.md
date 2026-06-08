# AGENTS.md

## Project Overview

This is a Spring Boot backend project using:

- Java 17
- Spring Boot 3.3.5
- Gradle
- Spring Web
- Spring Security
- Spring Data JPA
- Bean Validation
- Lombok
- springdoc OpenAPI
- MySQL for runtime
- H2 for tests

The base package is `com.ssafy.home`.

## Commands

- Run application: `./gradlew bootRun`
- Build: `./gradlew build`
- Run tests: `./gradlew test`
- Clean build: `./gradlew clean build`

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Dependency Rules

- Use Gradle only.
- Add dependencies in `build.gradle`.
- Do not introduce Maven, npm, or other package managers for backend work.
- Before adding a dependency, check whether Spring Boot or the existing dependencies already provide the needed feature.
- Do not add new libraries without explaining why.

## Project Structure Rules

- Main code goes under `src/main/java/com/ssafy/home`.
- Tests go under `src/test/java/com/ssafy/home`.
- Keep code organized by domain package, such as:
  - `auth`
  - `user`
  - `property`
  - `admin`
  - `common`
- For each domain, follow the existing package style used by nearby code:
  - `controller`
  - `service`
  - `repository`
  - `entity`
  - `dto`
  - `type` when enum types are needed
- Use `dto/request` and `dto/response` when the domain already uses that split.
- Keep flat `dto` packages when that is the established style for the domain.
- Shared infrastructure belongs in `common`.

## Coding Rules

- Use Java 17 features only when they improve readability.
- Prefer constructor injection. Use Lombok `@RequiredArgsConstructor` when consistent with nearby code.
- Keep controllers thin. Put business logic in services.
- Do not return JPA entities directly from controllers.
- Use DTOs for request and response bodies.
- Use Bean Validation annotations on request DTOs and `@Valid` in controllers.
- Do not modify unrelated domains unless the task requires it.
- When adding or modifying Swagger/OpenAPI documentation, include clear @Tag descriptions and @Operation summaries/descriptions so each API's purpose is easy to understand.
- When writing test code, use @DisplayName to describe in Korean what each test verifies.
- When writing Korean text in source code, documentation, or tests, ensure that file encoding is correctly handled, preferably UTF-8, so Korean characters are not broken.

## API Response Rules

- Wrap API responses with `ApiResponse`.
- Success responses should use the existing `ApiResponse.success(...)` factory method consistently.
- Failure responses should be handled through `GlobalExceptionHandler` when possible.
- Keep response fields consistent:
  - `success`
  - `message`
  - `data`
  - `errorCode`
- Do not invent a different response envelope for new APIs.
- When frontend integration is involved, update or verify the frontend against the actual backend DTO fields.

## Exception Rules

- Use `BusinessException` for expected business errors.
- Add new error cases to `ErrorCode`.
- Let `GlobalExceptionHandler` convert exceptions into `ApiResponse.fail(...)`.
- Do not throw generic `RuntimeException` for expected domain errors.
- Keep HTTP status codes defined in `ErrorCode`.

## Security Rules

- Security configuration belongs in `common/config/SecurityConfig.java`.
- JWT-related code belongs in `auth/jwt`.
- Refresh token logic belongs in `auth/refresh`.
- Use the existing current-user mechanism:
  - `@CurrentUser Long userId` for controller method arguments where applicable.
  - Existing security/user detail classes for authentication context.
- Do not bypass authorization checks in services.
- For owner-only resources, verify ownership in the service layer.

## JPA Rules

- Entities belong in each domain's `entity` package.
- Repositories should extend Spring Data JPA repositories and stay in `repository` packages.
- Keep entity relationships and fetch behavior explicit.
- Avoid exposing lazy-loaded entities through API responses.
- Use DTO mapping methods or constructors consistently with nearby code.
- Preserve `open-in-view: false` assumptions.

## Database And Configuration Rules

- Runtime configuration is in `src/main/resources/application.yml`.
- Local secret values belong in `application-secret.yml`.
- Do not commit real secrets, production credentials, or personal local settings.
- SQL seed/schema files belong under `src/main/resources/db`.
- Be careful when changing `ddl-auto`, schema, or seed data because tests and local setup may depend on them.

## Test Rules

- Use JUnit 5 and Spring Boot Test.
- Use MockMvc for API integration tests, following the existing tests.
- Prefer H2 in-memory database for tests.
- Add or update tests when changing:
  - authentication
  - authorization
  - API response shape
  - service business rules
  - entity/repository behavior
- Run `./gradlew test` or `gradlew.bat test` before finishing backend changes when possible.

## OpenAPI Rules

- Use springdoc annotations such as `@Tag`, `@Operation`, and `@Parameter` consistently with nearby controllers.
- Keep API documentation aligned with actual request and response DTOs.
- Do not document fields or behavior that the code does not implement.

## Frontend Integration Rules

- Backend API paths should remain under `/api` unless there is a clear reason to change them.
- Check existing controllers and DTOs before changing frontend API calls.
- Avoid breaking existing response shapes without updating frontend code and tests.
