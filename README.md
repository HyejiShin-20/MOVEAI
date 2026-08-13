# MOVE-AI

배송기사의 현장 경험을 음성으로 수집하고, AI가 검수 가능한 구조로 정리한 뒤,
다음 기사의 **Last 100m 배송 안내**에서 현재 상황에 맞는 것만 다시 꺼내 쓰는 서비스.

```
기사 음성 제보 → STT → AI 지식 추출 → 관리자 검수 → 승인 지식
              → 임베딩 → Hybrid 검색 → 고정 경로 위 단계별 안내
```

지도는 건물까지 안내하지만, 배송 기사의 일은 거기서 시작된다.
어느 게이트로 들어가는지, 몇 톤까지 지하로 내려갈 수 있는지, 어느 문턱에 카트가 걸리는지는
여러 번 방문한 기사의 기억에만 있다가 담당자가 바뀌면 사라진다. 그 경험을 데이터로 남긴다.

---

## 무엇이 다른가

**같은 주소, 같은 수취인인데 차량이 다르면 들어가는 길 자체가 다르다.**

```
1톤   ROUTE_B_01   후문 → 지하 2층 하역장 → 방화문 → 연결통로 → 화물EV → 12층   7단계
2.5톤 ROUTE_B_02   정문 → 지상 로비 → 승객용 EV → 12층                        3단계
```

경로는 AI가 만들지 않는다. 관리자가 등록한 고정 경로 중에서 **목적지와 차량 제약으로 고른다.**
후보가 없으면 기본 경로로 조용히 대체하지 않고 `404 NO_ROUTE_AVAILABLE`을 낸다 —
2.5톤 차량을 높이 2.3m 램프로 보내는 안내가 나가면 그게 사고다.

각 단계에서 붙는 현장 지식은 네 겹으로 걸러 고른다.

```
구조 후보 수집 → movement_mode 필터 → 조건 필터(톤수·높이·시간) → 의미 유사도 → 랭킹
```

톤수·높이·시간 같은 숫자 판단은 **코드가 한다.** LLM에게 숫자 비교를 맡기지 않는다.
`min_tonnage=1.0`이 "1톤 초과"인지 "1톤 이상"인지는 데이터마다 다르므로,
임포트 시점에 원문 문구로 판정해 파생 컬럼에 굳혀 둔다.

---

## 현재 상태

서버는 **제보 → 추출 → 검수 → 발행 → 임베딩 → 검색 → 안내** 전 구간이 이어진다.

| 구간 | 상태 | 확인된 수치 |
|---|---|---|
| 데이터 임포트 | 완료 | 장소 4 · 경로 8 · 구간 30 · 제보 41 · 지식 146 |
| 임베딩 · 검색 | 완료 | 146건 × 1536차원, 정답 질문 20개 Hit@3 100% |
| Last 100m 안내 | 완료 | 1톤 7단계 / 2.5톤 3단계 분기, 시간 조건 필터 동작 |
| 음성 제보 · 추출 | 서버 완료 | 실제 m4a → 전사 → 초안 3건 (타깃 코드·근거 구절 정확). **화면은 아직 정적** |
| 검수 · 발행 | 완료 | 승인 직후 같은 경로 재조회에서 새 카드가 `isRecentlyAdded`로 노출 |
| 화면 | 일부 | 기사 화면 · 관리자 **검수** 화면은 실제 API 연결. 나머지 관리자 화면은 시안 |

