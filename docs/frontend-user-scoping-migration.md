# Frontend Migration Guide: Strict User-Scoped APIs

**Date:** 2026-02-23  
**Backend status:** user scoping hardened in `publishing-service`, `asset-service`, and `content-service`.

---

## 1) What changed (high level)

All sensitive APIs now derive the user identity from the authenticated JWT (`Authorization: Bearer ...`) on the backend.

### New rule
- **Do send:** `Authorization: Bearer <access_token>` on every protected API request.
- **Do not send for identity:** `X-User-Id` or `userId` query/body params (except one temporary LinkedIn callback edge case below).

### Why this matters
Previously, some endpoints accepted user identity from client-controlled headers/params or used global queries; now ownership is enforced server-side, so cross-user reads/edits are blocked.

---

## 2) Global frontend changes

## 2.1 API client / interceptor

1. Ensure your API client always attaches:
   - `Authorization: Bearer <token>`
2. Remove any global injection of:
   - `X-User-Id`
3. Keep `X-User-Email` only where needed (YouTube publish endpoint can still use it as optional fallback).

## 2.2 Error handling updates

Expect more ownership-safe responses for cross-user resource IDs:
- `404` for resources not owned by current user (common pattern used here)
- `400` for invalid ownership-dependent operations in some endpoints

UI behavior recommendation:
- Show generic messages like **"Resource not found or access denied"**.
- Avoid exposing whether another user owns a resource.

---

## 3) Endpoint-by-endpoint request changes

> Paths below are service-relative controller paths. Keep your existing gateway prefixing/routing as-is.

## 3.1 Publishing service

### A) Connected accounts
Controller: `ConnectedAccountController`

- `GET /`
- `GET /platform/{platform}`
- `GET /{accountId}`
- `GET /youtube/channels`

**Frontend change:**
- Remove `X-User-Id` header if you were sending it.
- Keep Bearer token.

### B) Published posts
Controller: `PublishedPostController`

- `GET /published-posts`
- `GET /published-posts/{postId}`
- `GET /published-posts/platform/{platform}`
- `GET /published-posts/account/{accountId}`

**Frontend change:**
- Remove `X-User-Id` header.
- Keep Bearer token.

### C) LinkedIn posts
Controller: `LinkedInPostController`

- `GET /linkedin/posts/{accountId}`
- `POST /linkedin/posts/{accountId}`

**Frontend change:**
- Remove `X-User-Id` header.
- Keep Bearer token.

### D) YouTube OAuth + YouTube APIs
Controllers: `YouTubeOAuthController`, `YouTubeVideoController`

- `GET /youtube/login`
- `GET /youtube/callback?code=...&state=...`
- `GET /youtube/accounts/{accountId}/videos`
- `GET /youtube/accounts/{accountId}/videos/{videoId}`
- `GET /youtube/accounts/{accountId}/statistics`
- `GET /youtube/accounts/{accountId}/videos/{videoId}/analytics`
- `GET /youtube/accounts/{accountId}/analytics`
- `GET /youtube/accounts/{accountId}/top-videos`
- `POST /youtube/publish`

**Frontend change:**
- Remove `X-User-Id` header.
- Keep Bearer token.
- For `POST /youtube/publish`, `X-User-Email` is still optional; you may keep or remove depending on whether you send `email` in body.

### E) LinkedIn OAuth callback edge case (temporary)
Controller: `LinkedInOAuthController`

- `GET /linkedin/login` (no user header)
- `GET /linkedin/callback?code=...` **currently expects `X-User-Id` header**

**Important:**
- This endpoint is currently inconsistent with strict JWT-only pattern and with normal third-party callback behavior.
- LinkedIn redirect callbacks generally do **not** include your custom header.

**Frontend options now:**
1. If callback is handled by frontend first, then frontend can call backend callback with auth context and required header.
2. Prefer backend follow-up: migrate LinkedIn OAuth callback to a signed `state` pattern (same style as YouTube), then remove `X-User-Id` dependency.

---

## 3.2 Asset service

Controller: `AssetController`

### Before -> After request contract

1. `POST /upload` (multipart)
- **Before:** required `file`, `folderId`, `userId`
- **After:** required `file`, `folderId` only
- **Frontend action:** remove `userId` field from `FormData`

2. `POST /folders`
- **Before:** `name`, `description?`, `parentFolderId?`, `userId`
- **After:** `name`, `description?`, `parentFolderId?`
- **Frontend action:** remove `userId` request param

3. `GET /folders/{folderId}`
- **Before:** required query `userId`
- **After:** no `userId` query
- **Frontend action:** stop appending `?userId=...`

4. `GET /folders/root`
- **Before:** required query `userId`
- **After:** no `userId` query
- **Frontend action:** stop appending `?userId=...`

5. `GET /{fileId}/metadata`
- **Before:** no user validation in service lookup
- **After:** ownership-enforced lookup by authenticated user
- **Frontend action:** no shape change; ensure Bearer token present

6. `GET /view/{fileId}`
- **Before:** no user validation in service lookup
- **After:** ownership-enforced lookup by authenticated user
- **Frontend action:** no shape change; ensure Bearer token present

---

## 3.3 Content service

No request shape changes are required for existing content endpoints.

What changed internally:
- user identity now comes from JWT principal in security context (not request header lookup).

**Frontend action:**
- Ensure Bearer token is always sent.
- Remove any unnecessary `X-User-Id` if your frontend was sending it.

---

## 4) Concrete frontend diff examples

## 4.1 Axios/fetch headers

### Before
- `Authorization: Bearer ...`
- `X-User-Id: <uuid>`

### After
- `Authorization: Bearer ...`
- (optional for one endpoint) `X-User-Email: ...` only where intentionally used

## 4.2 Asset upload `FormData`

### Before fields
- `file`
- `folderId`
- `userId`

### After fields
- `file`
- `folderId`

---

## 5) Regression checklist for frontend QA

1. Login as User A, connect account, publish/upload content.
2. Logout, login as User B.
3. Verify User B **cannot**:
   - See User A connected accounts
   - See User A published posts
   - Read User A asset metadata
   - View/download User A file URL by ID
4. Verify User B can still access own resources normally.
5. Verify all protected calls include Bearer token after refresh/navigation.
6. Verify no frontend request still sends `userId` query/form for asset endpoints.

---

## 6) Recommended next backend follow-up

To complete strict consistency, update LinkedIn OAuth callback flow to signed `state` (like YouTube) and remove `X-User-Id` from `GET /linkedin/callback`.

That will make the entire platform fully JWT-principal/state-based with no client-asserted identity input.