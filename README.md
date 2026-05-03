# Hireikon Backend

AI-Powered Smart Recruitment & Skill-Gap Analyzer — Diploma 7th Semester Final Project.

Built with **Spring Boot + Kotlin**, powered by **Google Gemini AI**, backed by **Supabase (PostgreSQL + Storage)**.

---

## Tech Stack

| Layer           | Technology                           |
|-----------------|--------------------------------------|
| Language        | Kotlin 1.9.25                        |
| Framework       | Spring Boot 3.5.13                   |
| Database        | PostgreSQL via Supabase              |
| ORM             | Spring Data JPA + Hibernate          |
| AI Engine       | Google Gemini API                    |
| File Storage    | Supabase Storage                     |
| Auth            | JWT (HS512) + Refresh Token Rotation |
| Connection Pool | HikariCP                             |
| HTTP Client     | Spring WebFlux WebClient             |
| Build Tool      | Gradle (Kotlin DSL)                  |

---

## Project Structure

```
src/main/kotlin/com/hireikon/hireikon_backend/
├── ai/
│   ├── GeminiClient.kt                # Gemini API HTTP client
│   ├── ResumeParser.kt                # PDF resume extraction + profile population
│   ├── SkillGapAnalyzer.kt            # Match scoring + AI gap analysis
│   └── QuizGenerator.kt               # AI quiz question generation
├── controller/
│   ├── AuthController.kt
│   ├── CandidateController.kt
│   ├── AiController.kt
│   ├── JobController.kt
│   ├── ApplicationController.kt
│   └── QuizController.kt
├── database/
│   ├── model/                         # All JPA entities
│   │   └── enums/                     # All enums
│   └── repository/                    # All Spring Data JPA repositories
├── dto/
│   ├── AuthDto.kt
│   ├── CandidateDto.kt
│   ├── AiDto.kt
│   ├── JobDto.kt
│   ├── ApplicationDto.kt
│   └── QuizDto.kt
├── security/
│   ├── JwtAuthFilter.kt              # JWT request filter
│   ├── JwtService.kt                 # JWT generation + validation
│   └── SecurityConfig.kt             # Spring Security + JWT + CORS
├── service/
│   ├── AuthService.kt
│   ├── CandidateService.kt
│   ├── JobService.kt
│   ├── ApplicationService.kt
│   ├── QuizService.kt
│   └── StorageService.kt             # Supabase Storage upload/delete
├── shared/
│   ├── ApiResponse.kt                # Unified response wrapper + custom exceptions
│   ├── GlobalExceptionHandler.kt     # Centralized error handling
└── HireikonBackendApplication.kt
```

---

## Database Schema

| Table                 | Description                                          |
|-----------------------|------------------------------------------------------|
| `users`               | Auth credentials and role                            |
| `candidate_profiles`  | Candidate info, resume URL, social links             |
| `recruiters`          | Recruiter company and position                       |
| `skills`              | Shared skill lookup table                            |
| `candidate_skills`    | Candidate <-> Skill with proficiency level           |
| `experiences`         | Work experience entries                              |
| `educations`          | Education entries                                    |
| `jobs`                | Job postings                                         |
| `job_required_skills` | Job <-> Skill with required level and mandatory flag |
| `applications`        | Candidate applications with match score              |
| `skill_gap_reports`   | AI-generated gap analysis stored as JSONB            |
| `quizzes`             | AI-generated quizzes with answers and score          |
| `refresh_tokens`      | Hashed refresh tokens for rotation                   |

---

## Getting Started

### Prerequisites

- JDK 17
- Gradle
- Supabase project (PostgreSQL + Storage)
- Google Gemini API key

### Environment Variables

| Variable                        | Description                         |
|---------------------------------|-------------------------------------|
| `POSTGRES_PASSWORD`             | Supabase database password          |
| `ENCODED_JWT_SECRET_KEY_BASE64` | HS512 signing secret (min 32 chars) |
| `SPRING_PROFILES_ACTIVE`        | Active Profile (dev or prod)        |
| `FRONTEND_URL`                  | Frontend url to make requests       |
| `GEMINI_API_KEY`                | Google Gemini API key               |
| `SUPABASE_URL`                  | e.g. `https://xyz.supabase.co`      |
| `SUPABASE_ANON_KEY`             | Supabase project anon key           |

### Run Locally

