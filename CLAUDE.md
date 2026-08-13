# MOVE-AI — 하네스 작업 지침

배송기사의 현장 경험을 음성으로 수집하고, AI가 구조화한 뒤 사람이 검수하고,
다음 기사의 Last 100m 배송 안내에서 상황에 맞게 다시 꺼내 쓰는 서비스.

**지금 무엇을 할 차례인지는 `docs/IMPLEMENTATION_STATUS.md`에 있다. 작업 시작 전 반드시 먼저 읽는다.**

---

## 문서를 언제 읽는가

전부 읽지 않는다. 필요할 때 해당 문서만 연다.

| 상황 | 읽을 문서 |
|---|---|
| 항상 (세션 시작) | `docs/IMPLEMENTATION_STATUS.md` |
| 제품이 뭔지 헷갈릴 때 | `docs/MOVE_AI_01_MVP_PRD.md` |
| **DB 스키마 · 임포트** | `docs/MOVE_AI_05A_DB스키마_임포트.md` |
| **API 계약 (Spring · Python)** | `docs/MOVE_AI_05B_API계약.md` |
| **구현 순서 · 모듈 · 테스트 · 운용** | `docs/MOVE_AI_05C_구현순서_운용.md` |
| **검색·안내 내부 로직** | `docs/MOVE_AI_04_RETRIEVAL_GUIDANCE_구현명세.md` |
| 작업 규칙·범위 판단 | `docs/MOVE_AI_02_HARNESS_ENGINEERING_PLAN.md` |
| 시연 시나리오 | `docs/DEMO_SCRIPT.md` |
| 역할 분담 | `docs/TEAM_ROLES.md` |

충돌 시 우선순위: **사용자 지시 → 01 → 05 → 04 → 02 → 기존 코드**

`05A`/`05B`가 스키마와 API의 유일한 출처다. `API_CONTRACT.md`나 `DB_SCHEMA.md`를 따로 만들지 않는다.

---

## 절대 규칙

이 목록을 어기면 서비스 정체성이 무너진다.

1. **Route는 AI가 만들지 않는다.** 관리자가 미리 등록한 고정 경로다. 생성·재배열 금지.
2. **원문에 없는 것을 만들지 않는다.** 숫자, 대체 경로, 행동 지시, 제한. "빡빡하다" ≠ "금지".
3. **승인 전 Draft는 검색에 쓰지 않는다.** PUBLISHED만 임베딩하고 검색한다.
4. **별도 Vector DB를 쓰지 않는다.** MariaDB에 벡터를 JSON으로 저장한다.
5. **코사인 계산은 Spring에서 한다.** Python은 `/stt`, `/extract-knowledge`, `/embed` 3개만.
   `/similarity-search`는 만들지 않는다. (후보 벡터를 HTTP로 옮기지 마라)
6. **톤수·높이·시간 조건은 코드로 판단한다.** LLM에게 숫자 비교를 시키지 않는다.
7. **`extra_condition_text`는 필터가 아니다.** 화면 라벨로만 쓴다.
8. **미등록 위치는 `UNKNOWN + UNRESOLVED + target_free_text`로 보존한다.** 비슷한 노드에 억지로 붙이지 않는다.
9. **LLM 응답은 검증 후 사용한다.** 스키마 실패 시 1회 재시도, 또 실패하면 `EXTRACTION_FAILED`. 필드를 조용히 지우고 저장하지 않는다.
10. **P0 완결 전에 확장 기능을 만들지 않는다.**

### 절대 버리지 않는 흐름

```
제보 → 추출 → 검수 → 발행 → 임베딩 → 검색 → 안내
```

시간이 부족하면 버리는 순서: 고객 메시지 답장 → 지도 시각화 → 복수 장소(B만 남김) → UI 장식 → 관리자 고급 기능

---

## 작업 방식

- **한 번에 하나의 세로 슬라이스.** 끝나면 커밋 가능한 상태로 만든다.
- **큰 리팩터링 금지.** 기존 코드가 있으면 재사용한다.
- **DTO 계약을 임의로 바꾸지 않는다.** 프론트가 멈춘다.
- **오류가 나면 범위를 좁힌다.** 주변 파일로 번지지 않게 한다.
- **"코드 작성함"은 완료가 아니다.**

- **Phase가 끝나면 커밋한다.** 되돌릴 지점이 없으면 복구할 수 없다.
- **Phase 완료 후에는 사람의 확인을 받고 다음으로 넘어간다.** 혼자 계속 진행하지 않는다.

### 완료 정의

```
코드 + 빌드 성공 + 최소 테스트 통과 + 실행 환경에서 실제 호출 성공 + 문서 갱신
```

**완료 조건 체크리스트는 `docs/IMPLEMENTATION_STATUS.md`에만 있다.**
전부 통과하기 전에 다음 Phase로 넘어가지 않는다.
Phase의 목적·시간 예산·축소 경로는 `05C §7`.

### 시간이 밀릴 때

```
T+6.0h  Phase 3 미완  →  P1·지도·복수 장소 폐기. 장소 B 하나로 간다.
T+8.0h  Phase 4 미완  →  Phase 5~7을 수동 시연으로 대체 검토
T+11.5h Phase 7 미완  →  2막 녹화 포기. 1막만으로 발표 구성.
```

