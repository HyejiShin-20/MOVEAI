# MOVE-AI 팀원용 기능 흐름 · AI 원리 상세 설명

> 대상: 개발자가 아닌 팀원도 포함한 전체 팀  
> 목적: 본선 PPT, 화면 설계, 발표, 개발 협업 전에 “MOVE-AI가 실제로 어떻게 동작하는지” 공통 이해를 맞추기 위한 설명 자료  
> 수준: STT, LLM, Embedding, RAG가 각각 무엇을 하고 왜 필요한지부터 실제 기능 연결까지 설명

---

# 0. 먼저 한 줄로 이해하기

MOVE-AI는 단순히 기사에게 AI 채팅을 제공하는 서비스가 아니다.

```text
기사의 경험을
AI가 검색 가능한 데이터로 바꾸고,

다음 배송 때
현재 상황에 필요한 경험만 찾아서

고정된 Last 100m 배송 경로 위에
주의사항과 행동 팁으로 보여주는 서비스
```

전체 흐름은 다음과 같다.

```text
[베테랑 기사]
현장 경험을 음성으로 말함
        ↓
[STT]
음성을 글자로 변환
        ↓
[LLM]
긴 자연어를 여러 개의 현장 지식으로 분해
        ↓
[관리자]
AI가 제대로 이해했는지 검수
        ↓
[Knowledge DB]
승인된 현장 지식 저장
        ↓
[Embedding]
각 지식의 "의미"를 숫자 벡터로 변환
        ↓
[Hybrid RAG]
현재 차량·시간·위치 조건에 맞는 지식 검색
        ↓
[Last 100m]
현재 이동 단계에 필요한 Warning / Action / Tip 표시
```

---

# 1. 하나의 예시로 전체 기능 이해하기

베테랑 기사가 다음과 같이 말한다고 가정한다.

```text
"여기는 2.5톤 탑차로 오면 정문 지하주차장 못 들어가요.
후문으로 들어가서 지상 하역장에 대는 게 낫고,
점심 12시부터 1시까지는 화물 엘리베이터가 엄청 밀려요.
카트 끌고 방화문 지나갈 때는 문이 무거워서 박스 쏠리는 것도 조심해야 돼요."
```

사람은 이 말을 들으면 자연스럽게 네 가지 정보가 있다는 것을 이해한다.

```text
1. 2.5톤 탑차 → 정문 지하 진입 어려움
2. 후문 → 지상 하역장 이용
3. 12~13시 → 화물 엘리베이터 혼잡
4. 방화문 → 카트 이동 시 박스 쏠림 위험
```

컴퓨터가 이 정보를 실제 서비스에서 사용하려면 그냥 긴 문장 하나로 저장해서는 활용하기 어렵다.
그래서 MOVE-AI는 이 문장을 단계별로 가공한다.

---

# 2. STEP 1 — 음성 입력

## 기사에게 보이는 기능

```text
장소 선택
↓
[현장 팁 녹음 시작]
↓
녹음 종료
```

기사는 다음 같은 구조화 필드를 직접 입력하지 않는다.

```text
Category = ACCESS
Fact Type = RESTRICTION
Movement = VEHICLE
Target = NODE_B_04
```

이 작업은 AI가 담당한다.

---

# 3. STEP 2 — STT란?

STT = Speech To Text

쉽게 말하면:

> 음성을 글자로 바꾸는 기능

기사 음성:

```text
"점심 열두 시부터 한 시까지 엘리베이터 엄청 밀려요."
```

STT 결과:

```text
점심 12시부터 1시까지 엘리베이터 엄청 밀려요.
```

## 왜 STT 결과를 다시 보여주는가?

STT도 틀릴 수 있기 때문이다.

```text
기사:
"2.5톤 탑차는 못 들어가요."

STT:
"2.5톤 택시는 못 들어가요."
```

그래서:

```text
음성
↓
STT
↓
기사에게 텍스트 표시
↓
기사 수정
↓
corrected_stt_text 확정
```

으로 간다.

---

# 4. STEP 3 — LLM은 무엇을 하는가?

MOVE-AI에서 LLM은 단순 요약기가 아니다.

핵심 역할은:

> 자연어 제보에서 여러 개의 독립적인 현장 지식을 추출하는 것

입력:

```text
2.5톤 탑차는 정문 지하주차장 못 들어가요.
후문으로 들어가서 지상 하역장에 대는 게 낫고,
점심 12시부터 1시까지는 화물 엘리베이터가 엄청 밀려요.
```

출력 개념:

```text
Knowledge 1
대상: 정문 지하주차장 진입로
종류: 차량 진입 제한
조건: 2.5톤 탑차
내용: 2.5톤 탑차는 정문 지하주차장으로 진입하기 어렵다.
```

```text
Knowledge 2
대상: 후문 / 지상 하역장 경로
종류: 행동 안내
내용: 후문을 통해 진입하여 지상 하역장을 이용한다.
```

```text
Knowledge 3
대상: 화물 엘리베이터
종류: 혼잡
시간: 12:00 ~ 13:00
내용: 12시부터 13시까지 화물 엘리베이터 대기시간이 길다.
```

---

# 5. 왜 지식을 여러 개로 나누는가?

긴 제보 전체를 한 문장으로 저장하면 나중에 정확히 필요한 일부 정보만 찾기 어렵다.

예:

```text
질문:
"점심때 엘리베이터 오래 기다려?"
```

필요한 것은:

```text
점심시간 엘리베이터 혼잡
```

뿐이다.

그래서:

> 1 Knowledge = 1개의 독립적인 현장 사실

로 저장한다.

---

# 6. AI가 새 정보를 만들어서는 안 된다

원문:

```text
"1.5톤 차는 정문이 좀 빡빡해요."
```

잘못된 AI 출력:

```text
"1.5톤 이상 모든 차량은 정문 진입 금지"
```

이유:

```text
"좀 빡빡하다"
≠
"진입 금지"
```

MOVE-AI의 AI는 새로운 배송 지식을 창작하는 AI가 아니라 기사 경험을 구조화하는 AI다.

---

# 7. Target이란?

Target은:

> 이 Knowledge가 어느 위치에 관한 정보인지

를 뜻한다.

예:

```text
"화물 엘리베이터가 점심시간에 막혀요."
→ Target = 화물 엘리베이터
```

```text
"후문 차단기에서 인터폰 눌러야 돼요."
→ Target = 후문 차단기
```

---

# 8. 등록되지 않은 위치가 나오면?

기사:

```text
"하역장 들어가기 전에 화단 옆으로 갑자기 좁아지는 길이 있어요."
```

DB에 그 위치가 없다면 가까운 Node에 억지로 연결하지 않는다.

```text
UNKNOWN
UNRESOLVED
target_free_text:
"하역장 진입 직전 화단 옆 좁아지는 구간"
```

즉:

> 모르는 건 모른다고 저장한다.

---

# 9. STEP 4 — 관리자 검수

AI 출력은 바로 운영에 쓰지 않는다.

```text
AI Extraction
↓
Knowledge Draft
↓
관리자 승인 / 수정 / 반려
↓
Published Knowledge
```

관리자는:
- 실제 음성
- STT
- AI가 만든 Knowledge
- source_excerpt

를 함께 보고 판단한다.

이 구조를 Human-in-the-loop이라고 한다.

쉽게 말하면:

> AI가 초안을 만들고 사람이 최종 확인하는 구조

다.

---

# 10. 승인된 Knowledge는 MariaDB에 저장된다

예:

```text
ID: K102
Place: 가온스퀘어
Target: 화물 엘리베이터
Movement: 보행
내용: 12시부터 13시까지 화물 엘리베이터 대기시간이 길다.
Condition: 12:00 ~ 13:00
```

이런 구조화 정보가 DB에 저장된다.

---

# 11. 왜 Embedding이 또 필요한가?

DB에 이미 문장이 있는데도 Embedding을 만드는 이유는:

> 사람은 같은 의미를 항상 같은 단어로 표현하지 않기 때문

DB:

```text
화물 엘리베이터 대기시간이 길다.
```

기사 질문:

```text
점심에 엘베 많이 기다려?
```

표현은 다르지만 의미는 같다.

Embedding은 이런 의미의 유사성을 컴퓨터가 비교하게 해준다.

---

# 12. Embedding이란?

아주 쉽게 말하면:

> 문장의 의미를 여러 개의 숫자로 바꾸는 기술

예:

```text
"점심에는 화물 엘리베이터가 붐빈다."
```

↓

```text
[0.12, -0.44, 0.81, 0.03, ...]
```

