# Progress

## Status
In Progress

## Tasks
- [x] Task 9: Frontend API client updated with `ApiError` and `put` method

## Files Changed
- frontend/src/api/client.ts

## Notes
- Replaced generic `Error` throws with `ApiError` carrying HTTP status
- Removed dedicated 401 branch; now handled by generic non-OK branch
- Added `api.put` for upcoming profile endpoints
