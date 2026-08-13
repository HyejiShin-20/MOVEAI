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

현재는 폴더 구조를 Git에 보존하기 위한 `package-info.java`만 있다. 엔티티·DTO·서비스
클래스는 해당 Phase에서 API·DB 계약을 확인한 뒤 추가한다. Gemini STT 구현 자체는
`ai-service/app/services/stt.py`에 있고, `ai/stt`는 이후 Spring HTTP 클라이언트가 들어갈 경계다.

## 주의

- `retrieval` 4개 클래스는 **DB 없이 단위 테스트 가능한 순수 로직**으로 만든다.
  당일 디버깅 속도가 여기서 갈린다.
- AI 클라이언트는 인터페이스 뒤에 두고 `demo` 프로파일에서 mock으로 교체한다.
  단, mock 결과를 실제 AI 결과인 것처럼 발표하지 않는다.
- 코사인 유사도를 Python으로 보내지 않는다. 후보 벡터를 HTTP로 옮기면
  단계마다 수백 KB가 오간다. 내적/노름은 Java 20줄이다.
