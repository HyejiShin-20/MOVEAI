# MOVE-AI 가상 데이터 생성용 데이터셋 구조

> 목적: LLM에게 가상 물류 현장 데이터를 생성하도록 요청할 때 제공하는 공통 스키마 명세  
> 범위: PDA, 사용자 인증, 실제 파일 저장, 운영 로그, 임베딩 저장 등 서비스 운영용 DB 구조는 제외한다.  
> 핵심: 가상의 장소·경로·기사 제보·예상 원자 지식·RAG 평가 질문을 일관된 형식으로 생성하는 데 필요한 정보만 정의한다.

---

# 1. 이 문서가 필요한 이유

MOVE-AI의 실제 기사 제보는 종류가 매우 다양할 수 있다.

예:

```text
“1.5톤 탑차는 정문이 좀 좁아요.”
“카트 끌고 오른쪽 통로 지나가면 문턱 때문에 박스가 떨어질 수 있어요.”
“101동 들어가기 직전에 왼쪽으로 꺾는 길은 큰 차가 돌기 힘들어요.”
“엘리베이터 앞보다 계단 뒤쪽에 카트를 잠깐 두는 게 편해요.”
```

따라서 가상 데이터를 만들 때도 우리가 예상한 정보만 생성하면 안 된다.

이 데이터셋은 다음 원칙을 따른다.

```text
1. 기사 제보 원문을 자연어 그대로 만든다.
2. 한 제보에서 여러 개의 원자 지식을 추출한다.
3. 서비스 동작에 필요한 핵심 필드만 구조화한다.
4. 예상하지 못한 정보는 OTHER + custom label로 보존한다.
5. 기존 Node/Segment로 설명할 수 없는 위치는 free-text target으로 보존한다.
6. Category는 보조 정보이며 지식의 의미 자체는 statement에 보존한다.
7. RAG 테스트를 위해 일부 데이터에는 사람이 확인할 수 있는 기대 검색 결과를 같이 만든다.
```

---

# 2. 생성 데이터 전체 구조

LLM이 생성할 데이터는 다음 6개 묶음으로 구성한다.

```text
1. PLACE
2. PLACE_NODES
3. ROUTES
4. ROUTE_SEGMENTS
5. FIELD_REPORTS + EXPECTED_KNOWLEDGE_ITEMS
6. RAG_TEST_QUERIES
```

운영 DB의 다음 정보는 생성하지 않는다.

```text
users
password
PDA 인증 정보
delivery_jobs
delivery_items
scan_events
실제 audio file
moderation_reviews
실제 embedding vector
guidance_sessions
created_at / updated_at
DB auto increment PK
```

---

# 3. 코드 작성 규칙

팀원 데이터 병합 시 ID 충돌을 막기 위해 DB PK 대신 사람이 읽을 수 있는 임시 코드를 사용한다.

예:

```text
담당자 A

PLACE_A
NODE_A_01
NODE_A_02
ROUTE_A_01
SEG_A_01
REPORT_A_01
K_A_001
QUERY_A_01
```

담당자별 Prefix:

```text
A / B / C / D
```

실제 DB에 넣을 때는 코드 참조 관계를 실제 PK/FK로 변환할 수 있다.

---

# 4. PLACE 구조

가상의 배송 장소 하나를 정의한다.

```json
{
  "place_code": "PLACE_A",
  "name": "가상 장소명",
  "place_type": "APARTMENT",
  "custom_place_type": null,
  "description": "장소의 전체적인 특징",
  "synthetic": true
}
```

## 필드

| 필드 | 필수 | 설명 |
|---|---|---|
| place_code | O | 데이터셋 내부 장소 코드 |
| name | O | 가상 장소명 |
| place_type | O | 기본 장소 유형 |
| custom_place_type | 조건부 | OTHER인 경우 상세 유형 |
| description | O | 장소 전체 설명 |
| synthetic | O | 항상 true |

## place_type

```text
APARTMENT
OFFICE
LOGISTICS_CENTER
COMPLEX_FACILITY
OTHER
```

새 유형이면:

```json
{
  "place_type": "OTHER",
  "custom_place_type": "대학교 캠퍼스"
}
```

처럼 작성한다.

---

# 5. PLACE_NODE 구조

장소 내부에서 기사 안내나 현장 지식의 기준점이 되는 지점이다.

