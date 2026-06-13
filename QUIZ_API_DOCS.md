# Hireikon Backend — Quiz API Documentation

## Overview

The Quiz API allows candidates to generate AI-powered skill assessment quizzes, submit answers, and view results with detailed explanations. All endpoints are restricted to `CANDIDATE` role only.

**Base URL:** `https://hireikon-backend.onrender.com/api/v1/quiz`

**All requests require:**
```
Authorization: Bearer <candidateAccessToken>
```

---

## Endpoints Summary

| Endpoint                                            | Method | Description                               |
|-----------------------------------------------------|--------|-------------------------------------------|
| `/quiz/generate`                                    | POST   | Generate a new quiz for a skill           |
| `/quiz/{id}`                                        | GET    | Get quiz questions (no correct answers)   |
| `/quiz/{id}/submit`                                 | POST   | Submit answers and get scored result      |
| `/quiz/{id}/result`                                 | GET    | Get result of a submitted quiz            |
| `/quiz/history`                                     | GET    | Get all quizzes taken (paginated)         |
| `/quiz/history/skill?skillName=X&cursor=&pageSize=` | GET    | Get quizzes filtered by skill (paginated) |

---

## Quiz Lifecycle

```
Generate Quiz ──► Get Quiz (questions only)
                        │
                        ▼
               Submit Answers ──► Scored Result (correct answers revealed)
                                          │
                                          ▼
                                  Get Result (anytime after submission)
                                  Get History (all past quizzes)
```

- Correct answers are **hidden** until the quiz is submitted
- Each quiz can only be submitted **once** — generate a new one for another attempt
- Candidate answers are stored permanently — result can be retrieved anytime after submission

---

## Endpoints

### 1. Generate Quiz

Calls Gemini AI to generate multiple-choice questions for a skill at a given difficulty level.

```
POST /api/v1/quiz/generate
Content-Type: application/json
```

**Request Body:**

| Field              | Type    | Required | Default        | Constraints                |
|--------------------|---------|----------|----------------|----------------------------|
| `skillName`        | string  | ✅        | —              | Must exist in skills table |
| `proficiencyLevel` | string  | ❌        | `INTERMEDIATE` | See levels below           |
| `questionCount`    | integer | ❌        | `10`           | 10–30                      |

**Proficiency levels and difficulty:**

| Level          | Question Style                          |
|----------------|-----------------------------------------|
| `BEGINNER`     | Fundamental concepts, basic syntax      |
| `INTERMEDIATE` | Practical application, common patterns  |
| `ADVANCED`     | Edge cases, best practices, performance |
| `EXPERT`       | Deep internals, architecture decisions  |

**Example:**
```json
{
  "skillName": "Kotlin",
  "proficiencyLevel": "INTERMEDIATE",
  "questionCount": 10
}
```

**Success Response — `201 Created`:**
```json
{
  "success": true,
  "message": "Quiz generated successfully",
  "data": {
    "id": "quiz-uuid",
    "skillName": "Kotlin",
    "proficiencyLevel": "INTERMEDIATE",
    "questions": [
      {
        "index": 1,
        "question": "What is the difference between val and var in Kotlin?",
        "options": [
          "A. val is mutable, var is immutable",
          "B. val is immutable, var is mutable",
          "C. Both are mutable",
          "D. Both are immutable"
        ]
      },
      {
        "index": 2,
        "question": "Which keyword is used to define a nullable type in Kotlin?",
        "options": [
          "A. nullable",
          "B. ?",
          "C. Optional",
          "D. null"
        ]
      }
    ],
    "score": null,
    "submitted": false,
    "takenAt": "2026-05-01T10:00:00"
  },
  "errors": null
}
```

> **Notice:** `correctAnswer` and `explanation` are intentionally excluded from this response. They are only revealed after submission.

**Error Responses:**

| Status | Scenario                     | Message                                                |
|--------|------------------------------|--------------------------------------------------------|
| `400`  | `questionCount` outside 3–10 | `"questionCount must be between 3 and 10"`             |
| `404`  | Skill not found in DB        | `"Skill 'X' not found. Add it to your profile first."` |
| `502`  | Gemini API error             | `"Gemini API error ..."`                               |

---

### 2. Get Quiz

Retrieve a previously generated quiz. Returns questions and options only — correct answers remain hidden.