**Phase 4가 분기점이다.** 여기까지 되면 발표가 성립한다.
완료 즉시 **1막 시연을 직접 눌러보며 동작을 확인한다.** 영상 녹화는 개발 종료 후 사람이 한다.

---

## 응답 형식

```
CURRENT STATE   확인한 현재 상태
NEXT SLICE      지금 구현할 정확한 범위
IMPLEMENTED     변경 파일과 내용
VERIFIED        실행한 명령과 결과
REMAINING       blocker / 다음 정확한 작업 하나
```

---

## 세션 종료 전

`docs/IMPLEMENTATION_STATUS.md`를 반드시 갱신한다.

```
1. 무엇을 변경했는지
2. 실제 검증한 명령
3. 통과/실패 결과
4. 남은 blocker
5. 다음에 할 정확한 작업 하나
```

**이 파일이 다음 세션의 기억이다.** 비워두면 처음부터 다시 파악해야 한다.

---

## 프로젝트 구조

```
backend/     Spring Boot
  └─ src/main/java/com/moveai/
     ├─ common/
     ├─ place/ · route/ · job/ · report/
     ├─ knowledge/ · moderation/ · retrieval/ · guidance/
     ├─ ai/stt/ · ai/extraction/ · ai/embedding/
     └─ dataset/controller/ · dto/ · validation/ · service/
ai-service/  FastAPI       STT · 지식 추출 · 임베딩
mobile/      React + Vite  기사 모바일 웹 (배송목록·안내·팁등록)
admin-web/   React + Vite  관리자 검수 웹
datasets/    합성 데이터 4종 (임포트 대상, 검증 완료)
docs/        기획·구현 문서
scripts/     검증·실행 스크립트
```

`place`, `route`, `job`, `report`, `knowledge`, `moderation`, `guidance`는
`entity/repository/service/controller/dto`로 분리한다. 정확한 전체 트리는
`backend/README.md`와 `05C §6`을 따른다.

## 스택 버전 (임의로 바꾸지 말 것)

```
Java 17        Spring Boot 3.4.x   Gradle
Python 3.12    FastAPI
Node 20+       React 18 + TypeScript + Vite (mobile · admin-web 공통)
MariaDB 11.4+
```

버전이 팀원마다 다르면 사전에 채운 빌드 캐시가 무용지물이 된다.

## 명령

### DB

```bash
docker compose up -d                  # MariaDB (호스트에 이미 설치돼 있으면 띄우지 않는다)
```

**이 기기에는 MariaDB 12.3.2가 호스트에 설치돼 `3307`에서 돌고 있다.**
`.env`의 `DB_PORT=3307`이 그것을 가리킨다. 컨테이너를 같은 포트로 띄우면 둘이 충돌하므로
**둘 중 하나만 쓴다.** 3306은 다른 프로젝트의 mysqld가 쓰고 있으니 끄지 않는다. (`SETUP.md §1-B`)

### backend (Spring Boot)

```bash
cd backend && ./gradlew build          # 빌드 (Windows: .\gradlew.bat)
cd backend && ./gradlew bootRun        # 실행 → http://localhost:8080
curl http://localhost:8080/health      # 서버 · DB · ai-service 세 경계를 한 번에 확인
```

Gradle은 wrapper(8.11.1)로 고정돼 있다. 전역 설치가 필요 없고, **팀원 전원이 같은 버전으로 빌드한다.**
JDK는 17 이상이면 된다(이 기기는 21, 산출물은 17 타깃).
`bootRun`은 저장소 루트의 `.env`를 읽어 환경변수로 넣는다.

### ai-service (FastAPI)

```bash
conda activate moveai
pip install -r ai-service/requirements.txt
cd ai-service && uvicorn app.main:app --reload --port 8000
cd ai-service && pytest -q             # 48 passed
```

**이 기기의 포트 8000은 다른 앱이 IPv6(`::`, `::1`)로 물고 있다.** uvicorn은 IPv4에만 붙으므로
`localhost`가 IPv6로 풀리면 엉뚱한 서버에 간다. `.env`의 `AI_SERVICE_URL`을
`http://127.0.0.1:8000`으로 고정해 뒀다. ai-service 응답이 이상하면 이걸 먼저 의심한다.

### 데이터

```bash
python scripts/validate_datasets.py               # 데이터셋 정합성 (기대: 이슈 0건)
cd ai-service && python scripts/embed_dataset.py  # 지식 146건 임베딩 산출물 생성
```

---

## 데이터셋 사실 (임의로 바꾸지 말 것)

```
장소 4 · 경로 8 · 구간 30 · 제보 41 · 지식 146 · 정답질문 20
임포트 대상은 datasets/ 의 4개 파일이다. docs/dataset/ 은 작성 이력이므로 임포트하지 않는다.
```

**시연 장소는 B(가온스퀘어 오피스타워)다.** 같은 배송지에서 차량 톤수로 경로가 갈리는 유일한 데이터다.

```
ROUTE_B_01  후문→지하2층→12층   7단계   maxTon 1.0, maxH 2.3
ROUTE_B_02  정문→지상로비→12층  3단계   minTon 1.0
```

`min_tonnage`의 포함/배타가 데이터마다 다르다. 임포트 시 statement 문구로 파생 컬럼을 만든다 — **`05A §3-3`의 8건 표를 단위 테스트로 고정할 것.**
