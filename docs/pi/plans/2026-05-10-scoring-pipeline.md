# Scoring Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace placeholder scoring with real task-mix inference, time-saved calculation, and confidence scoring for every ingested activity session.

**Architecture:** Three independent calculators (TaskMixInferenceService, TimeSavedCalculator, ConfidenceCalculator) orchestrated by ScoringService. All tunable values in `@ConfigurationProperties`. Feedback rescoring uses `@TransactionalEventListener` to guarantee data consistency.

**Tech Stack:** Kotlin, Spring Boot 4, JPA, JDBC, JUnit 5, Testcontainers

**Spec:** `docs/pi/specs/2026-05-10-scoring-pipeline-design.md`

---

### Task 1: ScoringConfig — ConfigurationProperties

**Files:**
- Create: `backend/src/main/kotlin/app/rippl/ingestion/ScoringConfig.kt`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/kotlin/app/rippl/DashboardApplication.kt`

- [ ] **Step 1: Create ScoringConfig**

```kotlin
// backend/src/main/kotlin/app/rippl/ingestion/ScoringConfig.kt
package app.rippl.ingestion

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "rippl.scoring")
data class ScoringConfig(
    val multipliers: Multipliers = Multipliers(),
    val confidenceWeights: ConfidenceWeights = ConfidenceWeights(),
    val confidenceThresholds: ConfidenceThresholds = ConfidenceThresholds(),
    val minScorableMs: Long = 10_000
) {
    data class Multipliers(
        val writing: Double = 1.4,
        val coding: Double = 1.7,
        val research: Double = 1.3,
        val planning: Double = 1.5,
        val communication: Double = 1.35,
        val other: Double = 1.2
    )

    data class ConfidenceWeights(
        val duration: Double = 0.25,
        val interaction: Double = 0.20,
        val taskCertainty: Double = 0.30,
        val collectorSignal: Double = 0.15,
        val profile: Double = 0.10
    )

    data class ConfidenceThresholds(
        val medium: Double = 0.40,
        val high: Double = 0.70
    )
}
```

- [ ] **Step 2: Enable ConfigurationPropertiesScan on DashboardApplication**

Add `@ConfigurationPropertiesScan` to `DashboardApplication.kt`:

```kotlin
// backend/src/main/kotlin/app/rippl/DashboardApplication.kt
package app.rippl

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@ConfigurationPropertiesScan
@SpringBootApplication
class DashboardApplication

fun main(args: Array<String>) {
    runApplication<DashboardApplication>(*args)
}
```

- [ ] **Step 3: Add scoring config to application.yml**

Append to end of `backend/src/main/resources/application.yml`:

```yaml
rippl:
  scoring:
    multipliers:
      writing: 1.4
      coding: 1.7
      research: 1.3
      planning: 1.5
      communication: 1.35
      other: 1.2
    confidence-weights:
      duration: 0.25
      interaction: 0.20
      task-certainty: 0.30
      collector-signal: 0.15
      profile: 0.10
    confidence-thresholds:
      medium: 0.40
      high: 0.70
    min-scorable-ms: 10000
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/ingestion/ScoringConfig.kt \
       backend/src/main/kotlin/app/rippl/DashboardApplication.kt \
       backend/src/main/resources/application.yml
git commit -m "feat(scoring): add ScoringConfig with configurable multipliers and thresholds"
```

---

### Task 2: ScoringDtos — Data Types

**Files:**
- Create: `backend/src/main/kotlin/app/rippl/ingestion/ScoringDtos.kt`

- [ ] **Step 1: Create ScoringDtos**

```kotlin
// backend/src/main/kotlin/app/rippl/ingestion/ScoringDtos.kt
package app.rippl.ingestion

import app.rippl.profile.TaskMix
import java.util.UUID

enum class ScoringMethod(val dbValue: String) {
    FEEDBACK_ADJUSTED("feedback_adjusted"),
    COLLECTOR_SIGNAL_ADJUSTED("collector_signal_adjusted"),
    PROFILE_DEFAULT("profile_default"),
    GLOBAL_FALLBACK("global_fallback");
}

data class ScoringInput(
    val sessionId: UUID,
    val userId: UUID,
    val collectorType: String,
    val surface: String,
    val activeMs: Long,
    val durationMs: Long,
    val interactionCount: Int,
    val feedbackTaskType: String?
)

data class InferenceResult(
    val taskMix: TaskMix,
    val method: ScoringMethod
)

data class TimeSavedResult(
    val savedMs: Long,
    val effectiveMultiplier: Double
)

data class ConfidenceResult(
    val score: Double,
    val level: String
)
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/ingestion/ScoringDtos.kt
git commit -m "feat(scoring): add scoring DTOs and ScoringMethod enum"
```

---

### Task 3: TaskMixInferenceService + Unit Tests

**Files:**
- Create: `backend/src/main/kotlin/app/rippl/ingestion/TaskMixInferenceService.kt`
- Create: `backend/src/test/kotlin/app/rippl/ingestion/TaskMixInferenceServiceTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// backend/src/test/kotlin/app/rippl/ingestion/TaskMixInferenceServiceTest.kt
package app.rippl.ingestion

