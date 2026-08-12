# MOVE-AI MVP 기획서 / Product Requirements Document

> 문서 역할: 본선 구현과 발표의 기준이 되는 서비스 기획 Source of Truth  
> 대상: 기획 / 디자인 / 프론트엔드 / 백엔드 / AI 개발  
> 목표: 본선 당일 "현장 경험이 수집되어 다음 배송에서 재사용되는 과정"을 실제로 시연 가능한 MVP로 완성한다.

---

## 1. 프로젝트 정의

### 서비스명
MOVE-AI

### 한 줄 정의
베테랑 배송기사의 현장 경험을 음성으로 수집하고, AI가 검수 가능한 구조화 지식으로 변환한 뒤, 다음 배송기사의 Last 100m 배송 단계에서 현재 상황에 필요한 정보만 검색해 제공하는 서비스.

### 핵심 문제
기존 지도와 내비게이션은 배송지의 주소와 도로 경로는 알려주지만, 실제 배송 현장에서 필요한 맥락은 충분히 제공하지 못한다.

예:
- 어느 게이트로 진입해야 하는가
- 특정 톤수/높이 차량이 들어갈 수 있는가
- 어디에 정차하거나 하역해야 하는가
- 카트를 어느 출입문으로 이동해야 하는가
- 엘리베이터가 어디에 있고 언제 혼잡한가
- 지도에 없는 문턱/좁은 구간/자동문 문제는 무엇인가

이 정보는 반복 방문한 기사 개인의 기억에 남아 있다가 담당자 교체와 함께 사라지기 쉽다.

### 핵심 해결
```text
기사의 현장 경험
→ 음성 제보
→ STT
→ AI 구조화
→ 관리자 검수
→ 승인 Knowledge
→ Embedding
→ Hybrid RAG
→ 다음 기사의 Last 100m 안내
```

---

## 2. 서비스 포지셔닝

MOVE-AI는 다음 서비스가 아니다.

- 일반 AI 챗봇
- AI가 실시간으로 모든 경로를 만드는 내비게이션
- 단순 기사 팁 게시판
- RAG만 붙인 문서 검색 서비스

MOVE-AI의 핵심은 다음의 결합이다.

```text
고정된 배송 작업 Route
+
기사들의 검수된 현장 Knowledge
+
현재 차량/시간/위치 Context
+
Hybrid RAG
=
상황 맞춤형 Last 100m 배송 안내
```

---

## 3. 핵심 사용자

### 기사
- 현장 팁을 빠르게 음성으로 남긴다.
- 새로운 장소에서 Last 100m 안내를 받는다.
- AI가 제안한 고객 답장 후보를 확인 후 전송한다.

### 관리자
- AI가 구조화한 Knowledge Draft를 검수한다.
- 잘못된 위치/조건/문장을 수정하거나 반려한다.
- 승인된 정보만 운영 Knowledge로 발행한다.

---

## 4. P0 기능 범위

본선에서 반드시 end-to-end로 동작해야 하는 범위다.

### P0-1. 현장 팁 음성 등록

기사 화면:

```text
장소 선택
→ 대표 위치 선택
→ 녹음 시작
→ 녹음 종료
→ STT
→ STT 결과 확인/수정
→ 제보 제출
```

필수 저장:
- 오디오 파일 또는 파일 경로
- raw_stt_text
- corrected_stt_text
- 장소
- 대표 위치
- 작성자/시각

핵심 UX:
기사에게 category, condition, target 등의 구조화 필드를 직접 입력시키지 않는다.

---

### P0-2. AI Knowledge Extraction

입력:
- corrected_stt_text
- 현재 Place
- 해당 Place에 이미 등록된 Node 목록
- 해당 Place의 Segment 목록

출력:
한 제보를 여러 개의 원자 Knowledge Draft로 분해.

원칙:
- 1 Knowledge = 1 atomic fact
- 차량/보행 사실을 하나의 Knowledge에 섞지 않음
- 원문에 없는 숫자 생성 금지
- 원문에 없는 대체 경로 생성 금지
- 원문보다 강한 의미로 확대 금지
- source_excerpt는 실제 원문의 근거 구절
- 미등록 위치는 UNKNOWN + UNRESOLVED + target_free_text
- 예상 밖 정보는 OTHER + custom label

예:
```text
원문:
"점심에는 화물 엘리베이터가 많이 밀리고,
카트 끌고 방화문 지나갈 때 문이 무거워요."

↓ AI

K1
화물 엘리베이터 / 점심시간 혼잡

K2
방화문 / 카트 이동 / 문이 무거움
```

---

### P0-3. 관리자 검수

AI 결과는 바로 운영 데이터가 아니다.

