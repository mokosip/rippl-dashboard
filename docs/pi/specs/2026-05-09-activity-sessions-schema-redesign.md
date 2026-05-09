# Activity Sessions Schema Redesign

**Date:** 2026-05-09
**Status:** Approved

## Problem

Current `activity_sessions` table uses a hybrid of promoted columns and JSONB blobs with no clear principle for what gets promoted. `collector`, `source`, `session`, `privacy` stored as JSONB alongside `metrics` and `context`, while `collector_type`, `started_at`, `ended_at` are promoted for indexing. Inconsistent and makes queries on common fields (domain, duration, active time) require JSONB extraction.

## Design Principle

- **Column** if filtered, sorted, aggregated, or indexed
- **JSONB** if variable/collector-specific shape
- **raw_payload** as immutable archive — no intermediate JSONB blobs duplicating structured data
- Privacy enforcement stays in application validation layer, not persisted as columns

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
    started_at               BIGINT NOT NULL,
    ended_at                 BIGINT NOT NULL,
    duration_ms              BIGINT NOT NULL,
    active_ms                BIGINT NOT NULL,

    -- Variable/collector-specific
    collector_metrics        JSONB DEFAULT '{}'::jsonb,
    collector_context        JSONB DEFAULT '{}'::jsonb,
    raw_payload              JSONB NOT NULL,

    -- Constraints
    CONSTRAINT chk_time_range CHECK (ended_at >= started_at),
    CONSTRAINT chk_duration CHECK (duration_ms >= 0),
    CONSTRAINT chk_active CHECK (active_ms >= 0 AND active_ms <= duration_ms)
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

## Column Mapping from Collector Payload

| Payload path | Column | Notes |
|---|---|---|
| `collector.type` | `collector_type` | Dedupe key component |
| `collector.version` | `collector_version` | Nullable |
| `session.id` | `collector_session_id` | Dedupe key component |
| `source.type` | `source_type` | e.g. "browser" |
| `source.version` | `source_version` | Nullable |
| `context.domain` | `domain` | e.g. "Claude" |
| `context.surface` | `surface` | e.g. "web" |
| `session.started_at` | `started_at` | Unix ms |
| `session.ended_at` | `ended_at` | Unix ms |
| `metrics.duration_ms` | `duration_ms` | Promoted from metrics |
| `metrics.active_ms` | `active_ms` | Promoted from metrics |
| `metrics.*` (remaining) | `collector_metrics` | e.g. interaction_count, copy_events |
| `context.*` (remaining) | `collector_context` | Collector-specific context beyond domain/surface |
| `privacy.*` | — | Validated in app layer, not persisted as columns |
| Full payload | `raw_payload` | Immutable archive |

## What Changes from Current Schema

1. **Dropped JSONB blobs:** `collector`, `source`, `session`, `privacy` — replaced by flat columns
2. **New columns:** `collector_version`, `source_type`, `source_version`, `domain`, `surface`, `duration_ms`, `active_ms`
3. **Renamed:** `metrics` → `collector_metrics`, `context` → `collector_context`
4. **Added:** CHECK constraints for time range, duration, and active time
5. **Privacy:** No columns — enforced in `IngestionService.validateSessionPayload`, raw data in `raw_payload`

## Impact on Existing Code

- **IngestionRepository:** Rewrite INSERT to map flat columns instead of serializing JSONB blobs. Extract `duration_ms`/`active_ms` from metrics, `domain`/`surface` from context, strip promoted fields from `collector_metrics`/`collector_context`.
- **IngestionDtos:** No change — API payload shape stays the same.
- **EstimationRepository/Service:** Update queries from JSONB extraction to direct column access.
- **InsightsService:** Currently queries legacy `sessions` table. Migration to `activity_sessions` becomes much simpler with flat columns.
- **Migration:** New Flyway migration to alter table (add columns, drop old JSONB columns, migrate existing data from raw_payload).

## Migration Strategy

Flyway migration that:
1. Adds new columns (nullable initially)
2. Backfills from `raw_payload` JSONB
3. Sets NOT NULL constraints after backfill
4. Drops old JSONB columns (`collector`, `source`, `session`, `privacy`, `metrics`, `context`)
5. Renames/creates new JSONB columns (`collector_metrics`, `collector_context`)
