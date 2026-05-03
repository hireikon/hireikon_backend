# Hireikon Backend — Job & Application API Documentation

## Overview

The Job & Application API handles job posting by recruiters, job browsing by candidates, applying to jobs, and managing the full recruitment pipeline including shortlisting and rejection.

**Base URL:** `https://hireikon-backend.onrender.com/api/v1`

---

## Role Access Summary

| Route                       | Method | Who                      |
|-----------------------------|--------|--------------------------|
| `/jobs`                     | GET    | Public (no token)        |
| `/jobs/{id}`                | GET    | Public (no token)        |
| `/jobs`                     | POST   | `RECRUITER` only         |
| `/jobs/my`                  | GET    | `RECRUITER` only         |
| `/jobs/{id}`                | PUT    | `RECRUITER` only (owner) |
| `/jobs/{id}/status`         | PATCH  | `RECRUITER` only (owner) |
| `/jobs/{id}`                | DELETE | `RECRUITER` only (owner) |
| `/applications/{jobId}`     | POST   | `CANDIDATE` only         |
| `/applications/my`          | GET    | `CANDIDATE` only         |
| `/applications/{id}`        | GET    | `CANDIDATE` only (owner) |
| `/applications/job/{jobId}` | GET    | `RECRUITER` only (owner) |
| `/applications/{id}/status` | PATCH  | `RECRUITER` only (owner) |

---

## Job Endpoints

### 1. Get All Open Jobs (Public)

Browse all open jobs. Supports optional keyword and location filtering.

```
GET /api/v1/jobs
GET /api/v1/jobs?keyword=backend
GET /api/v1/jobs?location=dhaka
GET /api/v1/jobs?keyword=kotlin&location=dhaka
```

**Query Parameters:**

| Param      | Type   | Required | Description                             |
|------------|--------|----------|-----------------------------------------|
| `keyword`  | string | ❌        | Filters by job title (case-insensitive) |
| `location` | string | ❌        | Filters by location (case-insensitive)  |

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "job-uuid",
      "title": "Senior Backend Developer",
      "company": "TechCorp BD",
      "location": "Dhaka, Bangladesh",
      "status": "OPEN",
      "postedAt": "2026-05-01T10:00:00",
      "deadline": "2026-12-31T23:59:59",
      "requiredSkillCount": 4
    }
  ],
  "errors": null
}
```

---

### 2. Get Job By ID (Public)

Returns full job details including all required skills.

```
GET /api/v1/jobs/{id}
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "job-uuid",
    "recruiterId": "recruiter-uuid",
    "title": "Senior Backend Developer",
    "company": "TechCorp BD",
    "location": "Dhaka, Bangladesh",
    "description": "We are looking for a skilled backend developer...",
    "status": "OPEN",
    "postedAt": "2026-05-01T10:00:00",
    "deadline": "2026-12-31T23:59:59",
    "requiredSkills": [
      {
        "skillId": "skill-uuid",
        "skillName": "Kotlin",
        "levelRequired": "EXPERT",
        "isMandatory": true
      },
      {
        "skillId": "skill-uuid",
        "skillName": "Docker",
        "levelRequired": "INTERMEDIATE",
        "isMandatory": false
      }
    ]
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario      |
|--------|---------------|
| `404`  | Job not found |

---

### 3. Get My Jobs (Recruiter)

Returns all jobs posted by the authenticated recruiter.

```
GET /api/v1/jobs/my
Authorization: Bearer <recruiterAccessToken>
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "job-uuid",
      "title": "Senior Backend Developer",
      "company": "TechCorp BD",
      "location": "Dhaka, Bangladesh",
      "status": "OPEN",
      "postedAt": "2026-05-01T10:00:00",
      "deadline": "2026-12-31T23:59:59",
      "requiredSkillCount": 5
    }
  ],
  "errors": null
}
```

---

### 4. Create Job (Recruiter)

Posts a new job. Skills that don't exist in the shared `skills` table are created automatically.

```
POST /api/v1/jobs
Authorization: Bearer <recruiterAccessToken>
Content-Type: application/json
```

