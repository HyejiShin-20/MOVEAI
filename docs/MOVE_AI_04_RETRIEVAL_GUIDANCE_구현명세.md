# MOVE-AI Retrieval / Guidance 구현 명세

> 목적: 실제 데이터셋(B/C)을 검증한 결과를 바탕으로, Guidance 단계에서 Knowledge를 어떻게 붙일지 코드 수준으로 확정한다.
> 범위: `MOVE_AI_01_MVP_PRD.md` §P0-5, §P0-6 및 `MOVE_AI_팀원용_기능흐름_AI원리_상세설명.md` §24의 상세화.
> 전제: **데이터셋은 현재 상태 그대로 사용한다.** 아래 규칙은 전부 import/런타임 코드에서 흡수한다.

---

# 0. 제품의 검색 경로는 "질문"이 아니다

가장 먼저 고정할 전제다.

```text
사용자가 질문을 입력한다        ← 우리 제품 아님
현재 배송 단계가 질문을 대신한다  ← 우리 제품
```

기사는 아무것도 묻지 않는다. `[다음]`을 누를 뿐이다.
따라서 검색의 입력은 **질문 문장이 아니라 현재 RouteSegment + 차량/시간 Context**다.

이 전제가 아래 모든 설계를 결정한다.

## Gold 데이터의 위치

`rag_test_queries`(B 5개 / C 5개)의 `context`에 들어있는 필드는 실제로 다음이 전부다.

```text
vehicle_class / vehicle_tonnage / vehicle_height_m / vehicle_width_m / movement_mode
```

**현재 위치·세그먼트·시각 필드가 없다.** 즉 Gold는 제품의 주 경로(세그먼트 기반)를 검증하지 못한다.

Gold의 역할을 아래로 한정한다.

| 용도 | Gold 사용 | 근거 |
|---|---|---|
| ConditionEvaluator 정확성 검증 | **사용** | 톤수/높이 경계 케이스가 들어있음 |
| movement_mode 필터 검증 | **사용** | must_not 중 8건이 이 필터로 걸림 |
| embedding 모델 비교 | 사용 | 상대 비교용으로는 유효 |
| 세그먼트 랭킹 가중치 튜닝 | **사용 불가** | context에 세그먼트가 없음 |

세그먼트 랭킹은 데모 Route 2개(`ROUTE_B_01`, `ROUTE_C_01`)를 눈으로 검수하는 smoke test로 확인한다.

---

# 1. 세그먼트 후보 수집 규칙

## 1-1. 문제 — Node 지식이 인접 두 단계에 중복 등장한다

Node 타깃 지식을 `from_node`와 `to_node` 양쪽에서 수집하면 같은 카드가 연속 두 단계에 뜬다.

실제 `ROUTE_B_01` 검증 결과:

```text
seq1 (후문 → 지하램프)     to  : K_B_002, K_B_003, K_B_005, K_B_030
seq2 (지하램프 → 차단기)   from: K_B_002, K_B_003, K_B_005, K_B_030   ← 전부 중복
seq4 (하역장 → 방화문)     to  : K_B_012, K_B_013
seq5 (방화문 → 연결통로)   from: K_B_012, K_B_013                      ← 중복
seq6 (연결통로 → 화물EV)   to  : K_B_014, K_B_029, K_B_034
seq7 (화물EV → 12층)       from: K_B_014, K_B_029, K_B_034             ← 중복
```

## 1-2. 확정 규칙 — "도착 예고" 방식

```text
각 지식은 그 노드에 처음 도달하는 단계에서 한 번만 보여준다.
```

```java
// segment 후보 수집
List<Knowledge> collect(RouteSegment seg, boolean isFirstSegment) {
    var pool = new LinkedHashSet<Knowledge>();
    pool.addAll(bySegmentTarget(seg.getSegmentCode()));   // SEGMENT 직결
    pool.addAll(byNodeTarget(seg.getToNodeCode()));       // 이번 단계에서 도착할 지점
    if (isFirstSegment) {
        pool.addAll(byNodeTarget(seg.getFromNodeCode())); // 출발점은 1단계에서만
    }
    pool.addAll(byPlaceTarget());                          // PLACE 레벨은 항상 후보
    return pool;
}
```