import app.rippl.profile.TaskMix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TaskMixInferenceServiceTest {

    private val service = TaskMixInferenceService()

    @Test
    fun `feedback produces one-hot task mix`() {
        val result = service.infer(
            feedbackTaskType = "coding",
            collectorType = "chrome_extension",
            profile = TaskMix.GLOBAL_DEFAULT,
            profileOnboarded = true
        )
        assertEquals(TaskMix(coding = 1.0), result.taskMix)
        assertEquals(ScoringMethod.FEEDBACK_ADJUSTED, result.method)
    }

    @Test
    fun `unknown feedback type falls through to profile`() {
        val profile = TaskMix(writing = 0.6, research = 0.3, planning = 0.1)
        val result = service.infer(
            feedbackTaskType = "juggling",
            collectorType = "chrome_extension",
            profile = profile,
            profileOnboarded = true
        )
        assertEquals(profile, result.taskMix)
        assertEquals(ScoringMethod.PROFILE_DEFAULT, result.method)
    }

    @Test
    fun `cli_wrapper blends profile 50-50 with coding`() {
        val profile = TaskMix(coding = 0.2, writing = 0.8)
        val result = service.infer(
            feedbackTaskType = null,
            collectorType = "cli_wrapper",
            profile = profile,
            profileOnboarded = true
        )
        assertEquals(0.6, result.taskMix.coding, 0.001)
        assertEquals(0.4, result.taskMix.writing, 0.001)
        assertEquals(ScoringMethod.COLLECTOR_SIGNAL_ADJUSTED, result.method)
    }

    @Test
    fun `onboarded profile used when no feedback or collector hint`() {
        val profile = TaskMix(research = 0.5, writing = 0.3, planning = 0.2)
        val result = service.infer(
            feedbackTaskType = null,
            collectorType = "chrome_extension",
            profile = profile,
            profileOnboarded = true
        )
        assertEquals(profile, result.taskMix)
        assertEquals(ScoringMethod.PROFILE_DEFAULT, result.method)
    }

    @Test
    fun `non-onboarded profile falls through to global fallback`() {
        val result = service.infer(
            feedbackTaskType = null,
            collectorType = "chrome_extension",
            profile = TaskMix.GLOBAL_DEFAULT,
            profileOnboarded = false
        )
        assertEquals(TaskMix.GLOBAL_DEFAULT, result.taskMix)
        assertEquals(ScoringMethod.GLOBAL_FALLBACK, result.method)
    }

    @Test
    fun `feedback for writing produces one-hot`() {
        val result = service.infer(
            feedbackTaskType = "writing",
            collectorType = "chrome_extension",
            profile = TaskMix.GLOBAL_DEFAULT,
            profileOnboarded = false
        )
        assertEquals(TaskMix(writing = 1.0), result.taskMix)
        assertEquals(ScoringMethod.FEEDBACK_ADJUSTED, result.method)
    }

    @Test
    fun `cli_wrapper with feedback uses feedback not blend`() {
        val result = service.infer(
            feedbackTaskType = "research",
            collectorType = "cli_wrapper",
            profile = TaskMix.GLOBAL_DEFAULT,
            profileOnboarded = true
        )
        assertEquals(TaskMix(research = 1.0), result.taskMix)
        assertEquals(ScoringMethod.FEEDBACK_ADJUSTED, result.method)
    }
}
```

- [ ] **Step 2: Run tests — verify they fail**

Run: `cd backend && ../gradlew test --tests "app.rippl.ingestion.TaskMixInferenceServiceTest" --no-daemon`
Expected: Compilation failure — `TaskMixInferenceService` does not exist.

- [ ] **Step 3: Implement TaskMixInferenceService**

```kotlin
// backend/src/main/kotlin/app/rippl/ingestion/TaskMixInferenceService.kt
package app.rippl.ingestion

import app.rippl.profile.TaskMix
import org.springframework.stereotype.Service

@Service
class TaskMixInferenceService {

    companion object {
        private val KNOWN_TASK_TYPES = setOf("writing", "coding", "research", "planning", "communication", "other")
    }

    fun infer(
        feedbackTaskType: String?,
        collectorType: String,
        profile: TaskMix,
        profileOnboarded: Boolean
    ): InferenceResult {
        if (feedbackTaskType != null && feedbackTaskType in KNOWN_TASK_TYPES) {
            return InferenceResult(oneHot(feedbackTaskType), ScoringMethod.FEEDBACK_ADJUSTED)
        }

        if (collectorType == "cli_wrapper") {
            val codingHint = TaskMix(coding = 1.0)
            return InferenceResult(blend(profile, codingHint), ScoringMethod.COLLECTOR_SIGNAL_ADJUSTED)
        }

        if (profileOnboarded) {
            return InferenceResult(profile, ScoringMethod.PROFILE_DEFAULT)
        }

        return InferenceResult(TaskMix.GLOBAL_DEFAULT, ScoringMethod.GLOBAL_FALLBACK)
    }

    private fun oneHot(taskType: String): TaskMix = when (taskType) {
        "writing" -> TaskMix(writing = 1.0)
        "coding" -> TaskMix(coding = 1.0)
        "research" -> TaskMix(research = 1.0)
        "planning" -> TaskMix(planning = 1.0)
        "communication" -> TaskMix(communication = 1.0)
        "other" -> TaskMix(other = 1.0)
        else -> TaskMix.GLOBAL_DEFAULT
    }

    private fun blend(a: TaskMix, b: TaskMix): TaskMix = TaskMix(
        writing = a.writing * 0.5 + b.writing * 0.5,
        coding = a.coding * 0.5 + b.coding * 0.5,
        research = a.research * 0.5 + b.research * 0.5,
        planning = a.planning * 0.5 + b.planning * 0.5,
        communication = a.communication * 0.5 + b.communication * 0.5,
        other = a.other * 0.5 + b.other * 0.5
    )
}
```

- [ ] **Step 4: Run tests — verify they pass**

Run: `cd backend && ../gradlew test --tests "app.rippl.ingestion.TaskMixInferenceServiceTest" --no-daemon`
Expected: All 7 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/ingestion/TaskMixInferenceService.kt \
       backend/src/test/kotlin/app/rippl/ingestion/TaskMixInferenceServiceTest.kt
git commit -m "feat(scoring): implement task-mix inference with priority chain"
```

