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

## 현재 구현 — 임베딩

```text
POST /embed   { "texts": [...] } → { model, dimension, vectors }
```

**텍스트 1건에 벡터 1건이 1:1로 대응한다.** SDK에 문자열 배열을 그대로 넘기면 전체를
하나의 Content로 묶어 **벡터 1개만** 돌려주므로, 각 텍스트를 개별 `Content`로 감싼다.
개수나 차원이 어긋나면 저장하지 않고 502로 끊는다 — 조용히 밀리면 지식과 벡터의 짝이
통째로 깨진다.

지식 벡터와 질의 벡터가 같은 공간에 있어야 하므로 `task_type`은 방향성 없는
`SEMANTIC_SIMILARITY`로 통일한다. `04 §5-1`과 `§5-2`가 두 텍스트를 같은 포맷으로
맞춰 둔 것과 같은 이유다.

### 쿼터

```text
배치 상한        요청당 100건 (초과 시 400)
무료 등급 쿼터   분당 100건, 배치 안의 텍스트 1건 = 요청 1건
```

시드 146건은 1분 안에 끝낼 수 없다. 429가 나면 최대 60초까지 늘려가며 5회 재시도한다
(실측: 146건 약 67초 소요). `EMBEDDING_BATCH_SIZE` 기본값은 50이다.

### 데이터셋 임베딩 산출물

```bash
python scripts/embed_dataset.py --dry-run   # 텍스트만 확인 (API 호출 없음)
python scripts/embed_dataset.py             # data/embeddings/knowledge_embeddings.json 생성
```

`embedding_text` 조립 규칙은 `04 §5-1`. 산출물은 `05A §2-3` `knowledge_embeddings`
컬럼과 1:1이며 `knowledgeCode`로 조인해 Spring이 그대로 INSERT 한다.
**DB 쓰기는 Spring 담당이고 여기서 하지 않는다.**

`data/`는 gitignore 대상이라 산출물은 커밋되지 않는다. Spring 담당자는 같은 명령으로
직접 생성하거나, 키가 없으면 이 파일을 따로 전달받아야 한다.

## 현재 구조

```
app/
├─ main.py                       FastAPI 앱 · `/health` · `/stt` · `/extract-knowledge` · `/embed`
├─ config.py                     `.env` 로딩과 모델·제한 설정
├─ errors.py                     공개 오류 코드 매핑
├─ schemas.py                    요청·응답 모델 (추출은 Pydantic 강제)
├─ prompts/                      knowledge_extraction.txt
└─ services/
   ├─ gemini.py                  공용 클라이언트 팩토리
   ├─ stt.py                     Gemini 인라인 오디오 전사
   ├─ extraction.py              구조화 추출 + 서버 검증 + 1회 재요청
   ├─ embedding.py               배치 임베딩 · 개수/차원 검증 · 429 재시도
   ├─ embedding_text.py          04 §5-1 embedding_text 조립 (순수 함수)
   └─ dataset_embedding.py       datasets/*.json → 임베딩 입력 · 산출물 조립
scripts/                         실호출 검증 · 데이터셋 임베딩
tests/                           API·서비스 단위 테스트
```

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
