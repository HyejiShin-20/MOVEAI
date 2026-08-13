# MOVE-AI Backend Overview

## 1. App startup and runtime
- Main application: `backend/src/main/java/com/moveai/MoveAiApplication.java`
- Server port: `8080`
- Security is open for local development (`permitAll()`) in `SecurityConfig`.
- JWT secret is configured in `application.yml` with fallback values.
- Database URL is configured in `application.yml` and points to MariaDB.

## 2. Current backend endpoints

### Health
- `GET /api/health` -> service health check
- `GET /api/health/db` -> DB connectivity check

### Auth
- `GET /api/auth/login-id/duplicate?loginId=...` -> duplicate login-id check
- `POST /api/auth/login` -> login with `LoginRequest` and returns JWT token

### Dataset
- `GET /api/dataset/data` -> returns the dataset JSON file from configured path

### Places
- `GET /api/places`
- `GET /api/places/{placeId}`
- `POST /api/places`

### Routes
- `GET /api/routes/{placeId}`
- `GET /api/routes/{placeId}/{routeId}`
- `POST /api/routes/{placeId}`

### Jobs
- `GET /api/jobs`
- `GET /api/jobs/{jobId}`
- `POST /api/jobs`
- `POST /api/jobs/{jobId}/cancel`

### Reports (driver)
- `POST /api/driver/reports`
- `GET /api/driver/reports/{reportId}`
- `PUT /api/driver/reports/{reportId}`
- `POST /api/driver/reports/{reportId}/submit`
- `POST /api/driver/reports/{reportId}/audio` -> upload audio file

### Guidance
- `GET /api/driver/guidance/{placeId}/routes`
- `GET /api/driver/guidance/{placeId}/routes/{routeId}/nodes`
- `POST /api/driver/guidance/{placeId}/guidance/start`
- `POST /api/driver/guidance/{placeId}/guidance/{guidanceId}/stop`

### Knowledge
- `GET /api/knowledge`
- `GET /api/knowledge/{id}`
- `POST /api/knowledge`

### Moderation
- `POST /api/moderation/check`
- `POST /api/moderation/report/{reportId}/check`

### Retrieval
- `POST /api/retrieval/search`
- `POST /api/retrieval/search/similarity`

## 3. Data model notes
- `users` table includes: `login_id`, `password_hash`, `name`, `phone`, `role`, `enabled`
- Role enum includes: `ADMIN`, `MEMBER`
- Separate profile tables are expected for admin/member details:
  - `admin_profiles`
  - `member_profiles`

## 4. Important implementation status
- This backend is in a working Spring Boot skeleton / API scaffold state.
- Authentication, authorization, and DB-backed user creation are not fully implemented yet for production-grade registration.
- The app currently exposes stub endpoints returning sample payloads rather than real service logic.
- The app is ready for local API testing and further feature implementation.

## 5. How to run locally
1. Start MariaDB.
2. From `backend/` run:
   ```powershell
   $env:JWT_SECRET='MoveAiSuperSecureJwtSigningKey2026!@#'
   .\gradlew.bat bootRun --no-daemon
   ```
3. Check:
   - `http://localhost:8080/api/health`
   - `http://localhost:8080/api/health/db`

## 6. Known runtime note
- The app may fail to start if port 8080 is already occupied by another process. In that case, stop the old process or change `server.port`.