---

### Task 4: TimeSavedCalculator + Unit Tests

**Files:**
- Create: `backend/src/main/kotlin/app/rippl/ingestion/TimeSavedCalculator.kt`
- Create: `backend/src/test/kotlin/app/rippl/ingestion/TimeSavedCalculatorTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// backend/src/test/kotlin/app/rippl/ingestion/TimeSavedCalculatorTest.kt
package app.rippl.ingestion

import app.rippl.profile.TaskMix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimeSavedCalculatorTest {

    private val config = ScoringConfig()
    private val calculator = TimeSavedCalculator(config)

    @Test
    fun `pure coding session uses coding multiplier`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 1.0),
            activeMs = 600_000, // 10 min
            durationMs = 600_000,
            personalAdjustmentFactor = 1.0
        )
        // effective_multiplier = 1.7
        // base_saved = 600_000 * (1 - 1/1.7) = 600_000 * 0.41176 = 247_058
        assertEquals(1.7, result.effectiveMultiplier, 0.001)
        assertEquals(247_058, result.savedMs, 1000)
    }

    @Test
    fun `mixed task mix blends multipliers`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 0.5, writing = 0.5),
            activeMs = 600_000,
            durationMs = 600_000,
            personalAdjustmentFactor = 1.0
        )
        // effective_multiplier = 0.5*1.7 + 0.5*1.4 = 1.55
        assertEquals(1.55, result.effectiveMultiplier, 0.001)
        // base_saved = 600_000 * (1 - 1/1.55) = 600_000 * 0.35484 = 212_903
        assertEquals(212_903, result.savedMs, 1000)
    }

    @Test
    fun `personal adjustment factor scales result`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 1.0),
            activeMs = 600_000,
            durationMs = 600_000,
            personalAdjustmentFactor = 1.5
        )
        // base_saved = 247_058, adjusted = 247_058 * 1.5 = 370_588
        assertEquals(370_588, result.savedMs, 1500)
    }

    @Test
    fun `session under min threshold returns 0`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 1.0),
            activeMs = 5_000, // 5s, under 10s threshold
            durationMs = 5_000,
            personalAdjustmentFactor = 1.0
        )
        assertEquals(0L, result.savedMs)
    }

    @Test
    fun `zero active_ms falls back to duration`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 1.0),
            activeMs = 0,
            durationMs = 600_000,
            personalAdjustmentFactor = 1.0
        )
        // fallback activeMs = 600_000 * 0.7 = 420_000
        // base_saved = 420_000 * (1 - 1/1.7) = 420_000 * 0.41176 = 172_941
        assertEquals(172_941, result.savedMs, 1000)
    }

    @Test
    fun `zero duration returns 0`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 1.0),
            activeMs = 0,
            durationMs = 0,
            personalAdjustmentFactor = 1.0
        )
        assertEquals(0L, result.savedMs)
    }

    @Test
    fun `duration fallback under threshold returns 0`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 1.0),
            activeMs = 0,
            durationMs = 12_000, // fallback = 8_400ms, under 10s threshold
            personalAdjustmentFactor = 1.0
        )
        assertEquals(0L, result.savedMs)
    }
}
```

- [ ] **Step 2: Run tests — verify they fail**

Run: `cd backend && ../gradlew test --tests "app.rippl.ingestion.TimeSavedCalculatorTest" --no-daemon`
Expected: Compilation failure — `TimeSavedCalculator` does not exist.

- [ ] **Step 3: Implement TimeSavedCalculator**

```kotlin
// backend/src/main/kotlin/app/rippl/ingestion/TimeSavedCalculator.kt
package app.rippl.ingestion

import app.rippl.profile.TaskMix
import org.springframework.stereotype.Service

@Service
class TimeSavedCalculator(
    private val config: ScoringConfig
) {

    fun calculate(
        taskMix: TaskMix,
        activeMs: Long,
        durationMs: Long,
        personalAdjustmentFactor: Double
    ): TimeSavedResult {
        val effectiveActiveMs = if (activeMs > 0) activeMs else (durationMs * 0.7).toLong()

        if (effectiveActiveMs < config.minScorableMs) {
            return TimeSavedResult(savedMs = 0L, effectiveMultiplier = computeMultiplier(taskMix))
        }

        val multiplier = computeMultiplier(taskMix)
        val baseSaved = effectiveActiveMs * (1.0 - 1.0 / multiplier)
        val adjusted = (baseSaved * personalAdjustmentFactor).toLong()

        return TimeSavedResult(savedMs = adjusted, effectiveMultiplier = multiplier)
    }

    private fun computeMultiplier(taskMix: TaskMix): Double {
        val m = config.multipliers
        return taskMix.writing * m.writing +
            taskMix.coding * m.coding +
            taskMix.research * m.research +
            taskMix.planning * m.planning +
            taskMix.communication * m.communication +
            taskMix.other * m.other
    }
}
```

- [ ] **Step 4: Run tests — verify they pass**

Run: `cd backend && ../gradlew test --tests "app.rippl.ingestion.TimeSavedCalculatorTest" --no-daemon`
Expected: All 7 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/ingestion/TimeSavedCalculator.kt \
       backend/src/test/kotlin/app/rippl/ingestion/TimeSavedCalculatorTest.kt
