# Dashboard Rework — "Show scored, not self-reported"

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite all dashboard queries and UI to display backend-scored data (`scored_sessions` + `activity_sessions`) instead of self-reported values from legacy `sessions` table. Add confidence-aware displays.

**Architecture:** All trend/insight queries switch from `sessions` to `activity_sessions LEFT JOIN scored_sessions`. Active time always comes from `activity_sessions.active_ms`; time-saved comes from `scored_sessions.estimated_time_saved_ms` (converted to minutes in SQL). Confidence is aggregated as weighted average (low=1, med=2, high=3 weighted by time_saved). Spider chart uses weighted minutes (task proportion × time_saved). Frontend shows tilde prefix + label for confidence. Legacy `sessions` table left untouched — removal is a separate follow-up.

**Tech Stack:** Kotlin/Spring Boot (backend), TypeScript/React/Vite (frontend), PostgreSQL, JdbcTemplate (raw SQL)

**Decisions (from grill-me session):**

| Decision | Choice |
|---|---|
| Query strategy | `activity_sessions LEFT JOIN scored_sessions` — no fallback to legacy |
| Time unit in API | Minutes (backend converts ms→min) |
| Spider chart | Weighted minutes: proportion × time_saved per task |
| Aggregate confidence | Weighted avg (low=1, med=2, high=3), weight by time_saved |
| Heatmap value | Time saved (minutes), from scored_sessions |
| Mirror moments | Drop `timeSavingActivity`, keep other 3 adapted |
| Domain source | `activity_sessions.domain` |
| Confidence UI | Tilde prefix + small label (high: no tilde, medium: ~, low: "rough estimate") |
| Old sessions table | Leave for now |
| DTOs | Extend existing, add `confidence` field |
| totalSeconds source | `activity_sessions.active_ms / 1000` (always present) |

---

## File Structure

**Backend — modify:**
- `backend/src/main/kotlin/app/rippl/trends/TrendsDtos.kt` — add `confidence` field to all DTOs, add `byTaskMix` to `TimeSaved`
- `backend/src/main/kotlin/app/rippl/trends/TrendsService.kt` — rewrite all 4 queries to join scored_sessions + activity_sessions
- `backend/src/main/kotlin/app/rippl/insights/InsightsService.kt` — rewrite 3 mirror moments (drop `timeSavingActivity`), adapt queries
- `backend/src/test/kotlin/app/rippl/trends/TrendsServiceTest.kt` — rewrite tests with new table setup
- `backend/src/test/kotlin/app/rippl/insights/InsightsServiceTest.kt` — rewrite tests with new table setup

**Frontend — modify:**
- `frontend/src/types.ts` — add `confidence` to trend/timeSaved types, add `byTaskMix`
- `frontend/src/components/TimeSavedCard.tsx` — confidence-aware display
- `frontend/src/components/RippleSpider.tsx` — receive task mix data (already works with `{name, value}[]`)
- `frontend/src/pages/Trends.tsx` — wire `byTaskMix` into spider chart instead of `byActivity`
- `frontend/src/pages/Dashboard.tsx` — pass confidence to TimeSavedCard
- `frontend/src/components/MirrorMomentCard.tsx` — remove `time_saving_activity` icon entry

---

### Task 1: Update Backend DTOs

**Files:**
- Modify: `backend/src/main/kotlin/app/rippl/trends/TrendsDtos.kt`

- [ ] **Step 1: Add confidence and byTaskMix fields**

```kotlin
package app.rippl.trends

import java.time.LocalDate

data class WeeklyTrend(
    val week: LocalDate,
    val domain: String,
    val totalSeconds: Long,
    val totalSaved: Int,
    val confidence: String = "high"
)

data class MonthlyTrend(
    val month: LocalDate,
    val domain: String,
    val totalSeconds: Long,
    val totalSaved: Int,
    val confidence: String = "high"
)

data class TimeSaved(
    val total: Int,
    val confidence: String,
    val byDomain: Map<String, Int>,
    val byTaskMix: Map<String, Int>
)
```