실제로는 수백~수천 개 숫자의 Vector가 된다.

핵심:

```text
의미가 비슷한 문장
→ Vector도 가까움
```

---

# 13. Embedding 예시

문장 A:

```text
점심시간에는 화물 엘리베이터 대기가 길다.
```

문장 B:

```text
12시쯤 엘베 많이 기다려야 해?
```

A와 B는 의미가 비슷하므로 Vector도 가깝다.

문장 C:

```text
후문은 밤 10시에 닫힌다.
```

는 의미가 다르므로 멀다.

개념:

```text
A ●
 B ●


                  C ●
```

---

# 14. Embedding은 무엇을 해주는가?

Embedding이 직접 답을 만들지는 않는다.

역할:

> 현재 질문과 의미가 비슷한 Knowledge를 찾을 수 있게 하는 것

쉽게 말하면:

```text
Embedding = 검색을 위한 의미 좌표
```

---

# 15. Embedding은 언제 만드는가?

```text
Draft
↓
관리자 승인
↓
Published Knowledge
↓
Embedding 생성
```

승인 전 지식을 검색 DB에 넣지 않기 위해서다.

---

# 16. Vector Store는 무엇인가?

Embedding Vector를 저장하는 공간.

대규모 시스템에서는 Qdrant, Pinecone, Milvus, pgvector 등을 쓸 수 있다.

MOVE-AI MVP는 Knowledge가 약 150개 수준이라 별도 Vector DB를 두지 않는다.

MariaDB가 Vector Store 역할까지 같이 한다.

---

# 17. MOVE-AI에서는 어떻게 저장하는가?

개념:

```text
Knowledge
ID: K102

문장:
점심에는 화물 엘리베이터 대기가 길다.

조건:
12:00~13:00

Embedding:
[0.12, -0.44, 0.81, ...]
```

즉 MariaDB에:
- Knowledge
- Conditions
- Target
- Embedding Vector

를 함께 저장한다.

---

# 18. RAG란?

RAG = Retrieval-Augmented Generation

쉽게 설명하면:

> AI가 자기 기억만으로 답하지 않고, 먼저 우리 DB에서 필요한 정보를 찾아 그 정보를 활용하는 방식

일반 LLM:

```text
질문
↓
LLM이 자기 학습 데이터로 답
```

RAG:

```text
질문
↓
우리 DB에서 관련 Knowledge 검색
↓
검색된 현장 정보
↓
안내에 활용
```

MOVE-AI는 후자를 사용한다.

---

# 19. RAG가 실제로 찾는 것

현재 상황:

```text
장소: 가온스퀘어
현재 위치: B2 하역장 연결 통로
이동: 카트
시간: 12:20
```

검색 결과 예:

```text
방화문이 무겁다.

한 손으로 카트를 밀면
문이 닫히면서 박스가 쏠릴 수 있다.

12~13시는 화물 엘리베이터 대기가 길다.
```

---

# 20. 왜 Vector Search만 쓰지 않는가?

DB:

```text
K1
1톤 차량은 지하 진입 가능

K2
5톤 차량은 지하 진입 불가
```

현재 기사:

```text
5톤 차량
```

두 문장은 모두:
- 차량
- 지하
- 진입

의 의미를 공유하기 때문에 Embedding 유사도는 둘 다 높을 수 있다.

하지만 실제 적용은 K2다.

---

# 21. 그래서 Hybrid RAG를 쓴다

Hybrid RAG:

```text
구조화 조건 검색
+
Embedding 의미 검색
```

을 같이 쓰는 방식.

---

# 22. 구조화 조건 검색

예:

```text
현재 차량: 5톤
차량 높이: 2.7m
현재 시간: 12:20
현재 요일: 토요일
현재 장소: 가온스퀘어
```

Knowledge:

```text
2.3m 초과 차량 진입 불가
```

이건 LLM에게 판단시키지 않고 코드가 비교한다.

```text
2.7 > 2.3
→ 해당 Knowledge 적용
```

---

# 23. ConditionEvaluator

ConditionEvaluator는:

> 현재 배송 조건과 Knowledge 조건을 비교하는 계산기

판단:
- vehicle_class
- tonnage
- height
- width
- day
- time

LLM은 언어 의미를 이해하고, 숫자 비교는 프로그램 코드가 한다.

---

# 24. 실제 RAG 검색 순서

현재:

```text
장소: 가온스퀘어
차량: 2.5톤 탑차
높이: 2.7m
시간: 12:20
현재 Segment: 하역장 → 방화문
```

## 1단계 — Place 필터

```text
전체 Knowledge 150개
↓
가온스퀘어 Knowledge 37개
```

## 2단계 — 조건 판단

현재 시간/차량과 명백히 맞지 않는 후보 제거 또는 감점.

## 3단계 — Query 문장 생성

```text
가온스퀘어에서 2.5톤 탑차로 배송 중이며
현재 하역장에서 방화문으로 카트를 이동하고 있다.
12시 20분 현재 필요한 주의사항을 찾는다.
```

## 4단계 — Query Embedding

위 문장을 Vector로 변환.

## 5단계 — Knowledge Vector와 비교

예:

```text
K13 방화문 박스 쏠림      0.91
K12 방화문이 무거움       0.88
K14 점심 엘리베이터 혼잡 0.76
K30 아침 램프 정체        0.31
```

## 6단계 — 추가 Ranking

- 현재 Segment와 같음 → 가산점
- CART 이동과 일치 → 가산점
- 시간 조건 일치 → 가산점

## 7단계 — Top-K

최종 3~5개 Knowledge 반환.

---

# 25. Route와 RAG는 다르다

## Route

```text
어디로 이동하는가?
```

예:

```text
후문
→ B2 하역장
→ 방화문
→ 화물 엘리베이터
→ 12층 인계점
```

Route는 미리 정의되어 있다.

## Knowledge

```text
그곳에서 무엇을 알아야 하는가?
```

예:

```text
방화문이 무겁다.
점심에는 엘리베이터가 혼잡하다.
```

## RAG

```text
지금 단계에서 어떤 Knowledge가 필요한가?
```

를 찾는다.

---

# 26. 실제 Last 100m 화면

현재 STEP 2:

```text
2 / 4 단계

하역장 → 방화문

이동 안내
물품을 카트에 옮겨 방화문까지 이동하세요.

주의
방화문이 매우 무겁습니다.

주의
한 손으로 카트를 밀며 통과하면
문이 닫히면서 박스가 쏠릴 수 있습니다.

[다음]
```

기사에게는 DB 구조나 Vector 점수를 보여주지 않는다.

---

# 27. 왜 Last 100m인가?

기존 내비게이션:

```text
현재 위치
→ 건물 주소
```

MOVE-AI:

```text
건물 도착
→ 실제 진입구
→ 정차/하역
→ 건물 출입
→ 내부 이동
→ 엘리베이터
→ 실제 인계점
```

즉 주소 도착 이후 배송 작업이 끝날 때까지를 다룬다.

---

# 28. 전체 실제 기능 Sequence

```text
[기사]
음성 녹음
   ↓
[Frontend]
audio upload
   ↓
[STT]
speech → text
   ↓
[기사]
STT 수정
   ↓
[Backend]
FieldReport 저장
   ↓
[LLM Extraction]
text → atomic Knowledge JSON
   ↓
[MariaDB]
Knowledge Draft 저장
   ↓
[관리자]
승인 / 수정 / 반려
   ↓
[PUBLISHED Knowledge]
   ↓
[Embedding Model]
Knowledge → Vector
   ↓
[MariaDB]
Knowledge + Vector 저장
   ↓
[다음 기사]
배송 시작
   ↓
[Fixed Route]
현재 Segment 결정
   ↓
[ConditionEvaluator]
차량/시간 조건 판단
   ↓
[Embedding Search]
관련 Knowledge 검색
   ↓
[Hybrid Ranking]
Top-K
   ↓
[기사 화면]
Warning / Action / Tip
```

---

# 29. 실제 개발 구성

## Frontend — React / TypeScript

역할:
- 녹음
- STT 확인/수정
- 관리자 검수 UI
- Last 100m 화면
- 고객 답장 후보 버튼(P1)

## Backend — Spring Boot

역할:
- MariaDB 저장
- Place / Node / Route
- FieldReport
- Knowledge Draft
- 승인 처리
- 조건 계산
- Guidance 상태
- AI Service 호출

## AI Service — Python FastAPI

역할:
- STT
- LLM Knowledge Extraction
- Embedding
- Cosine Similarity

## Database — MariaDB

저장:
- Places
- Nodes
- Routes
- Segments
- Reports
- Knowledge
- Conditions
- Targets
- Embedding Vectors