git commit -m "feat(scoring): implement time-saved calculator with configurable multipliers"
```

---

### Task 5: ConfidenceCalculator + Unit Tests

**Files:**
- Create: `backend/src/main/kotlin/app/rippl/ingestion/ConfidenceCalculator.kt`
- Create: `backend/src/test/kotlin/app/rippl/ingestion/ConfidenceCalculatorTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// backend/src/test/kotlin/app/rippl/ingestion/ConfidenceCalculatorTest.kt
package app.rippl.ingestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfidenceCalculatorTest {

    private val config = ScoringConfig()
    private val calculator = ConfidenceCalculator(config)

    @Test
    fun `high confidence - long session with feedback and interactions`() {
        val result = calculator.calculate(
            durationMs = 1_800_000, // 30 min — caps at 1.0
            interactionCount = 25,  // caps at 1.0
            method = ScoringMethod.FEEDBACK_ADJUSTED, // task_certainty = 1.0
            collectorType = "chrome_extension",
            profileOnboarded = true // profile_quality = 1.0
        )
        // duration: 0.25*1.0 = 0.25
        // interaction: 0.20*1.0 = 0.20
        // task_certainty: 0.30*1.0 = 0.30
        // collector: 0.15*0.8 = 0.12  (browser + interactions>=1)
        // profile: 0.10*1.0 = 0.10
        // total = 0.97
        assertEquals(0.97, result.score, 0.01)
        assertEquals("high", result.level)
    }

    @Test
    fun `low confidence - short cli session no feedback no profile`() {
        val result = calculator.calculate(
            durationMs = 60_000, // 1 min
            interactionCount = 0,
            method = ScoringMethod.GLOBAL_FALLBACK, // task_certainty = 0.1
            collectorType = "cli_wrapper",
            profileOnboarded = false // profile_quality = 0.3
        )
        // duration: 0.25 * (60000/1800000) = 0.25 * 0.0333 = 0.0083
        // interaction: 0.20 * 0 = 0.0
        // task_certainty: 0.30 * 0.1 = 0.03
        // collector: 0.15 * 0.3 = 0.045
        // profile: 0.10 * 0.3 = 0.03
        // total = 0.1133
        assertEquals(0.11, result.score, 0.02)
        assertEquals("low", result.level)
    }

    @Test
    fun `medium confidence - moderate signals`() {
        val result = calculator.calculate(
            durationMs = 900_000, // 15 min
            interactionCount = 10,
            method = ScoringMethod.PROFILE_DEFAULT, // task_certainty = 0.4
            collectorType = "chrome_extension",
            profileOnboarded = true
        )
        // duration: 0.25 * 0.5 = 0.125
        // interaction: 0.20 * 0.5 = 0.10
        // task_certainty: 0.30 * 0.4 = 0.12
        // collector: 0.15 * 0.8 = 0.12  (browser + interactions>=1)
        // profile: 0.10 * 1.0 = 0.10
        // total = 0.565
        assertEquals(0.565, result.score, 0.01)
        assertEquals("medium", result.level)
    }

    @Test
    fun `browser without interactions gets lower collector signal`() {
        val result = calculator.calculate(
            durationMs = 1_800_000,
            interactionCount = 0,
            method = ScoringMethod.FEEDBACK_ADJUSTED,
            collectorType = "chrome_extension",
            profileOnboarded = true
        )
        // collector: 0.15 * 0.5 = 0.075  (browser + 0 interactions)
        // vs 0.15 * 0.8 = 0.12 with interactions
        // duration: 0.25, interaction: 0.0, task: 0.30, collector: 0.075, profile: 0.10
        // total = 0.725
        assertEquals(0.725, result.score, 0.01)
        assertEquals("high", result.level)
    }

    @Test
    fun `collector signal adjusted method gets 0_7 task certainty`() {
        val result = calculator.calculate(
            durationMs = 1_800_000,
            interactionCount = 20,
            method = ScoringMethod.COLLECTOR_SIGNAL_ADJUSTED,
            collectorType = "cli_wrapper",
            profileOnboarded = true
        )
        // task_certainty: 0.30 * 0.7 = 0.21
        // collector: 0.15 * 0.3 = 0.045 (cli)
        // total = 0.25 + 0.20 + 0.21 + 0.045 + 0.10 = 0.805
        assertEquals(0.805, result.score, 0.01)
        assertEquals("high", result.level)
    }

    @Test
    fun `duration caps at 30 minutes`() {
        val shortResult = calculator.calculate(
            durationMs = 3_600_000, // 60 min, but caps at 1.0
            interactionCount = 20,
            method = ScoringMethod.FEEDBACK_ADJUSTED,
            collectorType = "chrome_extension",
            profileOnboarded = true
        )
        val capResult = calculator.calculate(
            durationMs = 1_800_000, // exactly 30 min = 1.0
            interactionCount = 20,
            method = ScoringMethod.FEEDBACK_ADJUSTED,
            collectorType = "chrome_extension",
            profileOnboarded = true
        )
        assertEquals(capResult.score, shortResult.score, 0.001)
    }
}
```

- [ ] **Step 2: Run tests — verify they fail**

Run: `cd backend && ../gradlew test --tests "app.rippl.ingestion.ConfidenceCalculatorTest" --no-daemon`
Expected: Compilation failure — `ConfidenceCalculator` does not exist.

- [ ] **Step 3: Implement ConfidenceCalculator**

```kotlin
// backend/src/main/kotlin/app/rippl/ingestion/ConfidenceCalculator.kt
package app.rippl.ingestion

import org.springframework.stereotype.Service
import kotlin.math.min