```json
{
  "node_code": "NODE_A_01",
  "parent_node_code": null,
  "node_type": "ENTRANCE",
  "custom_node_type": null,
  "name": "정문",
  "floor_label": null,
  "is_indoor": false,
  "description": "단지 정면의 차량 출입구"
}
```

## 필드

| 필드 | 필수 | 설명 |
|---|---|---|
| node_code | O | Node 코드 |
| parent_node_code | 선택 | 상위 Node |
| node_type | O | 기본 Node 유형 |
| custom_node_type | 조건부 | OTHER일 때 상세 유형 |
| name | O | 실제 화면에서 사용할 이름 |
| floor_label | 선택 | B2, 1F 등 |
| is_indoor | O | 실내 여부 |
| description | O | 지점 설명 |

## 기본 node_type

```text
SITE
ENTRANCE
SECURITY_GATE
PARKING_POINT
LOADING_POINT
BUILDING
BUILDING_ENTRANCE
ELEVATOR
STAIRS
CORRIDOR
DELIVERY_POINT
EXIT_POINT
OTHER
```

### 예상하지 못한 Node

예:

```text
방화문
연결다리
회전문
경사로
카트 보관 공간
택배 보관대
```

이런 정보는 새로운 enum을 임의로 만들지 않는다.

```json
{
  "node_type": "OTHER",
  "custom_node_type": "방화문"
}
```

---

# 6. ROUTE 구조

관리자가 미리 등록했다고 가정하는 하나의 고정 Last 100m 경로다.

```json
{
  "route_code": "ROUTE_A_01",
  "name": "후문-101동 배송 경로",
  "start_node_code": "NODE_A_02",
  "destination_node_code": "NODE_A_10",
  "vehicle_class": "TRUCK",
  "min_tonnage": null,
  "max_tonnage": 1.5,
  "max_vehicle_height_m": null,
  "max_vehicle_width_m": null,
  "is_default": true
}
```

## 필드

| 필드 | 필수 | 설명 |
|---|---|---|
| route_code | O | Route 코드 |
| name | O | 경로명 |
| start_node_code | O | 시작 Node |
| destination_node_code | O | 목적 Node |
| vehicle_class | 선택 | 적용 차량 |
| min_tonnage | 선택 | 최소 톤수 |
| max_tonnage | 선택 | 최대 톤수 |
| max_vehicle_height_m | 선택 | 최대 높이 |
| max_vehicle_width_m | 선택 | 최대 폭 |
| is_default | O | 기본 경로 여부 |

### 중요한 원칙

Route는 LLM이 기사 제보를 보고 즉석에서 만들어내는 경로가 아니다.

가상 데이터 생성 단계에서 먼저 고정 Route를 정의하고, 이후 기사 제보와 지식을 해당 Route 또는 Segment에 연결한다.

---

# 7. ROUTE_SEGMENT 구조

Route를 구성하는 순서가 있는 세부 이동 구간이다.

```json
{
  "segment_code": "SEG_A_01",
  "route_code": "ROUTE_A_01",
  "sequence_no": 1,
  "from_node_code": "NODE_A_02",
  "to_node_code": "NODE_A_04",
  "movement_mode": "VEHICLE",
  "traversal_method": "OTHER",
  "custom_traversal_method": "배송차량 주행",
  "instruction": "후문으로 진입하여 101동 뒤편 정차 지점까지 이동한다.",
  "is_indoor": false
}
```

## movement_mode

```text
VEHICLE
PEDESTRIAN
GENERAL
```

## traversal_method

```text
DRIVE
WALK
STAIRS
ELEVATOR
ESCALATOR
CART
OTHER
```

상위 이동 방식은 `movement_mode`, 실제 이동 형태는 `traversal_method`로 구분한다.

예:

```text
movement_mode = PEDESTRIAN
traversal_method = ELEVATOR
```

---

# 8. FIELD_REPORT 구조

기사 한 명이 음성으로 남긴 하나의 자유 제보다.

가상 데이터 생성에서 가장 중요한 원본 입력 데이터다.

```json
{
  "report_code": "REPORT_A_01",
  "place_code": "PLACE_A",
  "selected_scope_node_code": "NODE_A_01",
  "source_type": "SYNTHETIC",
  "transcript": "여기는 1.5톤 탑차로 오면 정문이 좀 빡빡해요. 저는 보통 후문으로 들어가서 101동 뒤에 잠깐 세워놓고 오른쪽 출입구로 걸어가요.",
  "audio_recording_candidate": true,
  "expected_knowledge_items": []
}
```

## 필드

