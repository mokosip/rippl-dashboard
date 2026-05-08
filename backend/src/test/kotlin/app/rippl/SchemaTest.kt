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
}
