# 05B — API 계약

> Phase 2b·4~7에서 쓴다. 프론트·백엔드·AI 서비스가 동시에 참조하는 계약이다.
> **임의로 바꾸지 않는다.** 바꾸면 다른 쪽이 멈춘다.
>
> | 문서 | 내용 |
> |---|---|
> | `05A` | DB 스키마(DDL) · 데이터셋 임포트 |
> | **05B** ← 이 문서 | Spring API 계약 · Python AI 서비스 계약 |
> | `05C` | 시스템 구성 · 모듈 구조 · 구현 순서 · 테스트 · 운용 |
> | `04` | 검색·안내 내부 로직 (후보 수집 · 조건 평가 · 랭킹 · 경로 선택) |
>
> 계약의 출처는 05뿐이다. `API_CONTRACT.md`를 따로 만들지 않는다.

---

# 4. Spring API 계약

공통: `Content-Type: application/json`, 시각은 ISO-8601, 오류는 아래 형식.

```json
{ "error": { "code": "NO_ROUTE_AVAILABLE", "message": "이 차량으로 진입 가능한 경로가 없습니다." } }
```

## 4-1. 기준 정보

```text
GET /api/places
    → [{ id, placeCode, name, placeType, description }]

GET /api/places/{id}
    → { id, name, placeType, description,
        nodes:  [{ id, nodeCode, nodeType, name, floorLabel, isIndoor }],
        routes: [{ id, routeCode, name, isDefault, destinationNodeId,
                   constraints:{ vehicleClass, minTonnage, maxTonnage,
                                 maxVehicleHeightM, maxVehicleWidthM } }] }

GET /api/routes/{id}
    → { id, name, totalSteps,
        segments:[{ id, sequenceNo, fromNodeName, toNodeName,
                    movementMode, traversalMethod, instruction, isIndoor }] }
```

## 4-2. 배송 건

```text
GET /api/delivery-jobs?status=READY
    → [{ id, jobCode, placeId, placeName, recipientLabel, addressText,
         itemSummary, status }]

GET /api/delivery-jobs/{id}
    → { id, jobCode, place:{id,name}, destinationNode:{id,name},
        recipientLabel, addressText, itemSummary, status }
```

## 4-3. 안내 (핵심)

```text
POST /api/guidance
{
  "deliveryJobId": 12,
  "vehicle": { "vehicleClass":"TRUCK", "tonnage":2.5,
               "heightM":2.7, "widthM":null },
  "contextTime": "2026-08-20T12:20:00"        // 생략 시 서버 시각
}
→ 201
{
  "sessionId": 88,
  "route": { "id":3, "name":"정문-지상 로비 하차 배송 경로", "totalSteps":3 },
  "currentStep": { …§4-4 형식… }
}
```

경로 선택은 04 §11-3. 후보가 0개면 **`404 NO_ROUTE_AVAILABLE`**. 기본 경로로 조용히 대체하지 않는다 — 2.5톤을 높이 2.3m 램프로 보내면 그게 사고다.

```text
GET  /api/guidance/{id}          현재 단계 재조회 (새로고침 대응)
POST /api/guidance/{id}/next     다음 단계
POST /api/guidance/{id}/complete 배송 완료 → { sessionId, status:"COMPLETED", completedAt }
```

**세션 생성 시 확정한 `route_id`는 끝까지 바꾸지 않는다.** 중간에 바뀌면 `sequence_no`가 가리키는 대상이 흔들려 상태가 깨진다.

### ★ 한 배송 건에 여러 세션을 허용한다

시연 컷1과 컷3이 **같은 배송 건을 차량만 바꿔 두 번 시작**한다(1톤 → 2.5톤).
따라서 이미 진행 중이거나 완료된 배송 건이라도 `POST /api/guidance`를 **막지 않는다.**

```text
POST /api/guidance  (같은 deliveryJobId 로 재호출)
  → 해당 job 의 기존 ACTIVE 세션이 있으면 status = ABANDONED 로 바꾼다
  → 새 세션을 ACTIVE 로 생성한다
  → 201 반환
```

`409 CONFLICT`를 반환하거나 job 상태로 차단하면 **컷3을 찍을 때마다 DB를 손봐야 한다.**

