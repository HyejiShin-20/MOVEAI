# 설치 가이드

> **개발 경험이 없어도 따라할 수 있게 썼다.** 순서대로 하면 된다.
> 본선 전에 **미리** 끝내둔다. 당일에 받으면 네트워크 때문에 1~2시간이 날아간다.
>
> 모르는 게 나오면 넘어가지 말고 팀에 물어본다. 반쯤 설치된 상태가 제일 위험하다.

---

# 0. 시작하기 전에

## 명령어는 어디에 치나

**터미널**(명령 프롬프트)에 친다.

- **Windows** — 시작 버튼 → `PowerShell` 검색 → 실행
- **Mac** — `Command + Space` → `터미널` 검색 → 실행

프로젝트 폴더로 먼저 이동한다. 폴더 경로는 각자 다르다.

```bash
cd E:\MOVE-AI
```

Mac이면 이런 식이다.

```bash
cd ~/MOVE-AI
```

## 문서에 나오는 표기

```
$ 나 > 는 치지 않는다.  프롬프트 표시일 뿐이다.
명령을 친 뒤 Enter 를 누른다.
```

## 내 역할에 따라 필요한 것이 다르다

전부 설치할 필요 없다.

| 역할 | 필요한 것 |
|---|---|
| **전원** | Git · Docker |
| 백엔드 (R1) | + Java · Python |
| 기사 앱 (R2) | + Flutter |
| 관리자 화면 (R3) | + Node.js |
| 발표·기획 (R4) | 전원 항목만. 나머지는 안 깔아도 된다 |

역할은 `TEAM_ROLES.md`에 있다.

---

# 1. 전원 공통

## 1-1. Git

이미 깔려 있는 경우가 많다. 먼저 확인한다.

```bash
git --version
```

