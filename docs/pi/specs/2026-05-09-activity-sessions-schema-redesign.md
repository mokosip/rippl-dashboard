# Activity Sessions Schema Redesign

**Date:** 2026-05-09
**Status:** Approved

## Problem

Current `activity_sessions` table uses a hybrid of promoted columns and JSONB blobs with no clear principle for what gets promoted. `collector`, `source`, `session`, `privacy` stored as JSONB alongside `metrics` and `context`, while `collector_type`, `started_at`, `ended_at` are promoted for indexing. Inconsistent and makes queries on common fields (domain, duration, active time) require JSONB extraction.

## Design Principles

- **Column** if filtered, sorted, aggregated, or indexed
- **JSONB** if variable/collector-specific shape
- **raw_payload** as immutable archive — no intermediate JSONB blobs duplicating structured data
- Privacy enforcement stays in application validation layer, not persisted as columns
- Bad data > lost data — constraints should not reject sessions for non-critical inconsistencies
- Collector's reported values are authoritative (duration_ms may differ from ended_at - started_at due to pauses/suspensions)

## Final Schema

```sql
CREATE TABLE activity_sessions (
    -- Identity & audit
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    created_at               TIMESTAMPTZ DEFAULT now(),
    synced_at                TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Collector identity
    collector_type           TEXT NOT NULL,
    collector_version        TEXT,
    collector_session_id     TEXT NOT NULL,

    -- Source (runtime/tool)
    source_type              TEXT NOT NULL,
    source_version           TEXT,

    -- Context (where it happened)
    domain                   TEXT NOT NULL,
    surface                  TEXT NOT NULL,

    -- Time
    started_at               TIMESTAMPTZ NOT NULL,
    ended_at                 TIMESTAMPTZ NOT NULL,
    duration_ms              BIGINT NOT NULL,
    active_ms                BIGINT NOT NULL,

    -- Variable/collector-specific
    collector_metrics        JSONB DEFAULT '{}'::jsonb,
    collector_context        JSONB DEFAULT '{}'::jsonb,
    raw_payload              JSONB NOT NULL,

    -- Constraints
    CONSTRAINT chk_time_range CHECK (ended_at >= started_at),
    CONSTRAINT chk_duration CHECK (duration_ms >= 0),
    CONSTRAINT chk_active CHECK (active_ms >= 0)
);

CREATE UNIQUE INDEX uq_activity_sessions_dedupe
    ON activity_sessions(user_id, collector_type, collector_session_id);

CREATE INDEX idx_activity_sessions_user_time
    ON activity_sessions(user_id, started_at DESC);

CREATE INDEX idx_activity_sessions_domain
    ON activity_sessions(user_id, domain);

CREATE INDEX idx_activity_sessions_collector
    ON activity_sessions(user_id, collector_type);
```

## Key Decisions

### Timestamps as TIMESTAMPTZ (not BIGINT)
Collector sends unix ms. Conversion happens once at write time in IngestionRepository. Enables native Postgres date operations: `date_trunc('week', started_at)`, interval math, range queries without `to_timestamp()` wrapping.

### duration_ms from collector is authoritative
No constraint tying duration_ms to `ended_at - started_at`. Collector may account for pauses, suspensions, or background states that timestamps don't capture.

### active_ms has no upper bound constraint
No `active_ms <= duration_ms` check. Buggy collector data is better than rejected/lost sessions. Anomalies can be flagged in application layer or reporting queries.

### domain and surface are NOT NULL
Every collector must classify where the session happened. Vocabulary defined per collector type:
- Chrome extension: domain = "Claude", surface = "web"
- Future CLI collector: domain = "terminal", surface = "cli"
- Future IDE plugin: domain = "vscode", surface = "ide"

### Privacy not persisted as columns
All privacy flags currently enforced to false in `IngestionService.validateSessionPayload`. No value in 4 always-false columns. Raw data in `raw_payload` for audit if needed. Add columns when policy changes.

### JSONB extraction logic
Universal strip list applied to all collector types:
- `collector_metrics` = metrics payload minus `duration_ms`, `active_ms`
- `collector_context` = context payload minus `domain`, `surface`

No per-collector branching in repository.

## Column Mapping from Collector Payload

| Payload path | Column | Type | Notes |
|---|---|---|---|
| `collector.type` | `collector_type` | TEXT | Dedupe key component |
| `collector.version` | `collector_version` | TEXT | Nullable |
| `session.id` | `collector_session_id` | TEXT | Dedupe key component |
| `source.type` | `source_type` | TEXT | e.g. "browser" |
| `source.version` | `source_version` | TEXT | Nullable |
| `context.domain` | `domain` | TEXT | e.g. "Claude" |
| `context.surface` | `surface` | TEXT | e.g. "web" |
| `session.started_at` | `started_at` | TIMESTAMPTZ | Converted from unix ms at write |
| `session.ended_at` | `ended_at` | TIMESTAMPTZ | Converted from unix ms at write |
| `metrics.duration_ms` | `duration_ms` | BIGINT | Promoted from metrics |
| `metrics.active_ms` | `active_ms` | BIGINT | Promoted from metrics |
| `metrics.*` (remaining) | `collector_metrics` | JSONB | e.g. interaction_count, copy_events |
| `context.*` (remaining) | `collector_context` | JSONB | Collector-specific beyond domain/surface |
| `privacy.*` | — | — | Validated in app layer, not persisted |
| Full payload | `raw_payload` | JSONB | Immutable archive |

## Changes from Current Schema

1. **Dropped JSONB blobs:** `collector`, `source`, `session`, `privacy` — replaced by flat columns
2. **New columns:** `collector_version`, `source_type`, `source_version`, `domain`, `surface`, `duration_ms`, `active_ms`
3. **Type change:** `started_at`/`ended_at` from BIGINT to TIMESTAMPTZ
4. **Renamed:** `metrics` → `collector_metrics`, `context` → `collector_context`
5. **Added:** CHECK constraints for time range, duration, active time
6. **Privacy:** No columns — enforced in `IngestionService.validateSessionPayload`

## Impact on Existing Code

- **IngestionRepository:** Rewrite INSERT to map flat columns. Convert unix ms → TIMESTAMPTZ. Extract duration_ms/active_ms from metrics. Extract domain/surface from context. Strip promoted fields before storing collector_metrics/collector_context.
- **IngestionDtos:** No change — API payload shape stays the same.
- **EstimationRepository/Service:** Update queries from JSONB extraction to direct column access.
- **InsightsService:** Currently queries legacy `sessions` table. Migration to `activity_sessions` becomes much simpler with flat columns and TIMESTAMPTZ.

## Migration Strategy

Truncate-and-rebuild approach (early stage, data not precious):

1. Truncate `estimated_sessions` and `activity_sessions` (CASCADE)
2. Drop old columns (`collector`, `source`, `session`, `privacy`, `metrics`, `context`, old `started_at`/`ended_at` BIGINT)
3. Add new columns with correct types and constraints
4. Recreate indexes

No backfill needed. Estimation pipeline will re-estimate new sessions as they arrive.
