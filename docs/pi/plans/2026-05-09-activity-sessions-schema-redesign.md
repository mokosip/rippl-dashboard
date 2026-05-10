# Activity Sessions Schema Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Flatten activity_sessions table from JSONB blobs to typed columns per the approved spec.

**Architecture:** Flyway migration truncates and rebuilds activity_sessions + estimated_sessions. IngestionRepository rewrites INSERT to map flat columns. IngestionService adds validation for newly required context/metrics fields. Tests updated to include required fields.

**Tech Stack:** Kotlin, Spring Boot, JdbcTemplate, Flyway, PostgreSQL, Testcontainers, MockMvc

**Spec:** `docs/pi/specs/2026-05-09-activity-sessions-schema-redesign.md`

---

### Task 1: Flyway Migration V7

**Files:**
- Create: `backend/src/main/resources/db/migration/V7__flatten_activity_sessions.sql`

- [ ] **Step 1: Write the migration**

```sql
-- Truncate dependent tables first (early stage, data not precious)
TRUNCATE TABLE estimated_sessions CASCADE;
TRUNCATE TABLE activity_feedback CASCADE;
TRUNCATE TABLE activity_sessions CASCADE;

-- Drop old JSONB blob columns
ALTER TABLE activity_sessions
    DROP COLUMN collector,
    DROP COLUMN source,
    DROP COLUMN session,
    DROP COLUMN privacy,
    DROP COLUMN metrics,
    DROP COLUMN context;

-- Drop old BIGINT time columns (will be re-added as TIMESTAMPTZ)
ALTER TABLE activity_sessions
    DROP COLUMN started_at,
    DROP COLUMN ended_at;

-- Add new typed columns
ALTER TABLE activity_sessions
    ADD COLUMN collector_version    TEXT,
    ADD COLUMN source_type          TEXT NOT NULL,
    ADD COLUMN source_version       TEXT,
    ADD COLUMN domain               TEXT NOT NULL,
    ADD COLUMN surface              TEXT NOT NULL,
    ADD COLUMN started_at           TIMESTAMPTZ NOT NULL,
    ADD COLUMN ended_at             TIMESTAMPTZ NOT NULL,
    ADD COLUMN duration_ms          BIGINT NOT NULL,
    ADD COLUMN active_ms            BIGINT NOT NULL,
    ADD COLUMN collector_metrics    JSONB DEFAULT '{}'::jsonb,
    ADD COLUMN collector_context    JSONB DEFAULT '{}'::jsonb;

-- Add CHECK constraints
ALTER TABLE activity_sessions
    ADD CONSTRAINT chk_time_range CHECK (ended_at >= started_at),
    ADD CONSTRAINT chk_duration CHECK (duration_ms >= 0),
    ADD CONSTRAINT chk_active CHECK (active_ms >= 0);

-- Drop old indexes (they reference old columns or will be recreated)
DROP INDEX IF EXISTS idx_activity_sessions_user_created;

-- Create new indexes
CREATE INDEX idx_activity_sessions_user_time
    ON activity_sessions(user_id, started_at DESC);

CREATE INDEX idx_activity_sessions_domain
    ON activity_sessions(user_id, domain);

CREATE INDEX idx_activity_sessions_collector
    ON activity_sessions(user_id, collector_type);
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/db/migration/V7__flatten_activity_sessions.sql
git commit -m "🗃️ migration: flatten activity_sessions to typed columns (V7)"
```

---

### Task 2: Update IngestionService Validation

**Files:**
- Modify: `backend/src/main/kotlin/app/rippl/ingestion/IngestionService.kt`

- [ ] **Step 1: Write failing test for missing context.domain**

Add to `backend/src/test/kotlin/app/rippl/ingestion/IngestionControllerTest.kt`:

```kotlin
@Test
fun `POST v1 activity sessions rejects missing context domain`() {
    mockMvc.post("/v1/activity-sessions") {
        contentType = MediaType.APPLICATION_JSON
        header("Authorization", "Bearer $bearerToken")
        content = validPayload().replace(""""domain": "claude.ai",""", "")
    }.andExpect {
        status { isBadRequest() }
        jsonPath("$.error_code") { value("validation_error") }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "app.rippl.ingestion.IngestionControllerTest.POST v1 activity sessions rejects missing context domain"`