**Request Body:**

| Field            | Type     | Required | Constraints               |
|------------------|----------|----------|---------------------------|
| `title`          | string   | ✅        | Max 150 chars             |
| `company`        | string   | ✅        | Max 100 chars             |
| `location`       | string   | ❌        | Max 100 chars             |
| `description`    | string   | ✅        | No limit                  |
| `deadline`       | datetime | ❌        | `YYYY-MM-DDTHH:MM:SS`     |
| `requiredSkills` | array    | ✅        | At least 1 skill required |

**`requiredSkills` item fields:**

| Field           | Type    | Required | Values                                                                    |
|-----------------|---------|----------|---------------------------------------------------------------------------|
| `skillName`     | string  | ✅        | Any skill name                                                            |
| `levelRequired` | string  | ❌        | `BEGINNER` `INTERMEDIATE` `ADVANCED` `EXPERT`. Defaults to `INTERMEDIATE` |
| `isMandatory`   | boolean | ❌        | Defaults to `true`. Affects match score weighting                         |

**Example:**
```json
{
  "title": "Backend Developer",
  "company": "TechCorp BD",
  "location": "Dhaka, Bangladesh",
  "description": "We are looking for a skilled backend developer with experience in Kotlin and Spring Boot.",
  "deadline": "2026-12-31T23:59:59",
  "requiredSkills": [
    { "skillName": "Kotlin",      "levelRequired": "ADVANCED",    "isMandatory": true  },
    { "skillName": "Spring Boot", "levelRequired": "INTERMEDIATE", "isMandatory": true  },
    { "skillName": "PostgreSQL",  "levelRequired": "INTERMEDIATE", "isMandatory": true  },
    { "skillName": "Docker",      "levelRequired": "BEGINNER",     "isMandatory": false }
  ]
}
```

**Success Response — `201 Created`:**
```json
{
  "success": true,
  "message": "Job posted successfully",
  "data": {
    "id": "job-uuid",
    "recruiterId": "recruiter-uuid",
    "title": "Backend Developer",
    "company": "TechCorp BD",
    "location": "Dhaka, Bangladesh",
    "description": "We are looking for...",
    "status": "OPEN",
    "postedAt": "2026-05-01T10:00:00",
    "deadline": "2026-12-31T23:59:59",
    "requiredSkills": [...]
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                    | Message                                                           |
|--------|-----------------------------|-------------------------------------------------------------------|
| `400`  | Missing title               | `"title: Job title is required"`                                  |
| `400`  | Empty skills array          | `"requiredSkills: At least one required skill must be specified"` |
| `403`  | Candidate tries to post     | Forbidden                                                         |
| `404`  | Recruiter profile not found | `"Recruiter profile not found"`                                   |

---

### 5. Update Job (Recruiter)

Fully replaces a job's details and required skills. The old skill list is deleted and replaced with the new one.

```
PUT /api/v1/jobs/{id}
Authorization: Bearer <recruiterAccessToken>
Content-Type: application/json
```

Same request body as Create Job, plus:

| Field    | Type   | Required | Values                                               |
|----------|--------|----------|------------------------------------------------------|
| `status` | string | ❌        | `OPEN` `CLOSED` `DRAFT` `PAUSED`. Defaults to `OPEN` |

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Job updated successfully",
  "data": { ...updated job... },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                       |
|--------|--------------------------------|
| `401`  | Recruiter doesn't own this job |
| `404`  | Job not found                  |

> **Note:** Required skills are fully replaced on update — not merged. Send the complete desired skill list each time.

---

### 6. Update Job Status (Recruiter)

Quickly change a job's status without updating other fields.

```
PATCH /api/v1/jobs/{id}/status?status=CLOSED
Authorization: Bearer <recruiterAccessToken>
```

**Query Parameter:**

| Param    | Type   | Required | Values                           |
|----------|--------|----------|----------------------------------|
| `status` | string | ✅        | `OPEN` `CLOSED` `DRAFT` `PAUSED` |

**Job status meanings:**

| Status   | Meaning                    |
|----------|----------------------------|
| `OPEN`   | Accepting applications     |
| `CLOSED` | Not accepting applications |
| `DRAFT`  | Not yet published          |
| `PAUSED` | Temporarily paused         |

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Job status updated",
  "data": { ...job with updated status... },
  "errors": null
}
```