| 필드 | 필수 | 설명 |
|---|---|---|
| report_code | O | 제보 코드 |
| place_code | O | 장소 |
| selected_scope_node_code | 선택 | 기사가 제보 시작 시 선택한 대표 위치 |
| source_type | O | 가상 생성이면 SYNTHETIC |
| transcript | O | 자연스러운 기사 구어체 |
| audio_recording_candidate | O | 실제 음성 녹음 후보 여부 |
| expected_knowledge_items | O | 이 제보에서 추출되어야 할 원자 지식 |

### selected_scope_node_code 주의

이 값은 제보에 등장하는 모든 정보의 Target이 아니다.

예:

```text
selected_scope_node_code = 정문
```

이어도 실제 제보 안에는:

```text
정문
후문
101동 뒤편
오른쪽 출입구
```

정보가 동시에 포함될 수 있다.

각 원자 지식의 실제 Target은 `expected_knowledge_items`에서 따로 정의한다.

---

# 9. 기사 transcript 작성 원칙

DB 문장처럼 만들지 않는다.

나쁜 예:

```text
1.5톤 이상 차량은 정문 진입이 CONDITIONAL이며 후문 진입이 ALLOWED이다.
```

좋은 예:

```text
여기 큰 탑차로 오면 정문 쪽이 좀 빡빡해요.
1.5톤 정도부터는 후문 쪽으로 들어가는 게 편하고,
101동 뒤에 잠깐 세운 다음 오른쪽 출입구로 걸어가면 됩니다.
```

한 제보에는 보통 2~5개의 사실을 포함한다.

말투도 다양하게 한다.

```text
“여긴 큰 차 들어가기가 좀 그래요.”
“점심에는 엘리베이터 줄이 엄청 길어요.”
“뒤쪽 문으로 가면 되는데 카트 끌고 가면 턱이 하나 있어요.”
“정문은 막힌 건 아닌데 탑차면 많이 빡빡합니다.”
```

---

# 10. EXPECTED_KNOWLEDGE_ITEM 구조

한 행은 반드시 하나의 원자 사실만 담는다.

```json
{
  "knowledge_code": "K_A_001",

  "target": {
    "target_type": "NODE",
    "target_code": "NODE_A_01",
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

  "access_state": "CONDITIONAL",

  "statement": "1.5톤 이상 탑차는 정문 진입이 어렵다.",
  "action_text": null,

  "source_excerpt": "1.5톤 탑차로 오면 정문이 좀 빡빡해요.",

  "conditions": {
    "vehicle_class": "TRUCK",
    "min_tonnage": 1.5,
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
```

---

# 11. Target 구조

현장 지식이 실제로 적용되는 장소/지점/구간을 표현한다.

## 해결된 Target

```json
{
  "target_type": "NODE",
  "target_code": "NODE_A_03",
  "target_resolution_status": "RESOLVED",
  "target_free_text": null
}
```

`target_type`:

```text
PLACE
NODE
SEGMENT
UNKNOWN
```

### PLACE

장소 전체에 적용되는 정보.

```text
“이 단지는 아침 8시쯤 전체적으로 배송차가 많이 몰려요.”
```

### NODE

특정 지점.

```text
“정문은 1.5톤 이상 차량이 들어가기 어렵다.”
```

### SEGMENT

두 지점 사이 실제 이동 구간.

```text
“정차 지점에서 오른쪽 출입구까지는 카트를 끌고 이동한다.”
```

---

# 12. 미등록·예상 밖 Target

실제 기사 제보에는 우리가 미리 만든 Node/Segment에 없는 위치가 등장할 수 있다.

이 정보를 버리거나 억지로 가까운 Node에 연결하지 않는다.

예:

```text
“101동 들어가기 직전에 왼쪽으로 꺾는 좁은 길이 있는데
거기는 탑차가 돌기 힘들어요.”
```

아직 해당 구간이 데이터셋에 없다면:

```json
{
  "target_type": "UNKNOWN",
  "target_code": null,
  "target_resolution_status": "UNRESOLVED",
  "target_free_text": "101동 진입 직전 왼쪽으로 꺾는 좁은 길"
}
```

로 작성한다.

이 데이터는 나중에 관리자 검수 과정에서 새 Node/Segment 생성 여부를 판단하기 위한 테스트 데이터로 사용할 수 있다.

## target_resolution_status

```text
RESOLVED
UNRESOLVED
NEEDS_REVIEW
```

---

# 13. Category 구조

