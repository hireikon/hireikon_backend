# Hireikon Backend — AI API Documentation

## Overview

The AI Engine uses **Google Gemini** to power three core features:

- **Resume Parsing** — extract structured data from a PDF resume

- **Skill Gap Analysis** — compare candidate skills against job requirements

- **Quiz Generation** — generate skill assessment questions

**Base URL:** `http://localhost:8080/api/v1/ai`

**All requests require:**

```
Authorization: Bearer <candidateAccessToken>
```

> All AI endpoints are restricted to `CANDIDATE` role only.

---

## Endpoints Summary

| Endpoint                 | Method | AI Call | Description                                     |
|--------------------------|--------|---------|-------------------------------------------------|
| `/resume/parse`          | POST   | ✅       | Parse PDF, return structured data (no DB save)  |
| `/resume/parse-and-fill` | POST   | ✅       | Parse PDF, upload to storage, auto-fill profile |
| `/skill-gap`             | GET    | ✅       | Full AI skill gap report for a job              |
| `/match-score`           | GET    | ❌       | Fast local match score (no AI call)             |
| `/quiz`                  | GET    | ✅       | Generate skill assessment quiz                  |

---

## Resume Parsing

### Parse Resume (preview only)

Sends a PDF resume to Gemini and returns structured extracted data. **Nothing is saved to the database** — use this to preview what will be extracted before committing.

```
POST /api/v1/ai/resume/parse
Content-Type: multipart/form-data
Authorization: Bearer <candidateAccessToken>
```

**Form Data:**

| Key    | Type | Required | Constraints |
|--------|------|----------|-------------|
| `file` | File | ✅        | PDF only    |

**Success Response — `200 OK`:**

```json
{
 "success": true,
 "message": "Resume parsed successfully",
 "data": {
   "fullName": "Md. Noman Hassan Reshad",
   "email": "nomanreshad0@gmail.com",
   "phone": "+8801814931228",
   "location": "Dhaka, Bangladesh",
   "summary": "Passionate Android developer with experience in Kotlin and Jetpack Compose.",
   "linkedinUrl": "https://linkedin.com/in/nomanreshad",
   "githubUrl": "https://github.com/nomanreshad",
   "skills": [
     {
       "name": "Kotlin",
       "category": "PROGRAMMING",
       "proficiencyLevel": "EXPERT"
     },
     {
       "name": "Jetpack Compose",
       "category": "FRAMEWORK",
       "proficiencyLevel": "ADVANCED"
     }
   ],
   "experiences": [
     {
       "company": "TechCorp BD",
       "title": "Android Developer",
       "startDate": "2023-01-01",
       "endDate": null,
       "description": "Built production Android apps using Kotlin and Jetpack Compose."
     }
   ],
   "educations": [
     {
       "institution": "NIIST, Sreepur",
       "degree": "Diploma",
       "field": "Computer Science",
       "graduationDate": null
     }
   ]
 },
 "errors": null
}
```

**Error Responses:**

| Status | Scenario                    | Message                                      |
|--------|-----------------------------|----------------------------------------------|
| `400`  | Non-PDF file                | `"Only PDF files are accepted"`              |
| `502`  | Gemini API error            | `"Gemini API error 429: ..."`                |
| `502`  | Failed to parse AI response | `"Failed to parse AI response as JSON: ..."` |

> **Postman:** Body → form-data → key: `file`, type: **File** → select PDF.

---

### Parse Resume and Auto-Fill Profile

Parses the PDF **and** automatically populates the candidate's profile — skills, experiences, educations, LinkedIn URL, GitHub URL, and resume URL are all saved in one call.



```
POST /api/v1/ai/resume/parse-and-fill
Content-Type: multipart/form-data
Authorization: Bearer <candidateAccessToken>
```

**Form Data:**

| Key    | Type | Required | Constraints |
|--------|------|----------|-------------|
| `file` | File | ✅        | PDF only    |

**What happens internally:**

1. PDF is uploaded to Supabase Storage → `resumeUrl` saved to profile
2. PDF bytes sent to Gemini for extraction
3. Profile updated with `fullName`, `phone`, `location`, `summary`, `linkedinUrl`, `githubUrl`
4. Each skill added individually (duplicates skipped silently)
5. Each experience added individually (invalid dates skipped silently)
6. Each education added individually (missing fields fall back to `"Not specified"`)

**Success Response — `200 OK`:**

```json
{
 "success": true,
 "message": "Resume uploaded and profile populated successfully",
 "data": {
   "fullName": "Md. Noman Hassan Reshad",
   "email": "nomanreshad0@gmail.com",
   "phone": "+8801814931228",
   "location": null,
   "summary": null,
   "linkedinUrl": "https://linkedin.com/in/nomanreshad",
   "githubUrl": "https://github.com/nomanreshad",
   "skills": [...],
   "experiences": [...],
   "educations": [...]
 },
 "errors": null
}
```