Note: `TimeSaved.byActivity` renamed to `byTaskMix` — data source changed from self-reported activity_type to inferred task proportions. Old `byActivity` field removed.

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/trends/TrendsDtos.kt
git commit -m "refactor(trends): add confidence to DTOs, rename byActivity to byTaskMix"
```

---

### Task 2: Rewrite TrendsService Queries

**Files:**
- Modify: `backend/src/main/kotlin/app/rippl/trends/TrendsService.kt`

- [ ] **Step 1: Add confidence calculation helper**

Add at the top of the `TrendsService` class:

```kotlin
private fun weightedConfidence(rows: List<Pair<String, Int>>): String {
    val levelMap = mapOf("low" to 1, "medium" to 2, "high" to 3)
    var weightedSum = 0L
    var totalWeight = 0L
    for ((conf, saved) in rows) {
        val level = levelMap[conf] ?: 2
        weightedSum += level.toLong() * saved
        totalWeight += saved
    }
    if (totalWeight == 0L) return "high"
    val avg = weightedSum.toDouble() / totalWeight
    return when {
        avg >= 2.5 -> "high"
        avg >= 1.5 -> "medium"
        else -> "low"
    }
}
```

- [ ] **Step 2: Rewrite `weekly()` query**

```kotlin
fun weekly(userId: UUID, from: LocalDate, to: LocalDate): List<WeeklyTrend> {
    log.debug("Querying weekly trends for userId: {}, from: {}, to: {}", userId, from, to)
    val results = jdbc.query(
        """
        SELECT date_trunc('week', a.started_at)::date AS week,
               a.domain,
               SUM(a.active_ms / 1000)::bigint AS total_seconds,
               COALESCE(SUM(s.estimated_time_saved_ms / 60000), 0)::int AS total_saved,
               COALESCE(
                   (SELECT conf FROM (
                       SELECT s2.confidence AS conf,
                              SUM(s2.estimated_time_saved_ms)::bigint AS w
                       FROM scored_sessions s2
                       JOIN activity_sessions a2 ON a2.id = s2.activity_session_id
                       WHERE a2.user_id = a.user_id
                         AND date_trunc('week', a2.started_at)::date = date_trunc('week', a.started_at)::date
                         AND a2.domain = a.domain
                       GROUP BY s2.confidence
                       ORDER BY w DESC LIMIT 1
                   ) sub), 'high') AS confidence
        FROM activity_sessions a
        LEFT JOIN scored_sessions s ON s.activity_session_id = a.id
        WHERE a.user_id = ? AND a.started_at::date >= ? AND a.started_at::date <= ?
        GROUP BY week, a.domain, a.user_id
        ORDER BY week
        """,
        { rs, _ ->
            WeeklyTrend(
                rs.getDate("week").toLocalDate(),
                rs.getString("domain"),
                rs.getLong("total_seconds"),
                rs.getInt("total_saved"),
                rs.getString("confidence")
            )
        },
        userId, from, to
    )
    log.debug("Weekly trends result count: {}", results.size)
    return results
}
```

Wait — the per-row correlated subquery is ugly and slow. Simpler approach: compute confidence at application level.

**Revised approach:** Query rows with individual confidence values, aggregate in Kotlin.

```kotlin
fun weekly(userId: UUID, from: LocalDate, to: LocalDate): List<WeeklyTrend> {
    log.debug("Querying weekly trends for userId: {}, from: {}, to: {}", userId, from, to)

    data class RawRow(val week: LocalDate, val domain: String, val activeMs: Long, val savedMs: Long, val confidence: String?)

    val rows = jdbc.query(
        """
        SELECT date_trunc('week', a.started_at)::date AS week,
               a.domain,
               a.active_ms,
               COALESCE(s.estimated_time_saved_ms, 0) AS saved_ms,
               s.confidence::text AS confidence
        FROM activity_sessions a
        LEFT JOIN scored_sessions s ON s.activity_session_id = a.id
        WHERE a.user_id = ? AND a.started_at::date >= ? AND a.started_at::date <= ?
        ORDER BY week
        """,
        { rs, _ ->
            RawRow(
                rs.getDate("week").toLocalDate(),
                rs.getString("domain"),
                rs.getLong("active_ms"),
                rs.getLong("saved_ms"),
                rs.getString("confidence")
            )
        },
        userId, from, to
    )

    return rows.groupBy { it.week to it.domain }.map { (key, group) ->
        val (week, domain) = key
        val totalSeconds = group.sumOf { it.activeMs } / 1000
        val totalSaved = (group.sumOf { it.savedMs } / 60000).toInt()
        val confPairs = group.mapNotNull { r -> r.confidence?.let { it to (r.savedMs / 60000).toInt() } }
        WeeklyTrend(week, domain, totalSeconds, totalSaved, weightedConfidence(confPairs))
    }
}
```

- [ ] **Step 3: Rewrite `monthly()` query**

Same pattern as weekly, just `date_trunc('month', ...)`:

```kotlin
fun monthly(userId: UUID, from: LocalDate, to: LocalDate): List<MonthlyTrend> {
    log.debug("Querying monthly trends for userId: {}, from: {}, to: {}", userId, from, to)

    data class RawRow(val month: LocalDate, val domain: String, val activeMs: Long, val savedMs: Long, val confidence: String?)

    val rows = jdbc.query(
        """
        SELECT date_trunc('month', a.started_at)::date AS month,
               a.domain,
               a.active_ms,
               COALESCE(s.estimated_time_saved_ms, 0) AS saved_ms,
               s.confidence::text AS confidence
        FROM activity_sessions a
        LEFT JOIN scored_sessions s ON s.activity_session_id = a.id
        WHERE a.user_id = ? AND a.started_at::date >= ? AND a.started_at::date <= ?
        ORDER BY month
        """,
        { rs, _ ->
            RawRow(
                rs.getDate("month").toLocalDate(),
                rs.getString("domain"),
                rs.getLong("active_ms"),
                rs.getLong("saved_ms"),
                rs.getString("confidence")
            )
        },
        userId, from, to
    )

    return rows.groupBy { it.month to it.domain }.map { (key, group) ->
        val (month, domain) = key
        val totalSeconds = group.sumOf { it.activeMs } / 1000
        val totalSaved = (group.sumOf { it.savedMs } / 60000).toInt()
        val confPairs = group.mapNotNull { r -> r.confidence?.let { it to (r.savedMs / 60000).toInt() } }
        MonthlyTrend(month, domain, totalSeconds, totalSaved, weightedConfidence(confPairs))
    }
}
```

- [ ] **Step 4: Rewrite `timeSaved()` query**

```kotlin
fun timeSaved(userId: UUID): TimeSaved {
    log.debug("Querying time-saved for userId: {}", userId)

    data class SessionRow(val domain: String, val savedMs: Long, val confidence: String?, val taskMixJson: String?)

    val rows = jdbc.query(
        """
        SELECT a.domain,
               COALESCE(s.estimated_time_saved_ms, 0) AS saved_ms,
               s.confidence::text AS confidence,
               s.inferred_task_mix::text AS task_mix_json
        FROM activity_sessions a
        LEFT JOIN scored_sessions s ON s.activity_session_id = a.id
        WHERE a.user_id = ?
        """,
        { rs, _ ->
            SessionRow(
                rs.getString("domain"),
                rs.getLong("saved_ms"),
                rs.getString("confidence"),
                rs.getString("task_mix_json")
            )
        },
        userId
    )

    val totalSavedMin = (rows.sumOf { it.savedMs } / 60000).toInt()

    val byDomain = rows.groupBy { it.domain }
        .mapValues { (_, group) -> (group.sumOf { it.savedMs } / 60000).toInt() }
        .filterValues { it > 0 }
        .toSortedMap(compareByDescending { key -> rows.filter { it.domain == key }.sumOf { it.savedMs } })

    // Weighted task mix: proportion × time_saved per task key
    val taskTotals = mutableMapOf<String, Long>()
    for (row in rows) {
        if (row.taskMixJson == null || row.savedMs <= 0) continue
        try {
            @Suppress("UNCHECKED_CAST")
            val mix = com.fasterxml.jackson.databind.ObjectMapper().readValue(
                row.taskMixJson, Map::class.java
            ) as Map<String, Number>
            for ((task, proportion) in mix) {
                val contribution = (proportion.toDouble() * row.savedMs).toLong()
                taskTotals[task] = (taskTotals[task] ?: 0L) + contribution
            }
        } catch (_: Exception) { }
    }
    val byTaskMix = taskTotals
        .mapValues { (it.value / 60000).toInt() }
        .filterValues { it > 0 }
        .toSortedMap(compareByDescending { taskTotals[it] })

    val confPairs = rows.mapNotNull { r -> r.confidence?.let { it to (r.savedMs / 60000).toInt() } }
    val confidence = weightedConfidence(confPairs)

    log.debug("Time-saved for userId: {} — total: {}min, domains: {}, tasks: {}, confidence: {}",
        userId, totalSavedMin, byDomain.size, byTaskMix.size, confidence)
    return TimeSaved(totalSavedMin, confidence, byDomain, byTaskMix)
}
```

Note: inject `ObjectMapper` via constructor instead of creating inline. Update class signature:

```kotlin
@Service
class TrendsService(
    private val jdbc: JdbcTemplate,
    private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper
) {
```

Then in the task mix parsing:
```kotlin
val mix = objectMapper.readValue(row.taskMixJson, Map::class.java) as Map<String, Number>
```

- [ ] **Step 5: Rewrite `activityHeatmap()` query**

```kotlin
fun activityHeatmap(userId: UUID): List<List<Int>> {
    val rows = jdbc.query(
        """
        SELECT EXTRACT(DOW FROM a.started_at)::int AS dow,
               EXTRACT(HOUR FROM a.started_at)::int AS hour,
               COALESCE(SUM(s.estimated_time_saved_ms / 60000), 0)::int AS saved
        FROM activity_sessions a
        LEFT JOIN scored_sessions s ON s.activity_session_id = a.id
        WHERE a.user_id = ?
        GROUP BY dow, hour
        """,
        { rs, _ -> Triple(rs.getInt("dow"), rs.getInt("hour"), rs.getInt("saved")) },
        userId
    )
    val grid = Array(7) { IntArray(24) }
    for ((dow, hour, saved) in rows) {
        val mondayBased = if (dow == 0) 6 else dow - 1
        grid[mondayBased][hour] = saved
    }
    return grid.map { it.toList() }
}
```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/trends/TrendsService.kt
git commit -m "refactor(trends): rewrite queries to use scored_sessions + activity_sessions"
```

---

### Task 3: Rewrite InsightsService (Mirror Moments)

**Files:**
- Modify: `backend/src/main/kotlin/app/rippl/insights/InsightsService.kt`

- [ ] **Step 1: Remove `timeSavingActivity()` and update `mirrorMoments()`**

Remove the entire `timeSavingActivity()` private method. Update `mirrorMoments()` to not call it:

```kotlin
fun mirrorMoments(userId: UUID): List<MirrorMoment> {
    val moments = mutableListOf<MirrorMoment>()
    weeklyUsage(userId)?.let { moments.add(it) }
    topTool(userId)?.let { moments.add(it) }
    busiestDay(userId)?.let { moments.add(it) }
    log.debug("Generated {} mirror moments for userId: {} — types: {}", moments.size, userId, moments.map { it.type })
    return moments
}
```

- [ ] **Step 2: Rewrite `weeklyUsage()` query**

```kotlin
private fun weeklyUsage(userId: UUID): MirrorMoment? {
    val rows = jdbc.query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN a.started_at >= date_trunc('week', CURRENT_DATE)
                THEN a.active_ms END), 0) / 1000 AS this_week,
            COALESCE(SUM(CASE WHEN a.started_at >= date_trunc('week', CURRENT_DATE) - INTERVAL '7 days'
                              AND a.started_at < date_trunc('week', CURRENT_DATE)
                THEN a.active_ms END), 0) / 1000 AS last_week
        FROM activity_sessions a
        WHERE a.user_id = ?
        """,
        { rs, _ -> rs.getLong("this_week") to rs.getLong("last_week") },
        userId
    )
    val (thisWeek, lastWeek) = rows.firstOrNull() ?: return null
    if (thisWeek == 0L) return null

    val timeStr = if (thisWeek < 3600) {
        "${thisWeek / 60} minutes"
    } else {
        "%.1f hours".format(java.util.Locale.GERMAN, thisWeek / 3600.0)
    }
    val comparison = when {
        lastWeek == 0L -> "your first tracked week"
        thisWeek > lastWeek -> "${(thisWeek - lastWeek) * 100 / lastWeek}% more than last week"
        thisWeek < lastWeek -> "${(lastWeek - thisWeek) * 100 / lastWeek}% less than last week"
        else -> "same as last week"
    }
    return MirrorMoment("weekly_usage", "You used AI for $timeStr this week — $comparison.")
}
```

- [ ] **Step 3: Rewrite `topTool()` query**

```kotlin
private fun topTool(userId: UUID): MirrorMoment? {
    val tools = jdbc.query(
        """
        SELECT a.domain, SUM(a.active_ms)::bigint / 1000 AS total
        FROM activity_sessions a
        WHERE a.user_id = ?
        GROUP BY a.domain ORDER BY total DESC LIMIT 2
        """,
        { rs, _ -> rs.getString("domain") to rs.getLong("total") },
        userId
    )
    if (tools.size < 2) return null
    val ratio = "%.1f".format(tools[0].second.toDouble() / tools[1].second)
    return MirrorMoment("top_tool",
        "${tools[0].first} is your most-used tool. You spend ${ratio}x more time there than ${tools[1].first}.")
}
```

- [ ] **Step 4: Rewrite `busiestDay()` query**

```kotlin
private fun busiestDay(userId: UUID): MirrorMoment? {
    val byDay = jdbc.query(
        """
        SELECT EXTRACT(DOW FROM a.started_at)::int AS dow,
               SUM(a.active_ms)::bigint / 1000 AS total
        FROM activity_sessions a
        WHERE a.user_id = ?
        GROUP BY dow ORDER BY total DESC
        """,
        { rs, _ -> rs.getInt("dow") to rs.getLong("total") },
        userId
    )
    if (byDay.size < 2) return null
    return MirrorMoment("busiest_day",
        "Your busiest AI day is ${dayNames[byDay.first().first]}. You barely touch AI on ${dayNames[byDay.last().first]}.")
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/insights/InsightsService.kt
git commit -m "refactor(insights): rewrite mirror moments for scored data, drop timeSavingActivity"
```

---

### Task 4: Rewrite Backend Tests

**Files:**
- Modify: `backend/src/test/kotlin/app/rippl/trends/TrendsServiceTest.kt`
- Modify: `backend/src/test/kotlin/app/rippl/insights/InsightsServiceTest.kt`

Tests currently use `SessionRepository` + `Session` entity (legacy `sessions` table). New tests must insert into `activity_sessions` + `scored_sessions` via JdbcTemplate.

- [ ] **Step 1: Rewrite TrendsServiceTest setup and tests**

```kotlin
package app.rippl.trends

import app.rippl.TestcontainersConfig
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@Import(TestcontainersConfig::class)
class TrendsServiceTest {

    @Autowired lateinit var trendsService: TrendsService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var jdbc: JdbcTemplate

    private lateinit var userId: UUID

    private fun insertActivitySession(
        id: UUID, userId: UUID, domain: String, surface: String,
        startedAt: Instant, endedAt: Instant, durationMs: Long, activeMs: Long
    ) {
        jdbc.update(
            """
            INSERT INTO activity_sessions (id, user_id, collector_type, collector_session_id,
                source_type, domain, surface, started_at, ended_at, duration_ms, active_ms)
            VALUES (?, ?, 'browser', ?, 'extension', ?, ?, ?, ?, ?, ?)
            """,
            id, userId, UUID.randomUUID().toString(), domain, surface,
            java.sql.Timestamp.from(startedAt), java.sql.Timestamp.from(endedAt),
            durationMs, activeMs
        )
    }

    private fun insertScoredSession(
        activitySessionId: UUID, userId: UUID,
        taskMixJson: String, multiplier: Double, savedMs: Long,
        confidence: String, method: String
    ) {
        jdbc.update(
            """
            INSERT INTO scored_sessions (activity_session_id, user_id, inferred_task_mix,
                effective_multiplier, estimated_time_saved_ms, confidence, scoring_method, scored_at)
            VALUES (?, ?, ?::jsonb, ?, ?, ?::scoring_confidence, ?::scoring_method, now())
            """,
            activitySessionId, userId, taskMixJson, multiplier, savedMs, confidence, method
        )
    }

    @BeforeEach
    fun setup() {
        jdbc.update("DELETE FROM scored_sessions")
        jdbc.update("DELETE FROM activity_sessions")
        val user = userRepository.findByEmail("trends-test@example.com")
            ?: userRepository.save(User(email = "trends-test@example.com"))
        userId = user.id!!

        // Session 1: claude.ai, 2026-04-28, 10 min active, scored 19 min saved (high)
        val s1 = UUID.randomUUID()
        val april28 = Instant.parse("2026-04-28T10:00:00Z")
        insertActivitySession(s1, userId, "claude.ai", "web", april28, april28.plusSeconds(1200), 1200000, 600000)
        insertScoredSession(s1, userId, """{"coding":0.7,"research":0.3}""", 1.9, 19 * 60000L, "high", "profile_default")

        // Session 2: chatgpt.com, 2026-04-28, 5 min active, scored 8 min saved (medium)
        val s2 = UUID.randomUUID()
        insertActivitySession(s2, userId, "chatgpt.com", "web", april28.plusSeconds(3600), april28.plusSeconds(4200), 600000, 300000)
        insertScoredSession(s2, userId, """{"writing":0.8,"other":0.2}""", 1.6, 8 * 60000L, "medium", "profile_default")

        // Session 3: claude.ai, 2026-05-01, 15 min active, scored 25 min saved (high)
        val s3 = UUID.randomUUID()
        val may1 = Instant.parse("2026-05-01T14:00:00Z")
        insertActivitySession(s3, userId, "claude.ai", "web", may1, may1.plusSeconds(1800), 1800000, 900000)
        insertScoredSession(s3, userId, """{"coding":0.5,"planning":0.5}""", 1.7, 25 * 60000L, "high", "feedback_adjusted")
    }

    @Test
    fun `weekly returns data grouped by week and domain`() {
        val result = trendsService.weekly(userId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31))
        assertTrue(result.isNotEmpty())
        val claudeEntries = result.filter { it.domain == "claude.ai" }
        assertTrue(claudeEntries.isNotEmpty())
        assertTrue(claudeEntries.all { it.totalSaved > 0 })
    }

    @Test
    fun `weekly includes confidence from scored sessions`() {
        val result = trendsService.weekly(userId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31))
        assertTrue(result.all { it.confidence in listOf("low", "medium", "high") })
    }

    @Test
    fun `monthly returns data grouped by month and domain`() {
        val result = trendsService.monthly(userId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31))
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `timeSaved returns correct totals`() {
        val result = trendsService.timeSaved(userId)
        assertEquals(52, result.total) // 19 + 8 + 25
        assertEquals(44, result.byDomain["claude.ai"]) // 19 + 25
        assertEquals(8, result.byDomain["chatgpt.com"])
    }

    @Test
    fun `timeSaved returns byTaskMix with weighted minutes`() {
        val result = trendsService.timeSaved(userId)
        // coding: 0.7*19 + 0.5*25 = 13.3 + 12.5 = 25.8 → 25 min
        // research: 0.3*19 = 5.7 → 5 min
        // writing: 0.8*8 = 6.4 → 6 min
        // planning: 0.5*25 = 12.5 → 12 min
        // other: 0.2*8 = 1.6 → 1 min
        assertTrue(result.byTaskMix.containsKey("coding"))
        assertTrue(result.byTaskMix.containsKey("writing"))
        assertTrue(result.byTaskMix["coding"]!! > result.byTaskMix["writing"]!!)
    }

    @Test
    fun `timeSaved returns aggregate confidence`() {
        val result = trendsService.timeSaved(userId)
        assertEquals("high", result.confidence) // 2 high (19+25=44min) vs 1 medium (8min) → weighted avg > 2.5
    }

    @Test
    fun `timeSaved returns zeros for user with no sessions`() {
        val emptyUser = userRepository.save(User(email = "empty-trends@example.com"))
        val result = trendsService.timeSaved(emptyUser.id!!)
        assertEquals(0, result.total)
        assertTrue(result.byDomain.isEmpty())
        assertTrue(result.byTaskMix.isEmpty())
    }

    @Test
    fun `activityHeatmap returns 7x24 grid`() {
        val result = trendsService.activityHeatmap(userId)
        assertEquals(7, result.size)
        assertTrue(result.all { it.size == 24 })
    }
}
```

- [ ] **Step 2: Run TrendsServiceTest**

```bash
cd backend && ./gradlew test --tests "app.rippl.trends.TrendsServiceTest" -i
```

Expected: all tests pass.

- [ ] **Step 3: Rewrite InsightsServiceTest**

```kotlin
package app.rippl.insights

import app.rippl.TestcontainersConfig
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@SpringBootTest
@Import(TestcontainersConfig::class)
class InsightsServiceTest {

    @Autowired lateinit var insightsService: InsightsService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var jdbc: JdbcTemplate

    private lateinit var userId: UUID

    private fun insertActivitySession(
        id: UUID, userId: UUID, domain: String,
        startedAt: Instant, activeMs: Long
    ) {
        jdbc.update(
            """
            INSERT INTO activity_sessions (id, user_id, collector_type, collector_session_id,
                source_type, domain, surface, started_at, ended_at, duration_ms, active_ms)
            VALUES (?, ?, 'browser', ?, 'extension', ?, 'web', ?, ?, ?, ?)
            """,
            id, userId, UUID.randomUUID().toString(), domain,
            java.sql.Timestamp.from(startedAt),
            java.sql.Timestamp.from(startedAt.plusMillis(activeMs)),
            activeMs, activeMs
        )
    }

    @BeforeEach
    fun setup() {
        jdbc.update("DELETE FROM scored_sessions")
        jdbc.update("DELETE FROM activity_sessions")
        val user = userRepository.findByEmail("insights-test@example.com")
            ?: userRepository.save(User(email = "insights-test@example.com"))
        userId = user.id!!

        val thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastMonday = thisMonday.minusWeeks(1)
        val thisFriday = thisMonday.with(DayOfWeek.FRIDAY)

        // This week Monday: claude.ai, 1 hour active
        val s1 = UUID.randomUUID()
        insertActivitySession(s1, userId, "claude.ai",
            thisMonday.atStartOfDay().toInstant(ZoneOffset.UTC), 3600000)

        // Last week Monday: claude.ai, 30 min active
        val s2 = UUID.randomUUID()
        insertActivitySession(s2, userId, "claude.ai",
            lastMonday.atStartOfDay().toInstant(ZoneOffset.UTC), 1800000)

        // This week Monday: chatgpt.com, 10 min active
        val s3 = UUID.randomUUID()
        insertActivitySession(s3, userId, "chatgpt.com",
            thisMonday.atStartOfDay().plusSeconds(7200).toInstant(ZoneOffset.UTC), 600000)

        // This week Friday: claude.ai, 15 min active
        val s4 = UUID.randomUUID()
        insertActivitySession(s4, userId, "claude.ai",
            thisFriday.atStartOfDay().toInstant(ZoneOffset.UTC), 900000)
    }

    @Test
    fun `generates weekly usage moment`() {
        val moments = insightsService.mirrorMoments(userId)
        val weeklyMoment = moments.find { it.type == "weekly_usage" }
        assertNotNull(weeklyMoment)
        assertTrue(weeklyMoment!!.message.contains("this week"))
    }

    @Test
    fun `generates top tool moment when multiple tools used`() {
        val moments = insightsService.mirrorMoments(userId)
        val toolMoment = moments.find { it.type == "top_tool" }
        assertNotNull(toolMoment)
        assertTrue(toolMoment!!.message.contains("claude.ai"))
    }

    @Test
    fun `does not generate time_saving_activity moment`() {
        val moments = insightsService.mirrorMoments(userId)
        assertNull(moments.find { it.type == "time_saving_activity" })
    }

    @Test
    fun `returns empty list for user with no sessions`() {
        val emptyUser = userRepository.save(User(email = "empty-insights@example.com"))
        val moments = insightsService.mirrorMoments(emptyUser.id!!)
        assertTrue(moments.isEmpty())
    }
}
```

- [ ] **Step 4: Run InsightsServiceTest**

```bash
cd backend && ./gradlew test --tests "app.rippl.insights.InsightsServiceTest" -i
```

Expected: all tests pass.

- [ ] **Step 5: Run full test suite**

```bash
cd backend && ./gradlew test
```

Expected: all tests pass. May need to fix other tests that depend on `Session` entity if they share test data.

- [ ] **Step 6: Commit**

```bash
git add backend/src/test/
git commit -m "test(trends,insights): rewrite tests for scored_sessions data source"
```

---

### Task 5: Update Frontend Types

**Files:**
- Modify: `frontend/src/types.ts`

- [ ] **Step 1: Add confidence to types, rename byActivity to byTaskMix**

```typescript
export interface WeeklyTrend {
  week: string
  domain: string
  totalSeconds: number
  totalSaved: number
  confidence: 'low' | 'medium' | 'high'
}

export interface MonthlyTrend {
  month: string
  domain: string
  totalSeconds: number
  totalSaved: number
  confidence: 'low' | 'medium' | 'high'
}

export interface TimeSaved {
  total: number
  confidence: 'low' | 'medium' | 'high'
  byDomain: Record<string, number>
  byTaskMix: Record<string, number>
}
```

Leave all other types unchanged.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/types.ts
git commit -m "refactor(types): add confidence, rename byActivity to byTaskMix"
```

---

### Task 6: Update TimeSavedCard with Confidence Display

**Files:**
- Modify: `frontend/src/components/TimeSavedCard.tsx`

- [ ] **Step 1: Add confidence-aware display**

```tsx
import type { TimeSaved } from '../types'

const CONFIDENCE_DISPLAY = {
  high: { prefix: '', label: 'high confidence' },
  medium: { prefix: '~', label: 'estimate' },
  low: { prefix: '~', label: 'rough estimate' },
} as const

export function TimeSavedCard({ data }: { data: TimeSaved }) {
  const hours = Math.floor(data.total / 60)
  const minutes = data.total % 60
  const display = hours > 0 ? `${hours},${Math.round((minutes / 60) * 10)}h` : `${minutes}m`
  const conf = CONFIDENCE_DISPLAY[data.confidence]

  return (
    <div className="flex flex-col items-center py-8">
      <p className="text-6xl font-bold font-serif text-fg-accent">
        {conf.prefix}{display}
      </p>
      <p className="text-sm uppercase tracking-widest mt-1 text-fg-muted" style={{ letterSpacing: '2px' }}>
        Time Saved
      </p>
      <p className="text-xs text-fg-muted mt-1">{conf.label}</p>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/TimeSavedCard.tsx
git commit -m "feat(ui): add confidence display to TimeSavedCard"
```

---

### Task 7: Update Trends Page — Spider Chart Data Source

**Files:**
- Modify: `frontend/src/pages/Trends.tsx`
- Modify: `frontend/src/components/MirrorMomentCard.tsx`

- [ ] **Step 1: Switch spider chart from byActivity to byTaskMix**

In `Trends.tsx`, replace the `activityData` computation:

Old code (lines ~30-43):
```tsx
  const activityData = (() => {
    if (!timeSaved) return []
    const entries = Object.entries(timeSaved.byActivity).map(([activity, saved]) => ({
```

New code:
```tsx
  const activityData = (() => {
    if (!timeSaved) return []
    const entries = Object.entries(timeSaved.byTaskMix).map(([task, saved]) => ({
      name: task,
      value: saved,
      breakdown: undefined as { name: string; value: number }[] | undefined,
    }))
    const total = entries.reduce((sum, e) => sum + e.value, 0)
    if (total === 0) return entries
    const significant = entries.filter(e => (e.value / total) >= 0.01)
    const others = entries.filter(e => (e.value / total) < 0.01)
    const othersValue = others.reduce((sum, e) => sum + e.value, 0)
    if (othersValue > 0) significant.push({ name: 'Others', value: othersValue, breakdown: others })
    return significant
  })()
```

- [ ] **Step 2: Remove time_saving_activity icon from MirrorMomentCard**

In `MirrorMomentCard.tsx`, remove `'time_saving_activity': '⏱️'` from `TYPE_ICONS`:

```tsx
const TYPE_ICONS: Record<string, string> = {
  weekly_usage: '📈',
  top_tool: '🎯',
  busiest_day: '📅',
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/Trends.tsx frontend/src/components/MirrorMomentCard.tsx
git commit -m "feat(ui): wire spider chart to byTaskMix, remove time_saving_activity icon"
```

---

### Task 8: Manual Verification

- [ ] **Step 1: Start backend and verify API responses**

```bash
cd backend && ./gradlew bootRun
```

Hit endpoints with curl (or browser) and verify:
- `GET /api/trends/weekly` — returns `confidence` field per row
- `GET /api/trends/time-saved` — returns `confidence`, `byTaskMix` (not `byActivity`)
- `GET /api/trends/activity-heatmap` — returns 7×24 grid
- `GET /api/insights/mirror` — returns moments without `time_saving_activity` type

- [ ] **Step 2: Start frontend and verify UI**

```bash
cd frontend && npm run dev
```

Check:
- Dashboard TimeSavedCard shows confidence label
- Trends spider chart shows task mix categories (coding, writing, research, etc.)
- Mirror moments section has no "time saving activity" card
- Heatmap still renders

- [ ] **Step 3: Final commit if any fixups needed**

```bash
git add -A && git commit -m "fix: address manual verification findings"
```