---

# 30. 실제 구현 순서

## 1. Dataset Import
A/B/C/D → MariaDB

## 2. 기존 승인 Knowledge Embedding
Knowledge → Vector → MariaDB

## 3. RAG
Gold Query로 정답 Knowledge 검색 테스트

## 4. Last 100m
Route + RAG Knowledge를 화면 연결

여기까지 되면 기존 Seed 데이터 기반 데모 가능.

## 5. STT
실제 기사 음성 → 텍스트

## 6. Knowledge Extraction
STT → LLM → Draft

## 7. 관리자 검수
Draft → 승인

## 8. 새 Knowledge Embedding
승인 Knowledge → Vector

## 9. 다시 RAG
새 지식이 다음 Guidance에 실제 등장하는지 확인

---

# 31. 본선 데모 핵심

## BEFORE

```text
신입 기사:
"여기 처음 와봤는데 어디로 들어가야 하지?"
```

## CAPTURE

```text
베테랑:
"큰 탑차는 정문으로 못 들어가요..."
```

↓

```text
STT
→ AI Knowledge 카드
→ 관리자 승인
```

## AFTER

```text
다른 기사
현재 차량: 2.5톤 탑차
```

MOVE-AI:

```text
주의
현재 차량은 정문 지하 진입이 어렵습니다.

이동
후문 진입 Route를 이용하세요.

주의
12~13시에는 화물 엘리베이터가 혼잡합니다.
```

---

# 32. 각 기술을 한 문장으로 설명

## STT
기사가 말한 음성을 AI가 읽을 수 있는 텍스트로 바꾼다.

## LLM Knowledge Extraction
기사 한마디에 섞인 여러 현장 경험을 각각 검색 가능한 작은 지식으로 분해한다.

## 관리자 검수
AI가 잘못 이해한 정보를 실제 서비스에 쓰기 전에 사람이 확인한다.

## Embedding
각 지식의 의미를 숫자로 바꿔 표현이 달라도 의미가 비슷한 정보를 찾게 한다.

## Vector Store
Embedding 숫자를 저장하는 공간이며, MVP에서는 MariaDB가 그 역할도 같이 한다.

## RAG
AI가 자기 마음대로 답하지 않고 우리가 축적한 현장 지식을 먼저 찾아 활용하게 한다.

## Hybrid RAG
차량·시간 같은 정확한 조건 비교와 Embedding 의미 검색을 같이 사용한다.

## Route
실제 배송 시 이동해야 하는 미리 정의된 Last 100m 단계다.

## Guidance
현재 Route 단계에 필요한 Knowledge를 Warning / Action / Tip으로 보여준다.

---

# 33. 가장 쉽게 외우는 관계

```text
STT
말을 글자로

LLM Extraction
글을 데이터로

Embedding
데이터의 의미를 숫자로

RAG
지금 필요한 데이터를 찾기

Route
어디로 갈지

Guidance
가면서 무엇을 알아야 할지
```

---

# 34. 한 장 요약

```text
기사 경험
   ↓
[STT]
말 → 글
   ↓
[LLM]
글 → 구조화 Knowledge
   ↓
[Human Review]
AI 결과 검증
   ↓
[DB]
승인 Knowledge 저장
   ↓
[Embedding]
Knowledge 의미 → Vector
   ↓
[Hybrid RAG]
조건 + 의미로 관련 지식 검색
   ↓
[Fixed Route]
현재 배송 단계 확인
   ↓
[Guidance]
현재 단계에 필요한
Warning / Action / Tip 제공
```

---

# 35. MOVE-AI가 AI를 쓰는 진짜 이유

AI가 없으면 기사에게 이런 값을 직접 입력하게 해야 한다.

```text
출입구?
톤수?
높이?
시간대?
카트?
엘리베이터?
카테고리?
```

MOVE-AI는:

```text
기사:
그냥 말한다.

AI:
그 말을 데이터 구조로 바꾼다.
```

그리고 다음 배송에서는:

```text
기사:
복잡하게 검색하지 않는다.

RAG:
현재 상황에 필요한 경험을 자동으로 찾아준다.
```

즉 핵심 AI 가치는 두 가지다.

```text
1. 비정형 현장 경험 → 구조화
2. 축적된 현장 경험 → 상황에 맞게 재사용
```
