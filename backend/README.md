# MOVE-AI Backend

업로드된 `MOVE_AI_BACKEND_FULL_GUIDE.md`의 내용만을 기준으로 구성한
Spring Boot Modular Monolith 백엔드 기본 구현입니다.

## 기준
- Spring Boot + Java
- MariaDB
- package-by-feature
- Controller -> Service -> Repository
- JSON *_code와 DB PK 분리
- BIGINT AUTO_INCREMENT PK
- DatasetImportDto 하나로 A/B/C/D 공통 처리
- DatasetValidator
- DatasetImportService + code -> id Map
- @Transactional import
- DRIVER / ADMIN 권한
- ConditionEvaluator
- STT / Knowledge Extraction / Embedding Adapter
- PUBLISHED Knowledge만 검색 대상
- UNRESOLVED Knowledge publish 금지
- DRIVE 금지
- Secret / password / API key 하드코딩 금지

## 프로젝트 구조

```text
src/main/java/com/moveai/
├── common/
├── security/
├── place/
├── route/
├── report/
├── knowledge/
├── moderation/
├── guidance/
├── ai/
└── dataset/
```

## Dataset API

```text
POST /api/admin/datasets/validate
POST /api/admin/datasets/import
```

Multipart field:

```text
file
```

## 설정

`src/main/resources/application.yml`에서 환경변수를 사용합니다.

```text
DB_URL
DB_USERNAME
DB_PASSWORD
MOVE_AI_ADMIN_TOKEN
MOVE_AI_DRIVER_TOKEN
STT_MODE
EXTRACTION_MODE
EMBEDDING_MODE
SERVER_PORT
```

## DB

`db/move_ai_all_in_one.sql`

```bash
mariadb -u USER -p < db/move_ai_all_in_one.sql
```

## 중요

이 압축본은 **업로드된 MD 문서만을 구현 기준으로 삼았습니다.**
MD에서 별도 입력 파일로 요구하지만 현재 대화에 제공되지 않은
A/B/C/D 실제 JSON 데이터의 내용은 임의로 생성하지 않았습니다.

따라서 이 프로젝트는 MD가 정의한 백엔드 구조와 처리 규칙을 구현한
기본 백엔드이며, 실제 A/B/C/D 원본 데이터가 제공되면 Dataset Import
검증/실데이터 적재를 추가로 연결할 수 있습니다.