배송 건 상태는 다음과 같이 다룬다.

```text
세션 생성    delivery_jobs.status = IN_PROGRESS
세션 완료    delivery_jobs.status = DONE
재시작       DONE 이어도 다시 IN_PROGRESS 로 되돌린다
```

`GET /api/delivery-jobs?status=READY`가 목록 화면의 기본 조회다.
시연 중 목록에서 건이 사라지면 곤란하므로, **필터 없이 호출하면 전체를 반환**하도록 둔다.

> 실제 서비스라면 완료된 건의 재시작을 막아야 한다. MVP에서는 시연 가능성을 우선한다.
> 이 결정은 의도된 것이며, 발표에서 "완료 처리는 기사 버튼이 유일한 트리거"라는 원칙과 충돌하지 않는다.

## 4-4. 단계 응답 형식

```json
{
  "sequenceNo": 4,
  "totalSteps": 7,
  "fromNodeName": "지하 2층 하역장",
  "toNodeName": "하역장 방화문",
  "movementMode": "PEDESTRIAN",
  "traversalMethod": "CART",
  "instruction": "카트에 물품을 적재하여 방화문까지 이동한다.",
  "isLastStep": false,
  "cards": [
    {
      "knowledgeId": 213,
      "kind": "WARNING",                      // WARNING | ACTION | REFERENCE
      "statement": "하역장에서 연결 통로로 넘어가는 방화문이 매우 무겁다.",
      "actionText": null,
      "conditionLabel": null,                 // extra_condition_text 그대로
      "isRecentlyAdded": false,
      "targetName": "하역장 방화문",
      "isUnresolvedTarget": false
    }
  ]
}
```

### 카드 종류 매핑

```text
usage_scope = WARNING_ONLY                     → kind = WARNING
              ACTION_GUIDANCE / ROUTE_GUIDANCE → kind = ACTION
              REFERENCE_ONLY                   → kind = REFERENCE  (기본 접힘)
```

`actionText`가 비었는데 kind가 ACTION인 경우 `statement`로 대체한다(데이터에 4건 존재).

`isRecentlyAdded`는 `published_at`이 24시간 이내일 때 true. 화면의 **"새로 추가된 팁"** 배지 근거이며 시연의 핵심 장치다.

## 4-5. 제보

```text
POST /api/reports                    multipart
     placeId, selectedScopeNodeId?, audio(file)
     → { reportId, rawSttText }              // 내부에서 Python /stt 호출

PATCH /api/reports/{id}/transcript
     { "correctedText": "..." }
     → { reportId, correctedSttText }

POST /api/reports/{id}/extract
     → { reportId, status:"EXTRACTED",
         drafts:[{ draftId, draftIndex, payload:{…§5-2…} }] }
     실패 시 → { reportId, status:"EXTRACTION_FAILED", reason }
```

## 4-6. 검수

```text
GET  /api/moderation/drafts?status=PENDING
     → [{ draftId, reportId, placeName, createdAt, summary }]

GET  /api/moderation/drafts/{id}
     → { draftId,
         report:{ id, audioUrl, rawSttText, correctedSttText,
                  placeName, scopeNodeName },
         payload:{ …추출 결과 원본… },
         resolvedTargetName }              // UNKNOWN이면 free_text

POST /api/moderation/drafts/{id}/approve
     { "editedPayload": { … } }            // 무수정 승인이면 생략
     → { draftId, knowledgeId, embeddingCreated:true }

POST /api/moderation/drafts/{id}/reject
     { "reason": "..." }
     → { draftId, status:"REJECTED" }
```

**승인은 하나의 트랜잭션이다.**

```text
PUBLISHED 전환 → embedding_text 생성 → Python /embed 호출(동기)
              → knowledge_embeddings 저장 → 커밋
```

비동기로 빼면 "승인 직후 재조회"라는 시연의 결정적 순간에 경합이 생긴다. 관리자 한 명이 버튼 한 번 누르는 상황이므로 동기가 맞다. 실패하면 **롤백하고 명시적 오류**를 띄운다. 조용히 넘어가면 검색되지 않는 지식이 생긴다.

---

# 5. Python AI 서비스 계약