`isFirstSegment` 예외가 필요한 이유: `K_B_001`("배송차량은 후문으로 진입하는 것이 가장 빠르다")은 `SEG_B_01`의 `from_node`에만 붙어 있어, 이 예외가 없으면 영원히 노출되지 않는다.

기사 관점에서도 이 규칙이 맞다 — **도착하기 전에 미리 알아야** 대비할 수 있다.

## 1-3. 이 규칙 적용 후 단계별 후보 수 (ROUTE_B_01)

```text
seq1  6개    seq2  2개    seq3  1개    seq4  2개
seq5  1개    seq6  4개    seq7  3개
```

Top-K를 3으로 두면 대부분 단계에서 필터링이 거의 일어나지 않는다. **이 규모에서는 수집 규칙이 랭킹보다 중요하다.**

---

# 2. movement_mode 필터 — 반드시 넣는다

검증 결과 실제로 일하는 유일한 강력 필터다.

- Gold must_not 19건 중 **8건**이 이 필터로 차단됨
- `SEG_B_06`(VEHICLE 주행 중) 후보 6개 중 3개가 보행/카트 지식(`K_B_009`, `K_B_010`, `K_B_011` — 회전문에 카트가 낀다)이었고, 차로 이동 중인 기사에게는 무의미하다

```java
boolean movementMatches(Knowledge k, RouteSegment seg) {
    return k.getMovementMode() == GENERAL
        || k.getMovementMode() == seg.getMovementMode();
}
```

`GENERAL`은 항상 통과시킨다(B/C 합쳐 6건).

**Hard filter로 적용한다.** 감점이 아니다.

---

# 3. ConditionEvaluator — 데이터셋 실측 기반 명세

## 3-1. ⚠ 치명적 발견: `min_tonnage`의 포함/배타가 B와 C에서 반대다

```text
K_B_005  min_tonnage=1.0  "1톤을 초과하는 차량은 지하주차장으로 내려갈 수 없다."
K_C_005  min_tonnage=5.0  "5톤 이상 트럭은 2번 게이트로 진입한다."
```

같은 필드인데 B는 **초과(>)**, C는 **이상(≥)**이다. 그리고 Gold가 정확히 그 경계를 찌른다.

```text
QUERY_B_02  톤수 1.0 → K_B_005는 must_not   ⇒ 1.0 > 1.0 = false 여야 함 (배타)
QUERY_C_01  톤수 5.0 → K_C_005는 expected   ⇒ 5.0 ≥ 5.0 = true  여야 함 (포함)
```

`>=` 로 통일하면 QUERY_B_02가 깨지고, `>` 로 통일하면 QUERY_C_01이 깨진다. **한쪽을 고르면 반드시 하나는 틀린다.**

### 해결 — import 시점에 statement 문구로 포함 여부를 확정한다

데이터셋은 손대지 않고, DatasetImportService에서 파생 컬럼을 만든다.

```java
// knowledge_conditions.min_tonnage_inclusive 컬럼을 추가하고 import 때 채운다
boolean inclusive = !statement.contains("초과") && !statement.contains("넘");
// "이상" 또는 표현 없음 → 포함(>=),  "초과"/"넘" → 배타(>)
```

실측 확인 — 톤수·높이 조건이 붙은 지식은 B/C 통틀어 6건뿐이라 전수 검증 가능하다.

| code | 필드 | statement 표현 | 판정 |
|---|---|---|---|
| K_B_002 | height 2.3 | 초과 | 배타 |
| K_B_005 | min_ton 1.0 | 초과 | 배타 |
| K_B_006 | min_ton 1.0 | 초과 | 배타 |
| K_C_005 | min_ton 5.0 | 이상 | 포함 |
| K_C_006 | height 3.6 | (까지는) | 포함 |
| K_C_038 | height 3.6 | 그보다 높은 | 배타 |

6건이므로 **import 후 이 표를 단위 테스트로 고정**하는 것이 가장 안전하다. 문자열 규칙이 어긋나면 테스트가 바로 잡는다.

## 3-2. ⚠ 두 번째 발견: `max_vehicle_height_m`의 부호가 지식마다 반대다

