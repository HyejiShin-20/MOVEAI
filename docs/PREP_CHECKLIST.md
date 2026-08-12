# 본선 전 준비 체크리스트

> **코드는 미리 짜도 복붙할 수 없고 참고만 가능하다.**
> 따라서 준비의 무게중심은 코드가 아니라 **환경·모델·키·데이터**다.
> 당일 네트워크에서 내려받다가 1~2시간이 날아가는 것이 가장 큰 리스크다.
>
> **설치 방법 자체는 `SETUP.md`에 있다.** 이 문서는 설치 이후의 준비를 다룬다.

---

# 1. 환경 — 가장 중요

당일에 이걸 받으면 늦는다. **미리 받아두면 캐시가 남는다.**

## Docker  (백엔드 담당만)

**R2·R3·R4는 건너뛴다.** 화면 담당은 DB를 직접 쓰지 않고 백엔드 API만 호출한다.
Docker 대신 MariaDB를 직접 설치해도 된다 → `SETUP.md §1-B`

```bash
docker pull mariadb:11.4
```

```bash
docker pull adminer
```

기동까지 한 번 확인한다.

```bash
docker compose up -d
```

```bash
docker compose ps
```

- [ ] MariaDB 이미지 받음
- [ ] `docker compose up -d` 성공
- [ ] Adminer(`localhost:8081`)에서 DB 접속 확인
- [ ] `docker compose down` 후 재기동도 확인

## Python

conda를 쓰든 venv를 쓰든 **패키지는 pip로 설치**한다. conda로 깔면 `requirements.txt`에 안 잡혀 팀원과 환경이 갈린다.

```bash
conda create -n moveai python=3.12 -y
```

```bash
conda activate moveai
```

conda가 없는 팀원은 표준 venv로 동일하게 쓴다.

```bash
python -m venv .venv && .venv\Scripts\activate
```

```bash
pip install -r ai-service/requirements.txt
```

- [ ] 가상환경 생성
- [ ] `pip install -r ai-service/requirements.txt` 성공
- [ ] `python -c "import fastapi, openai; print('ok')"` 통과
- [ ] 팀원 전원이 각자 PC에서 설치 성공

## Java / Gradle

Spring Initializr에서 빈 프로젝트를 받아 한 번 빌드하면 배포판과 의존성이 `~/.gradle`에 캐시된다.
**당일에는 프로젝트를 새로 만들되 캐시는 남는다.**

```bash
./gradlew build
```

- [ ] JDK 17 이상 설치 확인 (`java -version`)
- [ ] 빈 Spring Boot 프로젝트 빌드 1회 성공

## Flutter — ★ 가장 오래 걸린다  (기사 앱 담당만)

설치 방법과 경로 선택은 `SETUP.md §4`. **웹 경로(A)로 시작하면 설치가 몇 배 빠르다.**

```bash
flutter --version
```

```bash
flutter devices
```

- [ ] Flutter 3.24+ 설치 (`flutter --version`)
- [ ] **경로 결정** — A(웹, 권장) / B(Android 포함)
- [ ] `flutter devices` 에 실행 대상이 보임 (A면 Chrome, B면 에뮬레이터)
- [ ] `flutter create` + 실행 1회 성공 (pub·Gradle 캐시 채우기)
- [ ] **마이크 녹음 확인** — A는 브라우저 권한, B는 에뮬레이터 마이크 설정
- [ ] 화면 녹화 방식 결정 → `mobile/README.md`

> B로 갈 경우 Android SDK·에뮬레이터 이미지가 수 GB다. **반드시 본선 전에 받는다.**

## Node (관리자 검수 화면)

```bash
npm create vite@latest _warmup -- --template react-ts
```

```bash
cd _warmup && npm install
```

- [ ] Node 20 이상 (`node -v`)
- [ ] `npm install` 1회 성공 (캐시 채우기)
- [ ] `_warmup` 폴더는 지워도 됨

---

# 2. AI 모델 — 당일 바꿀 수 없다

## STT

**미리 정해야 한다.** 당일 처음 돌렸는데 "2.5톤 택시"로 나오면 대안을 찾을 시간이 없다.