After calling this, verify with:

```
GET /api/v1/candidate/profile
```

All fields should be populated including `resumeUrl`, `linkedinUrl`, and `githubUrl`.

**Error Responses:**

| Status | Scenario                                 |
|--------|------------------------------------------|
| `400`  | Non-PDF file                             |
| `500`  | Supabase storage upload failed           |
| `502`  | Gemini API unreachable or quota exceeded |

> **Note:** If Gemini extracts a skill you already have, it is silently skipped — no `409` error, no failure.

---

## Skill Gap Analysis

### Get Match Score (fast)

Calculates a match score between the candidate's skills and a job's required skills **locally — no Gemini call**. Returns instantly. Used when browsing jobs.

```
GET /api/v1/ai/match-score?jobId={jobId}
Authorization: Bearer <candidateAccessToken>
```

**Query Parameters:**

| Param   | Type          | Required | Description                      |
|---------|---------------|----------|----------------------------------|
| `jobId` | string (UUID) | ✅        | ID of the job to compare against |

**Scoring formula:**
- Mandatory skills = \*\*70%\*\* of score
- Optional skills = \*\*30%\*\* of score

**Success Response — `200 OK`:**

```json
{
 "success": true,
 "message": "Match score calculated",
 "data": {
   "matchScore": 75
 },
 "errors": null
}
```

**Error Responses:**

| Status | Scenario                    |
|--------|-----------------------------|
| `404`  | Candidate profile not found |
| `404`  | Job not found               |

---

### Full Skill Gap Analysis (AI)

Sends candidate skills and job requirements to Gemini for a detailed analysis. Returns matched skills, missing skills with importance levels, and a full learning roadmap with resources.

```
GET /api/v1/ai/skill-gap?jobId={jobId}
Authorization: Bearer <candidateAccessToken>
```

**Query Parameters:**

| Param   | Type          | Required | Description                      |
|---------|---------------|----------|----------------------------------|
| `jobId` | string (UUID) | ✅        | ID of the job to analyze against |

**Success Response — `200 OK`:**

```json
{
 "success": true,
 "message": "Skill gap analysis complete",
 "data": {
   "matchScore": 65,
   "matchedSkills": [
     {
       "skillName": "Kotlin",
       "candidateLevel": "EXPERT",
       "requiredLevel": "ADVANCED",
       "meetsRequirement": true
     },
     {
       "skillName": "Spring Boot",
       "candidateLevel": "INTERMEDIATE",
       "requiredLevel": "ADVANCED",
       "meetsRequirement": false
     }
   ],
   "missingSkills": [
     {
       "skillName": "Docker",
       "requiredLevel": "INTERMEDIATE",
       "importance": "CRITICAL",
       "reason": "Required for containerized deployment pipeline in this role."
     },
     {
       "skillName": "Kubernetes",
       "requiredLevel": "BEGINNER",
       "importance": "IMPORTANT",
       "reason": "Team uses K8s for orchestration — basic knowledge expected."
     }
   ],
   "learningRoadmap": {
     "Docker": {
       "skillName": "Docker",
       "estimatedWeeks": 3,
       "resources": [
         {
           "title": "Docker Official Documentation",
           "type": "DOCUMENTATION",
           "url": "https://docs.docker.com",
           "isFree": true
         },
         {
           "title": "Docker & Kubernetes: The Practical Guide",
           "type": "COURSE",
           "url": "https://www.udemy.com/course/docker-kubernetes-the-practical-guide",
           "isFree": false
         }
       ]
     },
     "Kubernetes": {
       "skillName": "Kubernetes",
       "estimatedWeeks": 4,
       "resources": [
         {
           "title": "Kubernetes Official Documentation",
           "type": "DOCUMENTATION",
           "url": "https://kubernetes.io/docs",
           "isFree": true
         }
       ]
     }
   }
 },
 "errors": null
}
```

**Skill importance levels:**

| Value            | Meaning                                             |
|------------------|-----------------------------------------------------|
| `CRITICAL`       | Mandatory for the role — must learn before applying |
| `IMPORTANT`      | Strongly preferred — will affect hiring decision    |
| `NICE\_TO\_HAVE` | Bonus — not required but helpful                    |

**Resource types:**

| Value           | Description                                             |
|-----------------|---------------------------------------------------------|
| `COURSE`        | Online course (Udemy, Coursera, etc.)                   |
| `DOCUMENTATION` | Official docs                                           |
| `BOOK`          | Technical book                                          |
| `YOUTUBE`       | YouTube tutorial                                        |
| `PRACTICE`      | Hands-on practice platform (LeetCode, HackerRank, etc.) |

