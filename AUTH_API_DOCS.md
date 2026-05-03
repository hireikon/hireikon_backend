# Hireikon Backend — Auth API Documentation

## Overview

The Auth API handles user registration, login, token management, and logout for the Hireikon platform. It uses **JWT (JSON Web Tokens)** for stateless authentication with a **refresh token rotation** strategy for security.

**Base URL:** `https://hireikon-backend.onrender.com/api/v1/auth`

**All responses follow this shape:**
```json
{
  "success": true | false,
  "message": "string",
  "data": { ... } | null,
  "errors": ["string"] | null
}
```

---

## Authentication

Protected endpoints require a Bearer token in the `Authorization` header:

```
Authorization: Bearer <accessToken>
```

Access tokens expire in **1 day (dev)** / **1 hour (prod)**.
Refresh tokens expire in **7 days**.

---

## Token Strategy

```
Register / Login
      │
      ▼
 accessToken  ──────────────────────────► Call protected APIs
 refreshToken ─── expires? ─────────────► POST /refresh → new accessToken + refreshToken
                                                              (old refreshToken deleted from DB)
```

- **Access token** — short-lived, used on every API call
- **Refresh token** — long-lived, stored as SHA-256 hash in DB, rotated on every use
- **Rotation** — each refresh token can only be used once. Reuse = `401`

---

## Endpoints

### 1. Register

Register a new Candidate or Recruiter account. Returns tokens immediately — no need to login separately after registering.

```
POST /api/v1/auth/register
```

**Headers:**
```
Content-Type: application/json
```

**Request Body:**

| Field         | Type   | Required       | Description                                                                                                                               |
|---------------|--------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `email`       | string | ✅              | Must be a valid email                                                                                                                     |
| `password`    | string | ✅              | Must be at least 9 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character. |
| `fullName`    | string | ✅              | Candidate or recruiter's full name                                                                                                        |
| `role`        | string | ✅              | `CANDIDATE` or `RECRUITER`                                                                                                                |
| `companyName` | string | ✅ if RECRUITER | Recruiter's company name                                                                                                                  |
| `position`    | string | ✅ if RECRUITER | Recruiter's job position                                                                                                                  |

**Example — Register Candidate:**
```json
{
  "email": "rahim@example.com",
  "password": "Password123!",
  "fullName": "Rahim Uddin",
  "role": "CANDIDATE"
}
```

**Example — Register Recruiter:**
```json
{
  "email": "sara@techcorp.com",
  "password": "Securepass99@",
  "fullName": "Sara Khan",
  "role": "RECRUITER",
  "companyName": "TechCorp BD",
  "position": "HR Manager"
}
```

**Success Response — `201 Created`:**
```json
{
  "success": true,
  "message": "Account created successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "user": {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "email": "rahim@example.com",
      "role": "CANDIDATE",
      "fullName": "Rahim Uddin"
    }
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                      | Message                                              |
|--------|-------------------------------|------------------------------------------------------|
| `400`  | Missing/invalid fields        | `"Validation failed"` + field errors                 |
| `400`  | Recruiter missing companyName | `"Company name is required for recruiter accounts."` |
| `409`  | Email already registered      | `"An account with this email already exists."`       |

**Example `400` Validation Error:**
```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": [
    "email: Must be a valid email",
    "password: Password must be at least 9 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character."
  ]
}
```

---

### 2. Login

Authenticate an existing user.

```
POST /api/v1/auth/login
```

**Headers:**
```
Content-Type: application/json
```

**Request Body:**

| Field      | Type   | Required | Description      |
|------------|--------|----------|------------------|
| `email`    | string | ✅        | Registered email |
| `password` | string | ✅        | Account password |

**Example:**
```json
{
  "email": "rahim@example.com",
  "password": "password123"
}
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "user": {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "email": "rahim@example.com",
      "role": "CANDIDATE",
      "fullName": "Rahim Uddin"
    }
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                | Message                        |
|--------|-------------------------|--------------------------------|
| `401`  | Wrong email or password | `"Invalid email or password."` |

> **Security note:** Wrong email and wrong password return the same message intentionally — this prevents user enumeration attacks (finding out which emails are registered).

---

### 3. Refresh Token

Exchange a valid refresh token for a new access token + refresh token pair. The old refresh token is deleted immediately after use — reusing it returns `401`.

```
POST /api/v1/auth/refresh
```

**Headers:**
```
Content-Type: application/json
```

**Request Body:**

| Field          | Type   | Required | Description                                    |
|----------------|--------|----------|------------------------------------------------|
| `refreshToken` | string | ✅        | The refresh token received from login/register |

**Example:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Token refreshed",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...(new token)",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...(new token)",
    "tokenType": "Bearer",
    "user": {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "email": "rahim@example.com",
      "role": "CANDIDATE",
      "fullName": "Rahim Uddin"
    }
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                                | Message                                                   |
|--------|-----------------------------------------|-----------------------------------------------------------|
| `401`  | Token expired or invalid signature      | `"Invalid or expired refresh token."`                     |
| `401`  | Access token sent instead of refresh    | `"Expected a refresh token, not an access token."`        |
| `401`  | Token already used (rotation violation) | `"Refresh token not recognized (maybe used or expired)."` |