Server starts at `http://localhost:8080` - development  
Server starts at `https://hireikon-backend.onrender.com` - live

### Supabase Setup (one-time)

**1. Create storage bucket:**
Supabase Dashboard → Storage → New bucket → Name: `resumes` → Public: ✅ ON

**2. Add RLS policy:**

```sql
CREATE
POLICY "Allow all operations on resumes"
ON storage.objects
FOR ALL
USING (bucket_id = 'resumes')
WITH CHECK (bucket_id = 'resumes');
```

---

## API Overview

### Base URL

```
http://localhost:8080/api/v1
https://hireikon-backend.onrender.com/api/v1
```

### Response Format

All endpoints return a unified response:

```json
{
  "success": true,
  "message": "string",
  "data": {},
  "errors": null
}
```

### Authentication

Protected endpoints require:

```
Authorization: Bearer <accessToken>
```

---

## Modules

### Auth `/api/v1/auth`

| Method | Endpoint      | Auth | Description                     |
|--------|---------------|------|---------------------------------|
| POST   | `/register`   | ❌    | Register candidate or recruiter |
| POST   | `/login`      | ❌    | Login and get tokens            |
| POST   | `/refresh`    | ❌    | Rotate refresh token            |
| GET    | `/me`         | ✅    | Get current user from JWT       |
| POST   | `/logout`     | ✅    | Invalidate current session      |
| POST   | `/logout-all` | ✅    | Invalidate all sessions         |

**Token strategy:**

- Access token — short-lived (1hr prod / 1day dev), used on every request
- Refresh token — 7 days, stored as SHA-256 hash in DB, rotated on every use
- Rotation — reusing a consumed refresh token returns `401`

---

### Candidate `/api/v1/candidate` — `CANDIDATE` role

| Method | Endpoint            | Description                                           |
|--------|---------------------|-------------------------------------------------------|
| GET    | `/profile`          | Get full profile with skills, experiences, educations |
| PUT    | `/profile`          | Update profile info                                   |
| POST   | `/resume`           | Upload PDF resume to Supabase Storage                 |
| DELETE | `/resume`           | Delete resume                                         |
| GET    | `/skills`           | List all skills                                       |
| POST   | `/skills`           | Add a skill                                           |
| PATCH  | `/skills/{id}`      | Update skill proficiency                              |
| DELETE | `/skills/{id}`      | Remove a skill                                        |
| GET    | `/experiences`      | List experiences                                      |
| POST   | `/experiences`      | Add experience                                        |
| PUT    | `/experiences/{id}` | Update experience                                     |
| DELETE | `/experiences/{id}` | Delete experience                                     |
| GET    | `/educations`       | List educations                                       |
| POST   | `/educations`       | Add education                                         |
| PUT    | `/educations/{id}`  | Update education                                      |
| DELETE | `/educations/{id}`  | Delete education                                      |

---

### AI Engine `/api/v1/ai` — `CANDIDATE` role

| Method | Endpoint                             | AI Call | Description                                |
|--------|--------------------------------------|---------|--------------------------------------------|
| POST   | `/resume/parse`                      | ✅       | Parse PDF — preview only, no DB save       |
| POST   | `/resume/parse-and-fill`             | ✅       | Parse PDF and auto-populate entire profile |
| GET    | `/match-score?jobId=`                | ❌       | Instant local match score                  |
| GET    | `/skill-gap?jobId=`                  | ✅       | Full AI gap report with learning roadmap   |
| GET    | `/quiz?skillName=&proficiencyLevel=` | ✅       | Generate quiz questions                    |

**Resume parsing extracts:** name, email, phone, location, summary, LinkedIn URL, GitHub URL, skills with proficiency,
work experiences, educations.

**Skill gap report includes:** match score (0–100), matched skills, missing skills with importance level, learning
roadmap with resources per skill.

---

### Jobs `/api/v1/jobs`

| Method | Endpoint       | Auth      | Description                                  |
|--------|----------------|-----------|----------------------------------------------|
| GET    | `/`            | ❌         | Browse open jobs (keyword + location filter) |
| GET    | `/{id}`        | ❌         | Get job details with required skills         |
| GET    | `/my`          | RECRUITER | Get recruiter's own jobs                     |
| POST   | `/`            | RECRUITER | Post a new job                               |
| PUT    | `/{id}`        | RECRUITER | Update job (replaces skill list)             |
| PATCH  | `/{id}/status` | RECRUITER | Change job status                            |
| DELETE | `/{id}`        | RECRUITER | Delete job (cascades to applications)        |