@Service
class ConfidenceCalculator(
    private val config: ScoringConfig
) {

    fun calculate(
        durationMs: Long,
        interactionCount: Int,
        method: ScoringMethod,
        collectorType: String,
        profileOnboarded: Boolean
    ): ConfidenceResult {
        val w = config.confidenceWeights

        val durationQuality = min(durationMs / 1_800_000.0, 1.0)
        val interactionQuality = min(interactionCount / 20.0, 1.0)
        val taskCertainty = taskCertaintyFor(method)
        val collectorSignal = collectorSignalFor(collectorType, interactionCount)
        val profileQuality = if (profileOnboarded) 1.0 else 0.3

        val score = w.duration * durationQuality +
            w.interaction * interactionQuality +
            w.taskCertainty * taskCertainty +
            w.collectorSignal * collectorSignal +
            w.profile * profileQuality

        val level = when {
            score >= config.confidenceThresholds.high -> "high"
            score >= config.confidenceThresholds.medium -> "medium"
            else -> "low"
        }

        return ConfidenceResult(score = score, level = level)
    }

    private fun taskCertaintyFor(method: ScoringMethod): Double = when (method) {
        ScoringMethod.FEEDBACK_ADJUSTED -> 1.0
        ScoringMethod.COLLECTOR_SIGNAL_ADJUSTED -> 0.7
        ScoringMethod.PROFILE_DEFAULT -> 0.4
        ScoringMethod.GLOBAL_FALLBACK -> 0.1
    }

    private fun collectorSignalFor(collectorType: String, interactionCount: Int): Double = when {
        collectorType == "cli_wrapper" -> 0.3
        interactionCount >= 1 -> 0.8
        else -> 0.5
    }
}
```

- [ ] **Step 4: Run tests — verify they pass**

Run: `cd backend && ../gradlew test --tests "app.rippl.ingestion.ConfidenceCalculatorTest" --no-daemon`
Expected: All 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/ingestion/ConfidenceCalculator.kt \
       backend/src/test/kotlin/app/rippl/ingestion/ConfidenceCalculatorTest.kt
git commit -m "feat(scoring): implement confidence calculator with weighted factors"
```

---

### Task 6: ScoringRepository — Load Session Data

**Files:**
- Modify: `backend/src/main/kotlin/app/rippl/ingestion/ScoringRepository.kt`

- [ ] **Step 1: Add loadScoringInput query to ScoringRepository**

Add this method to the existing `ScoringRepository` class, below the existing `upsertScoring` method:

```kotlin
fun loadScoringInput(sessionId: UUID): ScoringInput? {
    return jdbc.query(
        """
        SELECT
            a.id AS session_id,
            a.user_id,
            a.collector_type,
            a.surface,
            a.active_ms,
            a.duration_ms,
            a.collector_metrics,
            f.feedback_value AS task_type_feedback
        FROM activity_sessions a
        LEFT JOIN activity_feedback f
            ON f.session_id = a.id AND f.feedback_type = 'task_type'
        WHERE a.id = ?
        """.trimIndent(),
        { rs, _ ->
            val metricsJson = rs.getString("collector_metrics")
            val metrics = try {
                @Suppress("UNCHECKED_CAST")
                com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(metricsJson, Map::class.java) as Map<String, Any?>
            } catch (_: Exception) {
                emptyMap<String, Any?>()
            }

            val interactionCount = listOf("interaction_count", "copy_events", "paste_events")
                .sumOf { key -> (metrics[key] as? Number)?.toInt() ?: 0 }

            val feedbackRaw = rs.getString("task_type_feedback")
            val feedbackTaskType = if (feedbackRaw != null) {
                try {
                    val node = com.fasterxml.jackson.databind.ObjectMapper().readTree(feedbackRaw)
                    if (node.isTextual) node.asText() else null
                } catch (_: Exception) {
                    null
                }
            } else null

            ScoringInput(
                sessionId = UUID.fromString(rs.getString("session_id")),
                userId = UUID.fromString(rs.getString("user_id")),
                collectorType = rs.getString("collector_type"),
                surface = rs.getString("surface"),
                activeMs = rs.getLong("active_ms"),
                durationMs = rs.getLong("duration_ms"),
                interactionCount = interactionCount,
                feedbackTaskType = feedbackTaskType
            )
        },
        sessionId
    ).firstOrNull()
}
```

Also add the `ObjectMapper` import and inject it. The full updated class:

```kotlin
// backend/src/main/kotlin/app/rippl/ingestion/ScoringRepository.kt
package app.rippl.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class ScoringRepository(
    private val jdbc: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {

    fun upsertScoring(
        sessionId: UUID,
        inferredTaskMixJson: String,
        effectiveMultiplier: Double,
        estimatedTimeSavedMs: Long,
        confidence: String,
        scoringMethod: String
    ) {
        jdbc.update(
            """
            INSERT INTO scored_sessions (
                activity_session_id,
                user_id,
                inferred_task_mix,
                effective_multiplier,
                estimated_time_saved_ms,
                confidence,
                scoring_method,
                scored_at
            )
            SELECT
                a.id,
                a.user_id,
                ?::jsonb,
                ?,
                ?,
                ?::scoring_confidence,
                ?::scoring_method,
                now()
            FROM activity_sessions a
            WHERE a.id = ?
            ON CONFLICT (activity_session_id) DO UPDATE SET
                inferred_task_mix       = EXCLUDED.inferred_task_mix,
                effective_multiplier    = EXCLUDED.effective_multiplier,
                estimated_time_saved_ms = EXCLUDED.estimated_time_saved_ms,
                confidence              = EXCLUDED.confidence,
                scoring_method          = EXCLUDED.scoring_method,
                scored_at               = now()
            """.trimIndent(),
            inferredTaskMixJson,
            effectiveMultiplier,
            estimatedTimeSavedMs,
            confidence,
            scoringMethod,
            sessionId
        )
    }

    fun loadScoringInput(sessionId: UUID): ScoringInput? {
        return jdbc.query(
            """
            SELECT
                a.id AS session_id,
                a.user_id,
                a.collector_type,
                a.surface,
                a.active_ms,
                a.duration_ms,
                a.collector_metrics,
                f.feedback_value AS task_type_feedback
            FROM activity_sessions a
            LEFT JOIN activity_feedback f
                ON f.session_id = a.id AND f.feedback_type = 'task_type'
            WHERE a.id = ?
            """.trimIndent(),
            { rs, _ ->
                val metricsJson = rs.getString("collector_metrics")
                val metrics = try {
                    @Suppress("UNCHECKED_CAST")
                    objectMapper.readValue(metricsJson, Map::class.java) as Map<String, Any?>
                } catch (_: Exception) {
                    emptyMap<String, Any?>()
                }

                val interactionCount = listOf("interaction_count", "copy_events", "paste_events")
                    .sumOf { key -> (metrics[key] as? Number)?.toInt() ?: 0 }

                val feedbackRaw = rs.getString("task_type_feedback")
                val feedbackTaskType = if (feedbackRaw != null) {
                    try {
                        val node = objectMapper.readTree(feedbackRaw)
                        if (node.isTextual) node.asText() else null
                    } catch (_: Exception) {
                        null
                    }
                } else null

                ScoringInput(
                    sessionId = UUID.fromString(rs.getString("session_id")),
                    userId = UUID.fromString(rs.getString("user_id")),
                    collectorType = rs.getString("collector_type"),
                    surface = rs.getString("surface"),
                    activeMs = rs.getLong("active_ms"),
                    durationMs = rs.getLong("duration_ms"),
                    interactionCount = interactionCount,
                    feedbackTaskType = feedbackTaskType
                )
            },
            sessionId
        ).firstOrNull()
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/ingestion/ScoringRepository.kt
git commit -m "feat(scoring): add loadScoringInput query to ScoringRepository"
```

---

### Task 7: ScoringService — Replace Placeholders with Real Logic

**Files:**
- Modify: `backend/src/main/kotlin/app/rippl/ingestion/ScoringService.kt`

- [ ] **Step 1: Rewrite ScoringService to orchestrate real scoring**

```kotlin
// backend/src/main/kotlin/app/rippl/ingestion/ScoringService.kt
package app.rippl.ingestion

import app.rippl.profile.TaskMix
import app.rippl.profile.UserProfileRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ScoringService(
    private val scoringRepository: ScoringRepository,
    private val userProfileRepository: UserProfileRepository,
    private val inferenceService: TaskMixInferenceService,
    private val timeSavedCalculator: TimeSavedCalculator,
    private val confidenceCalculator: ConfidenceCalculator,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun scoreInitial(sessionId: UUID) {
        score(sessionId)
    }

    fun rescoreFromFeedback(sessionId: UUID) {
        score(sessionId)
    }

    private fun score(sessionId: UUID) {
        val input = scoringRepository.loadScoringInput(sessionId)
        if (input == null) {
            log.warn("Cannot score session={}: not found", sessionId)
            return
        }

        val profile = userProfileRepository.findByUserId(input.userId)

        val inference = inferenceService.infer(
            feedbackTaskType = input.feedbackTaskType,
            collectorType = input.collectorType,
            profile = profile?.taskMix ?: TaskMix.GLOBAL_DEFAULT,
            profileOnboarded = profile?.onboarded ?: false
        )

        val timeSaved = timeSavedCalculator.calculate(
            taskMix = inference.taskMix,
            activeMs = input.activeMs,
            durationMs = input.durationMs,
            personalAdjustmentFactor = profile?.personalAdjustmentFactor ?: 1.0
        )

        val confidence = confidenceCalculator.calculate(
            durationMs = input.durationMs,
            interactionCount = input.interactionCount,
            method = inference.method,
            collectorType = input.collectorType,
            profileOnboarded = profile?.onboarded ?: false
        )

        val taskMixJson = objectMapper.writeValueAsString(inference.taskMix)

        scoringRepository.upsertScoring(
            sessionId = sessionId,
            inferredTaskMixJson = taskMixJson,
            effectiveMultiplier = timeSaved.effectiveMultiplier,
            estimatedTimeSavedMs = timeSaved.savedMs,
            confidence = confidence.level,
            scoringMethod = inference.method.dbValue
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/ingestion/ScoringService.kt
git commit -m "feat(scoring): wire real inference, time-saved, and confidence into ScoringService"
```

---

### Task 8: TransactionalEventListener for Feedback Rescoring

**Files:**
- Modify: `backend/src/main/kotlin/app/rippl/ingestion/ScoringDispatchService.kt`
- Modify: `backend/src/main/kotlin/app/rippl/ingestion/IngestionService.kt`
- Create: `backend/src/main/kotlin/app/rippl/ingestion/ScoringEvents.kt`

- [ ] **Step 1: Create event class**

```kotlin
// backend/src/main/kotlin/app/rippl/ingestion/ScoringEvents.kt
package app.rippl.ingestion

import java.util.UUID

data class FeedbackSavedEvent(val sessionId: UUID)
```

- [ ] **Step 2: Update IngestionService to publish event instead of direct async call**

In `backend/src/main/kotlin/app/rippl/ingestion/IngestionService.kt`, inject `ApplicationEventPublisher` and replace the direct `scoringDispatchService.triggerFeedbackRescoring()` call in `saveFeedback`:

```kotlin
// backend/src/main/kotlin/app/rippl/ingestion/IngestionService.kt
package app.rippl.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class IngestionService(
    private val repository: IngestionRepository,
    private val scoringDispatchService: ScoringDispatchService,
    private val objectMapper: ObjectMapper,
    private val eventPublisher: ApplicationEventPublisher
) {

    companion object {
        private const val MAX_SESSION_DURATION_MILLIS = 24 * 60 * 60 * 1000L
    }

    fun ingest(userId: UUID, payload: ActivitySessionRequest, rawPayload: String): IngestWriteResult {
        validateSessionPayload(payload)

        val result = repository.ingest(userId, payload, rawPayload)
        if (!result.deduped) {
            scoringDispatchService.triggerInitialScoring(result.sessionId)
        }
        return result
    }

    @Transactional
    fun saveFeedback(userId: UUID, sessionId: UUID, request: SessionFeedbackRequest) {
        validateFeedback(request)

        val ownedSession = repository.findOwnedSession(sessionId, userId)
            ?: throw IngestionProblemException(
                httpStatus = HttpStatus.NOT_FOUND,
                errorCode = "session_not_found",
                message = "Activity session not found"
            )

        val valueJson = objectMapper.writeValueAsString(request.value)
        repository.upsertFeedback(ownedSession, request.type, valueJson)
        eventPublisher.publishEvent(FeedbackSavedEvent(ownedSession))
    }

    private fun validateSessionPayload(payload: ActivitySessionRequest) {
        val errors = mutableListOf<FieldError>()

        if (payload.collector.type.isBlank()) {
            errors.add(FieldError("collector.type", "must not be blank"))
        }

        if (payload.source.type.isBlank()) {
            errors.add(FieldError("source.type", "must not be blank"))
        }

        if (payload.session.id.isBlank()) {
            errors.add(FieldError("session.id", "must not be blank"))
        }

        if (payload.session.endedAt < payload.session.startedAt) {
            errors.add(FieldError("session.ended_at", "must be greater than or equal to session.started_at"))
        }

        if (payload.session.startedAt <= 0) {
            errors.add(FieldError("session.started_at", "must be a positive unix timestamp in milliseconds"))
        }

        if (payload.session.endedAt <= 0) {
            errors.add(FieldError("session.ended_at", "must be a positive unix timestamp in milliseconds"))
        }

        val duration = payload.session.endedAt - payload.session.startedAt
        if (duration > MAX_SESSION_DURATION_MILLIS) {
            errors.add(FieldError("session.ended_at", "duration exceeds max bound of 24h"))
        }

        val domain = payload.context["domain"]
        if (domain !is String || domain.isBlank()) {
            errors.add(FieldError("context.domain", "must be a non-blank string"))
        }

        val surface = payload.context["surface"]
        if (surface !is String || surface.isBlank()) {
            errors.add(FieldError("context.surface", "must be a non-blank string"))
        }

        val durationMs = payload.metrics["duration_ms"]
        if (durationMs !is Number || durationMs.toLong() < 0) {
            errors.add(FieldError("metrics.duration_ms", "must be a non-negative number"))
        }

        val activeMs = payload.metrics["active_ms"]
        if (activeMs !is Number || activeMs.toLong() < 0) {
            errors.add(FieldError("metrics.active_ms", "must be a non-negative number"))
        }

        if (errors.isNotEmpty()) {
            throw IngestionProblemException(
                httpStatus = HttpStatus.BAD_REQUEST,
                errorCode = "validation_error",
                message = "Invalid activity session payload",
                fieldErrors = errors
            )
        }

        if (
            payload.privacy.contentCollected ||
            payload.privacy.contentSent ||
            payload.privacy.promptCollected ||
            payload.privacy.responseCollected
        ) {
            throw IngestionProblemException(
                httpStatus = HttpStatus.BAD_REQUEST,
                errorCode = "policy_violation",
                message = "Forbidden privacy flags must be false",
                fieldErrors = listOf(
                    FieldError("privacy", "content_collected, content_sent, prompt_collected, response_collected must remain false")
                )
            )
        }
    }

    private fun validateFeedback(request: SessionFeedbackRequest) {
        val errors = mutableListOf<FieldError>()

        if (request.type != "task_type" && request.type != "accuracy") {
            errors.add(FieldError("type", "must be one of: task_type, accuracy"))
        }

        if (request.type == "task_type" && (!request.value.isTextual || request.value.asText().isBlank())) {
            errors.add(FieldError("value", "must be non-empty string when type=task_type"))
        }

        if (request.type == "accuracy" && !request.value.isNumber) {
            errors.add(FieldError("value", "must be numeric when type=accuracy"))
        }

        if (errors.isNotEmpty()) {
            throw IngestionProblemException(
                httpStatus = HttpStatus.BAD_REQUEST,
                errorCode = "validation_error",
                message = "Invalid feedback payload",
                fieldErrors = errors
            )
        }
    }
}
```

- [ ] **Step 3: Update ScoringDispatchService — add TransactionalEventListener, remove old triggerFeedbackRescoring**

```kotlin
// backend/src/main/kotlin/app/rippl/ingestion/ScoringDispatchService.kt
package app.rippl.ingestion

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import java.util.UUID

@Service
class ScoringDispatchService(
    private val scoringService: ScoringService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun triggerInitialScoring(sessionId: UUID) {
        try {
            scoringService.scoreInitial(sessionId)
            log.debug("Completed async initial scoring for sessionId={}", sessionId)
        } catch (ex: Exception) {
            log.warn("Failed async initial scoring for sessionId={}", sessionId, ex)
        }
    }

    @Async
    @TransactionalEventListener
    fun onFeedbackSaved(event: FeedbackSavedEvent) {
        try {
            scoringService.rescoreFromFeedback(event.sessionId)
            log.debug("Completed feedback re-scoring for sessionId={}", event.sessionId)
        } catch (ex: Exception) {
            log.warn("Failed feedback re-scoring for sessionId={}", event.sessionId, ex)
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/kotlin/app/rippl/ingestion/ScoringEvents.kt \
       backend/src/main/kotlin/app/rippl/ingestion/IngestionService.kt \
       backend/src/main/kotlin/app/rippl/ingestion/ScoringDispatchService.kt
git commit -m "feat(scoring): use TransactionalEventListener for feedback rescoring"
```

