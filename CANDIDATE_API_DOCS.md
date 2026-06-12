# Hireikon Backend — Candidate API Documentation

## Overview

The Candidate API allows candidates to manage their profile, upload resumes, and maintain their skills, work experience, and education. All endpoints require a valid `CANDIDATE` role JWT access token.

**Base URL:** `https://hireikon-backend.onrender.com/api/v1/candidate`

**All requests require:**
```
Authorization: Bearer <accessToken>
```

---

## Endpoints Summary

| Endpoint            | Method | Description              |
|---------------------|--------|--------------------------|
| `/profile`          | GET    | Get full profile         |
| `/profile`          | PUT    | Update profile info      |
| `/resume`           | POST   | Upload PDF resume        |
| `/resume`           | DELETE | Delete resume            |
| `/avatar`           | POST   | Upload profile photo     |
| `/avatar`           | DELETE | Delete profile photo     |
| `/skills`           | GET    | Get all skills           |
| `/skills`           | POST   | Add a skill              |
| `/skills/{id}`      | PATCH  | Update skill proficiency |
| `/skills/{id}`      | DELETE | Remove a skill           |
| `/experiences`      | GET    | Get all experiences      |
| `/experiences`      | POST   | Add experience           |
| `/experiences/{id}` | PUT    | Update experience        |
| `/experiences/{id}` | DELETE | Delete experience        |
| `/educations`       | GET    | Get all educations       |
| `/educations`       | POST   | Add education            |
| `/educations/{id}`  | PUT    | Update education         |
| `/educations/{id}`  | DELETE | Delete education         |

---

## Profile

### Get Profile

Returns the full candidate profile including skills, experiences, and educations in one response.

```
GET /api/v1/candidate/profile
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "profile-uuid",
    "userId": "user-uuid",
    "email": "rahim@email.com",
    "fullName": "Rahim Uddin",
    "phone": "+8801712345678",
    "location": "Dhaka, Bangladesh",
    "avatarUrl": "https://xyz.supabase.co/.../profile_avatars/candidate/user-uuid.jpg",
    "resumeUrl": "https://arjnikafdfwrkqfgsnzf.supabase.co/storage/v1/object/public/resumes/user-uuid/resume.pdf",
    "linkedinUrl": "https://linkedin.com/in/rahimuddin",
    "summary": "Passionate backend developer with 2 years of experience.",
    "totalApplications": 3,
    "skills": [...],
    "experiences": [...],
    "educations": [...]
  },
  "errors": null
}
```

---

### Update Profile

```
PUT /api/v1/candidate/profile
Content-Type: application/json
```

**Request Body:**

| Field         | Type   | Required | Constraints   |
|---------------|--------|----------|---------------|
| `fullName`    | string | ✅        | Max 100 chars |
| `phone`       | string | ❌        | Max 20 chars  |
| `location`    | string | ❌        | Max 100 chars |
| `linkedinUrl` | string | ❌        | Max 255 chars |
| `summary`     | string | ❌        | No limit      |