```text
K_B_002  h=2.3  "2.3미터를 초과하는 탑차는 진입할 수 없다"   → 내 차가 2.3 초과일 때 적용
K_C_006  h=3.6  "차량 높이 3.6m까지는 진입할 수 있다"        → 내 차가 3.6 이하일 때 적용
K_C_038  h=3.6  "제한은 3.6m이며 그보다 높은 탑차는 진입 불가" → 내 차가 3.6 초과일 때 적용
```

필드 이름이 `max_...`라고 해서 항상 "초과하면 걸린다"가 아니다. **부호는 `access_state`/`fact_type`이 결정한다.**

```java
boolean heightApplies(Knowledge k, double vehicleH) {
    Double limit = k.getConditions().getMaxVehicleHeightM();
    if (limit == null || vehicleH <= 0) return true;

    boolean isRestriction = k.getAccessState() == PROHIBITED
                         || k.getFactType()   == RESTRICTION;
    return isRestriction
        ? vehicleH >  limit    // 제한: 넘을 때 이 경고가 적용됨
        : vehicleH <= limit;   // 허용: 이내일 때 이 안내가 적용됨
}
```

톤수(`min_tonnage`)는 B/C 3건 모두 "그 이상/초과일 때 적용" 방향이 같아 부호 분기가 필요 없다.

## 3-3. `extra_condition_text` — 필터로 쓰지 말 것

B+C 77개 중 **34개**가 이 필드를 채우고 있다. 압도적 1위다.

```text
"비 오는 날"        "큰 박스인 경우"      "작은 바퀴 카트"
"오후 5시 전후"     "야간"                "앞차가 이미 주차되어 있는 경우"
```

코드로 비교 불가능하고, 기사가 입력하는 값도 아니다. **필터로 쓰면 안 된다.**

```text
용도: 카드 UI의 조건 라벨로 그대로 출력한다.
   ⚠ 비 오는 날 — 경사로가 미끄럽다
```

이러면 구조화 실패가 오히려 UX 정보가 된다. 발표에서도 "구조화하지 못한 조건은 버리지 않고 원문 그대로 보존해 기사에게 보여준다"로 설명 가능하다.

## 3-4. 시간·요일 조건 — 데모 킬러

시간 조건이 걸린 지식은 B/C 통틀어 6개뿐이지만, 그중 데모 핵심 후보가 있다.

```text
K_B_014  12:00~13:00  화물용 엘리베이터는 12시부터 13시까지 대기 시간이 길다
K_B_030  08:30~09:00  출근 차량 때문에 지하 진입 램프가 정체된다
K_B_036  22:00~        후문은 22시 이후 폐쇄되어 진입할 수 없다
K_C_030  10:00~11:00  지게차 교차구간은 10~11시에 지게차 통행이 많다
K_B_031  SAT,SUN      주말에는 지하 차단기가 자동으로 열리지 않는다
```

`K_B_014`는 `ROUTE_B_01` seq6의 핵심 카드인데, **발표가 15시면 필터에 걸려 사라진다.**

```java
// GuidanceSession에 명시적 컨텍스트 시각을 둔다. 기본값 now.
public class GuidanceContext {
    private LocalDateTime contextTime = LocalDateTime.now(); // 데모 시 override
}
```

`POST /api/guidance`가 `context_time`을 옵션으로 받게 한다. 10줄이고 데모를 구한다.

**시간/요일 불일치는 hard filter로 제거한다.** 단, 위 override가 있으므로 데모는 안전하다.

---

# 4. UNRESOLVED 지식 — embedding이 실제로 필요한 유일한 곳

## 4-1. 규모

```text
B: 37개 중 7개 UNRESOLVED
C: 40개 중 12개 UNRESOLVED
합계 19 / 77  =  약 25%
```

이들은 `target_type = UNKNOWN`이라 **어떤 노드·세그먼트에도 붙지 않는다.** §1의 수집 규칙만 쓰면 전체 지식의 4분의 1이 데모에서 단 한 번도 나오지 않는다.