```text
Knowledge Draft
→ 관리자 승인 / 수정 후 승인 / 반려
→ PUBLISHED Knowledge
```

관리자 화면 최소 요소:
- 음성 재생
- raw STT
- corrected STT
- AI Draft 카드
- Target
- Category / Fact Type
- Movement
- Conditions
- Statement
- Action
- Source excerpt
- 승인 / 수정 / 반려

Human-in-the-loop을 통해 AI hallucination이 운영 지식으로 바로 들어가는 것을 막는다.

---

### P0-4. 승인 Knowledge Embedding

승인된 Knowledge만 embedding을 생성한다.

```text
PUBLISHED Knowledge
→ 검색용 embedding_text 생성
→ Embedding Model
→ Vector
→ MariaDB knowledge_embeddings 저장
```

Draft에는 embedding을 만들지 않는다.

검색용 embedding text 예:
```text
장소: 한빛 스마트물류센터
위치: 상온창고 카트 통로
이동: 보행 / 카트
내용: 오후 5시 전후에는 카트 두 대가 마주 지나가기 어렵다.
행동: 반대편 카트를 먼저 보내고 이동한다.
조건: 오후 5시 전후
```

---

### P0-5. Hybrid RAG

별도 Vector DB를 사용하지 않는다.

MVP:
```text
MariaDB
+
Embedding Vector JSON
+
Python cosine similarity
```

검색 순서:
```text
1. Place / status 등 SQL 후보 필터
2. 차량/시간/요일 조건 평가
3. Query embedding 생성
4. 후보 Knowledge embedding과 cosine similarity
5. Target / Segment / Movement 등의 bonus 반영
6. Top-K 반환
```

Embedding만으로 모든 것을 판단하지 않는다.

예:
```text
A: 1톤 차량은 진입 가능
B: 5톤 차량은 진입 어려움
```

두 문장은 의미적으로 비슷하므로 Vector Search만으로는 충분하지 않다.
톤수/높이/시간 등 명확한 조건은 코드로 판단한다.

---

### P0-6. Last 100m Guidance

중요:
Route는 AI가 생성하지 않는다.

Route / RouteSegment는 미리 등록된 논리적 배송 작업 경로다.

예:
```text
후문
→ 지하 정차지
→ 하역장
→ 방화문
→ 내부 카트 통로
→ 화물 엘리베이터
→ 인계점
```

Guidance는 현재 Segment에 필요한 Knowledge를 붙인다.

예:
```text
3 / 6 단계

하역장 → 방화문

이동
카트에 물품을 적재한 뒤 방화문까지 이동하세요.

주의
방화문이 무거워 한 손으로 밀면 적재물이 쏠릴 수 있습니다.

[다음]
```

마지막 Segment 도착만으로 자동 배송 완료하지 않는다.
기사의 [배송 완료] 버튼이 최종 완료 트리거다.

---

## 5. P1 기능 범위

P0 전체가 동작한 뒤 시간이 남으면 구현한다.

### 고객 메시지 답장 보조

```text
고객 메시지
→ AI 의도 파악
→ 답장 후보 생성
→ 기사 버튼 표시
→ 기사 클릭
→ 전송 또는 Mock 전송
```

절대 자동 전송하지 않는다.

예:
```text
고객: "105동인데 언제 와요?"

추천 답장
[오후 2시~4시 사이 도착 예정입니다.]
```

ETA 근거 데이터가 없으면 AI가 시간을 임의 생성하면 안 된다.

### 배송지 변경 요청

예:
```text
고객: "문 앞 말고 경비실에 맡겨주세요."

배송 위치 변경 요청
기존: 105동 1203호
변경: 경비실

[반영]
```

실제 실시간 Route 재계산은 확장 범위다.

---

## 6. 데이터 모델 개념

### Place
배송 장소 전체.

### Node
배송 과정의 의미 있는 지점.
예: 게이트, 하역장, 주차 지점, 출입문, 엘리베이터, 통로, 인계점.

모든 위치를 처음부터 등록할 필요는 없다.
새 현장 제보로 새로운 위치가 발견될 수 있다.

### Route
특정 배송 목적지까지 가는 고정 논리 경로.

### RouteSegment
Route를 구성하는 단계.

### FieldReport
기사의 원본 제보.

### Knowledge Draft
AI가 추출한 승인 전 지식.

### Knowledge
관리자가 승인한 실제 운영 지식.

### Condition
차량/시간/요일 등 적용 조건.

### Target
Knowledge가 가리키는 Place / Node / Segment / UNKNOWN 위치.

### Embedding
Knowledge의 의미 검색을 위한 Vector.

---

## 7. Synthetic Dataset의 역할

A/B/C/D 데이터셋은 단순 dummy data가 아니다.