**Example:**
```json
{
  "fullName": "Rahim Uddin",
  "phone": "+8801712345678",
  "location": "Dhaka, Bangladesh",
  "linkedinUrl": "https://linkedin.com/in/rahimuddin",
  "summary": "Passionate backend developer with 2 years of experience in Kotlin and Spring Boot."
}
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": { ...updated profile... },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                                 |
|--------|------------------------------------------|
| `400`  | `fullName` is blank or exceeds 100 chars |

---

## Resume

### Upload Resume

Uploads a PDF resume to Supabase Storage. Re-uploading automatically **overwrites** the previous file — only one resume is kept per candidate.

```
POST /api/v1/candidate/resume
Content-Type: multipart/form-data
```

**Form Data:**

| Key    | Type | Required | Constraints        |
|--------|------|----------|--------------------|
| `file` | File | ✅        | PDF only, max 10MB |

**Success Response — `201 Created`:**
```json
{
  "success": true,
  "message": "Resume uploaded successfully",
  "data": {
    "resumeUrl": "https://arjnikafdfwrkqfgsnzf.supabase.co/storage/v1/object/public/resumes/user-uuid/resume.pdf"
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario               | Message                                              |
|--------|------------------------|------------------------------------------------------|
| `500`  | Non-PDF file uploaded  | `"Only PDF files are allowed. Received: image/jpeg"` |
| `413`  | File exceeds 10MB      | `"File size exceeds 10MB limit"`                     |
| `500`  | Supabase Storage error | `"Failed to upload resume: ..."`                     |

> **Postman tip:** In the Body tab, select `form-data`. Set the key to `file` and change the type dropdown from `Text` to `File`, then select your PDF.

> **Storage path:** Files are stored at `{userId}/resume.pdf` inside the `resumes` bucket. The path is fixed per user so re-uploading overwrites cleanly.

---

### Delete Resume

Deletes the resume from Supabase Storage and clears the `resumeUrl` from the profile.

```
DELETE /api/v1/candidate/resume
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Resume deleted successfully",
  "data": null,
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                   | Message                       |
|--------|----------------------------|-------------------------------|
| `400`  | No resume exists to delete | `"No resume found to delete"` |

---

### Upload Profile Photo

Uploads a profile photo. Automatically compressed and resized to max 400×400 pixels. Re-uploading replaces the existing photo.

```
POST /api/v1/candidate/avatar
Content-Type: multipart/form-data
```

**Form Data:**

| Key    | Type | Required | Constraints                |
|--------|------|----------|----------------------------|
| `file` | File | ✅        | JPEG, PNG or WebP. Max 5MB |

**Compression details:**
- Resized to max 400×400 (aspect ratio preserved)
- Re-encoded as JPEG at 82% quality
- All formats (PNG, WebP) converted to JPEG on save

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Profile photo uploaded successfully",
  "data": {
    "avatarUrl": "https://xyz.supabase.co/storage/v1/object/public/resumes/avatars/candidate/user-uuid.jpg"
  }
}
```

**Error Responses:**

| Status | Scenario        | Message                                             |
|--------|-----------------|-----------------------------------------------------|
| `400`  | Wrong file type | `"Only JPEG, PNG and WebP images are allowed."`     |
| `400`  | File too large  | `"Image size must not exceed 5MB"`                  |
| `400`  | Corrupted image | `"Could not read image file — it may be corrupted"` |

---

### Delete Profile Photo

```
DELETE /api/v1/candidate/avatar
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Profile photo deleted successfully",
  "data": null
}
```

---

## Skills

Skills are stored in a shared `skills` lookup table. When you add a skill by name, it is either found in the table (case-insensitive) or created automatically. Deleting a skill removes it from your profile only — the skill entry remains in the shared table.

### Get Skills

```
GET /api/v1/candidate/skills
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "candidate-skill-uuid",
      "skillId": "skill-uuid",
      "skillName": "Kotlin",
      "category": "PROGRAMMING",
      "proficiencyLevel": "INTERMEDIATE"
    }
  ],
  "errors": null
}
```

---

### Add Skill

```
POST /api/v1/candidate/skills
Content-Type: application/json
```

**Request Body:**

| Field              | Type   | Required | Constraints                               |
|--------------------|--------|----------|-------------------------------------------|
| `skillName`        | string | ✅        | Max 50 chars                              |
| `category`         | string | ❌        | See categories below. Defaults to `OTHER` |
| `proficiencyLevel` | string | ✅        | See levels below                          |

**Skill Categories:**
`PROGRAMMING` `FRAMEWORK` `DATABASE` `CLOUD` `DEVOPS` `DESIGN` `SOFT_SKILL` `LANGUAGE` `OTHER`

**Proficiency Levels:**
`BEGINNER` `INTERMEDIATE` `ADVANCED` `EXPERT`

**Example:**
```json
{
  "skillName": "Kotlin",
  "category": "PROGRAMMING",
  "proficiencyLevel": "INTERMEDIATE"
}
```

**Success Response — `201 Created`:**
```json
{
  "success": true,
  "message": "Skill added successfully",
  "data": {
    "id": "candidate-skill-uuid",
    "skillId": "skill-uuid",
    "skillName": "Kotlin",
    "category": "PROGRAMMING",
    "proficiencyLevel": "INTERMEDIATE"
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                | Message                                      |
|--------|-------------------------|----------------------------------------------|
| `409`  | Skill already added     | `"You already have 'Kotlin' in your skills"` |
| `400`  | Missing required fields | Validation errors                            |

---

### Update Skill Proficiency

Only the proficiency level can be updated. To change the skill name, delete it and add a new one.

```
PATCH /api/v1/candidate/skills/{id}?proficiencyLevel=ADVANCED
```

**Query Parameter:**

| Param              | Type   | Required | Values                                        |
|--------------------|--------|----------|-----------------------------------------------|
| `proficiencyLevel` | string | ✅        | `BEGINNER` `INTERMEDIATE` `ADVANCED` `EXPERT` |

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Skill updated successfully",
  "data": {
    "id": "candidate-skill-uuid",
    "skillName": "Kotlin",
    "proficiencyLevel": "ADVANCED"
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                           |
|--------|------------------------------------|
| `404`  | Skill not found                    |
| `401`  | Skill belongs to another candidate |

---

### Remove Skill

```
DELETE /api/v1/candidate/skills/{id}
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Skill removed successfully",
  "data": null,
  "errors": null
}
```

---

## Experience

### Get Experiences

Returns experiences sorted by `startDate` descending (most recent first).

```
GET /api/v1/candidate/experiences
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "experience-uuid",
      "company": "TechCorp BD",
      "title": "Backend Developer",
      "startDate": "2023-01-15",
      "endDate": "2024-06-30",
      "isCurrent": false,
      "description": "Developed REST APIs using Spring Boot and Kotlin."
    },
    {
      "id": "experience-uuid-2",
      "company": "StartupXYZ",
      "title": "Backend Engineer",
      "startDate": "2024-07-01",
      "endDate": null,
      "isCurrent": true,
      "description": null
    }
  ],
  "errors": null
}
```

> `isCurrent: true` means `endDate` is `null` — the candidate is currently in this role.

---

### Add Experience

```
POST /api/v1/candidate/experiences
Content-Type: application/json
```

**Request Body:**

| Field         | Type   | Required | Constraints                                                      |
|---------------|--------|----------|------------------------------------------------------------------|
| `company`     | string | ✅        | Max 100 chars                                                    |
| `title`       | string | ✅        | Max 150 chars                                                    |
| `startDate`   | date   | ✅        | `YYYY-MM-DD`, cannot be future                                   |
| `endDate`     | date   | ❌        | `YYYY-MM-DD`, must be after startDate. Omit if currently working |
| `description` | string | ❌        | No limit                                                         |

**Example — past job:**
```json
{
  "company": "TechCorp BD",
  "title": "Junior Backend Developer",
  "startDate": "2023-01-15",
  "endDate": "2024-06-30",
  "description": "Developed REST APIs using Spring Boot and Kotlin."
}
```

**Example — current job (no endDate):**
```json
{
  "company": "StartupXYZ",
  "title": "Backend Engineer",
  "startDate": "2024-07-01"
}
```

**Success Response — `201 Created`:**
```json
{
  "success": true,
  "message": "Experience added successfully",
  "data": {
    "id": "experience-uuid",
    "company": "StartupXYZ",
    "title": "Backend Engineer",
    "startDate": "2024-07-01",
    "endDate": null,
    "isCurrent": true,
    "description": null
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                     | Message                                  |
|--------|------------------------------|------------------------------------------|
| `400`  | `endDate` before `startDate` | `"End date cannot be before start date"` |
| `400`  | `startDate` in the future    | `"Start date cannot be in the future"`   |
| `400`  | Missing required fields      | Validation errors                        |

---

### Update Experience

```
PUT /api/v1/candidate/experiences/{id}
Content-Type: application/json
```

Same request body as Add Experience. All fields are replaced.

**Error Responses:**

| Status | Scenario                                |
|--------|-----------------------------------------|
| `404`  | Experience not found                    |
| `401`  | Experience belongs to another candidate |
| `400`  | Invalid dates                           |

---

### Delete Experience

```
DELETE /api/v1/candidate/experiences/{id}
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Experience deleted successfully",
  "data": null,
  "errors": null
}
```

---

## Education

### Get Educations

Returns educations sorted by `graduationDate` descending (most recent first).

```
GET /api/v1/candidate/educations
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "education-uuid",
      "institution": "Dhaka Polytechnic Institute",
      "degree": "Diploma in Engineering",
      "field": "Computer Science & Technology",
      "graduationDate": "2025-06-01"
    }
  ],
  "errors": null
}
```

---

### Add Education

```
POST /api/v1/candidate/educations
Content-Type: application/json
```

**Request Body:**

| Field            | Type   | Required | Constraints                          |
|------------------|--------|----------|--------------------------------------|
| `institution`    | string | ✅        | Max 150 chars                        |
| `degree`         | string | ✅        | Max 100 chars                        |
| `field`          | string | ✅        | Max 100 chars                        |
| `graduationDate` | date   | ❌        | `YYYY-MM-DD`. Omit if still studying |

**Example:**
```json
{
  "institution": "Dhaka Polytechnic Institute",
  "degree": "Diploma in Engineering",
  "field": "Computer Science & Technology",
  "graduationDate": "2025-06-01"
}
```

**Success Response — `201 Created`:**
```json
{
  "success": true,
  "message": "Education added successfully",
  "data": {
    "id": "education-uuid",
    "institution": "Dhaka Polytechnic Institute",
    "degree": "Diploma in Engineering",
    "field": "Computer Science & Technology",
    "graduationDate": "2025-06-01"
  },
  "errors": null
}
```

---

### Update Education

```
PUT /api/v1/candidate/educations/{id}
Content-Type: application/json
```

Same request body as Add Education. All fields are replaced.

---

### Delete Education

```
DELETE /api/v1/candidate/educations/{id}
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Education deleted successfully",
  "data": null,
  "errors": null
}
```

---

## Security Notes

- All endpoints are restricted to `CANDIDATE` role only. Recruiters and admins cannot access these routes.
- Ownership is enforced on every skill, experience, and education operation — a candidate cannot modify another candidate's data even if they know the UUID.
- Attempting to modify another candidate's resource returns `401 Unauthorized`.

---

## Supabase Storage Setup (one-time)

For resume upload/delete to work, these must be configured in your Supabase project:

**1. Create bucket:**
Supabase Dashboard → Storage → New bucket → Name: `resumes` → Public: ✅ ON

**2. Add RLS policy (SQL Editor):**
```sql
CREATE POLICY "Allow all operations on resumes"
ON storage.objects
FOR ALL
USING (bucket_id = 'resumes')
WITH CHECK (bucket_id = 'resumes');
```

**Storage path format:** `{userId}/resume.pdf` inside the `resumes` bucket.