예:
```text
K_B_015  "하역장 옆에서 카트를 세워두고 기다리는 대기 공간"
K_B_020  "정문에서 로비 쪽으로 도는 지상 순환로 중간의 화단 옆 좁아지는 구간"
```

`K_B_020`은 `ROUTE_B_02` seq1에서 반드시 떠야 하는 정보지만 구조적으로는 도달 경로가 없다.

## 4-2. 그래서 Top-K 자리를 분리한다

```text
Top-K = 5
  ├─ 4자리: 구조 연결 지식 (SEGMENT/NODE/PLACE 타깃)
  └─ 1자리: UNRESOLVED 지식 중 의미 유사도 최상위 1개  ← embedding의 본진
```

이렇게 하면:
- 구조 연결 지식이 UNRESOLVED를 항상 이겨서 25%가 사장되는 일이 없다
- embedding이 **실제로 기여하는 지점이 명확해진다** — 발표에서도 설명하기 좋다

UNRESOLVED 후보에도 §2 movement 필터와 §3 조건 필터는 동일하게 적용한다.

---

# 5. Embedding 텍스트 / Query 텍스트

## 5-1. Knowledge embedding_text

PRD §P0-4의 예시에서 **`장소:` 줄을 뺀다.**

```text
위치: {target 이름 or target_free_text}
이동: {movement_mode} / {traversal_method}
내용: {statement}
행동: {action_text}
```

- `장소:` 제거 이유 — Place는 이미 SQL hard filter다. 같은 place 후보 전부가 동일 토큰을 공유하므로 변별력이 0이고 벡터만 희석시킨다.
- `조건:` 제거 이유 — 실측 결과 **statement가 이미 조건을 문장 안에 포함**하고 있다.
  ```text
  K_B_014  "화물용 엘리베이터는 12시부터 13시까지 이사 물량과 겹쳐 대기 시간이 길다."
  K_B_036  "후문은 22시 이후 폐쇄되어 진입할 수 없다."
  ```
  별도로 붙이면 같은 정보가 두 번 들어가 가중치만 왜곡된다.
- `action_text`가 null이면 그 줄 자체를 생략한다(B+C에 다수 존재).

## 5-2. Query text — 세그먼트에서 조립, 같은 포맷으로

`기능흐름_AI원리` §24 3단계의 예시 문장은 쓰지 않는다.

```text
✗ "가온스퀘어에서 2.5톤 탑차로 배송 중이며 현재 하역장에서 방화문으로
    카트를 이동하고 있다. 12시 20분 현재 필요한 주의사항을 찾는다."
```

문제 두 가지 — knowledge 쪽과 형식이 완전히 다르고, `"필요한 주의사항을 찾는다"`는 내용이 아닌 **메타 언어**라 대응물이 없는 방향으로 벡터를 끌고 간다.

```text
✓ 위치: 하역장 방화문
  이동: PEDESTRIAN / CART
  내용: 카트에 물품을 적재하여 방화문까지 이동한다
```

`내용:` 줄은 해당 RouteSegment의 `instruction`을 그대로 쓴다. 데이터셋에 이미 전 세그먼트가 채워져 있다.

`위치:`는 `to_node`의 이름을 쓴다(§1의 "도착 예고" 규칙과 일치).

---

# 6. 랭킹

## 6-1. 공식

```text
score = cosine
      + 0.20 × (target이 이번 SEGMENT에 직결)
      + 0.12 × (target이 to_node)
      + 0.06 × (traversal_method 일치)
      + 0.05 × (최근 24시간 내 승인)
```

## 6-2. 가산점이 cosine보다 큰 것은 의도된 것이다

cosine은 보통 0.75~0.92의 좁은 구간에 몰린다. 이 규모(단계당 후보 1~6개)에서는 **구조적 연결이 의미 유사도보다 신뢰도가 높다.** 세그먼트에 직결된 지식이 다른 곳의 유사 문장에 밀리면 안 된다.

## 6-3. 최근 승인 가산점 (+0.05)

데모의 핵심 순간 — 방금 승인한 지식이 다음 Guidance에 등장 — 을 보장한다.
제품 관점에서도 "최신 현장 정보 우선"으로 방어 가능하다.