---

### 7. Delete Job (Recruiter)

Deletes the job and all associated required skills and applications (cascade).

```
DELETE /api/v1/jobs/{id}
Authorization: Bearer <recruiterAccessToken>
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Job deleted successfully",
  "data": null,
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                       |
|--------|--------------------------------|
| `401`  | Recruiter doesn't own this job |
| `404`  | Job not found                  |

> **Warning:** Deleting a job also deletes all applications and skill gap reports for that job via cascade.

---

## Application Endpoints

### 8. Apply to a Job (Candidate)

Submits an application for a job. The match score is calculated instantly. A full AI skill gap report is generated and stored asynchronously — if AI fails, the application still succeeds.

```
POST /api/v1/applications/{jobId}
Authorization: Bearer <candidateAccessToken>
```

No request body needed — `jobId` is in the path.

**What happens on apply:**
1. Validates job is `OPEN`
2. Checks for duplicate application
3. Calculates match score locally (instant, no AI)
4. Saves application with match score
5. Calls Gemini for skill gap report → saves to `skill_gap_reports` table

**Success Response — `201 Created`:**
```json
{
  "success": true,
  "message": "Application submitted successfully",
  "data": {
    "id": "application-uuid",
    "jobId": "job-uuid",
    "jobTitle": "Senior Backend Developer",
    "company": "TechCorp BD",
    "candidateId": "profile-uuid",
    "candidateName": "Rahim Uddin",
    "matchScore": 75,
    "status": "PENDING",
    "appliedAt": "2026-05-01T10:00:00"
  },
  "errors": null
}
```

**Match score formula:**
- Mandatory skills matched = **70%** of score
- Optional skills matched = **30%** of score

**Error Responses:**

| Status | Scenario                    | Message                                          |
|--------|-----------------------------|--------------------------------------------------|
| `400`  | Job is not `OPEN`           | `"This job is no longer accepting applications"` |
| `404`  | Job not found               | `"Job not found"`                                |
| `404`  | Candidate profile not found | `"Candidate profile not found"`                  |
| `409`  | Already applied             | `"You have already applied for this job"`        |

---

### 9. Get My Applications (Candidate)

Returns all applications submitted by the authenticated candidate, including job summary and current status.

```
GET /api/v1/applications/my
Authorization: Bearer <candidateAccessToken>
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "applicationId": "application-uuid",
      "job": {
        "id": "job-uuid",
        "title": "Senior Backend Developer",
        "company": "TechCorp BD",
        "location": "Dhaka, Bangladesh",
        "status": "OPEN",
        "postedAt": "2026-05-01T10:00:00",
        "deadline": "2026-12-31T23:59:59",
        "requiredSkillCount": 5
      },
      "matchScore": 75,
      "status": "SHORTLISTED",
      "appliedAt": "2026-05-01T10:00:00"
    }
  ],
  "errors": null
}
```

**Application status values:**

| Status        | Meaning                     |
|---------------|-----------------------------|
| `PENDING`     | Submitted, not yet reviewed |
| `REVIEWED`    | Recruiter has seen it       |
| `SHORTLISTED` | Candidate moved forward     |
| `REJECTED`    | Not selected                |
| `HIRED`       | Offer accepted              |

---

### 10. Get Application By ID (Candidate)

```
GET /api/v1/applications/{id}
Authorization: Bearer <candidateAccessToken>
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "application-uuid",
    "jobId": "job-uuid",
    "jobTitle": "Senior Backend Developer",
    "company": "TechCorp BD",
    "candidateId": "profile-uuid",
    "candidateName": "Rahim Uddin",
    "matchScore": 75,
    "status": "PENDING",
    "appliedAt": "2026-05-01T10:00:00"
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                                 |
|--------|------------------------------------------|
| `401`  | Application belongs to another candidate |
| `404`  | Application not found                    |