Expected: FAIL — currently no validation on context.domain

- [ ] **Step 3: Write failing test for missing context.surface**

Add to `IngestionControllerTest.kt`:

```kotlin
@Test
fun `POST v1 activity sessions rejects missing context surface`() {
    mockMvc.post("/v1/activity-sessions") {
        contentType = MediaType.APPLICATION_JSON
        header("Authorization", "Bearer $bearerToken")
        content = validPayload().replace(""""surface": "web",""", "")
    }.andExpect {
        status { isBadRequest() }
        jsonPath("$.error_code") { value("validation_error") }
    }
}
```

- [ ] **Step 4: Write failing test for missing metrics.duration_ms**

Add to `IngestionControllerTest.kt`:

```kotlin
@Test
fun `POST v1 activity sessions rejects missing metrics duration_ms`() {
    val payload = """
        {
          "collector": {"type": "chrome_extension", "version": "1.0.0"},
          "source": {"type": "browser", "version": "126"},
          "session": {
            "id": "sess-${UUID.randomUUID()}",
            "started_at": ${Instant.now().minusSeconds(120).toEpochMilli()},
            "ended_at": ${Instant.now().toEpochMilli()}
          },
          "privacy": {
            "content_collected": false,
            "content_sent": false,
            "prompt_collected": false,
            "response_collected": false
          },
          "metrics": {"active_ms": 5000},
          "context": {"domain": "claude.ai", "surface": "web"}
        }
    """.trimIndent()

    mockMvc.post("/v1/activity-sessions") {
        contentType = MediaType.APPLICATION_JSON
        header("Authorization", "Bearer $bearerToken")
        content = payload
    }.andExpect {
        status { isBadRequest() }
        jsonPath("$.error_code") { value("validation_error") }
    }
}
```

- [ ] **Step 5: Write failing test for missing metrics.active_ms**

Add to `IngestionControllerTest.kt`:

```kotlin
@Test
fun `POST v1 activity sessions rejects missing metrics active_ms`() {
    val payload = """
        {
          "collector": {"type": "chrome_extension", "version": "1.0.0"},
          "source": {"type": "browser", "version": "126"},
          "session": {
            "id": "sess-${UUID.randomUUID()}",
            "started_at": ${Instant.now().minusSeconds(120).toEpochMilli()},
            "ended_at": ${Instant.now().toEpochMilli()}
          },
          "privacy": {
            "content_collected": false,
            "content_sent": false,
            "prompt_collected": false,
            "response_collected": false
          },
          "metrics": {"duration_ms": 5000},
          "context": {"domain": "claude.ai", "surface": "web"}
        }
    """.trimIndent()

    mockMvc.post("/v1/activity-sessions") {
        contentType = MediaType.APPLICATION_JSON
        header("Authorization", "Bearer $bearerToken")
        content = payload
    }.andExpect {
        status { isBadRequest() }
        jsonPath("$.error_code") { value("validation_error") }
    }
}
```

- [ ] **Step 6: Implement validation in IngestionService**

Add to `validateSessionPayload` in `backend/src/main/kotlin/app/rippl/ingestion/IngestionService.kt`, after existing validations:

```kotlin
val domain = payload.context["domain"]
if (domain == null || domain !is String || domain.isBlank()) {
    errors.add(FieldError("context.domain", "must be a non-blank string"))
}

val surface = payload.context["surface"]
if (surface == null || surface !is String || surface.isBlank()) {
    errors.add(FieldError("context.surface", "must be a non-blank string"))
}

val durationMs = payload.metrics["duration_ms"]
if (durationMs == null || durationMs !is Number) {
    errors.add(FieldError("metrics.duration_ms", "must be a number"))
}

val activeMs = payload.metrics["active_ms"]
if (activeMs == null || activeMs !is Number) {
    errors.add(FieldError("metrics.active_ms", "must be a number"))
}
```

- [ ] **Step 7: Run new validation tests**

Run: `./gradlew test --tests "app.rippl.ingestion.IngestionControllerTest"`