응답 DTO에 `is_recently_added: true`를 실어 UI에서 **"방금 추가된 팁"** 배지를 띄운다. 발표에서 시각적으로 짚을 지점이 생긴다.

---

# 7. 계산은 Spring에서 한다

Python으로 벡터를 넘기지 않는다.

```text
Spring  ── POST /embed  (질의문 1개) ──▶  Python
Spring  ◀── vector ────────────────────
Spring  : 자기 DB에서 후보 벡터 로드 → Java로 cosine → 랭킹 → 응답 조립
```

이유: 후보 벡터를 HTTP로 보내면 단계마다 수백 KB~1MB가 오간다. cosine은 내적/노름, Java로 20줄이다. 서비스 경계를 넘을 이유가 없다.

PRD §7의 `POST /similarity-search`는 **구현하지 않는다**(원문에도 "선택"으로 표기됨).

Python이 담당하는 것: `/stt`, `/extract-knowledge`, `/embed`. 이 셋뿐이다.

## 캐싱하지 않는다

place당 후보 40개 이하다. 매 요청 DB 조회로 충분하다.
캐시를 넣으면 **승인 직후 새 지식이 캐시에 없어 데모가 깨지는** 위험이 생긴다. 넣지 않는다.

## 승인 → embedding은 동기 처리

```text
관리자 [승인] 클릭
  → Draft 확인·최종 payload 확정
  → embedding_text 생성
  → /embed 호출 (동기, 스피너 표시)        ← DB transaction 밖
  → DB transaction 시작
     PUBLISHED + embedding + review + APPROVED 저장
  → 커밋 → 200
```

비동기로 빼면 "승인 직후 RAG 재조회"에서 레이스가 생기므로 HTTP 요청 자체는 동기로 유지한다.
다만 느리거나 timeout 가능한 외부 호출 중에는 DB 트랜잭션을 열지 않는다. `/embed` 실패 시 DB는
그대로 두고 명시적 에러와 재시도 버튼을 제공한다. DB 저장 실패 시에는 저장 묶음 전체를 롤백한다.

---

# 8. 최종 파이프라인

```text
POST /api/guidance/{id}/next
        │
        ▼
[1] 현재 RouteSegment 확정
        │
        ▼
[2] 후보 수집  §1
     SEGMENT직결 + to_node(+1단계는 from_node) + PLACE
        │
        ▼
[3] movement_mode hard filter  §2
        │
        ▼
[4] ConditionEvaluator hard filter  §3
     톤수(포함/배타 파생컬럼) · 높이(부호 분기) · 시간/요일(context_time)
     ※ extra_condition_text는 필터 아님 — 라벨로만 사용
        │
        ▼
[5] Query text 조립 → /embed  §5-2
        │
        ▼
[6] cosine (Spring/Java)  §7
        │
        ▼
[7] 랭킹  §6
        │
        ▼
[8] Top-K 조립  §4-2
     구조연결 4 + UNRESOLVED 1
        │
        ▼
     Warning / Action / Tip 카드
```

## 카드 분류 규칙

`usage_scope`를 그대로 UI에 매핑한다.

```text
WARNING_ONLY      → ⚠ 주의 카드   (action_text 없음)
ACTION_GUIDANCE   → → 행동 카드
ROUTE_GUIDANCE    → → 이동 카드
REFERENCE_ONLY    → 참고 카드 (접힘 상태 기본)
```

`REFERENCE_ONLY`는 C에 7건 있다. 기본 노출하면 화면이 산만해지므로 접어둔다.

---

# 9. 데이터 이슈 처리 (코드에서 흡수)

데이터셋은 수정하지 않으므로 import 또는 렌더링에서 처리한다.

| 항목 | 내용 | 처리 |
|---|---|---|
| `K_B_007` | ACTION_GUIDANCE인데 `action_text` 없음 | 렌더링 시 `statement`로 대체 |
| `K_B_024` | ROUTE_GUIDANCE인데 `action_text` 없음 | 동일 |
| `K_B_029` | ROUTE_GUIDANCE인데 `action_text` 없음 | 동일 |

```java
String actionLine = k.getActionText() != null ? k.getActionText() : k.getStatement();
```