---

### 11. Get Applicants for a Job (Recruiter Dashboard)

Returns all candidates who applied to a job, sorted by match score descending by default. Optionally filter by status.

```
GET /api/v1/applications/job/{jobId}
GET /api/v1/applications/job/{jobId}?status=SHORTLISTED
Authorization: Bearer <recruiterAccessToken>
```

**Query Parameters:**

| Param    | Type   | Required | Values                                                                         |
|----------|--------|----------|--------------------------------------------------------------------------------|
| `status` | string | ❌        | `PENDING` `REVIEWED` `SHORTLISTED` `REJECTED` `HIRED`. Omit for all applicants |

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "applicationId": "application-uuid",
      "candidateId": "profile-uuid",
      "candidateName": "Rahim Uddin",
      "candidateEmail": "rahim@example.com",
      "resumeUrl": "https://...supabase.co/.../resume.pdf",
      "linkedinUrl": "https://linkedin.com/in/rahimuddin",
      "githubUrl": "https://github.com/rahimuddin",
      "matchScore": 75,
      "status": "PENDING",
      "appliedAt": "2026-05-01T10:00:00"
    }
  ],
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                       |
|--------|--------------------------------|
| `401`  | Recruiter doesn't own this job |
| `404`  | Job not found                  |

> **Recruiter dashboard tip:** Default sort is by `matchScore DESC` — highest matching candidates appear first. Combine with `?status=PENDING` to see unreviewed candidates ranked by fit.

---

### 12. Update Application Status (Recruiter)

Move an application through the recruitment pipeline.

```
PATCH /api/v1/applications/{id}/status
Authorization: Bearer <recruiterAccessToken>
Content-Type: application/json
```

**Request Body:**

| Field    | Type   | Required | Values                                                |
|----------|--------|----------|-------------------------------------------------------|
| `status` | string | ✅        | `PENDING` `REVIEWED` `SHORTLISTED` `REJECTED` `HIRED` |

**Example:**
```json
{
  "status": "SHORTLISTED"
}
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Application status updated",
  "data": {
    "id": "application-uuid",
    "jobId": "job-uuid",
    "jobTitle": "Senior Backend Developer",
    "company": "TechCorp BD",
    "candidateId": "profile-uuid",
    "candidateName": "Rahim Uddin",
    "matchScore": 75,
    "status": "SHORTLISTED",
    "appliedAt": "2026-05-01T10:00:00"
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                                                  |
|--------|-----------------------------------------------------------|
| `401`  | Recruiter doesn't own the job this application belongs to |
| `403`  | Candidate tries to update status                          |
| `404`  | Application not found                                     |

---

## Skill Matching & Scoring

When a candidate applies to a job, a match score (0–100) is calculated instantly:

```
mandatoryMatched / totalMandatory × 70  +  optionalMatched / totalOptional × 30
```

**Example:**
- Job requires: Kotlin ✅ mandatory, Spring Boot ✅ mandatory, Docker ❌ optional
- Candidate has: Kotlin ✅, Spring Boot ✅
- Score = (2/2 × 70) + (0/1 × 30) = **70**

A full AI skill gap report is also generated and stored in the `skill_gap_reports` table on application. Retrieve it via:
```
GET /api/v1/ai/skill-gap?jobId={jobId}
```

---

## Common Errors

| Status | Message                                          | Cause                                    |
|--------|--------------------------------------------------|------------------------------------------|
| `400`  | `"This job is no longer accepting applications"` | Job status is not `OPEN`                 |
| `401`  | Unauthorized                                     | Trying to modify another user's resource |
| `403`  | Forbidden                                        | Wrong role for the endpoint              |
| `404`  | `"Job not found"`                                | Invalid job ID                           |
| `404`  | `"Application not found"`                        | Invalid application ID                   |
| `409`  | `"You have already applied for this job"`        | Duplicate application                    |