Expected: New tests PASS. Existing tests may FAIL (missing required fields in payloads — fixed in Task 3).

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/ingestion/IngestionService.kt
git add backend/src/test/kotlin/app/rippl/ingestion/IngestionControllerTest.kt
git commit -m "✨ feat: validate required context/metrics fields for flat schema"
```

---

### Task 3: Update Test Payload Helpers

**Files:**
- Modify: `backend/src/test/kotlin/app/rippl/ingestion/IngestionControllerTest.kt`
- Modify: `backend/src/test/kotlin/app/rippl/ingestion/EstimatedSessionStorageTest.kt`

- [ ] **Step 1: Update validPayload in IngestionControllerTest**

Replace `validPayload` method:

```kotlin
private fun validPayload(
    sessionExternalId: String = "sess-${UUID.randomUUID()}",
    startedAt: Long = Instant.now().minusSeconds(120).toEpochMilli(),
    endedAt: Long = Instant.now().toEpochMilli(),
    contentCollected: Boolean = false,
    metricValue: Int = 1,
    extraCollectorField: Boolean = false
): String {
    val extra = if (extraCollectorField) ",\"unexpected\":\"nope\"" else ""
    val durationMs = endedAt - startedAt
    return """
        {
          "collector": {"type": "chrome_extension", "version": "1.0.0"$extra},
          "source": {"type": "browser", "version": "126"},
          "session": {
            "id": "$sessionExternalId",
            "started_at": $startedAt,
            "ended_at": $endedAt
          },
          "privacy": {
            "content_collected": $contentCollected,
            "content_sent": false,
            "prompt_collected": false,
            "response_collected": false
          },
          "metrics": {
            "duration_ms": $durationMs,
            "active_ms": $durationMs,
            "sample_metric": $metricValue
          },
          "context": {
            "domain": "claude.ai",
            "surface": "web",
            "mode": "legacy-sync"
          }
        }
    """.trimIndent()
}
```

- [ ] **Step 2: Update validPayloadWithFlexibleMetricsAndContext**

Replace method — must include required fields alongside extra fields:

```kotlin
private fun validPayloadWithFlexibleMetricsAndContext(): String {
    val now = Instant.now().toEpochMilli()
    val start = now - 30_000
    return """
        {
          "collector": {"type": "chrome_extension", "version": "1.0.0"},
          "source": {"type": "browser", "version": "126"},
          "session": {
            "id": "sess-flex-${UUID.randomUUID()}",
            "started_at": $start,
            "ended_at": $now
          },
          "privacy": {
            "content_collected": false,
            "content_sent": false,
            "prompt_collected": false,
            "response_collected": false
          },
          "metrics": {
            "duration_ms": 30000,
            "active_ms": 30000,
            "totally_new_metric": {
              "deep": {"number": 7}
            }
          },
          "context": {
            "domain": "claude.ai",
            "surface": "web",
            "extra": {
              "nested": ["a", "b"]
            }
          }
        }
    """.trimIndent()
}
```

- [ ] **Step 3: Update sessionPayload in EstimatedSessionStorageTest**

Replace `sessionPayload` method:

```kotlin
private fun sessionPayload(
    sessionExternalId: String = "sess-${UUID.randomUUID()}",
    startedAt: Long = Instant.now().minusSeconds(120).toEpochMilli(),
    endedAt: Long = Instant.now().toEpochMilli()
): String {
    val durationMs = endedAt - startedAt
    return """
        {
          "collector": {"type": "chrome_extension", "version": "1.0.0"},
          "source": {"type": "browser", "version": "126"},
          "session": {
            "id": "$sessionExternalId",
            "started_at": $startedAt,
            "ended_at": $endedAt
          },
          "privacy": {
            "content_collected": false,
            "content_sent": false,
            "prompt_collected": false,
            "response_collected": false
          },
          "metrics": {
            "duration_ms": $durationMs,
            "active_ms": $durationMs,
            "sample_metric": 1
          },
          "context": {
            "domain": "claude.ai",
            "surface": "web",
            "mode": "legacy-sync"
          }
        }
    """.trimIndent()
}
```

- [ ] **Step 4: Update dedupe test assertions**

In `POST v1 activity sessions dedupes and keeps immutable raw payload` test, the query reading `raw_payload -> 'metrics' ->> 'sample_metric'` stays unchanged — raw_payload still has full payload.

In `POST v1 activity sessions dedupe advances synced_at and keeps raw payload immutable` test, same — raw_payload query unchanged.

No code changes needed in these tests — only the payload helpers.

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/kotlin/app/rippl/ingestion/IngestionControllerTest.kt
git add backend/src/test/kotlin/app/rippl/ingestion/EstimatedSessionStorageTest.kt
git commit -m "✅ test: add required context/metrics fields to test payloads"
```