Category는 지식을 대략 분류하기 위한 보조 메타데이터다.

```text
ACCESS
PARKING_STOPPING
LOADING
BUILDING_ENTRANCE
INTERNAL_ROUTE
ELEVATOR_STAIRS
CONGESTION_WAIT
DELIVERY_POINT
OTHER
```

새로운 종류를 억지로 기존 Category에 넣지 않는다.

예:

```text
“자동문 센서가 카트를 잘 못 잡아서 문이 금방 닫혀요.”
```

```json
{
  "category": "OTHER",
  "custom_category_label": "자동문 센서 문제"
}
```

`OTHER`는 오류가 아니다.

가상 데이터 전체 중 15~20% 정도는 의도적으로 `OTHER` 또는 기존 분류에 애매한 정보를 포함하는 것을 권장한다.

---

# 14. Fact Type 구조

```text
RESTRICTION
ALLOWANCE
LOCATION
INSTRUCTION
WARNING
CONDITION
OTHER
```

예:

```text
정문 차량 진입 금지
→ RESTRICTION

후문 차량 진입 가능
→ ALLOWANCE

101동 뒤편이 정차 위치
→ LOCATION

정차 후 오른쪽 출입구로 이동
→ INSTRUCTION

바닥이 미끄러움
→ WARNING

오전 8~9시에만 혼잡
→ CONDITION
```

예상 밖이라면:

```json
{
  "fact_type": "OTHER",
  "custom_fact_type_label": "적재물 낙하 위험"
}
```

을 사용할 수 있다.

---

# 15. movement_mode와 traversal_method

## movement_mode

상위 이동 맥락.

```text
VEHICLE
PEDESTRIAN
GENERAL
```

한 원자 지식에는 하나만 지정한다.

차량과 도보 사실이 함께 있으면 반드시 분리한다.

## traversal_method

실제 이동 방식.

```text
DRIVE
WALK
STAIRS
ELEVATOR
ESCALATOR
CART
OTHER
```

예:

```json
{
  "movement_mode": "PEDESTRIAN",
  "traversal_method": "CART"
}
```

---

# 16. access_state

접근 가능성을 표현할 필요가 있는 경우에만 사용한다.

```text
ALLOWED
CONDITIONAL
PROHIBITED
UNKNOWN
null
```

규칙:

```text
“힘들다”
“좁다”
“불편하다”
```

정도의 표현을 자동으로 `PROHIBITED`로 바꾸지 않는다.

일반적으로:

```text
CONDITIONAL
```

로 처리한다.

명시적으로:

```text
“차량 출입 금지”
“여기로는 들어갈 수 없다”
```

라고 할 때 `PROHIBITED`를 사용한다.

접근 여부와 관련 없는 지식은 `null`.

---

# 17. Conditions 구조

정확한 숫자나 시간 비교가 필요한 값만 구조화한다.

```json
{
  "vehicle_class": "TRUCK",
  "min_tonnage": 1.5,
  "max_tonnage": null,
  "max_vehicle_height_m": null,
  "max_vehicle_width_m": null,
  "active_time_start": "08:00",
  "active_time_end": "09:00",
  "active_days": ["MON", "TUE", "WED", "THU", "FRI"],
  "extra_condition_text": null
}
```

MVP에서 정식으로 만드는 조건:

```text
차량 종류
최소/최대 톤수
차량 높이
차량 폭
요일
하나의 시간 범위
```

조건은 기본적으로 AND 관계다.

복잡한 OR 조건은 가상 데이터 생성 범위에서 제외한다.

원문에 없는 숫자를 추론해서 넣지 않는다.

---

# 18. usage_scope

지식이 실제 서비스에서 어떻게 사용될 수 있는지를 나타낸다.

```text
WARNING_ONLY
ACTION_GUIDANCE
ROUTE_GUIDANCE
REFERENCE_ONLY
```

## WARNING_ONLY

위험/제약은 확인됐지만 대안 행동은 확인되지 않은 경우.

원문:

```text
“정문은 큰 탑차 들어가기 힘들어요.”
```

가능:

```text
statement = "대형 탑차는 정문 진입이 어렵다."
action_text = null
usage_scope = WARNING_ONLY
```

금지:

```text
action_text = "후문으로 진입한다."
```

원문에 후문 정보가 없기 때문이다.

## ACTION_GUIDANCE

실제 행동이 원문에 명확히 포함된 경우.

```text
“배송차는 후문으로 들어가세요.”
```

