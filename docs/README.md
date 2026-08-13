# 문서 안내

## 역할별 읽는 순서

**개발**
```
1. MOVE_AI_01_MVP_PRD.md            무엇을 만드는가
2. MOVE_AI_05C_구현순서_운용.md       구현 순서 · 시간 예산 · 모듈 · 테스트  ★ 항상
3. MOVE_AI_05A_DB스키마_임포트.md     DB DDL · 임포트            (Phase 2)
4. MOVE_AI_05B_API계약.md            Spring·Python 계약        (Phase 2b~7)
5. MOVE_AI_04_RETRIEVAL_GUIDANCE_구현명세.md   검색·안내 내부 로직
6. MOVE_AI_02_HARNESS_ENGINEERING_PLAN.md      작업 규칙 · 범위 판단
```

**본선 전 준비 (전원)**
```
SETUP.md            설치 가이드. 여기부터 시작한다
PREP_CHECKLIST.md   환경·모델·키 준비. 미루면 당일 1~2시간 손해
TEAM_ROLES.md       역할 분담. 하네스는 1명만 운용한다
```

**기획 · 디자인 · 발표**
```
1. MOVE_AI_기획디자인_이해자료.pdf     원리 · 화면 지침 · 시연 · PPT 구성   ★ 이거 하나면 됨
2. MOVE_AI_본선_PPT_준비_기능원리_안내.md   더 자세한 서비스 스토리
```

**데이터 담당**
```
dataset/MOVE-AI_가상데이터_생성용_데이터셋_구조.md   스키마 규칙
dataset/MOVE-AI_팀원용_가상데이터_제작_가이드.md     작성 가이드
```

---

## 전체 목록

| 문서 | 내용 |
|---|---|
| `IMPLEMENTATION_STATUS.md` | **현재 진행 상황과 다음 작업.** 세션마다 갱신 |
| `MOVE_AI_01_MVP_PRD.md` | 제품 정의, 기능 범위, 성공 기준 |
| `MOVE_AI_02_HARNESS_ENGINEERING_PLAN.md` | 작업 루프, Phase, 커밋 규칙, 컷라인 |
| `MOVE_AI_03_HACKATHON_DAY_START_PROMPT.md` | 당일 하네스 첫 입력 |
| `MOVE_AI_04_RETRIEVAL_GUIDANCE_구현명세.md` | 후보 수집 · 조건 평가 · 랭킹 · 경로 선택 |
| `MOVE_AI_05A_DB스키마_임포트.md` | DB DDL · 데이터셋 임포트 |
| `MOVE_AI_05B_API계약.md` | Spring API · Python AI 서비스 계약 |
| `MOVE_AI_05C_구현순서_운용.md` | 시스템 구성 · 모듈 · 구현 순서 · 시간 예산 · 테스트 |
| `DEMO_SCRIPT.md` | 시연 컷 구성 + **녹화 진행 가이드(§7)** |
| `PREP_CHECKLIST.md` | **본선 전 준비** — 환경 · 모델 · 키 · 규정 |
| `TEAM_ROLES.md` | 역할 분담과 하네스 운용 방식 |
| `SETUP.md` | **설치 가이드** — 비개발자도 따라할 수 있게 |
| `MOVE_AI_기획디자인_이해자료.pdf` | 비개발자용 이해 자료 (18쪽) |
| `MOVE_AI_기획디자인_이해자료.html` | 위 PDF의 편집용 원본 |
| `MOVE_AI_본선_PPT_준비_기능원리_안내.md` | 서비스 스토리 상세 |
| `MOVE_AI_기능구현_원리_PPT준비_안내.pdf` | 위 문서의 초기 PDF본 |
| `MOVE_AI_팀원용_기능흐름_AI원리_상세설명.md` | 전 팀원용 AI 원리 설명 |
| `dataset/` | 데이터 작성 가이드 및 이력 |
| `archive/` | 폐기된 초안. 구현 판단에 사용하지 않음 |

화면 설계는 각 앱의 README에 있다 — `mobile/README.md`(React 기사 모바일 웹), `admin-web/README.md`(React 관리자 검수).

---

## 규칙

**스키마와 API의 출처는 `05A`/`05B` 뿐이다.** `API_CONTRACT.md`나 `DB_SCHEMA.md`를 따로 만들지 않는다.
두 곳에 적으면 반드시 어긋난다.

**04와 05는 겹치지 않는다.** 검색 파이프라인 내부는 04, 그 로직이 들어갈 그릇은 05.

**진행 상태는 `IMPLEMENTATION_STATUS.md` 한 곳에만 기록한다.** 05C는 목적·시간 예산만 정의한다.

**충돌 시 우선순위**
```
사용자의 당일 지시 → 01 → 05A/05B/05C → 04 → 02 → 기존 코드
```

## 임포트할 데이터는 어디에 있나

**`/datasets`** 의 4개 파일이다.

`docs/dataset/` 에도 JSON이 있지만 그것은 **작성 이력**이다(원본과 정정본).
임포트하면 검증에 실패한다.