```
GET /api/v1/quiz/{id}
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "quiz-uuid",
    "skillName": "Kotlin",
    "proficiencyLevel": "INTERMEDIATE",
    "questions": [
      {
        "index": 1,
        "question": "What is the difference between val and var in Kotlin?",
        "options": [
          "A. val is mutable, var is immutable",
          "B. val is immutable, var is mutable",
          "C. Both are mutable",
          "D. Both are immutable"
        ]
      }
    ],
    "score": null,
    "submitted": false,
    "takenAt": "2026-05-01T10:00:00"
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                          |
|--------|-----------------------------------|
| `401`  | Quiz belongs to another candidate |
| `404`  | Quiz not found                    |

---

### 3. Submit Quiz

Submit answers for a quiz. Each answer must be exactly `A`, `B`, `C`, or `D`. The number of answers must match the number of questions exactly.

Returns the full scored result with correct answers and explanations revealed immediately.

```
POST /api/v1/quiz/{id}/submit
Content-Type: application/json
```

**Request Body:**

| Field     | Type     | Required | Description                                                                  |
|-----------|----------|----------|------------------------------------------------------------------------------|
| `answers` | string[] | ✅        | One answer per question in order. Each must be `"A"`, `"B"`, `"C"`, or `"D"` |

**Example (10-question quiz):**
```json
{
  "answers": ["B", "B", "A", "C", "D", "B", "B", "A", "C", "D"]
}
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Quiz submitted successfully",
  "data": {
    "id": "quiz-uuid",
    "skillName": "Kotlin",
    "proficiencyLevel": "INTERMEDIATE",
    "score": 80,
    "correctCount": 4,
    "totalQuestions": 5,
    "questions": [
      {
        "index": 1,
        "question": "What is the difference between val and var in Kotlin?",
        "options": [
          "A. val is mutable, var is immutable",
          "B. val is immutable, var is mutable",
          "C. Both are mutable",
          "D. Both are immutable"
        ],
        "correctAnswer": "B",
        "candidateAnswer": "B",
        "isCorrect": true,
        "explanation": "val declares a read-only reference that cannot be reassigned after initialization, while var declares a mutable reference."
      },
      {
        "index": 2,
        "question": "Which keyword is used to define a nullable type in Kotlin?",
        "options": [
          "A. nullable",
          "B. ?",
          "C. Optional",
          "D. null"
        ],
        "correctAnswer": "B",
        "candidateAnswer": "A",
        "isCorrect": false,
        "explanation": "In Kotlin, you append ? to the type name to make it nullable. For example, String? allows null while String does not."
      }
    ]
  },
  "errors": null
}
```

**Score formula:**
```
score = (correctCount / totalQuestions) × 100
```

**Error Responses:**

| Status | Scenario                          | Message                                       |
|--------|-----------------------------------|-----------------------------------------------|
| `400`  | Quiz already submitted            | `"This quiz has already been submitted"`      |
| `400`  | Wrong number of answers           | `"Expected 10 answers but got 5"`             |
| `400`  | Invalid answer value              | `"Invalid answer 'X'. Must be A, B, C, or D"` |
| `401`  | Quiz belongs to another candidate | Unauthorized                                  |
| `404`  | Quiz not found                    | `"Quiz not found"`                            |

---

### 4. Get Quiz Result

Retrieve the result of a previously submitted quiz. Correct answers, candidate answers, and explanations are all included. Can be called anytime after submission.

```
GET /api/v1/quiz/{id}/result
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "quiz-uuid",
    "skillName": "Kotlin",
    "proficiencyLevel": "INTERMEDIATE",
    "score": 80,
    "correctCount": 4,
    "totalQuestions": 5,
    "questions": [
      {
        "index": 1,
        "question": "What is the difference between val and var in Kotlin?",
        "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
        "correctAnswer": "B",
        "candidateAnswer": "B",
        "isCorrect": true,
        "explanation": "val declares a read-only reference..."
      }
    ]
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                          | Message                                  |
|--------|-----------------------------------|------------------------------------------|
| `400`  | Quiz not yet submitted            | `"This quiz has not been submitted yet"` |
| `401`  | Quiz belongs to another candidate | Unauthorized                             |
| `404`  | Quiz not found                    | `"Quiz not found"`                       |

---

### 5. Get Quiz History

Returns all quizzes taken by the authenticated candidate, sorted by most recent first. Paginated.

```
GET /api/v1/quiz/history?cursor={cursor}&pageSize={pageSize}
```

**Query Parameters:**

