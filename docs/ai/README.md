# AI RAG 문서 투입 가이드

이 폴더에는 AI가 계약, 전세사기, 위험도 기준처럼 문서 근거가 필요한 질문에 답할 때 사용할 문서를 넣습니다.

권장 문서 수는 처음에는 5~6개입니다. PDF나 DOCX 원본은 함께 보관할 수 있지만, RAG 검색에는 정제된 Markdown 문서를 우선 사용합니다.

- `contract-checklist.md`: 계약 전 확인사항
- `jeonse-fraud-warning.md`: 전세사기 주요 위험 신호
- `housing-lease-protection-law-guide.md`: 주택임대차보호법 기반 임차인 보호 가이드
- `risk-score-guide.md`: 위험도 점수와 라벨 해석 기준
- `register-check-guide.md`: 등기부등본 확인 가이드
- `deposit-market-price-guide.md`: 보증금과 주변 시세 비교 해석
- `real-estate-terms.md`: 부동산 기본 용어 설명

문서는 Markdown 형식으로 작성하고, 한 문서 안에서는 `##` 단위로 주제를 나누는 것을 권장합니다.

예시:

```md
# 전세사기 체크리스트

## 등기부등본 확인
계약 전 등기부등본의 갑구와 을구를 확인해야 한다...

## 보증금과 시세 차이
보증금이 주변 시세와 크게 다르면 추가 확인이 필요하다...
```

나중에 RAG 인덱싱을 붙일 때는 이 폴더의 Markdown 파일을 읽고, 섹션 단위로 chunk를 나눈 뒤 Vector Store에 저장하면 됩니다.

현재 백엔드는 애플리케이션 시작 시 이 폴더의 Markdown 파일을 읽어 `##` 섹션 단위로 나눈 뒤 메모리 Vector Store에 적재합니다.