B/C 나머지 정합성은 검증 통과했다 — 코드 중복 없음, 참조 무결성 정상, `source_excerpt` 전건 transcript 내 존재, Route 세그먼트 순서 연속, enum 허용목록 준수, RAG 코드 참조 유효.

---

# 10. 검증 체크리스트

## 단위 테스트 (필수)

```text
[ ] min_tonnage 포함/배타 6건 표(§3-1)가 전부 기대대로 판정되는가
[ ] K_B_002(2.3 초과 적용) / K_C_006(3.6 이하 적용) 부호가 반대로 나오는가
[ ] QUERY_B_02(1.0톤)에서 K_B_005·K_B_006이 제외되는가
[ ] QUERY_C_01(5.0톤)에서 K_C_005가 살아남는가
[ ] movement 필터가 SEG_B_06에서 K_B_009·K_B_010·K_B_011을 제거하는가
```

## Guidance smoke test (필수)

```text
[ ] ROUTE_B_01 7단계에서 같은 지식이 연속 두 단계에 중복 노출되지 않는가
[ ] Demo fixture B의 시연 단계마다 예상 Knowledge가 1개 이상 뜨는가
[ ] 일반 Guidance에서 relevant Knowledge가 없을 때 cards=[]를 허용하는가
[ ] K_B_001이 seq1에 뜨는가 (from_node 예외 규칙 동작)
[ ] context_time=12:30 일 때 seq6에 K_B_014가 뜨는가
[ ] context_time=15:00 일 때 K_B_014가 사라지는가
[ ] UNRESOLVED 슬롯에 K_B_015 또는 K_B_020이 등장하는가
```

## End-to-end (데모 리허설)

```text
[ ] 새 제보 → 승인 → 같은 Route 재시작 시 새 카드가 Top-K 안에 뜨는가
[ ] "방금 추가된 팁" 배지가 표시되는가
```

---

# 11. 배송 건 → 길안내 → Last 100m 연결

## 11-1. 우리가 만드는 구간과 만들지 않는 구간

```text
┌─ 배송 목록 ─┬─ 일반 도로 주행 ─┬───── Last 100m ─────┐
│             │                  │                     │
│  MOVE-AI    │  카카오/티맵     │      MOVE-AI        │
│             │  (만들지 않음)   │                     │
└─────────────┴──────────────────┴─────────────────────┘
                       ▲                 ▲
                  주소까지 안내      여기부터 우리 차별점
```

**일반 도로 주행 구간은 구현하지 않는다.** 이미 완성도 높은 서비스가 있고, 우리 서비스의 정의 자체가 "주소 도착 이후"다. 그 구간을 다시 만들면 발표 메시지도 흐려진다("지도는 건물까지, 배송은 그 다음부터").

MVP에서의 처리:

```text
[길안내 시작] 버튼 → 카카오맵 deep link 또는 정적 지도 이미지
                    (연동 여부는 시간에 따라 결정, P0 아님)
```

## 11-2. 전환 트리거는 기사의 수동 버튼

```text
✗ GPS geofence 자동 전환
✓ [현장 도착] 버튼
```

GPS를 쓰지 않는 이유:
- 데모 환경(실내 발표장)에서 좌표를 신뢰할 수 없다
- 지하 진입로·건물 내부는 실제로도 GPS가 흔들린다
- 실패 시 데모가 그냥 멈춘다

제품 관점에서도 방어된다 — **어디서 차를 세울지는 기사가 판단**하는 것이고, 그 지점이 Last 100m의 시작이다. 자동 전환보다 이쪽이 현장에 맞다.

## 11-3. ★ Route 선택 = ConditionEvaluator (데이터에 이미 있음)

가장 중요한 연결 고리다. **배송 건 하나가 곧 Route 하나가 아니다.** 차량과 물품에 따라 갈린다.

### B — 차량 톤수로 갈리는 케이스

```text
ROUTE_B_01  후문 → 지하2층 하역장 → 12층    maxTon=1.0  maxH=2.3   is_default=true
ROUTE_B_02  정문 → 지상 로비    → 12층    minTon=1.0
                                            ↑ 목적지 동일(12층 안내데스크)
```