## ROUTE_GUIDANCE

특정 Route Segment에 붙일 수 있는 검증된 이동 안내.

```text
“101동 뒤 정차 지점에서 오른쪽 출입구까지 걸어가세요.”
```

## REFERENCE_ONLY

관리자 참고용으로 보존하지만 실제 안내에는 바로 사용하지 않는 정보.

---

# 19. source_excerpt 규칙

`source_excerpt`는 반드시 transcript에 실제 존재하는 연속된 구절이어야 한다.

Transcript:

```text
여기는 1.5톤 탑차로 오면 정문이 좀 빡빡해요.
```

가능:

```text
source_excerpt = "1.5톤 탑차로 오면 정문이 좀 빡빡해요."
```

잘못된 예:

```text
source_excerpt = "1.5톤 이상의 대형 차량은 정문 진입이 제한됨"
```

이 문장은 원문에 존재하지 않는다.

정규화된 표현은 `statement`에 넣는다.

---

# 20. 예상 밖 정보 예시

가상 데이터는 일반 데이터만 만들지 않는다.

반드시 일부는 다음과 같은 정보를 포함한다.

```text
카트와 자동문 센서 문제
문턱 때문에 박스가 떨어질 위험
방화문 통과 어려움
연결다리 바닥이 울퉁불퉁함
미등록 좁은 회전 구간
예상하지 못한 임시 대기 공간
화물 엘리베이터 버튼 위치가 특이함
회전문 때문에 대형 박스 통과 어려움
경사로가 젖으면 미끄러움
카트 보관 위치
```

이런 경우:

```text
OTHER
custom_category_label
custom_node_type
target_free_text
```

를 적극적으로 사용한다.

---

# 21. 한 제보를 원자 지식으로 나누는 예

기사 제보:

```text
“1.5톤 탑차는 정문이 좀 좁아요.
후문으로 들어가서 101동 뒤에 잠깐 세워놓고
오른쪽 출입구로 걸어가면 됩니다.
아침에는 후문 쪽이 좀 막혀요.”
```

예상 원자 지식:

```text
K1
정문 / VEHICLE
1.5톤 탑차는 정문 진입이 어렵다.

K2
후문 / VEHICLE
후문으로 차량 진입할 수 있다.

K3
101동 뒤편 / VEHICLE
101동 뒤편을 임시 정차 지점으로 사용할 수 있다.

K4
정차 지점 → 오른쪽 출입구 / PEDESTRIAN
해당 구간을 도보로 이동한다.

K5
후문 / GENERAL
아침 시간에는 후문 주변이 혼잡하다.
```

한 knowledge item에 다섯 내용을 합치지 않는다.

---

# 22. RAG_TEST_QUERY 구조

가상 데이터 중 일부는 검색 성능 테스트에 사용한다.

```json
{
  "query_code": "QUERY_A_01",
  "place_code": "PLACE_A",
  "question": "1.5톤 탑차가 정문으로 들어가도 돼?",
  "context": {
    "vehicle_class": "TRUCK",
    "vehicle_tonnage": 1.5,
    "vehicle_height_m": null,
    "vehicle_width_m": null,
    "movement_mode": "VEHICLE"
  },
  "expected_knowledge_codes": [
    "K_A_001"
  ],
  "must_not_return_codes": [
    "K_A_010"
  ],
  "reason": "정문과 1.5톤 이상 차량 조건에 직접 해당하는 지식이 검색되어야 하며, 도보 전용 정보는 핵심 답변으로 검색되면 안 된다."
}
```

---

# 23. RAG 질문 작성 원칙

질문은 실제 기사나 사용자가 입력할 법한 자연어로 만든다.

좋은 예:

```text
1.5톤으로 정문 들어가도 돼?
여기 카트 끌고 가기 힘든 데 있어?
후문 아침에도 괜찮아?
101동 어디에 차 세우는 게 좋아?
엘리베이터까지 어떻게 가?
```

일부 질문은 의도적으로 어렵게 만든다.

예:

```text
같은 Node인데 차량/도보 결과가 다름
1톤과 1.5톤 결과가 다름
평소와 특정 시간대 결과가 다름
이름이 비슷한 Node가 존재함
Category가 OTHER인 지식이 정답임
미등록 위치를 언급하는 지식이 존재함
```

---

# 24. 가상 데이터 권장 구성

전체 승인 예상 지식 약 150개 기준:

| 데이터 유형 | 권장 비율 |
|---|---:|
| 일반적인 진입/주차/하역/경로 정보 | 45~50% |
| 시간·차량 등 조건부 정보 | 15~20% |
| 실내 이동/엘리베이터/계단/출입구 | 15~20% |
| 예상 밖 OTHER/custom 유형 | 10~15% |
| 미등록/애매한 Target | 5~10% |

정확한 비율을 맞추는 것보다 다양성을 확보하는 것이 중요하다.

---

# 25. 팀원 4명이 나눠 만들 때 권장 단위

각 담당자:

```text
PLACE 1개
PLACE_NODE 10~15개
ROUTE 1~2개
ROUTE_SEGMENT 4~7개
FIELD_REPORT 약 10개
EXPECTED_KNOWLEDGE_ITEM 약 35~40개
RAG_TEST_QUERY 5개
AUDIO_RECORDING_CANDIDATE 5개
```

전체:

```text
PLACE 약 4개
FIELD_REPORT 약 40개
EXPECTED_KNOWLEDGE_ITEM 약 140~160개
RAG_TEST_QUERY 약 20개
실제 음성 녹음 후보 약 20개
```

---

# 26. 생성 단계와 실제 테스트 단계 구분

## 가상 데이터 생성

```text
LLM
→ PLACE/NODE/ROUTE 설계
→ 자연어 기사 제보 생성
→ expected knowledge 생성
→ 사람이 검수
```

## 실제 음성 테스트

가상 제보 중 일부를 팀원이 실제로 녹음한다.

```text
실제 음성
→ STT
→ AI knowledge extraction
→ 가상 데이터의 expected knowledge와 비교
```

따라서 `expected_knowledge_items`는 단순한 DB seed 데이터뿐 아니라 구조화 AI의 테스트 기준으로도 활용할 수 있다.

---

# 27. LLM 생성 시 금지 사항

LLM은 다음을 만들면 안 된다.

```text
실제 embedding vector
DB PK
사용자 비밀번호
실제 개인정보
실제 공동현관 비밀번호
실제 보안 코드
근거 없는 차량 제한 숫자
원문에 없는 대체 경로
원문에 없는 행동 지시
등록하지 않은 Node 코드를 임의로 참조
허용 목록에 없는 enum을 새로 생성
```

새로운 유형이 필요하면 enum을 새로 만들지 말고:

```text
OTHER + custom label
```

을 사용한다.

새로운 위치라면:

```text
UNKNOWN + UNRESOLVED + target_free_text
```

를 사용한다.

---

# 28. 최종 JSON 개념 구조

한 담당자의 최종 결과물은 다음 구조로 묶는 것을 권장한다.

```json
{
  "dataset_meta": {
    "dataset_version": "synthetic_v1",
    "author_prefix": "A",
    "synthetic": true
  },

  "place": {},

  "nodes": [],

  "routes": [],

  "route_segments": [],

  "field_reports": [
    {
      "report_code": "REPORT_A_01",
      "place_code": "PLACE_A",
      "selected_scope_node_code": "NODE_A_01",
      "source_type": "SYNTHETIC",
      "transcript": "...",
      "audio_recording_candidate": true,
      "expected_knowledge_items": []
    }
  ],

  "rag_test_queries": []
}
```

---

# 29. 이 데이터셋에서 가장 중요한 필드

모든 필드를 같은 중요도로 보지 않는다.

## 반드시 정확해야 함

```text
transcript
statement
source_excerpt
place
target 또는 target_free_text
movement_mode
conditions의 숫자/시간
usage_scope
```

## 검색 보조용

```text
category
fact_type
traversal_method
custom labels
```

따라서 Category 분류가 애매하더라도 원문과 `statement`의 의미를 잃지 않는 것이 더 중요하다.

---

# 30. 핵심 요약

이 가상 데이터셋의 목적은:

```text
“현장 정보의 모든 종류를 미리 정의한다”
```

가 아니다.

목표는:

```text
자유로운 기사 제보
        ↓
원문 보존
        ↓
가능한 부분만 구조화
        ↓
예상한 정보
→ 기본 enum

예상 밖 정보
→ OTHER + custom label

기존 위치
→ NODE / SEGMENT

미등록 위치
→ UNRESOLVED + free text
        ↓
원자 지식
        ↓
RAG 검색 가능
```

한 형태로 모든 정보를 수용하는 것이다.

이 구조를 LLM 데이터 생성 프롬프트의 공통 스키마로 사용한다.