> **How rotation works:** When you call `/refresh`, the server deletes the old refresh token from the DB and saves a new one. If an attacker steals a refresh token and uses it, the real user's next refresh will fail — alerting them to re-login and invalidate everything.

---

### 4. Get Current User

Returns the currently authenticated user's info decoded from the JWT — no database call.

```
GET /api/v1/auth/me
```

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "rahim@example.com",
    "role": "CANDIDATE",
    "fullName": ""
  },
  "errors": null
}
```

> **Note:** `fullName` is empty here — it is not stored in the JWT. Call `/api/v1/candidate/profile` or `/api/v1/recruiter/profile` to get full profile details.

**Error Responses:**

| Status | Scenario                 | Message      |
|--------|--------------------------|--------------|
| `401`  | No token provided        | Unauthorized |
| `401`  | Token expired or invalid | Unauthorized |

---

### 5. Logout

Invalidates the current refresh token. The access token remains valid until it naturally expires (max 1 hour in prod), but the refresh token can no longer be used to get new tokens.

```
POST /api/v1/auth/logout
```

**Headers:**
```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request Body:**

| Field          | Type   | Required | Description                     |
|----------------|--------|----------|---------------------------------|
| `refreshToken` | string | ✅        | The refresh token to invalidate |

**Example:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null,
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                   | Message      |
|--------|----------------------------|--------------|
| `401`  | No or invalid access token | Unauthorized |

---

### 6. Logout All Devices

Invalidates **all** refresh tokens for the current user across every device or session. Use this when a user suspects their account is compromised.

```
POST /api/v1/auth/logout-all
```

**Headers:**
```
Authorization: Bearer <accessToken>
```

**No request body required.**

**Success Response — `200 OK`:**
```json
{
  "success": true,
  "message": "Logged out from all devices",
  "data": null,
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                   | Message      |
|--------|----------------------------|--------------|
| `401`  | No or invalid access token | Unauthorized |

---

## JWT Token Structure

Decode any token at [jwt.io](https://jwt.io) to inspect the payload:

```json
{
  "sub": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "rahim@example.com",
  "role": "CANDIDATE",
  "type": "access",
  "iat": 1713700000,
  "exp": 1713786400
}
```

| Claim   | Description                          |
|---------|--------------------------------------|
| `sub`   | User ID (UUID)                       |
| `email` | User email                           |
| `role`  | `CANDIDATE`, `RECRUITER`, or `ADMIN` |
| `type`  | `access` or `refresh`                |
| `iat`   | Issued at (Unix timestamp)           |
| `exp`   | Expiry (Unix timestamp)              |

**Algorithm:** HS512
**Signing key:** From `app.jwt.secret` environment variable

---

## Role-Based Access

Different API routes are restricted by role:

| Route Pattern                     | Allowed Roles            |
|-----------------------------------|--------------------------|
| `POST /api/v1/auth/**`            | Public (no token needed) |
| `GET /api/v1/jobs/**`             | Public (no token needed) |
| `GET /api/v1/auth/me`             | Any authenticated user   |
| `/api/v1/candidate/**`            | `CANDIDATE` only         |
| `/api/v1/applications/**`         | `CANDIDATE` only         |
| `/api/v1/quiz/**`                 | `CANDIDATE` only         |
| `POST/PUT/DELETE /api/v1/jobs/**` | `RECRUITER` only         |
| `/api/v1/recruiter/**`            | `RECRUITER` only         |
| `/api/v1/admin/**`                | `ADMIN` only             |

Sending a request to a role-restricted route with the wrong role returns:
```json
{
  "success": false,
  "message": "You don't have permission to perform this action",
  "data": null,
  "errors": null
}
```

---

## Common Error Reference

| HTTP Status                 | Meaning                 | When it happens                            |
|-----------------------------|-------------------------|--------------------------------------------|
| `400 Bad Request`           | Invalid input           | Missing fields, failed `@Valid` validation |
| `401 Unauthorized`          | Authentication failed   | Bad credentials, expired/missing token     |
| `403 Forbidden`             | Wrong role              | Correct token but insufficient permissions |
| `404 Not Found`             | Resource missing        | User ID from token no longer in DB         |
| `409 Conflict`              | Duplicate resource      | Email already registered                   |
| `500 Internal Server Error` | Unexpected server error | Bug or DB connection issue                 |

---

## Quick Reference

| Endpoint                  | Method | Auth Required  | Purpose                |
|---------------------------|--------|----------------|------------------------|
| `/api/v1/auth/register`   | POST   | ❌              | Create new account     |
| `/api/v1/auth/login`      | POST   | ❌              | Login + get tokens     |
| `/api/v1/auth/refresh`    | POST   | ❌              | Rotate tokens          |
| `/api/v1/auth/me`         | GET    | ✅ Access token | Get current user       |
| `/api/v1/auth/logout`     | POST   | ✅ Access token | Logout current session |
| `/api/v1/auth/logout-all` | POST   | ✅ Access token | Logout all sessions    |