두 Route의 제약이 1.0톤에서 **정확히 상호배타적으로** 맞물려 있다. 우연이 아니라 데이터 작성자의 의도다. 그리고 Knowledge가 같은 이야기를 한다.

```text
K_B_005  "1톤을 초과하는 차량은 지하주차장으로 내려갈 수 없다."      → B_01 차단 근거
K_B_006  "1톤을 초과하는 차량은 정문으로 진입해 로비 앞에 정차한다."  → B_02 = SEG_B_06
```

### C — 물품 종류(목적지)로 갈리는 케이스

```text
ROUTE_C_01  1번 게이트 → 상온 배송 인계점    (제약 없음)
ROUTE_C_02  2번 게이트 → 냉장 배송 인계점    (제약 없음)
```

목적지 자체가 다르다. 차량 제약은 둘 다 없고 `is_default`가 **둘 다 true**다. 따라서 `is_default`만으로는 고를 수 없다.

### 확정 알고리즘

두 케이스를 모두 처리하려면 순서가 중요하다.

```java
Route selectRoute(DeliveryJob job, VehicleContext v) {
    var candidates = routeRepo.findByPlace(job.getPlaceId()).stream()
        // [1] 목적지 일치 — C 케이스를 여기서 가른다
        .filter(r -> r.getDestinationNodeCode().equals(job.getDestinationNodeCode()))
        // [2] 차량 제약 — B 케이스를 여기서 가른다
        .filter(r -> vehicleFits(r, v))
        .toList();

    if (candidates.isEmpty()) throw new NoRouteAvailableException();
    return candidates.stream()
        .filter(Route::isDefault).findFirst()
        .orElse(candidates.get(0));
}

boolean vehicleFits(Route r, VehicleContext v) {
    if (r.getVehicleClass() != null && v.getVehicleClass() != r.getVehicleClass()) return false;
    // 경계 규칙은 §3-1과 동일하게 통일한다
    if (r.getMaxTonnage() != null && v.getTonnage() >  r.getMaxTonnage()) return false; // 이하 허용
    if (r.getMinTonnage() != null && v.getTonnage() <= r.getMinTonnage()) return false; // 초과만 허용
    if (r.getMaxVehicleHeightM() != null && v.getHeightM() > r.getMaxVehicleHeightM()) return false;
    if (r.getMaxVehicleWidthM()  != null && v.getWidthM()  > r.getMaxVehicleWidthM())  return false;
    return true;
}
```

`maxTonnage`는 **이하 허용(≤)**, `minTonnage`는 **초과만 허용(>)**으로 둔다. 이래야 정확히 1.0톤일 때 `ROUTE_B_01`만 남고 두 Route가 동시에 살아남지 않는다. `K_B_005`의 "초과" 문구와도 일치한다.

### 후보가 0개일 때 숨기지 않는다

```text
이 차량으로 진입 가능한 등록된 경로가 없습니다.
현장 확인이 필요합니다.
```

조용히 기본 Route로 떨어뜨리면 안 된다. 2.5톤 차량을 maxH 2.3m 램프로 보내는 안내가 나가면 그게 사고다.

## 11-4. 데이터 모델 — 지금 없는 것

`delivery_jobs`는 PRD §11 최소 테이블에도, 데이터셋에도 **의도적으로 빠져 있다**(데이터셋 구조 문서 §2에서 생성 제외 항목으로 명시). 배송 건 선택 흐름을 만들려면 최소한이 필요하다.

```sql
-- 신규. 시드 3~4건이면 충분하다.
CREATE TABLE delivery_jobs (
  id                     BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_code               VARCHAR(32)  NOT NULL UNIQUE,  -- JOB_B_01
  place_id               BIGINT       NOT NULL,
  destination_node_id    BIGINT       NOT NULL,         -- Route 선택 [1]단계 입력
  recipient_label        VARCHAR(100),                  -- "12층 입주사"
  address_text           VARCHAR(255),                  -- 일반 내비 목적지
  item_summary           VARCHAR(100),                  -- "상온 / 박스 3"
  status                 VARCHAR(20)  NOT NULL          -- READY / IN_PROGRESS / DONE
);
```