- [ ] 로컬(Whisper)인지 API인지 결정
- [ ] 로컬이면 **가중치 파일 미리 다운로드** (수 GB)
- [ ] `datasets/`의 음성 후보 중 **3건 이상 실제로 녹음해서 돌려봄**
- [ ] 한국어 구어체 인식 확인 — "빡빡해요", "탑차", "1.5톤", "방화문"
- [ ] 숫자가 제대로 나오는지 확인 (톤수·시간이 틀리면 추출이 전부 틀어진다)

인식률이 나쁘면 **기사가 텍스트를 수정하는 화면**이 더 중요해진다. 그건 원래 설계에 있다.

## 임베딩

모델을 바꾸면 저장된 벡터를 전부 다시 만들어야 한다. **당일 변경은 사실상 불가능하다.**

- [ ] 모델 결정
- [ ] **차원 수 확인** → `05A §2-3`의 `embedding_dimension`
- [ ] 한국어 문장 2개로 유사도 계산해 보기 (비슷한 문장이 실제로 가까운지)
- [ ] 146건 임베딩 비용·소요 시간 대략 확인

## LLM (지식 추출)

- [ ] 모델 결정
- [ ] JSON 스키마 강제 출력이 되는지 확인
- [ ] `datasets/`의 transcript 1건으로 추출 시험 → `expected_knowledge_items`와 비교

---

# 3. API 키 — 발급만으로 부족하다

**실제로 호출해서 200을 받아둔다.** 결제 미등록·리전 제한·쿼터 0으로 막히는 경우가 흔하다.

- [ ] LLM 키 발급 + **실제 호출 성공**
- [ ] 임베딩 키 발급 + **실제 호출 성공**
- [ ] STT 키 발급 + **실제 호출 성공** (API 방식인 경우)
- [ ] 잔액·쿼터 확인
- [ ] 키를 팀 내 공유 방법 정해두기 (`.env`는 커밋 금지)

---

# 4. 데이터

- [ ] `python scripts/validate_datasets.py` → 이슈 0건
- [ ] 시연용 신규 제보 문구 확정 (`DEMO_SCRIPT.md §3`)
- [ ] 그 제보가 기존 146건과 겹치지 않는지 확인
- [ ] 시연용 음성 녹음본 준비 (여러 번 녹음해서 잘 나온 것 선택)

---

# 5. 문서와 역할

- [ ] 팀 전원이 `MOVE_AI_기획디자인_이해자료.pdf` 읽음
- [ ] 역할 분담 확정 (`TEAM_ROLES.md`)
- [ ] 하네스 운용자 1명 지정
- [ ] **Figma 시안 확정** — 화면 4개(기사) + 2개(관리자)
      시안이 없으면 `mobile/README.md`·`admin-web/README.md`의 원칙대로 만든다
- [ ] 하네스 시작 프롬프트 위치 확인 (`MOVE_AI_03_START_PROMPT.md`)
- [ ] PPT 뼈대 12장 미리 만들어 둠 (내용은 당일 채움)

---

# 6. 규정 확인

- [ ] **`scripts/validate_datasets.py`가 "미리 작성한 코드"에 해당하는지 확인**
      제품 코드가 아니라 데이터 검증 도구지만, 문제되면 당일 재작성한다(150줄).
- [ ] `requirements.txt` 사전 작성이 허용되는지 확인
- [ ] 데이터셋 사전 준비가 허용되는지 확인 (통상 허용)
- [ ] 제출물 형식·마감 시각 확인

**애매하면 운영진에게 미리 묻는다.** 지적받고 나서 아는 것보다 낫다.

---

# 7. 당일 아침 최종 점검

```
[ ] 노트북 충전기 · 멀티탭
[ ] 인터넷 연결 확인 (테더링 대비책)
[ ] docker compose up -d 로 DB 기동 확인
[ ] 가상환경 활성화 확인
[ ] .env 파일 준비 (키 채워서)
[ ] git 상태 깨끗한지 확인
[ ] 화면 녹화 도구 동작 확인
[ ] 마이크 동작 확인
```