### 역할 A — Seed
Place / Node / Route / Segment / Knowledge를 개발 DB에 넣어 화면과 Guidance를 테스트한다.

### 역할 B — AI Gold
```text
transcript
→ 실제 AI Extraction
→ expected_knowledge_items와 비교
```

### 역할 C — RAG Gold
```text
rag_test_query
→ 실제 Retrieval
→ expected_knowledge_codes / must_not_return_codes와 비교
```

현재 준비 상태 메모:
- C: 최종 검수 완료본 존재
- B: 전체 규모는 적절하나 일부 Gold 수정 필요
- D: 현재 업로드본은 축약 상태라 Report/Knowledge/RAG 보충 필요
- A: 최종 사용본을 본선 전 파일 목록에서 다시 확인할 것

---

## 8. 핵심 화면

### 기사 — 팁 등록
- 장소
- 대표 위치
- 녹음
- STT 텍스트
- 수정
- 제출

### 관리자 — 검수
- 오디오
- 원문
- AI Draft 카드
- 승인/수정/반려

### 기사 — Last 100m
- 현재 단계
- 다음 이동
- Warning
- Action
- Tip
- 다음
- 배송 완료

### P1 — 고객 메시지
- 원문 메시지
- AI 추천 답장 버튼
- 기사 전송 버튼

---

## 9. 시스템 구성

```text
React / TypeScript
        |
        v
Spring Boot
  ├─ Place / Route
  ├─ Report
  ├─ Knowledge / Moderation
  ├─ Retrieval Orchestration
  └─ Guidance
        |
   +----+--------------------+
   |                         |
   v                         v
MariaDB                 Python FastAPI
                         ├─ STT
                         ├─ Knowledge Extraction
                         ├─ Embedding
                         └─ Cosine Similarity
```

---

## 10. 핵심 기술 책임 분리

### Spring Boot
- DB Transaction
- Place / Node / Route
- Report 상태
- 관리자 승인
- PUBLISHED 여부
- Structured SQL Filter
- ConditionEvaluator
- Guidance Session
- 최종 API 응답 조립

### Python FastAPI
- STT
- 자연어 Knowledge Extraction
- Embedding
- Vector Similarity

### LLM에게 맡기지 않을 것
- Route 생성/재배열
- 숫자 조건 계산
- 없는 위치를 임의로 기존 Node에 연결
- 관리자 승인
- 자동 고객 문자 발송
- 근거 없는 ETA 생성

---

## 11. 최소 운영 테이블

```text
users

places
place_nodes
routes
route_segments

field_reports
report_audio_files

knowledge_drafts
moderation_reviews

knowledge_items
knowledge_conditions
knowledge_targets
knowledge_embeddings

guidance_sessions
```

---

## 12. 성공 기준

본선 MVP 성공은 다음 하나의 흐름이 실제로 실행되는 것이다.

```text
1. Seed dataset import
2. 기존 PUBLISHED Knowledge embedding 생성
3. Place / Route 조회
4. Guidance 시작
5. 현재 Segment에 맞는 Knowledge 검색
6. 기사 새 음성 제보
7. STT
8. AI Knowledge Draft 생성
9. 관리자 승인
10. 새 Knowledge embedding 생성
11. 같은 장소 RAG 재조회
12. 방금 승인된 Knowledge가 실제 안내에 반영
13. 기사 [배송 완료]
```

발표에서 가장 중요한 메시지:

> 한 기사의 경험이 검증된 현장 지식이 되어 다음 기사의 배송 과정에서 다시 사용된다.

---

## 13. 본선에서 하지 않을 것

P0 완료 전 아래 기능으로 범위를 확장하지 않는다.

- AI 실시간 Route 생성
- 실제 정밀 실내 위치 추적
- PDA/WMS/TMS 완전 연동
- 실시간 배송 순서 최적화
- 별도 대규모 Vector DB 구축
- 자동 고객 메시지 발송
- 전체 고객 상담 챗봇
- 고도화된 Friction Score
- 실제 카카오 길찾기 재탐색

---

## 14. 향후 확장

MVP 이후:
- 카카오맵 위 추천 게이트/정차 지점 표시
- 실제 이동 로그를 이용한 Route 고도화
- 지식 충돌 탐지
- 최근성/신뢰도 관리
- 배송 마찰도
- 배송지 변경에 따른 경로 반영
- 대규모 Vector DB 전환
- 배송 시스템 연동

---

## 15. 한 줄 아키텍처

```text
MOVE-AI =
고정 Last 100m Route
+
기사 음성 → 검수 가능한 atomic Knowledge
+
MariaDB에 저장된 PUBLISHED Knowledge Embedding
+
Structured Filter + ConditionEvaluator + Vector Similarity
+
현재 배송 단계에 맞는 현장 안내
```
