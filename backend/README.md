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

대부분은 폴더 구조를 Git에 보존하기 위한 `package-info.java`만 있다. 엔티티·DTO·서비스
클래스는 해당 Phase에서 API·DB 계약을 확인한 뒤 추가한다. Gemini STT 구현 자체는
`ai-service/app/services/stt.py`에 있고, `ai/stt`는 이후 Spring HTTP 클라이언트가 들어갈 경계다.

## 실행 (Phase 1 완료)

```bash
./gradlew build      # Windows: .\gradlew.bat build
./gradlew bootRun    # http://localhost:8080
curl http://localhost:8080/health
```

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

- `retrieval` 4개 클래스는 **DB 없이 단위 테스트 가능한 순수 로직**으로 만든다.
  당일 디버깅 속도가 여기서 갈린다.
- AI 클라이언트는 인터페이스 뒤에 두고 `demo` 프로파일에서 mock으로 교체한다.
  단, mock 결과를 실제 AI 결과인 것처럼 발표하지 않는다.
- 코사인 유사도를 Python으로 보내지 않는다. 후보 벡터를 HTTP로 옮기면
  단계마다 수백 KB가 오간다. 내적/노름은 Java 20줄이다.
