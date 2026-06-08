# Hireikon Backend — Recruiter API Documentation

## Overview

The Recruiter API allows recruiters to manage their profile and profile photo. Job posting and applicant management are covered in the Job & Application API docs.

**Base URL:** `https://hireikon-backend.onrender.com/api/v1/recruiter`

**All requests require:**
```
Authorization: Bearer <recruiterAccessToken>
```

> All endpoints are restricted to `RECRUITER` role only.

---

## Endpoints Summary

| Endpoint             | Method | Description              |
|----------------------|--------|--------------------------|
| `/recruiter/profile` | GET    | Get recruiter profile    |
| `/recruiter/profile` | PUT    | Update recruiter profile |
| `/recruiter/avatar`  | POST   | Upload profile photo     |
| `/recruiter/avatar`  | DELETE | Delete profile photo     |

---

## Endpoints

### 1. Get Profile

Returns the recruiter's full profile including job statistics.

```
GET /api/v1/recruiter/profile
```

**Success Response — `200 OK`:**

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "recruiter-uuid",
    "userId": "user-uuid",
    "email": "sara@techcorp.com",
    "fullName": "Sara Khan",
    "avatarUrl": "https://...supabase.co/storage/v1/object/public/profile_avatars/recruiter/user-uuid.jpg",
    "companyName": "TechCorp BD",
    "position": "HR Manager",
    "companyWebsite": "https://techcorp.com",
    "linkedinUrl": "https://linkedin.com/in/sarakhan",
    "location": "Dhaka, Bangladesh",
    "bio": "Passionate about finding the right talent for the right role.",
    "totalJobsPosted": 5,
    "totalOpenJobs": 3
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                    |
|--------|-----------------------------|
| `401`  | No or invalid token         |
| `403`  | Non-recruiter token         |
| `404`  | Recruiter profile not found |

---

### 2. Update Profile

Updates the recruiter's profile. All fields except `fullName` and `companyName` and `position` are optional.

```
PUT /api/v1/recruiter/profile
Content-Type: application/json
```

**Request Body:**

| Field            | Type   | Required | Constraints   |
|------------------|--------|----------|---------------|
| `fullName`       | string | ✅        | Max 100 chars |
| `companyName`    | string | ✅        | Max 100 chars |
| `position`       | string | ✅        | Max 150 chars |
| `companyWebsite` | string | ❌        | Max 255 chars |
| `linkedinUrl`    | string | ❌        | Max 255 chars |
| `location`       | string | ❌        | Max 100 chars |
| `bio`            | string | ❌        | No limit      |

**Example:**

```json
{
  "fullName": "Sara Khan",
  "companyName": "TechCorp BD",
  "position": "Senior HR Manager",
  "companyWebsite": "https://techcorp.com",
  "linkedinUrl": "https://linkedin.com/in/sarakhan",
  "location": "Dhaka, Bangladesh",
  "bio": "Passionate about finding the right talent for the right role."
}
```

**Success Response — `200 OK`:**

```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": "recruiter-uuid",
    "userId": "user-uuid",
    "email": "sara@techcorp.com",
    "fullName": "Sara Khan",
    "avatarUrl": null,
    "companyName": "TechCorp BD",
    "position": "Senior HR Manager",
    "companyWebsite": "https://techcorp.com",
    "linkedinUrl": "https://linkedin.com/in/sarakhan",
    "location": "Dhaka, Bangladesh",
    "bio": "Passionate HR professional with 5 years of experience in tech recruitment.",
    "totalJobsPosted": 12,
    "totalOpenJobs": 4
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario                    | Message                         |
|--------|-----------------------------|---------------------------------|
| `400`  | Missing required fields     | Validation errors               |
| `404`  | Recruiter profile not found | `"Recruiter profile not found"` |

---

### 3. Upload Profile Photo

Uploads a profile photo. The image is automatically compressed and resized to max `400×400` pixels at 82% JPEG quality
before storing. Re-uploading replaces the existing photo.

```
POST /api/v1/recruiter/avatar
Content-Type: multipart/form-data
```

**Form Data:**

| Key    | Type | Required | Constraints                     |
|--------|------|----------|---------------------------------|
| `file` | File | ✅        | JPEG, PNG or WebP only. Max 5MB |

**What happens internally:**

1. Validates file type and size
2. Resizes to max 400×400 (preserving aspect ratio)
3. Re-encodes as JPEG at 82% quality
4. Uploads to Supabase Storage at `profile_avatars/recruiter/{userId}.jpg`
5. Saves public URL to recruiter profile

**Success Response — `200 OK`:**

```json
{
  "success": true,
  "message": "Profile photo uploaded successfully",
  "data": {
    "avatarUrl": "https://...supabase.co/storage/v1/object/public/profile_avatars/recruiter/user-uuid.jpg"
  },
  "errors": null
}
```

**Error Responses:**

| Status | Scenario               | Message                                             |
|--------|------------------------|-----------------------------------------------------|
| `400`  | Empty file             | `"File is empty"`                                   |
| `400`  | Wrong file type        | `"Only JPEG, PNG and WebP images are allowed."`     |
| `400`  | File too large         | `"Image size must not exceed 5MB"`                  |
| `400`  | Corrupted image        | `"Could not read image file — it may be corrupted"` |
| `500`  | Supabase upload failed | `"Failed to upload profile photo: ..."`             |

> **Postman:** Body → form-data → key: `file`, type: **File** → select image.

---

### 4. Delete Profile Photo

Deletes the profile photo from Supabase Storage and clears `avatarUrl` from the profile.

```
DELETE /api/v1/recruiter/avatar
```

**Success Response — `200 OK`:**

```json
{
  "success": true,
  "message": "Profile photo deleted successfully",
  "data": null,
  "errors": null
}
```

---

## Image Compression Details

| Property              | Value                                                         |
|-----------------------|---------------------------------------------------------------|
| Accepted formats      | JPEG, PNG, WebP                                               |
| Max input size        | 5MB                                                           |
| Output format         | Always JPEG                                                   |
| Max output dimensions | 400 × 400 px (aspect ratio preserved)                         |
| JPEG quality          | 82%                                                           |

> Output is always JPEG regardless of input format. A 2MB PNG typically compresses to under 50KB.

---

## Supabase Storage Setup (one-time)

For image upload/delete to work, these must be configured in your Supabase project:

**1. Create bucket:**
Supabase Dashboard → Storage → New bucket → Name: `profile_avatars` → Public: ✅ ON

**2. Add RLS policy (SQL Editor):**
```sql
CREATE POLICY "Allow all operations on profile_avatars"
ON storage.objects
FOR ALL
USING (bucket_id = 'profile_avatars')
WITH CHECK (bucket_id = 'profile_avatars');
```

**Storage path format:** `recruiter/{userId}.jpg` inside the `profile_avatars` bucket.