**Error Responses:**

| Status | Scenario                    |
|--------|-----------------------------|
| `404`  | Candidate profile not found |
| `502`  | Gemini API error            |

> **Performance note:** This endpoint makes a Gemini API call — expect 3–8 seconds response time. Use `/match-score` for real-time UI feedback and `/skill-gap` only when the candidate clicks "View Full Analysis".

---

## Quiz Generation

### Generate Skill Quiz

Generates multiple-choice questions for a specific skill at a given difficulty level. Questions are practical and test real-world knowledge.

```
GET /api/v1/ai/quiz?skillName={skillName}\&proficiencyLevel={level}\&questionCount={n}
Authorization: Bearer <candidateAccessToken>
```

**Query Parameters:**

| Param              | Type    | Required | Default | Description                                 |
|--------------------|---------|----------|---------|---------------------------------------------|
| `skillName`        | string  | ✅        | —       | Must match an existing skill name in the DB |
| `proficiencyLevel` | string  | ✅        | —       | See levels below                            |
| `questionCount`    | integer | ❌        | `10`    | Must be between 10 and 30                   |

**Proficiency levels and their difficulty:**

| Level          | Difficulty                              |
|----------------|-----------------------------------------|
| `BEGINNER`     | Fundamental concepts, basic syntax      |
| `INTERMEDIATE` | Practical application, common patterns  |
| `ADVANCED`     | Edge cases, best practices, performance |
| `EXPERT`       | Deep internals, architecture decisions  |

**Example request:**

```
GET /api/v1/ai/quiz?skillName=Kotlin\&proficiencyLevel=INTERMEDIATE\&questionCount=5
```

**Success Response — `200 OK`:**

```json
{
 "success": true,
 "message": "Quiz generated successfully",
 "data": {
   "skillName": "Kotlin",
   "questions": [
     {
       "question": "What is the difference between `val` and `var` in Kotlin?",
       "options": [
         "A. val is mutable, var is immutable",
         "B. val is immutable, var is mutable",
         "C. Both are mutable",
         "D. Both are immutable"
       ],
       "correctAnswer": "B",
       "explanation": "val declares a read-only reference that cannot be reassigned after initialization, while var declares a mutable reference."
     },
     {
       "question": "Which of the following correctly creates a nullable String in Kotlin?",
       "options": [
         "A. var name: String = null",
         "B. var name: String? = null",
         "C. var name: Nullable<String> = null",
         "D. var name: String = Optional.empty()"
       ],
       "correctAnswer": "B",
       "explanation": "In Kotlin, you must explicitly declare a type as nullable using the ? suffix. String? allows null, String does not."
     }
   ]
 },
 "errors": null
}
```

**Error Responses:**

| Status | Scenario                      | Message                                     |
|--------|-------------------------------|---------------------------------------------|
| `400`  | `questionCount` outside 10–30 | `"questionCount must be between 10 and 30"` |
| `404`  | Skill not found in DB         | `"Skill 'FakeSkill' not found"`             |
| `502`  | Gemini API error              | `"Gemini API error ..."`                    |

> **Note:** The skill must already exist in the `skills` table. Skills are created automatically when candidates add them to their profile — so run `/candidate/skills` first to populate the DB.

---

## Gemini Integration Details

**Model:** Configured via `app.gemini.model` in `application.yml`
- Dev: `gemini-2.5-flash` (fast, free tier)
- Prod: `gemini-2.5-flash or gemini-2.5-pro` (higher quality, paid)

**Temperature settings:**
- Resume parsing: `0.1` — very deterministic, exact data extraction
- Skill gap analysis: `0.2` — slightly flexible for nuanced recommendations
- Quiz generation: default — creative but structured

**Response cleaning:** Gemini sometimes wraps JSON in markdown fences (` ```json ``` `) or formats emails as markdown links (`[email](mailto:email)`). Both are stripped automatically before parsing.

**Retry behavior:** No automatic retry — if Gemini fails, a `502` is returned immediately. The frontend should handle this with a retry button.

---

## Common Errors

| Status | Message                                 | Fix                                       |
|--------|-----------------------------------------|-------------------------------------------|
| `502`  | `"Gemini API error 401"`                | Check `GEMINI_API_KEY` env var            |
| `502`  | `"Gemini API error 429"`                | Quota exceeded — wait or upgrade plan     |
| `502`  | `"Failed to parse AI response as JSON"` | Gemini returned unexpected format — retry |
| `500`  | `"Failed to upload resume to storage"`  | Check Supabase Storage RLS policy         |
| `404`  | `"Skill 'X' not found"`                 | Add the skill to your profile first       |