세부 진행과 다음 작업은 [`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md)에 있다.

### 화면은 정적이지만 서버는 동작하는 것 — 음성 제보

**화면만 보면 음성 제보가 안 되는 것처럼 보이지만, 백엔드 파이프라인은 구현돼 있고 실제로 검증했다.**

```
mobile/src/pages/  VoiceRecordPage · VoiceUploadPage · TranscriptionPage
                   ReportConfirmPage · MyReportsPage        모두 정적 화면
```

이 다섯 화면은 레이아웃과 흐름만 있고 서버를 부르지 않는다.
브라우저에서 눌러보면 녹음·전사·저장이 일어나지 않는다.

**서버는 다음이 실제로 동작한다.** 저장소의 실제 녹음 파일(`.m4a`)로 확인한 결과다.

```
POST  /api/reports                  음성 업로드 → Gemini STT → 제보 생성 · 원본 파일 저장
PATCH /api/reports/{id}/transcript   기사가 고친 문장 저장
POST  /api/reports/text              텍스트 직접 입력 (STT 대체 경로)
POST  /api/reports/{id}/extract      전사문 → 구조화 추출 → 초안 생성
```

실제 응답 예시 — 현장 녹음 한 건을 올렸을 때다.

```json
{ "reportId": 42,
  "rawSttText": "여기 후문으로 들어가면 제일 빠른데 근데 지하 내려가는 램프 입구에
                 높이 바가 2.3m라서 탑차 높은 거는 아예 못 들어가고요…" }
```

이 전사문으로 추출하면 초안 3건이 나오고, 타깃 노드 코드와 근거 구절이 모두 원문과 일치한다.
즉 **음성 → 전사 → 구조화 → 검수 → 발행까지의 사슬은 끊겨 있지 않다.**
끊긴 곳은 화면과 서버 사이 한 칸뿐이다.

프론트 API 클라이언트(`mobile/src/api/client.ts`)에는 **텍스트 제보와 추출 호출은 이미 들어 있고,
음성 업로드(multipart) 호출만 없다.** 화면을 연결하는 작업은 이 함수 하나와
브라우저 녹음(MediaRecorder) 연동이 남은 상태다.

당장 서버 동작을 확인하려면 화면 없이 직접 호출하면 된다.

```bash
curl -X POST "http://localhost:8080/api/reports?placeId=2" -F "audio=@datasets/voice/샘플.m4a;type=audio/mp4"
curl -X POST "http://localhost:8080/api/reports/42/extract"
```

### 구현하지 못한 것 — 관리자 지식 조회 화면

MVP 범위에 있었지만 **완성하지 못했다.** 승인되어 쌓인 지식 146건을
장소·경로·노드 기준으로 목록화하고 검색·수정·비활성화하는 관리자 화면이다.

```
관리자 화면        상태
─────────────────────────────────────────────────
검수 대기 목록      실제 API 연결 완료
검수 상세·승인·반려  실제 API 연결 완료
지식 조회·관리      정적 시안만 있음  ←
장소 관리           정적 시안만 있음
경로 편집·검증      정적 시안만 있음
대시보드            정적 시안만 있음
```

화면은 `mobile/src/pages/AdminRouteKnowledgePage.tsx`에 레이아웃까지 있지만
데이터가 하드코딩돼 있고, **백엔드에 지식 조회 API 자체가 없다.**
`GET /api/knowledge` 계열 엔드포인트는 구현되지 않았다.

**왜 잘랐는가** — MVP에서 지키기로 한 흐름은
`제보 → 추출 → 검수 → 발행 → 임베딩 → 검색 → 안내` 한 줄이다.
지식 조회 화면은 이 흐름 **바깥**에 있다. 없어도 기사는 안내를 받고,
관리자는 검수하고, 승인된 지식은 검색에 반영된다.
한정된 시간에서 흐름을 끊기게 두는 것보다 이쪽을 미루는 선택을 했다.

**지금은 무엇으로 대신하는가**

- 개별 지식의 원문·AI 결과·근거 구절은 **검수 상세 화면**에서 확인한다
- 장소의 노드와 경로 구성은 `GET /api/places/{id}`·`GET /api/routes/{id}`로 조회된다
- 전체 지식 조회·수정은 현재 **DB 직접 조회**로만 가능하다

**완성하려면 무엇이 필요한가**

```
1. GET /api/knowledge          장소·타깃·상태 필터 + 페이지네이션
2. GET /api/knowledge/{id}     조건·타깃·근거 제보까지 함께
3. PATCH /api/knowledge/{id}   문구 수정 → embedding_text 재생성 → 재임베딩
4. 화면 연결                    AdminRouteKnowledgePage 의 하드코딩 제거
```

3번은 단순한 수정이 아니다. **문구가 바뀌면 벡터를 다시 만들어야** 검색 결과가 어긋나지 않는다.
승인 흐름과 같은 트랜잭션 규칙(`/embed`는 트랜잭션 밖, 성공 후 한 묶음 저장)을 그대로 따라야 한다.
이 점 때문에 "화면만 붙이면 되는 일"이 아니며, 남은 작업량을 낮게 잡지 않는 편이 좋다.

---

## 구조

```
backend/     Spring Boot   트랜잭션 · 조건 계산 · 코사인 유사도 · 응답 조립
  └─ src/main/java/com/moveai/
     ├─ common/                    오류 형식 · CORS · /health
     ├─ place/ · route/ · job/     기준 정보와 배송 건
     ├─ report/                    제보 · STT 연동 · 추출 호출
     ├─ knowledge/ · moderation/   지식 · 초안 · 검수 · 발행
     ├─ retrieval/                 후보 수집 · 조건 평가 · 코사인 · 랭킹
     ├─ guidance/                  세션 · 단계 이동 · 카드 조립
     ├─ ai/stt · ai/extraction · ai/embedding    Python 호출 경계
     └─ dataset/                   DDL 적용 · 데이터셋 임포트
ai-service/  FastAPI       STT · 지식 추출 · 임베딩 벡터 생성
mobile/      React + Vite  기사 화면 + 관리자 검수 화면 (한 프로젝트로 합침)
admin-web/   디자인 시안(HTML·PNG)만 보관. 실제 화면은 mobile/ 안에 있다
datasets/    합성 데이터 4종 (임포트 대상, 검증 완료)
docs/        기획 · 구현 문서
scripts/     데이터셋 검증 · 배포 스크립트
```

**경계를 넘지 않는다.** Spring은 DB·상태·계산을, Python은 모델 호출만 담당한다.
코사인 유사도는 Spring에서 계산한다 — 후보 벡터를 HTTP로 옮기면 단계마다 수백 KB가 오간다.
별도 Vector DB를 쓰지 않고 MariaDB에 벡터를 JSON으로 저장한다.

---

## 로컬 실행

### 1. 사전 준비

| | 버전 |
|---|---|
| Java | 17 이상 (Gradle은 wrapper로 고정, 별도 설치 불필요) |
| Python | 3.12 |
| Node | 20 이상 |
| MariaDB | 11.4 이상 |

```bash
cp .env.example .env
```

`.env`에서 최소 두 가지를 채운다.

```
GEMINI_API_KEY=<발급받은 키>
DB_PORT=3306          # 이미 3306을 쓰는 DB가 있으면 3307 등으로 바꾼다
```

DB는 둘 중 **하나만** 쓴다. 이미 MariaDB가 설치돼 있으면 컨테이너를 띄우지 않는다 —
같은 포트를 다투면 어느 쪽에 붙는지 알 수 없게 된다.

```bash
docker compose up -d          # 호스트에 MariaDB가 없을 때만
```

```sql
-- 호스트 MariaDB를 쓸 때. charset 지정이 없으면 한글이 깨진다.
CREATE DATABASE moveai CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER 'moveai'@'localhost' IDENTIFIED BY 'moveai';
GRANT ALL PRIVILEGES ON moveai.* TO 'moveai'@'localhost';
```

### 2. 의존성

```bash
pip install -r ai-service/requirements.txt
```

```bash
cd mobile && npm install && cp .env.example .env
```

### 3. 데이터 적재 — **두 명령은 항상 세트로**

```bash
python scripts/validate_datasets.py
```

기대 출력은 **이슈 0건**이다. 여기서 걸리면 데이터가 바뀐 것이니 임포트하지 않는다.

```bash
cd ai-service && python scripts/embed_dataset.py
```

지식 146건의 벡터를 만들어 `data/embeddings/`에 떨어뜨린다.
무료 등급은 분당 100건 제한이라 약 70초 걸린다. 결과 파일은 커밋되지 않으므로 기기마다 한 번 필요하다.

```bash
cd backend && ./gradlew bootRun --args="--import-datasets"
cd backend && ./gradlew bootRun --args="--import-embeddings"
```

첫 명령이 DDL을 적용하고 데이터셋을 넣는다. **전체 삭제 후 재삽입**이라 몇 번을 돌려도 같은 결과가 나오지만,
`knowledge_embeddings`도 함께 비워지므로 **두 번째 명령을 반드시 이어서 돌린다.**
빠뜨리면 안내 화면에 카드가 하나도 뜨지 않는다.

기대 건수:

```
places=4  place_nodes=57  routes=8  route_segments=30  field_reports=41
knowledge_items=146  knowledge_embeddings=146  delivery_jobs=5
```

### 4. 실행 (터미널 3개)

```bash
cd ai-service && uvicorn app.main:app --port 8000
```

```bash
cd backend && ./gradlew bootRun
```

```bash
cd mobile && npm run dev
```

```
기사 화면    http://localhost:5173/
관리자 화면  http://localhost:5173/admin/reviews
API          http://localhost:8080
```

---

## 동작 확인

```bash
curl http://localhost:8080/health
```

세 경계가 모두 살아 있어야 한다. 하나가 죽어도 200을 돌려주고 상태만 다르게 표시한다.

```json
{ "status": "ok",
  "database":  { "status": "up", "product": "11.4.x-MariaDB" },
  "aiService": { "status": "ok", "provider": "gemini", "model": "..." } }
```

**차량에 따라 경로가 갈리는지** — 이게 이 서비스의 핵심이다.

```bash
curl -X POST http://localhost:8080/api/guidance -H "Content-Type: application/json" \
  -d '{"deliveryJobId":1,"vehicle":{"vehicleClass":"TRUCK","tonnage":1.0,"heightM":2.0},"contextTime":"2026-08-13T12:30:00"}'
```

`totalSteps: 7`이 나온다. 톤수만 `2.5`로 바꾸면 `totalSteps: 3`이 나와야 한다.

`contextTime`은 시간 조건 지식을 제어한다. 생략하면 서버 시각이 쓰이고,
"화물 엘리베이터가 12~13시에 혼잡하다" 같은 지식은 그 시간대 밖에서 사라진다.

**검색 품질 평가** — 정답 질문 20개로 Hit@3·Hit@5를 잰다. ai-service가 떠 있어야 한다.

```bash
cd backend && ./gradlew bootRun --args="--evaluate-rag"
```

**테스트**

```bash
cd backend && ./gradlew test
cd ai-service && pytest -q
```

---

## API

```
GET   /health                              서버 · DB · ai-service 상태

GET   /api/places                          장소 목록
GET   /api/places/{id}                     노드 · 경로 · 차량 제약
GET   /api/routes/{id}                     구간을 순서대로

GET   /api/delivery-jobs                   배송 목록 (필터 없으면 전체)
GET   /api/delivery-jobs/{id}              배송 상세

POST  /api/guidance                        차량 정보로 경로 확정 + 1단계
GET   /api/guidance/{id}                   현재 단계 재조회
POST  /api/guidance/{id}/next              다음 단계
POST  /api/guidance/{id}/complete          배송 완료

POST  /api/reports                         음성 업로드 → STT → 제보 생성
POST  /api/reports/text                    텍스트 직접 입력 (STT 대체 경로)
PATCH /api/reports/{id}/transcript         기사 수정본 저장
POST  /api/reports/{id}/extract            AI 지식 추출 → 초안 생성

GET   /api/moderation/drafts               검수 대기 목록
GET   /api/moderation/drafts/{id}          원문 · AI 결과 · 근거 구절
POST  /api/moderation/drafts/{id}/approve  승인 → 임베딩 → 발행
POST  /api/moderation/drafts/{id}/reject   반려
```

계약 전문은 [`docs/MOVE_AI_05B_API계약.md`](docs/MOVE_AI_05B_API계약.md)에 있다. **임의로 바꾸지 않는다.**

승인은 동기 처리다. `/embed` 호출은 DB 트랜잭션 **밖**에서 끝내고,
성공한 뒤에야 지식·조건·타깃·임베딩·검수 이력을 **한 트랜잭션**으로 저장한다.
임베딩이 실패하면 DB를 건드리지 않고 명시적 오류를 낸다.

---

## 데이터

`datasets/`의 4개 파일이 임포트 대상이며 정합성 검증을 통과한 상태다.

| | 장소 | 경로 | 구간 | 제보 | 지식 |
|---|---|---:|---:|---:|---:|
| A | 해든마루 센트럴 아파트 | 2 | 6 | 10 | 36 |
| B | 가온스퀘어 오피스타워 *(시연)* | 2 | 10 | 11 | 37 |
| C | 한빛 스마트물류센터 | 2 | 8 | 10 | 40 |
| D | 해피가든몰 복합단지 | 2 | 6 | 10 | 33 |
| | | **8** | **30** | **41** | **146** |

**시연 장소는 B다.** 같은 배송지에서 차량 톤수로 경로가 갈리는 유일한 데이터다.

지식의 4분의 1은 `target_type=UNKNOWN`이다. 기사가 말한 위치가 등록된 노드로 설명되지 않을 때
비슷한 노드에 억지로 붙이지 않고 **원문 표현 그대로 보존한다.** 이들은 의미 유사도로만 찾을 수 있어,
Top-K에 전용 자리를 하나 비워 둔다 — 임베딩이 실제로 기여하는 지점이다.

`docs/dataset/`은 데이터 작성 가이드와 이력이며 **임포트하지 않는다.**

---

## 범위

**만든다** — 음성 제보, AI 지식 추출, 관리자 검수, 승인 지식 임베딩,
조건 필터 + 의미 검색, 고정 경로 기반 단계별 안내

**만들지 않는다** — 실시간 경로 생성/재탐색, 실내 위치 추적, GPS 기반 자동 단계 전환,
배송 시스템 연동, 배송 순서 최적화, 별도 Vector DB, 고객 메시지 자동 발송

**서버는 됐지만 화면이 남았다** — 음성 제보 화면. 백엔드 파이프라인은 실제 녹음으로 검증했고,
화면과 서버를 잇는 호출 한 칸이 비어 있다.

**만들려 했으나 못 만들었다** — 관리자 지식 조회·관리 화면. 화면도 API도 없다.

이 셋은 성격이 다르므로 구분해서 읽어야 한다. 처음부터 범위 밖이었던 것,
서버까지 됐는데 화면이 안 붙은 것, 아예 만들지 못한 것은 남은 작업량이 서로 다르다.
자세한 내용은 위 [현재 상태](#현재-상태)의 두 절에 있다.

일반 도로 주행 구간은 구현하지 않는다. 이미 완성도 높은 서비스가 있고,
이 서비스의 정의 자체가 **"주소 도착 이후"** 다.

---

## 문서

| 문서 | 언제 보는가 |
|---|---|
| [`docs/SETUP.md`](docs/SETUP.md) | 설치가 처음일 때 — 순서대로 안내 |
| [`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md) | **작업 시작 전 항상** — 현재 상태와 다음 할 일 |
| [`docs/MOVE_AI_01_MVP_PRD.md`](docs/MOVE_AI_01_MVP_PRD.md) | 제품이 무엇인지 |
| [`docs/MOVE_AI_05A_DB스키마_임포트.md`](docs/MOVE_AI_05A_DB스키마_임포트.md) | DB 스키마 · 임포트 |
| [`docs/MOVE_AI_05B_API계약.md`](docs/MOVE_AI_05B_API계약.md) | API 계약 (Spring · Python) |
| [`docs/MOVE_AI_04_RETRIEVAL_GUIDANCE_구현명세.md`](docs/MOVE_AI_04_RETRIEVAL_GUIDANCE_구현명세.md) | 검색·안내 내부 로직 |
| [`docs/DEMO_SCRIPT.md`](docs/DEMO_SCRIPT.md) | 시연 시나리오와 촬영 가이드 |
| [`docs/TEAM_ROLES.md`](docs/TEAM_ROLES.md) | 역할 분담 |

AI 코딩 하네스를 쓴다면 [`CLAUDE.md`](CLAUDE.md)가 진입점이다.

전달용 압축은 탐색기에서 직접 만들지 않고 아래 명령을 쓴다. `.env`·`.git`·`data`를 제외한다.

```bash
python scripts/build_release_zip.py
```
