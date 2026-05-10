package app.rippl

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest
@Import(TestcontainersConfig::class)
class SchemaTest {

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Test
    fun `migration creates all tables`() {
        val tables = jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
            String::class.java
        )
        assert(tables.containsAll(listOf(
            "users",
            "auth_tokens",
            "collectors",
            "sessions",
            "extension_tokens",
            "activity_sessions",
            "activity_feedback",
            "estimated_sessions"
        ))) {
            "Missing tables. Found: $tables"
        }
    }

    @Test
    fun `estimated_sessions user_id has no FK to users`() {
        // user_id is denormalized; must NOT have a FK constraint referencing users
        val fkCount = jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.referential_constraints rc
            JOIN information_schema.key_column_usage kcu
                ON kcu.constraint_name = rc.constraint_name
               AND kcu.constraint_schema = rc.constraint_schema
            WHERE rc.constraint_schema = 'public'
              AND kcu.table_name = 'estimated_sessions'
              AND kcu.column_name = 'user_id'
            """.trimIndent(),
            Int::class.java
        )
        assert(fkCount == 0) {
            "estimated_sessions.user_id must not have a FK constraint but found $fkCount"
        }
    }

    @Test
    fun `sessions table has correct indexes`() {
        val indexes = jdbc.queryForList(
            "SELECT indexname FROM pg_indexes WHERE tablename = 'sessions'",
            String::class.java
        )
        assert(indexes.contains("idx_sessions_user_date")) { "Missing idx_sessions_user_date" }
        assert(indexes.contains("idx_sessions_user_domain")) { "Missing idx_sessions_user_domain" }
    }

    @Test
    fun `estimated_sessions has idx_estimated_sessions_user_confidence index`() {
        val indexes = jdbc.queryForList(
            "SELECT indexname FROM pg_indexes WHERE tablename = 'estimated_sessions'",
            String::class.java
        )
        assert(indexes.contains("idx_estimated_sessions_user_confidence")) {
            "Missing idx_estimated_sessions_user_confidence. Found: $indexes"
        }
    }

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
}