---

### Task 9: Update Integration Tests

**Files:**
- Modify: `backend/src/test/kotlin/app/rippl/ingestion/ScoredSessionStorageTest.kt`

- [ ] **Step 1: Add assertions for real scoring values**

Add a new test to the existing `ScoredSessionStorageTest` class that verifies real values are produced (not placeholders). Add these tests after the existing ones:

```kotlin
@Test
fun `scoring produces real values not placeholders`() {
    val ingestResponse = mockMvc.post("/v1/activity-sessions") {
        contentType = MediaType.APPLICATION_JSON
        header("Authorization", "Bearer $bearerToken")
        content = sessionPayload(
            sessionExternalId = "sess-real-${UUID.randomUUID()}",
            startedAt = Instant.now().minusSeconds(1200).toEpochMilli(), // 20 min session
            endedAt = Instant.now().toEpochMilli()
        )
    }.andExpect { status { isCreated() } }.andReturn()

    val sessionId = UUID.fromString(
        objectMapper.readTree(ingestResponse.response.contentAsString).get("session_id").asText()
    )

    val scored = pollUntil {
        jdbc.query(
            """
            SELECT effective_multiplier, estimated_time_saved_ms, confidence, scoring_method, inferred_task_mix
            FROM scored_sessions WHERE activity_session_id = ?
            """,
            { rs, _ ->
                mapOf(
                    "multiplier" to rs.getDouble("effective_multiplier"),
                    "saved" to rs.getLong("estimated_time_saved_ms"),
                    "confidence" to rs.getString("confidence"),
                    "method" to rs.getString("scoring_method"),
                    "task_mix" to rs.getString("inferred_task_mix")
                )
            },
            sessionId
        ).firstOrNull()
    } ?: error("scored_sessions row never appeared for session $sessionId")

    val multiplier = scored["multiplier"] as Double
    val saved = scored["saved"] as Long
    val method = scored["method"] as String
    val taskMix = scored["task_mix"] as String

    assert(multiplier > 1.0) { "effective_multiplier should be > 1.0, was $multiplier" }
    assert(saved > 0) { "estimated_time_saved_ms should be > 0 for a 20min session, was $saved" }
    assertEquals("global_fallback", method) { "no profile or feedback, should be global_fallback" }
    assert(!taskMix.contains("unknown")) { "task_mix should not contain 'unknown' placeholder, was $taskMix" }
}

@Test
fun `feedback rescoring updates method to feedback_adjusted`() {
    val ingestResponse = mockMvc.post("/v1/activity-sessions") {
        contentType = MediaType.APPLICATION_JSON
        header("Authorization", "Bearer $bearerToken")
        content = sessionPayload(
            sessionExternalId = "sess-fb-${UUID.randomUUID()}",
            startedAt = Instant.now().minusSeconds(600).toEpochMilli(),
            endedAt = Instant.now().toEpochMilli()
        )
    }.andExpect { status { isCreated() } }.andReturn()

    val sessionId = UUID.fromString(
        objectMapper.readTree(ingestResponse.response.contentAsString).get("session_id").asText()
    )

    pollUntil {
        jdbc.queryForObject(
            "SELECT scored_at FROM scored_sessions WHERE activity_session_id = ?",
            java.sql.Timestamp::class.java,
            sessionId
        )
    } ?: error("Initial scoring never completed for session $sessionId")

    mockMvc.post("/v1/activity-sessions/$sessionId/feedback") {
        contentType = MediaType.APPLICATION_JSON
        header("Authorization", "Bearer $bearerToken")
        content = """{"type":"task_type","value":"coding"}"""
    }.andExpect { status { isOk() } }

    val method = pollUntil {
        val m = jdbc.queryForObject(
            "SELECT scoring_method FROM scored_sessions WHERE activity_session_id = ?",
            String::class.java,
            sessionId
        )
        if (m == "feedback_adjusted") m else null
    }

    assertEquals("feedback_adjusted", method) {
        "After task_type feedback, scoring_method should be feedback_adjusted"
    }

    val multiplier = jdbc.queryForObject(
        "SELECT effective_multiplier FROM scored_sessions WHERE activity_session_id = ?",
        Double::class.java,
        sessionId
    )
    assertEquals(1.7, multiplier!!, 0.001) { "coding feedback should produce coding multiplier 1.7" }
}
```

- [ ] **Step 2: Run all tests**

Run: `cd backend && ../gradlew test --no-daemon`
Expected: All tests PASS, including new and existing.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/kotlin/app/rippl/ingestion/ScoredSessionStorageTest.kt
git commit -m "test(scoring): add integration tests for real scoring values and feedback rescoring"
```

---

### Task 10: Verify Full Pipeline End-to-End

- [ ] **Step 1: Run full test suite**

Run: `cd backend && ../gradlew test --no-daemon`
Expected: All tests PASS. No compilation errors.

- [ ] **Step 2: Verify no placeholder references remain**

Run: `grep -r "PLACEHOLDER" backend/src/main/kotlin/app/rippl/ingestion/ --include="*.kt"`
Expected: No matches.

- [ ] **Step 3: Final commit (if any fixups needed)**

```bash
git add -A && git commit -m "feat(scoring): complete scoring pipeline implementation"
```