---

### Task 4: Rewrite IngestionRepository

**Files:**
- Modify: `backend/src/main/kotlin/app/rippl/ingestion/IngestionRepository.kt`

- [ ] **Step 1: Rewrite the ingest method**

Replace `ingest` method in `backend/src/main/kotlin/app/rippl/ingestion/IngestionRepository.kt`:

```kotlin
fun ingest(userId: UUID, payload: ActivitySessionRequest, rawPayload: String): IngestWriteResult {
    val collectorMetrics = payload.metrics
        .filterKeys { it !in setOf("duration_ms", "active_ms") }
    val collectorContext = payload.context
        .filterKeys { it !in setOf("domain", "surface") }

    val collectorMetricsJson = objectMapper.writeValueAsString(collectorMetrics)
    val collectorContextJson = objectMapper.writeValueAsString(collectorContext)

    val startedAt = java.sql.Timestamp.from(java.time.Instant.ofEpochMilli(payload.session.startedAt))
    val endedAt = java.sql.Timestamp.from(java.time.Instant.ofEpochMilli(payload.session.endedAt))
    val durationMs = (payload.metrics["duration_ms"] as Number).toLong()
    val activeMs = (payload.metrics["active_ms"] as Number).toLong()
    val domain = payload.context["domain"] as String
    val surface = payload.context["surface"] as String

    val (sessionId, wasInserted) = jdbc.query(
        """
        INSERT INTO activity_sessions (
            user_id,
            collector_type,
            collector_version,
            collector_session_id,
            source_type,
            source_version,
            domain,
            surface,
            started_at,
            ended_at,
            duration_ms,
            active_ms,
            collector_metrics,
            collector_context,
            raw_payload
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb)
        ON CONFLICT (user_id, collector_type, collector_session_id)
        DO UPDATE SET synced_at = now()
        RETURNING id, (xmax = 0) AS was_inserted
        """.trimIndent(),
        { rs, _ -> Pair(UUID.fromString(rs.getString("id")), rs.getBoolean("was_inserted")) },
        userId,
        payload.collector.type,
        payload.collector.version,
        payload.session.id,
        payload.source.type,
        payload.source.version,
        domain,
        surface,
        startedAt,
        endedAt,
        durationMs,
        activeMs,
        collectorMetricsJson,
        collectorContextJson,
        rawPayload
    ).first()

    return IngestWriteResult(sessionId = sessionId, deduped = !wasInserted)
}
```

- [ ] **Step 2: Remove unused imports**

