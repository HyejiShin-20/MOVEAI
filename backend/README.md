# backend — Spring Boot

DB 트랜잭션, 상태 전이, 조건 계산, **코사인 유사도 계산**, 응답 조립을 담당한다.
LLM 호출과 음성 처리는 하지 않는다 (→ `ai-service`).

스키마는 `docs/MOVE_AI_05A_DB스키마_임포트.md`, API 계약은 `docs/MOVE_AI_05B_API계약.md`,
검색 내부 로직은 `docs/MOVE_AI_04_RETRIEVAL_GUIDANCE_구현명세.md`.

## 현재 패키지 구조 (05C §6)

```
src/main/java/com/moveai/
├─ common/
├─ place/
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ └─ dto/
├─ route/
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ └─ dto/
├─ job/
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ └─ dto/
├─ report/
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ └─ dto/
├─ knowledge/
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ ├─ dto/ └─ embedding/
├─ moderation/
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ └─ dto/
├─ retrieval/
├─ guidance/
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ └─ dto/
├─ ai/
│  ├─ stt/ ├─ extraction/ └─ embedding/
└─ dataset/
   ├─ controller/ ├─ dto/ ├─ validation/ └─ service/
```

Phase 3까지 장소·경로·배송 조회와 임베딩 적재·하이브리드 검색 로직이 구현돼 있다.
Gemini STT·추출·임베딩 생성 자체는 `ai-service`가 담당하며 Spring은 HTTP 클라이언트,
조건 계산, 코사인 유사도와 랭킹을 담당한다.

## 실행 (Phase 1 완료)

```bash
./gradlew build      # Windows: .\gradlew.bat build
./gradlew bootRun    # http://localhost:8080
curl http://localhost:8080/health
```

## Phase 3 — 임베딩 적재와 평가

Python에서 시드 벡터를 만든 뒤 Spring이 텍스트·모델·차원·지식 코드를 전건 검증해
MariaDB에 적재한다. 두 명령은 저장소 루트 `.env`를 사용한다.

```bash
cd ../ai-service
python scripts/embed_dataset.py

cd ../backend
./gradlew bootRun --args="--import-embeddings"
./gradlew bootRun --args="--evaluate-rag"
```

`--evaluate-rag`는 ai-service `/embed`가 실행 중이어야 한다. 출력은 정답 질문 20개의
Hit@3, Hit@5, Top-5 `must_not` 위반 수와 질문별 상위 코드를 포함한다.

```json
{ "status": "ok",
  "database":  { "status": "up", "product": "12.3.2-MariaDB" },
  "aiService": { "status": "ok", "provider": "gemini", "model": "gemini-3.6-flash" } }
```

`/health`는 **어느 하나가 죽어도 200을 돌려주고 상태만 다르게 표시한다.** 여기서 500이 나면
무엇이 끊겼는지 확인할 수단 자체가 사라진다.

- Gradle은 wrapper(8.11.1)로 고정. 전역 설치 불필요, 팀원 전원 같은 버전.
- JDK는 17 이상이면 된다. 이 기기는 21이고 산출물은 17 타깃(`sourceCompatibility`)이다.
- `bootRun`이 저장소 루트의 `.env`를 읽어 환경변수로 넣는다 (`SETUP.md §1-B`가 전제하는 동작).
- **`spring.jpa.hibernate.ddl-auto=none`.** 스키마는 `05A §2`의 DDL로만 만든다.
  Hibernate가 테이블을 자동 생성하면 05A와 어긋난 스키마가 조용히 생긴다.

## 주의

- `CandidateCollector`, `ConditionEvaluator`, `QueryTextBuilder`, `CosineCalculator`,
  `RankingService`, `HybridSearchService`는 **DB 없이 단위 테스트 가능한 순수 로직**이다.
  당일 디버깅 속도가 여기서 갈린다.
- AI 클라이언트는 인터페이스 뒤에 두고 `demo` 프로파일에서 mock으로 교체한다.
  단, mock 결과를 실제 AI 결과인 것처럼 발표하지 않는다.
- 코사인 유사도를 Python으로 보내지 않는다. 후보 벡터를 HTTP로 옮기면
  단계마다 수백 KB가 오간다. 내적/노름은 Java 20줄이다.
