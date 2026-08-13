# scripts

## 지금 동작하는 것

```bash
python scripts/validate_datasets.py
```

`datasets/`의 4개 파일 정합성을 검사한다. **기대 결과는 이슈 0건.**

검사 항목 — 코드 유일성, 참조 무결성, Route 구간 연속성(`sequence_no` 1..N,
첫/마지막 노드, 인접 구간의 `to`/`from` 일치),
`source_excerpt`의 transcript 포함, enum 허용목록, `usage_scope` ↔ `action_text` 정합,
조건 숫자의 원문 등장, 정답 질문의 지식 코드 참조.

`pathlib.Path`로 경로를 조립하므로 Windows, Linux, WSL에서 같은 명령을 쓴다.

데이터셋 파일을 수정했다면 **임포트 전에 반드시 이걸 먼저 돌린다.**

## 전달용 ZIP 만들기

```bash
python scripts/build_release_zip.py
```

`dist/MOVE-AI.zip`을 만들고 `.env`/`.env.*`(단 `.env.example`은 포함), `.git/`,
`data/`, 키 파일, 오디오, 빌드·캐시 산출물을 제외했는지 다시 검사한다.
탐색기에서 프로젝트 폴더를 직접 압축하지 않는다.

---

## Phase 3에서 동작하는 명령

```bash
cd ai-service && python scripts/embed_dataset.py
cd ../backend && ./gradlew bootRun --args="--import-embeddings"
./gradlew bootRun --args="--evaluate-rag"
```

- `embed-seed`: Gemini `gemini-embedding-2`로 146건 × 1536차원 산출물 생성
- `import-embeddings`: Spring 포맷 재생성 검증 후 `knowledge_embeddings` 원자 적재
- `eval-rag`: 실제 `/embed` 질의 벡터로 Hit@3 / Hit@5 / must_not 위반 출력

## 구현하면서 추가할 것

각 스택이 생기면 아래를 만든다. 지금 빈 껍데기를 만들어 두지 않는다.

| 스크립트 | 하는 일 |
|---|---|
| `dev-up` / `dev-down` | docker compose + 4개 앱 기동/종료 |
| `import-datasets` | `datasets/` 4개 → MariaDB. **재실행 가능해야 한다** |
| `smoke-test` | 시연 시나리오 1회 자동 실행 |

`import-datasets`는 데모 중 초기화가 필요할 수 있으므로 **몇 번을 돌려도 같은 결과**가 나와야 한다.
장소 단위 트랜잭션으로 감싸 부분 실패 상태가 남지 않게 한다.

`eval-rag`의 숫자로 랭킹 가중치를 조정한다. **눈으로 보고 고치지 않는다.**