| Param      | Type          | Required | Default | Description                                             |
|------------|---------------|----------|---------|---------------------------------------------------------|
| `cursor`   | string (UUID) | ❌        | `null`  | ID of last item from previous page. Omit for first page |
| `pageSize` | integer       | ❌        | `20`    | Items per page. Min 1, max 100                          |

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "data": [
      {
        "id": "quiz-uuid-1",
        "skillName": "Kotlin",
        "proficiencyLevel": "INTERMEDIATE",
        "score": 80,
        "submitted": true,
        "takenAt": "2026-05-01T10:00:00"
      },
      {
        "id": "quiz-uuid-2",
        "skillName": "Spring Boot",
        "proficiencyLevel": "ADVANCED",
        "score": null,
        "submitted": false,
        "takenAt": "2026-05-01T11:00:00"
      }
    ],
    "nextCursor": "quiz-uuid-2",
    "hasMore": true,
    "pageSize": 20
  },
  "errors": null
}
```

> `submitted: false` means the quiz was generated but answers were never submitted. `score: null` for unsubmitted quizzes.

---

### 6. Get Quiz History by Skill

Returns quizzes for a specific skill, sorted by most recent first. Useful for tracking score progression. Paginated.

```
GET /api/v1/quiz/history/skill?skillName=Kotlin&cursor={cursor}&pageSize={pageSize}
```

**Query Parameters:**

| Param       | Type          | Required | Default | Description                                          |
|-------------|---------------|----------|---------|------------------------------------------------------|
| `skillName` | string        | ✅        | —       | Must match an existing skill name (case-insensitive) |
| `cursor`    | string (UUID) | ❌        | `null`  | ID of last item from previous page                   |
| `pageSize`  | integer       | ❌        | `20`    | Items per page. Min 1, max 100                       |

**Success Response — `200 OK`:**

```json
{
  "success": true,
  "message": "Success",
  "data":  {
    "data": [
      {
        "id": "quiz-uuid-1",
        "skillName": "Kotlin",
        "proficiencyLevel": "INTERMEDIATE",
        "score": 60,
        "submitted": true,
        "takenAt": "2026-05-01T10:00:00"
      },
      {
        "id": "quiz-uuid-3",
        "skillName": "Kotlin",
        "proficiencyLevel": "EXPERT",
        "score": 80,
        "submitted": true,
        "takenAt": "2026-05-02T09:00:00"
      }
    ],
    "nextCursor": null,
    "hasMore": false,
    "pageSize": 20
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario        |
|--------|-----------------|
| `404`  | Skill not found |

---

## Data Storage

Quiz data in the `quizzes` table:

| Column              | Type      | Description                                                          |
|---------------------|-----------|----------------------------------------------------------------------|
| `id`                | UUID      | Quiz identifier                                                      |
| `candidate_id`      | UUID      | FK to candidate profile                                              |
| `skill_id`          | UUID      | FK to skill                                                          |
| `proficiency_level` | VARCHAR   | Difficulty level                                                     |
| `questions`         | JSONB     | Full questions with correct answers and explanations                 |
| `candidate_answers` | JSONB     | Submitted answers e.g. `["A","B","C","D","A"]`. Null until submitted |
| `score`             | INT       | 0–100. Null until submitted                                          |
| `taken_at`          | TIMESTAMP | When quiz was generated                                              |

> `questions` stores correct answers in the DB but they are **never sent to the client** until after submission. The API intentionally strips them from `GET /quiz/{id}` and `POST /quiz/generate` responses.

---

## Common Errors

| Status | Message                                                | Cause                                      |
|--------|--------------------------------------------------------|--------------------------------------------|
| `400`  | `"questionCount must be between 3 and 10"`             | Out of range value                         |
| `400`  | `"This quiz has already been submitted"`               | Re-submission attempt                      |
| `400`  | `"Expected N answers but got M"`                       | Answer count mismatch                      |
| `400`  | `"Invalid answer 'X'. Must be A, B, C, or D"`          | Bad answer value                           |
| `400`  | `"This quiz has not been submitted yet"`               | Accessing result before submit             |
| `401`  | Unauthorized                                           | Quiz belongs to another candidate          |
| `403`  | Forbidden                                              | Non-candidate trying to use quiz endpoints |
| `404`  | `"Quiz not found"`                                     | Invalid quiz ID                            |
| `404`  | `"Skill 'X' not found. Add it to your profile first."` | Skill not in DB                            |
| `502`  | `"Gemini API error ..."`                               | AI service unavailable                     |