**Match score weighting:** mandatory skills = 70%, optional skills = 30%

---

### Applications `/api/v1/applications`

| Method | Endpoint               | Auth      | Description                           |
|--------|------------------------|-----------|---------------------------------------|
| POST   | `/{jobId}`             | CANDIDATE | Apply to a job                        |
| GET    | `/my`                  | CANDIDATE | View my applications                  |
| GET    | `/{id}`                | CANDIDATE | Get application detail                |
| GET    | `/job/{jobId}`         | RECRUITER | View applicants sorted by match score |
| GET    | `/job/{jobId}?status=` | RECRUITER | Filter applicants by status           |
| PATCH  | `/{id}/status`         | RECRUITER | Shortlist / reject / hire             |

**Application statuses:** `PENDING` → `REVIEWED` → `SHORTLISTED` → `HIRED` / `REJECTED`

---

### Quiz `/api/v1/quiz` — `CANDIDATE` role

| Method | Endpoint                    | Description                                      |
|--------|-----------------------------|--------------------------------------------------|
| POST   | `/generate`                 | Generate AI quiz for a skill                     |
| GET    | `/{id}`                     | Get quiz questions (correct answers hidden)      |
| POST   | `/{id}/submit`              | Submit answers — reveals correct answers + score |
| GET    | `/{id}/result`              | Get result of submitted quiz                     |
| GET    | `/history`                  | All quizzes taken                                |
| GET    | `/history/skill?skillName=` | Quizzes filtered by skill                        |

**Quiz flow:** Generate → Answer → Submit → Score (0–100) → Result with explanations

---

## Security

- **Stateless JWT** — no server-side sessions
- **Refresh token rotation** — each refresh token is single-use, stored as SHA-256 hash
- **Role-based access** — `CANDIDATE`, `RECRUITER`, `ADMIN` enforced at route level
- **Ownership checks** — candidates/recruiters can only access their own resources
- **BCrypt (cost 12)** — for passwords
- **SHA-256** — for refresh token hashing (high-entropy input, no salt needed)
- **Supabase PgBouncer** — `prepareThreshold=0&preferQueryMode=simple` in JDBC URL

---

## Known Quirks & Fixes Applied

| Issue                                              | Fix                                                           |
|----------------------------------------------------|---------------------------------------------------------------|
| Supabase PgBouncer prepared statement conflict     | Added `prepareThreshold=0&preferQueryMode=simple` to JDBC URL |
| Gemini wraps JSON in markdown fences               | Strip ` ```json ``` ` before parsing                          |
| Gemini formats emails as `[email](mailto:email)`   | Regex cleanup before Jackson parses                           |
| Transaction rollback-only on resume parse-and-fill | `ResumeProfileSaver` with `REQUIRES_NEW` propagation          |
| Duplicate key on job skill update                  | `@Modifying deleteByJobId()` + `flush()` before re-insert     |
| 403 on recruiter application routes                | Reordered `SecurityConfig` rules — specific before general    |
| Quiz result showing empty `candidateAnswer`        | Store answers as JSONB on submit, read back on result         |

---

## API Documentation Files

| File                          | Description                                              |
|-------------------------------|----------------------------------------------------------|
| `AUTH_API_DOCS.md`            | Auth endpoints — register, login, refresh, logout        |
| `CANDIDATE_API_DOCS.md`       | Candidate profile, resume, skills, experience, education |
| `AI_API_DOCS.md`              | Resume parsing, skill gap analysis, quiz generation      |
| `JOB_APPLICATION_API_DOCS.md` | Job posting, browsing, applying, recruiter dashboard     |
| `QUIZ_API_DOCS.md`            | Quiz generation, submission, results, history            |

---

## Profiles

| Profile | `ddl-auto` | SQL Logging | JWT Expiry | Gemini Model       |
|---------|------------|-------------|------------|--------------------|
| `dev`   | `update`   | DEBUG       | 1 day      | `gemini-2.5-flash` |
| `prod`  | `validate` | WARN        | 1 hour     | `gemini-2.5-flash` |

Run with profile:

```bash
# Dev
./gradlew bootRun --args='--spring.profiles.active=dev'

# Prod
./gradlew bootRun --args='--spring.profiles.active=prod'
```