Remove these imports from IngestionRepository (no longer needed since we don't serialize collector/source/session/privacy to JSON):

The old code serialized `collectorJson`, `sourceJson`, `sessionJson`, `privacyJson` — those variables are gone. The method now only serializes `collectorMetrics` and `collectorContext` maps. Remove any unused variable references.

- [ ] **Step 3: Run all ingestion tests**

Run: `./gradlew test --tests "app.rippl.ingestion.*"`

Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/ingestion/IngestionRepository.kt
git commit -m "♻️ refactor: rewrite IngestionRepository for flat column schema"
```

---

### Task 5: Update SchemaTest

**Files:**
- Modify: `backend/src/test/kotlin/app/rippl/SchemaTest.kt`

- [ ] **Step 1: Add column structure test**

Add to `backend/src/test/kotlin/app/rippl/SchemaTest.kt`:

```kotlin
@Test
fun `activity_sessions has flat columns not JSONB blobs`() {
    val columns = jdbc.queryForList(
        """
        SELECT column_name, data_type
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'activity_sessions'
        ORDER BY ordinal_position
        """.trimIndent()
    ).associate { it["column_name"] as String to it["data_type"] as String }

    // New flat columns exist
    assert("domain" in columns) { "Missing column: domain" }
    assert("surface" in columns) { "Missing column: surface" }
    assert("source_type" in columns) { "Missing column: source_type" }
    assert("duration_ms" in columns) { "Missing column: duration_ms" }
    assert("active_ms" in columns) { "Missing column: active_ms" }
    assert("collector_version" in columns) { "Missing column: collector_version" }
    assert(columns["started_at"] == "timestamp with time zone") {
        "started_at should be TIMESTAMPTZ, got: ${columns["started_at"]}"
    }

    // Old JSONB blob columns are gone
    assert("collector" !in columns) { "Old column 'collector' should be dropped" }
    assert("source" !in columns) { "Old column 'source' should be dropped" }
    assert("session" !in columns) { "Old column 'session' should be dropped" }
    assert("privacy" !in columns) { "Old column 'privacy' should be dropped" }

    // JSONB columns for variable data
    assert("collector_metrics" in columns) { "Missing column: collector_metrics" }
    assert("collector_context" in columns) { "Missing column: collector_context" }
    assert("raw_payload" in columns) { "Missing column: raw_payload" }
}
```

- [ ] **Step 2: Update existing index test**

Add to `SchemaTest.kt`:

```kotlin
@Test
fun `activity_sessions has new indexes`() {
    val indexes = jdbc.queryForList(
        "SELECT indexname FROM pg_indexes WHERE tablename = 'activity_sessions'",
        String::class.java
    )
    assert(indexes.contains("uq_activity_sessions_dedupe")) { "Missing uq_activity_sessions_dedupe" }
    assert(indexes.contains("idx_activity_sessions_user_time")) { "Missing idx_activity_sessions_user_time" }
    assert(indexes.contains("idx_activity_sessions_domain")) { "Missing idx_activity_sessions_domain" }
    assert(indexes.contains("idx_activity_sessions_collector")) { "Missing idx_activity_sessions_collector" }

    // Old index should be gone
    assert(!indexes.contains("idx_activity_sessions_user_created")) {
        "Old idx_activity_sessions_user_created should be dropped"
    }
}
```

- [ ] **Step 3: Run SchemaTest**

Run: `./gradlew test --tests "app.rippl.SchemaTest"`

Expected: All PASS

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/kotlin/app/rippl/SchemaTest.kt
git commit -m "✅ test: verify flat activity_sessions schema structure and indexes"
```

---

### Task 6: Full Test Suite Verification

- [ ] **Step 1: Run complete test suite**

Run: `./gradlew test`

Expected: All tests PASS

- [ ] **Step 2: Verify collector_metrics stripping works**

Add to `IngestionControllerTest.kt`:

```kotlin
@Test
fun `POST v1 activity sessions strips promoted fields from collector_metrics`() {
    val externalId = "sess-strip-${UUID.randomUUID()}"

    mockMvc.post("/v1/activity-sessions") {
        contentType = MediaType.APPLICATION_JSON
        header("Authorization", "Bearer $bearerToken")
        content = validPayload(sessionExternalId = externalId)
    }.andExpect { status { isCreated() } }

    val collectorMetrics = jdbc.queryForObject(
        "SELECT collector_metrics FROM activity_sessions WHERE collector_session_id = ?",
        String::class.java,
        externalId
    )!!

    val metricsNode = objectMapper.readTree(collectorMetrics)
    assert(!metricsNode.has("duration_ms")) { "duration_ms should be stripped from collector_metrics" }
    assert(!metricsNode.has("active_ms")) { "active_ms should be stripped from collector_metrics" }
    assert(metricsNode.has("sample_metric")) { "sample_metric should remain in collector_metrics" }

    val domain = jdbc.queryForObject(
        "SELECT domain FROM activity_sessions WHERE collector_session_id = ?",
        String::class.java,
        externalId
    )
    assertEquals("claude.ai", domain)

    val surface = jdbc.queryForObject(
        "SELECT surface FROM activity_sessions WHERE collector_session_id = ?",
        String::class.java,
        externalId
    )
    assertEquals("web", surface)
}
```

- [ ] **Step 3: Run full test suite again**

Run: `./gradlew test`

Expected: All PASS

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/kotlin/app/rippl/ingestion/IngestionControllerTest.kt
git commit -m "✅ test: verify collector_metrics stripping and flat column storage"
```