차량 정보를 담을 곳도 없다. `users`에 고정 컬럼으로 넣지 말고 **배송 시작 시점의 입력값**으로 다룬다.

```java
// GuidanceSession 생성 요청에 포함
public record VehicleContext(
    VehicleClass vehicleClass,  // TRUCK
    Double tonnage,             // 2.5
    Double heightM,             // 2.7
    Double widthM
) {}
```

이유 두 가지 — 실제로도 기사 차량은 날마다 바뀔 수 있고, **데모에서 차량을 즉석에서 바꿔 Route가 갈리는 장면을 보여줄 수 있다**(§11-7).

## 11-5. API 시퀀스

```text
GET  /api/delivery-jobs?status=READY
        → 배송 목록 화면

GET  /api/delivery-jobs/{id}
        → 주소 / 수취인 / 물품 / place 정보
        → [길안내 시작] 은 외부 지도 deep link (백엔드 관여 없음)

POST /api/guidance
     { job_id, vehicle: {class, tonnage, height_m, width_m}, context_time? }
        → [1] Route 선택 (§11-3)
        → [2] GuidanceSession 생성, current_sequence = 1
        → [3] 1단계 Knowledge 조립 (§8 파이프라인)
        ← { session_id, route: {name, total_steps}, current_step: {...} }

POST /api/guidance/{id}/next
        → current_sequence += 1 → §8 파이프라인 재실행
        ← current_step

POST /api/guidance/{id}/complete
        → session DONE, delivery_job.status = DONE
```

`context_time`은 §3-4의 데모 안전장치다. 생략하면 서버 시각.

**Route는 세션 생성 시 한 번 확정하고 중간에 바꾸지 않는다.** 세그먼트 인덱스가 흔들리면 Guidance 상태가 깨진다.

## 11-6. 전체 흐름 한 장

```text
[배송 목록]
    │  job 선택
    ▼
[배송 상세]  주소 · 수취인 · 물품
    │  [길안내 시작]
    ▼
( 카카오/티맵 — 우리 구현 아님 )
    │  [현장 도착]
    ▼
[차량 확인]  톤수 / 높이            ← 데모에서 여기를 바꾼다
    │  POST /api/guidance
    ▼
┌───────────────────────────────┐
│ Route 선택  §11-3              │
│   목적지 필터 → 차량 제약 필터  │
└───────────────────────────────┘
    │
    ▼
[Last 100m]  단계별 반복
    │
    │   각 단계마다 §8 파이프라인
    │     후보수집 → movement필터 → 조건필터
    │     → query embed → cosine → 랭킹 → Top-K
    │
    │  [다음] × N
    ▼
[배송 완료]  기사 버튼이 최종 트리거
```

## 11-7. ★ 데모 시나리오 — 같은 배송 건, 차량만 바꾼다

B 데이터로 바로 만들 수 있는 가장 강한 장면이다.

```text
1회차 — 1톤 차량
   Route: 후문 → 지하2층 하역장 → 방화문 → 연결통로 → 화물EV → 12층
   7단계. 방화문·배수구 턱·화물 엘리베이터 혼잡 카드가 뜬다.

2회차 — 같은 배송 건, 2.5톤으로 변경
   Route: 정문 → 지상 로비 → 승객용 EV → 12층
   3단계. 완전히 다른 경로. 회전문·카트 끼임 카드가 뜬다.
```

전달되는 메시지:

> 같은 주소, 같은 수취인인데 **차량이 다르면 들어가는 길 자체가 다릅니다.**
> 일반 내비는 이걸 구분하지 못합니다.

여기에 §6-3의 신규 승인 지식까지 얹으면 데모 한 사이클이 완성된다.

## 11-8. 스코프 가드

```text
✗ 카카오내비 SDK 실연동 / 실시간 재탐색
✗ GPS 기반 자동 단계 전환
✗ 실내 측위
✗ 배송 순서 최적화
✗ PDA / WMS / TMS 연동
✗ 여러 배송 건 묶음 처리
```

`delivery_jobs`는 **Route 선택의 입력을 제공하는 최소 껍데기**다. 배송 관리 기능으로 키우지 않는다.
