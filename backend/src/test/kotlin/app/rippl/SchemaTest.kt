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
            "activity_feedback"
        ))) {
            "Missing tables. Found: $tables"
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
}
