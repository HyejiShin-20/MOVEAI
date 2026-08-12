# ai-service — Python FastAPI

STT, 지식 추출, 임베딩 벡터 생성만 담당한다.
DB 쓰기, 상태 판단, 순위 결정은 하지 않는다 (→ `backend`).

계약 전문은 `docs/MOVE_AI_05_구현_상세명세.md §5`.

## 엔드포인트

```
GET  /health
POST /stt                 multipart: audio  → { text, durationMs }
POST /extract-knowledge   → { items: [...] }   Pydantic 강제
POST /embed               { texts: [...] } → { model, dimension, vectors: [[...]] }
```

**`/similarity-search` 는 만들지 않는다.** 코사인 계산은 Spring에서 한다.
`/embed` 는 배열을 받는다 — 시드 146건을 한 번에 처리해야 한다.

## 구조

```
app/
├─ main.py
├─ api/         stt.py · extraction.py · embedding.py
├─ schemas/     knowledge.py        ← Pydantic 모델
├─ services/    stt · extraction · embedding
├─ prompts/     knowledge_extraction.txt
└─ tests/
```

## 추출에서 반드시 지킬 것

LLM 응답을 그대로 믿지 않는다. 서버에서 검증한다.

```
[ ] enum이 허용목록 안 (05 §2-6)
[ ] source_excerpt 가 transcript의 부분 문자열   ← 가장 중요
[ ] target_code 가 전달한 knownNodes/knownSegments 안에 존재
[ ] conditions의 숫자가 transcript에 등장
[ ] WARNING_ONLY 면 action_text 없음
```

실패 시 오류 내용을 붙여 **1회 재요청**, 또 실패하면 `EXTRACTION_FAILED`.
필드를 조용히 지우고 저장하지 않는다.

추출 품질은 `datasets/`의 `expected_knowledge_items`와 비교해 확인한다.
41개 제보에 대한 정답이 이미 있다.
