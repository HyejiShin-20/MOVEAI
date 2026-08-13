# ai-service — Python FastAPI

STT, 지식 추출, 임베딩 벡터 생성만 담당한다.
DB 쓰기, 상태 판단, 순위 결정은 하지 않는다 (→ `backend`).

계약 전문은 `docs/MOVE_AI_05B_API계약.md §5`.

## 목표 API 계약

```
GET  /health
POST /stt                 multipart: audio  → { text, durationMs }
POST /extract-knowledge   → { items: [...] }   Pydantic 강제
POST /embed               { texts: [...] } → { model, dimension, vectors: [[...]] }
```

확정 모델은 `.env.example` 기준으로 `gemini-3.5-flash-lite`(구조화 추출),
`gemini-embedding-2`(1536차원), `gemini-3.6-flash`(STT)다.
임베딩 모델이나 차원을 바꾸면 저장 벡터를 전부 재생성해야 한다.

## 현재 구현 — STT · 지식 추출

```bash
conda activate moveai
cd ai-service
uvicorn app.main:app --reload --port 8000
```

```text
GET  /health  → { status, provider:"gemini", model }
POST /stt     multipart audio → { text, durationMs }
POST /extract-knowledge JSON → { items: [...] }
```

지원 형식은 WAV, MP3, M4A, AIFF, AAC, OGG, FLAC이며 기본 업로드 제한은 10MB다.
작은 파일은 Gemini Interactions API에 inline base64로 보내고, API 키와 오디오 바이트는
로그에 남기지 않는다. `GOOGLE_API_KEY`가 시스템에 함께 있어도 `.env`의
`GEMINI_API_KEY`를 명시적으로 우선한다.

실제 샘플 호출:

```bash
python scripts/smoke_stt.py
python scripts/smoke_stt.py "../datasets/voice/파일.m4a"
python scripts/smoke_extraction.py --dataset B --report REPORT_B_01
```

`/extract-knowledge`는 `knowledge_code`를 반환하지 않는다. 이 코드는 관리자 승인 뒤
Spring/DB가 발급한다. Gemini 응답은 Pydantic JSON Schema로 제한하고, 서버가 원문 구절,
알려진 타깃 코드, 숫자 근거, custom label과 usage scope를 다시 검증한다. 실패 시 검증
오류를 첨부해 1회 재요청하고, 다시 실패하면 `EXTRACTION_FAILED`를 반환한다.

**`/similarity-search` 는 만들지 않는다.** 코사인 계산은 Spring에서 한다.
`/embed` 는 배열을 받는다 — 시드 146건을 한 번에 처리해야 한다.

## 현재 구조

```
app/
├─ main.py              FastAPI 앱 · `/health` · `/stt`
├─ config.py            `.env` 로딩과 STT 제한
├─ errors.py            공개 오류 코드 매핑
├─ schemas.py           응답 모델
└─ services/stt.py      Gemini 인라인 오디오 전사
scripts/smoke_stt.py    `datasets/voice` 실호출 검증
tests/                  API·서비스 단위 테스트
```

`/embed`는 다음 구현 대상이며 아직 라우트가 열려 있지 않다.

## 추출에서 반드시 지킬 것

LLM 응답을 그대로 믿지 않는다. 서버에서 검증한다.

```
[ ] enum이 허용목록 안 (05A §2-6)
[ ] source_excerpt 가 transcript의 부분 문자열   ← 가장 중요
[ ] target_code 가 전달한 knownNodes/knownSegments 안에 존재
[ ] conditions의 숫자가 transcript에 등장
[ ] WARNING_ONLY 면 action_text 없음
```

실패 시 오류 내용을 붙여 **1회 재요청**, 또 실패하면 `EXTRACTION_FAILED`.
필드를 조용히 지우고 저장하지 않는다.

추출 품질은 `datasets/`의 `expected_knowledge_items`와 비교해 확인한다.
41개 제보에 대한 정답이 이미 있다.
