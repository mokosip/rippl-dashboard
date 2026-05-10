# Estimated Session Storage Design

## Goal
Store mutable, backend-derived estimation results separate from immutable raw `activity_sessions`, with async/non-blocking ingest behavior.

## Decision: terminology
Use **estimation** naming everywhere (service, repository, DB table/types), even though backlog story says “scored”.

## Data model
- Migration `V5__estimated_sessions.sql`
- Enum type `estimation_confidence`: `low`, `medium`, `high`
- Enum type `estimation_method`: `profile_default`, `feedback_adjusted`, `collector_signal_adjusted`, `global_fallback`
- Table `estimated_sessions`
  - `id UUID PK default gen_random_uuid()`
  - `activity_session_id UUID NOT NULL REFERENCES activity_sessions(id) ON DELETE CASCADE`
  - `user_id UUID NOT NULL` (denormalized from `activity_sessions.user_id`, intentionally no FK to `users`)
  - `inferred_task_mix JSONB NOT NULL`
  - `effective_multiplier DOUBLE PRECISION NOT NULL`
  - `estimated_time_saved_ms BIGINT NOT NULL`
  - `confidence estimation_confidence NOT NULL`
  - `estimation_method estimation_method NOT NULL`
  - `estimated_at TIMESTAMPTZ NOT NULL DEFAULT now()`
- Constraints/indexes
  - `UNIQUE(activity_session_id)`
  - index `(user_id, confidence)`

## Write path
- New `EstimationRepository` with SQL upsert:
  - insert-from-`activity_sessions` by `sessionId`
  - `ON CONFLICT (activity_session_id) DO UPDATE` (last-write-wins)
  - update all estimation fields and `estimated_at = now()`
- New `EstimationService`
  - `estimateInitial(sessionId)` for ingest path
  - `reestimateFromFeedback(sessionId)` for feedback path
  - placeholder deterministic estimate for now:
    - mix `{"unknown": 1.0}`
    - multiplier `1.0`
    - saved `0`
    - confidence `low`
    - method `global_fallback`
- Rename `ScoringDispatchService` → `EstimationDispatchService`
  - keep `@Async`, catch/log exceptions
  - call estimation service methods
- Ingestion path remains non-blocking; estimation failure does not fail API response.

## Testing
- Update `SchemaTest` to include `estimated_sessions`
- New integration tests in ingestion package:
  - ingest eventually writes one `estimated_sessions` row
  - feedback re-estimation keeps one row and advances `estimated_at`
  - deleting raw `activity_sessions` cascades delete to `estimated_sessions`
- Polling helper for async eventual write assertions.

## Non-goals
- Real estimation algorithm
- Dashboard query rewrites
