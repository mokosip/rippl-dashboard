# Progress

## Status
Completed Task 10 — Frontend Profile Types + API Client

## Tasks
- [x] Append `TaskMix`, `UserProfile`, `ProfileTemplate` interfaces to `frontend/src/types.ts`
- [x] Create `frontend/src/api/profile.ts` with `getProfile`, `updateProfile`, `getProfileTemplates`

## Files Changed
- frontend/src/types.ts
- frontend/src/api/profile.ts

## Notes
- `getProfile()` returns `null` on `ApiError` with `status === 404`
- API paths use `/profile` and `/profile/templates` with frontend `BASE = '/api'`
