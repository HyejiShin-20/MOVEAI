# backend — Spring Boot

DB 트랜잭션, 상태 전이, 조건 계산, **코사인 유사도 계산**, 응답 조립을 담당한다.
LLM 호출과 음성 처리는 하지 않는다 (→ `ai-service`).

전체 스키마·API 계약은 `docs/MOVE_AI_05_구현_상세명세.md`,
검색 내부 로직은 `docs/MOVE_AI_04_RETRIEVAL_GUIDANCE_구현명세.md`.

## 패키지 (05 §6)

```
com.moveai
├─ common/       예외, 응답 래퍼, enum
├─ place/        Place, PlaceNode
├─ route/        Route, RouteSegment, RouteSelector
├─ job/          DeliveryJob
├─ report/       FieldReport, ReportAudioFile
├─ knowledge/    KnowledgeItem, Condition, Target, embedding/
├─ moderation/   KnowledgeDraft, ModerationReview, PublishService
├─ retrieval/    CandidateCollector · ConditionEvaluator
│                QueryTextBuilder · CosineCalculator · RankingService
├─ guidance/     GuidanceSession, GuidanceService, StepAssembler
├─ ai/           SttClient · ExtractionClient · EmbeddingClient (interface)
└─ dataset/      DatasetValidator, DatasetImportService
```

## 주의

- `retrieval` 4개 클래스는 **DB 없이 단위 테스트 가능한 순수 로직**으로 만든다.
  당일 디버깅 속도가 여기서 갈린다.
- AI 클라이언트는 인터페이스 뒤에 두고 `demo` 프로파일에서 mock으로 교체한다.
  단, mock 결과를 실제 AI 결과인 것처럼 발표하지 않는다.
- 코사인 유사도를 Python으로 보내지 않는다. 후보 벡터를 HTTP로 옮기면
  단계마다 수백 KB가 오간다. 내적/노름은 Java 20줄이다.
