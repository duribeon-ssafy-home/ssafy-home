# 가격 단위 작업 주의사항

SSAFY HOME의 매물 가격 값은 API, DB, 전처리, 테스트에서 모두 **만원 단위**로 다룬다.

## 기준

- `properties.deposit`: 보증금, 만원 단위
- `properties.monthly_rent`: 월세, 만원 단위
- `lifestyle_results.deposit_max`: 설문 기반 보증금 상한, 만원 단위
- `lifestyle_results.monthly_rent_max`: 설문 기반 월세 상한, 만원 단위

예시는 아래처럼 해석한다.

```json
{
  "deposit": 1000,
  "monthlyRent": 50
}
```

- `deposit: 1000` -> 보증금 1,000만원
- `monthlyRent: 50` -> 월세 50만원

## 작업 규칙

- 백엔드 테스트 데이터는 원 단위 값(`500000`, `10000000`)이 아니라 만원 단위 값(`50`, `1000`)을 사용한다.
- 검색 파라미터 `minDeposit`, `maxDeposit`, `minMonthlyRent`, `maxMonthlyRent`도 만원 단위로 받는다.
- 공공데이터 전처리 원천 컬럼이 이미 `보증금(만원)`, `월세금(만원)`이므로 DB 적재 과정에서 10,000을 곱하지 않는다.
- 프론트 가격 표시는 숫자만 노출하지 말고 `월세 50만`, `보증금 1,000만`처럼 단위를 붙인다.
- 향후 매물 등록/수정 폼을 만들 때 입력 라벨은 반드시 `보증금(만원)`, `월세(만원)`으로 고정한다.

단위가 섞이면 가격 필터와 추천 점수 조건이 모두 틀어질 수 있다. 특히 설문 프리셋의 `monthlyRentMax = 50`, `depositMax = 1000`은 각각 월세 50만원, 보증금 1,000만원 기준이다.