`git version 2.xx.x` 처럼 나오면 **이미 설치된 것이다. 넘어간다.**
`명령을 찾을 수 없습니다` 라고 나오면 [git-scm.com](https://git-scm.com/downloads)에서 받는다.
설치 중 옵션은 **전부 기본값**으로 두면 된다.

## 1-2. Docker Desktop

데이터베이스(MariaDB)를 띄우는 데 쓴다. 직접 설치하지 않고 도커가 대신 띄워준다.

[docker.com](https://www.docker.com/products/docker-desktop/)에서 받아 설치한 뒤 **실행해 둔다.**
설치 후 확인:

```bash
docker --version
```

> **Windows 주의** — 설치 중 "WSL 2를 설치하라"는 안내가 나올 수 있다.
> 안내대로 따라하면 되고, 재부팅이 필요할 수 있다.
> Docker Desktop이 **실행 중이어야** 아래 명령이 동작한다. 트레이 아이콘이 켜져 있는지 본다.

### 이미지 미리 받아두기 ★

**이게 이 문서에서 가장 중요한 명령이다.** 당일에 받으면 오래 걸린다.

```bash
docker pull mariadb:11.4
```

```bash
docker pull adminer
```

### 실제로 뜨는지 확인

프로젝트 폴더에서:

```bash
docker compose up -d
```

```bash
docker compose ps
```

두 줄(`moveai-mariadb`, `moveai-adminer`)이 `running` 으로 보이면 성공이다.

브라우저에서 `http://localhost:8081` 을 열면 DB 접속 화면이 나온다.

```
시스템    MySQL
서버      mariadb
사용자    moveai
비밀번호  moveai
데이터베이스  moveai
```

확인이 끝나면 꺼둔다.

```bash
docker compose down
```

---

# 2. Python  (백엔드·AI 담당)

**버전은 3.12로 통일한다.** 팀원마다 다르면 설치가 서로 안 맞는다.

## 가상환경이 뭔가

프로젝트별로 **패키지를 따로 담아두는 상자**다.
없어도 동작하지만, 다른 프로젝트와 버전이 충돌할 수 있다.

아래 **A · B · C 중 하나만** 고르면 된다.

---

## 방법 A — conda  (권장)

Anaconda 또는 Miniconda가 이미 있다면 이걸 쓴다.

```bash
conda create -n moveai python=3.12 -y
```

만들고 나면 **매번 이 명령으로 켠다.** 터미널을 새로 열 때마다 필요하다.

```bash
conda activate moveai
```

켜지면 프롬프트 앞에 `(moveai)` 가 붙는다. 그게 켜졌다는 표시다.

```bash
pip install -r ai-service/requirements.txt
```

> **주의** — conda를 쓰더라도 패키지는 **`pip`로 설치한다.**
> `conda install`로 깔면 `requirements.txt`에 안 잡혀서 팀원과 환경이 갈린다.

---

## 방법 B — venv  (conda가 없을 때)

파이썬에 기본으로 들어 있는 기능이다. 따로 설치할 게 없다.

먼저 파이썬이 있는지 확인한다.

```bash
python --version
```

`3.12.x` 가 아니면 [python.org](https://www.python.org/downloads/)에서 3.12를 받는다.

> **Windows 설치 주의** — 첫 화면의 **`Add python.exe to PATH`** 를 반드시 체크한다.
> 이걸 놓치면 터미널에서 `python` 명령을 못 찾는다.

가상환경을 만든다.

```bash
python -m venv .venv
```

**켜는 명령이 OS마다 다르다.**

Windows PowerShell:

```bash
.venv\Scripts\Activate.ps1
```

Mac / Linux:

```bash
source .venv/bin/activate
```

켜지면 프롬프트 앞에 `(.venv)` 가 붙는다.

```bash
pip install -r ai-service/requirements.txt
```

> **Windows에서 "이 시스템에서 스크립트를 실행할 수 없습니다" 오류가 나면** — 아래를 한 번 실행하고 다시 시도한다.
> ```
> Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
> ```

---

## 방법 C — 가상환경 없이  (가장 간단)

명령이 하나뿐이라 제일 쉽다. 다른 파이썬 프로젝트를 안 쓴다면 이걸로 충분하다.

```bash
pip install -r ai-service/requirements.txt
```

**단점** — 다른 프로젝트와 패키지 버전이 충돌할 수 있다.
이 노트북에서 파이썬을 이 프로젝트에만 쓴다면 문제없다.

> `pip: 명령을 찾을 수 없습니다` 라고 나오면 `pip` 대신 `python -m pip` 로 바꿔 친다.
> ```
> python -m pip install -r ai-service/requirements.txt
> ```

---

## 설치 확인 (A·B·C 공통)

```bash
python -c "import fastapi, openai, numpy; print('ok')"
```

`ok` 가 나오면 끝이다.

---

# 3. Java  (백엔드 담당)

**JDK 17 이상**이 필요하다.

```bash
java -version
```

`17` 이상이면 넘어간다. 없으면 [Temurin](https://adoptium.net/)에서 **17 LTS**를 받는다.

## 캐시 미리 채우기

빈 프로젝트를 한 번 빌드해두면 필요한 라이브러리가 미리 받아진다.
**당일에는 프로젝트를 새로 만들지만 캐시는 남는다.**

1. [start.spring.io](https://start.spring.io) 접속
2. Gradle - Groovy / Java / Spring Boot 3.4.x / Java 17 선택
3. `Spring Web`, `Spring Data JPA`, `MariaDB Driver` 추가
4. GENERATE → 압축 풀기
5. 그 폴더에서:

```bash
./gradlew build
```

Windows면:

```bash
gradlew.bat build
```

`BUILD SUCCESSFUL` 이 나오면 성공이다. **이 폴더는 지워도 된다.**

---

# 4. Flutter  (기사 앱 담당) ★ 가장 오래 걸린다

SDK만 1GB가 넘고, 안드로이드 관련까지 받으면 **수 GB**다.
**반드시 미리 한다.** 당일에 하면 오전이 통째로 날아간다.

## 4-1. 설치

[docs.flutter.dev/get-started](https://docs.flutter.dev/get-started/install)의 OS별 안내를 따른다.
Android Studio도 함께 설치하는 것을 권한다 — 에뮬레이터와 안드로이드 SDK가 같이 깔린다.

```bash
flutter --version
```

`3.24` 이상이면 된다.

## 4-2. 진단 — 경고가 0건이어야 한다

```bash
flutter doctor
```

항목마다 체크 표시가 나온다. **`[!]` 나 `[X]` 가 하나도 없어야 한다.**

가장 흔한 문제는 안드로이드 라이선스 미동의다. 이 명령으로 해결된다.

```bash
flutter doctor --android-licenses
```

`y` 를 여러 번 입력하면 된다.

## 4-3. 에뮬레이터 준비

Android Studio → `Device Manager` → 가상 기기 하나 생성 후 실행한다.

## 4-4. 캐시 채우기 + 동작 확인

```bash
flutter create _warmup
```

```bash
cd _warmup
```

```bash
flutter run
```

에뮬레이터에 앱이 뜨면 성공이다. **`_warmup` 폴더는 지워도 된다.**

## 4-5. ★ 마이크 녹음 확인

**이걸 꼭 확인한다.** 기사 앱에서 음성 녹음을 해야 하는데,
에뮬레이터에서 마이크가 안 잡히면 **시연 방식을 바꿔야 한다.**
당일에 알면 대응할 시간이 없다.

에뮬레이터 설정에서 마이크가 호스트(노트북) 마이크로 연결되는지 본다.
안 되면 팀에 알린다 — 실기기로 찍거나 텍스트 입력으로 대체하는 계획이 이미 있다.

---

# 5. Node.js  (관리자 화면 담당)

**Node 20 이상**이 필요하다.

```bash
node -v
```

없거나 20 미만이면 [nodejs.org](https://nodejs.org/)에서 **LTS** 버전을 받는다.

## 캐시 채우기

```bash
npm create vite@latest _warmup -- --template react-ts
```

```bash
cd _warmup
```

```bash
npm install
```

에러 없이 끝나면 된다. **`_warmup` 폴더는 지워도 된다.**

---

# 6. 환경 변수 파일

API 키 같은 값을 담는 파일이다. **`.env`는 절대 깃에 올리지 않는다.**

예시 파일을 복사해서 만든다.

```bash
cp .env.example .env
```

Windows PowerShell이면:

```bash
Copy-Item .env.example .env
```

만들어진 `.env`를 메모장으로 열어 빈 항목을 채운다.

```
LLM_API_KEY=       ← 여기에 발급받은 키
LLM_MODEL=
EMBEDDING_MODEL=
STT_MODEL=
```

키는 팀에서 공유받는다. **채팅에 그대로 붙여넣지 않는다.**

---

# 7. 전체 확인

설치가 끝나면 하나씩 쳐본다. **자기 역할에 해당하는 것만** 되면 된다.

```bash
git --version
```

```bash
docker compose up -d
```

```bash
python -c "import fastapi, openai, numpy; print('ok')"
```

```bash
java -version
```

```bash
flutter doctor
```

```bash
node -v
```

데이터가 온전한지도 한 번 확인한다.

```bash
python scripts/validate_datasets.py
```

맨 아래에 **`전체 이슈 합계: {} 총 0 건`** 이 나오면 정상이다.

---

# 8. 자주 나는 오류

| 증상 | 원인과 해결 |
|---|---|
| `python: 명령을 찾을 수 없습니다` | 설치 시 `Add python.exe to PATH` 체크를 놓쳤다. 재설치하거나 PATH에 직접 추가 |
| `pip: 명령을 찾을 수 없습니다` | `pip` 대신 `python -m pip` 로 친다 |
| `docker: 명령을 찾을 수 없습니다` | Docker Desktop이 **실행 중이 아니다.** 프로그램을 켠다 |
| `Cannot connect to the Docker daemon` | 위와 같다. Docker Desktop 실행 |
| `이 시스템에서 스크립트를 실행할 수 없습니다` | PowerShell 정책. `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| `port is already allocated` | 3306 포트를 다른 DB가 쓰고 있다. 그걸 끄거나 `.env`에서 `DB_PORT` 변경 |
| `flutter doctor`에 `[!]` | 대부분 안드로이드 라이선스. `flutter doctor --android-licenses` |
| 가상환경을 켰는데 패키지가 없다 | 터미널을 새로 열면 꺼진다. `conda activate moveai` 를 다시 친다 |
| `.env` 를 만들었는데 안 읽힌다 | 파일명이 `.env.txt` 로 저장된 경우가 많다. 확장자 표시를 켜서 확인 |

---

# 9. 다음 단계

설치가 끝나면 `PREP_CHECKLIST.md` 로 넘어간다.
거기에는 **AI 모델 결정, API 키 실제 호출 확인, 시연용 음성 녹음** 같은
설치 이후의 준비가 정리돼 있다.

서비스가 뭘 만드는 건지는 `MOVE_AI_기획디자인_이해자료.pdf` 하나면 충분하다.
