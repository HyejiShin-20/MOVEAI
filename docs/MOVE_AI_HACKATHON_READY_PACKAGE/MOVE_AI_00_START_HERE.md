# MOVE-AI 본선 구현 준비 패키지

이 폴더의 문서를 다음 순서로 사용한다.

## 1. `MOVE_AI_01_MVP_PRD.md`
서비스가 **무엇을 만드는지** 정의한다.

읽어야 하는 사람:
- 전 팀원
- 기획
- 디자인
- 개발
- 발표 담당

## 2. `MOVE_AI_02_HARNESS_ENGINEERING_PLAN.md`
서비스를 **어떤 순서와 구조로 구현할지** 정의한다.

읽어야 하는 사람:
- 개발 담당
- Harness/Codex 세션

## 3. `MOVE_AI_03_HACKATHON_DAY_START_PROMPT.md`
본선 당일 새 Harness 세션 첫 메시지에 그대로 사용한다.

---

## 본선 전 준비

가능한 준비:
- 문서
- 데이터 검수
- 환경 설치
- API key
- 도구 연결
- 모델/패키지 확인
- 데모 시나리오

핵심 구현은 대회 운영 규정에 맞춰 본선에서 진행한다.

---

## 현재 MVP Core

```text
Voice Report
→ STT
→ LLM Knowledge Extraction
→ Human Moderation
→ Published Knowledge
→ Embedding in MariaDB
→ Hybrid RAG
→ Fixed-Route Last100m Guidance
```

P1:
```text
Customer Message
→ AI Reply Candidate
→ Driver Confirmation
→ Send
```

---

## 본선 당일 가장 중요한 규칙

P0 전체 연결이 되기 전에는 확장 기능을 만들지 않는다.

최종 데모의 핵심:
**새로운 기사 경험 하나를 등록하고, 그 지식이 다음 Guidance에서 실제로 다시 나타나는 것.**
