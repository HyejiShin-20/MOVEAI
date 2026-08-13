# mobile — React 기사용 모바일 웹

기사가 쓰는 반응형 웹 화면 전부. **관리자 검수 화면은 `admin-web/`이다.**

API 계약은 `docs/MOVE_AI_05B_API계약.md`.
화면 설계 원칙은 `docs/MOVE_AI_기획디자인_이해자료.pdf` 3부.

**디자인 시안** — [Figma](https://www.figma.com/design/4girj3oH3g2JxyIM5erfUC/Untitled?node-id=0-1)
시안과 아래 원칙이 어긋나면 시안을 따르고 카드 종류·정보 우선순위는 유지한다.

## 스택

```text
React 18 + TypeScript + Vite
라우팅    react-router
HTTP      fetch
녹음      MediaRecorder API (브라우저 마이크 권한 처리)
스타일    모바일 우선 반응형 CSS
```

상태관리 라이브러리는 추가하지 않는다. 화면 4개와 짧은 시연 흐름은 React 기본 상태로 충분하다.

## 화면

```text
S1  배송 목록      GET /api/delivery-jobs
S2  배송 상세      GET /api/delivery-jobs/{id} → [현장 도착] → 차량 확인
S3  Last 100m 안내 POST /api/guidance → next → complete   ★ 시연의 중심
S4  현장 팁 등록   POST /api/reports → PATCH transcript → POST extract
```

`S3`가 이 앱의 중심이다. 나머지는 거기 도달하기 위한 화면이다.

## 설계 규칙

채팅처럼 보이면 안 된다. 기사는 운전 중이거나 카트를 밀고 있으므로 현장 지식은
말풍선이 아니라 짧은 신호 카드로 표시한다.

```text
WARNING    주의   위험·제약은 있으나 해법은 확인되지 않음
ACTION     행동   무엇을 하면 되는지 원문에 명확히 있음
REFERENCE  참고   알아두면 좋지만 당장 행동은 아님 (기본 접힘)
```

주의 카드에 원문에 없는 행동 문구를 붙이지 않는다.

```text
1. 현재 위치 / 단계     2. 다음 이동 행동     3. 주의
4. 추가 팁              5. 다음 버튼
```

지도는 보조이며 차별점은 카드다. 관련 지식이 없는 일반 단계에서는 빈 카드 목록을 허용하고,
라우팅 문구만으로 단계를 진행한다.

## 반드시 구현할 것

- `isRecentlyAdded=true` 카드에 "새로 추가된 팁" 배지를 표시한다.
- 마지막 단계 도착으로 자동 완료하지 않는다. `[배송 완료]` 버튼이 유일한 트리거다.
- `conditionLabel`이 있으면 조건 라벨로 표시한다.
- 360px 너비에서도 핵심 버튼과 카드가 잘리지 않아야 한다.
- 녹음 실패 시 텍스트 직접 입력으로 이어갈 수 있어야 한다.

## 인증과 시연

인증은 구현하지 않고 기사 계정 하나로 고정한다. Chrome 창을 모바일 비율로 줄여 녹화한다.
마이크는 `localhost`에서 브라우저 권한을 허용하고 본선 전에 실제 녹음을 확인한다.

```bash
npm install
npm run dev
```