```text
GET  /health              → { "status":"ok", "model":"..." }
POST /stt
POST /extract-knowledge
POST /embed
```

`/similarity-search`는 **만들지 않는다**(§1).

## 5-1. STT / 임베딩

```text
POST /stt          multipart: audio
     → { "text":"...", "durationMs":8200 }

POST /embed        { "texts": ["...", "..."] }
     → { "model":"...", "dimension":1536,
         "vectors":[[0.12,-0.44,...], [...]] }
```

`/embed`는 **배열을 받는다.** 시드 146건을 한 번에 처리해야 한다(Phase 3).

## 5-2. 지식 추출

```text
POST /extract-knowledge
{
  "placeName": "가온스퀘어 오피스타워",
  "transcript": "...",
  "scopeNodeName": "후문 차량 출입구",
  "knownNodes":    [{ "code":"NODE_B_03", "name":"후문 차량 출입구" }, …],
  "knownSegments": [{ "code":"SEG_B_01",  "name":"후문 → 지하주차장 진입 램프" }, …]
}
```

응답은 **Pydantic으로 강제**한다.

```json
{
  "items": [
    {
      "target": {
        "target_type": "NODE",
        "target_code": "NODE_B_04",
        "target_resolution_status": "RESOLVED",
        "target_free_text": null
      },
      "category": "ACCESS",
      "custom_category_label": null,
      "fact_type": "RESTRICTION",
      "custom_fact_type_label": null,
      "movement_mode": "VEHICLE",
      "traversal_method": "DRIVE",
      "custom_traversal_method": null,
      "access_state": "PROHIBITED",
      "statement": "1톤을 초과하는 차량은 지하주차장으로 내려갈 수 없다.",
      "action_text": null,
      "source_excerpt": "1톤 넘는 차는 지하 못 내려가니까",
      "conditions": {
        "vehicle_class": "TRUCK",
        "min_tonnage": 1.0,
        "max_tonnage": null,
        "max_vehicle_height_m": null,
        "max_vehicle_width_m": null,
        "active_time_start": null,
        "active_time_end": null,
        "active_days": null,
        "extra_condition_text": null
      },
      "usage_scope": "WARNING_ONLY"
    }
  ]
}
```

### 서버측 검증 (LLM 응답 신뢰 금지)

```text
[ ] 모든 enum이 §2-6 허용목록 안
[ ] source_excerpt 가 transcript의 부분 문자열   ← 가장 중요
[ ] target_code 가 knownNodes/knownSegments 안에 존재
[ ] UNKNOWN 이면 code 없고 free_text 있음
[ ] conditions의 숫자가 transcript에 문자열로 등장
[ ] WARNING_ONLY 면 action_text 없음
```

실패 처리:

```text
1차 출력 → 검증 실패 → 오류 내용을 붙여 1회 재요청
        → 또 실패 → EXTRACTION_FAILED 반환
```

**필드를 조용히 지우고 저장하지 않는다.** 잘못된 추출은 실패로 남기고 사람이 본다.

### 프롬프트 핵심 규칙

`ai-service/app/prompts/knowledge_extraction.txt`에 둔다.

```text
1. 하나의 항목에는 하나의 사실만 담는다.
2. 차량 이동 사실과 보행/카트 사실을 한 항목에 섞지 않는다.
3. 원문에 없는 숫자·조건·행동·대체 경로를 만들지 않는다.
4. "빡빡하다/힘들다/좁다"를 "금지"로 바꾸지 않는다. → CONDITIONAL
5. 명시적으로 "못 간다/금지"일 때만 PROHIBITED.
6. source_excerpt는 원문에 실제로 있는 연속된 구절이어야 한다.
7. 대안이 원문에 없으면 action_text는 null, usage_scope는 WARNING_ONLY.
8. 알려진 위치로 설명되지 않으면 UNKNOWN + UNRESOLVED + target_free_text.
9. 허용목록에 없는 값을 만들지 않는다. 애매하면 OTHER + custom label.
```

**추출 품질 확인은 데이터셋으로 한다.** `field_reports.transcript`를 입력하고 `expected_knowledge_items`와 비교하면 41건의 정답 세트가 이미 있다.

---
