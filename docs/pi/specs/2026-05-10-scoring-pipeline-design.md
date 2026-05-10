# Scoring Pipeline Design

**Date:** 2026-05-10
**Epic:** scoring-pipeline
**Stories:** task-mix-inference, time-saved-scoring, confidence-model

## Context

Raw activity sessions from collectors carry facts (duration, interactions, source). The scoring pipeline turns these into meaning: task type, time saved, confidence. Currently `ScoringService` writes hardcoded placeholders — this replaces them with real logic.

## Architecture

```
ScoringService (orchestrator)
  ├── TaskMixInferenceService  — priority chain → TaskMix + method
  ├── TimeSavedCalculator      — multipliers → time_saved_ms
  ├── ConfidenceCalculator     — signal quality → 0-1 score → low/medium/high
  └── ScoringConfig            — @ConfigurationProperties for all tunable values
```

Each stage is a standalone Spring service with pure logic (no DB access). `ScoringService` loads data, calls stages, writes results.

## Data Flow

```
scoreInitial(sessionId) / rescoreFromFeedback(sessionId)
  │
  ├─ load session (active_ms, duration_ms, collector_type, surface, collector_metrics)
  ├─ load feedback (task_type if exists)
  ├─ load user profile (task_mix, adjustment_factor, onboarded)
  │
  ├─ TaskMixInference.infer() → InferenceResult(taskMix, method)
  ├─ TimeSavedCalculator.calculate() → TimeSavedResult(savedMs, multiplier)
  ├─ ConfidenceCalculator.calculate() → ConfidenceResult(score, level)
  │
  └─ ScoringRepository.upsertScoring(...)
```

## DTOs

```kotlin
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

data class InferenceResult(val taskMix: TaskMix, val method: ScoringMethod)
data class TimeSavedResult(val savedMs: Long, val effectiveMultiplier: Double)
data class ConfidenceResult(val score: Double, val level: String)

enum class ScoringMethod {
    FEEDBACK_ADJUSTED,
    COLLECTOR_SIGNAL_ADJUSTED,
    PROFILE_DEFAULT,
    GLOBAL_FALLBACK
}
```

## Configuration

All tunable values in `application.yml` via `@ConfigurationProperties(prefix = "rippl.scoring")`:

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

## Stage 1: Task-Mix Inference

Priority chain (first match wins):

1. **Feedback exists** → one-hot TaskMix for that task type, method=`feedback_adjusted`
2. **CLI collector** (`collector_type=cli_wrapper`) → blend(profile, {coding:1.0}, 0.5), method=`collector_signal_adjusted`
3. **Profile exists + onboarded** → profile.taskMix, method=`profile_default`
4. **Fallback** → `TaskMix.GLOBAL_DEFAULT`, method=`global_fallback`

Blending: `blended[type] = profile[type] * 0.5 + hint[type] * 0.5`

TaskMix values always sum to 1.0.

## Stage 2: Time-Saved Scoring

```
effective_multiplier = Σ(taskMix[type] × config.multipliers[type])
base_saved_ms = activeMs × (1 - 1/effective_multiplier)
adjusted_saved_ms = base_saved_ms × personalAdjustmentFactor
```

Edge cases:
- `activeMs` < `min-scorable-ms` (10s) → 0 saved
- `activeMs` missing/0 → use `durationMs × 0.7` as fallback, then apply same threshold
- 0-duration session → 0 saved

## Stage 3: Confidence Model

Five factors, each 0-1, combined via weighted sum:

| Factor | Weight | Calculation |
|--------|--------|-------------|
| duration_quality | 0.25 | `min(durationMs / 1_800_000.0, 1.0)` — caps at 30 min |
| interaction_quality | 0.20 | `min(interactionCount / 20.0, 1.0)` — caps at 20 |
| task_certainty | 0.30 | feedback=1.0, collector_hint=0.7, profile=0.4, fallback=0.1 |
| collector_signal | 0.15 | browser+interactions≥1 = 0.8, browser = 0.5, cli = 0.3 |
| profile_quality | 0.10 | onboarded=1.0, default=0.3 |

Level mapping: 0.00–0.39 → low, 0.40–0.69 → medium, 0.70–1.00 → high

## Files

**New:**
- `ingestion/ScoringConfig.kt` — `@ConfigurationProperties`
- `ingestion/ScoringDtos.kt` — DTOs and enum
- `ingestion/TaskMixInferenceService.kt`
- `ingestion/TimeSavedCalculator.kt`
- `ingestion/ConfidenceCalculator.kt`

**Modified:**
- `ingestion/ScoringService.kt` — replace placeholders with orchestration
- `ingestion/ScoringRepository.kt` — add `loadScoringInput()` query
- `application.yml` — add `rippl.scoring.*` properties

**Tests (new):**
- `ingestion/TaskMixInferenceServiceTest.kt`
- `ingestion/TimeSavedCalculatorTest.kt`
- `ingestion/ConfidenceCalculatorTest.kt`

**Tests (modified):**
- `ingestion/ScoredSessionStorageTest.kt` — assert real values instead of placeholders

## Decisions

- **Separate classes per stage** over monolithic ScoringService — each stage unit-testable with pure functions, matches story boundaries.
- **`@ConfigurationProperties`** over hardcoded constants — multipliers changeable without deploy, per story requirement.
- **Reuse existing `TaskMix`** from profile package — same data shape, avoids duplication.
- **`ScoringMethod` enum** matches DB enum `scoring_method` — feedback_adjusted, collector_signal_adjusted, profile_default, global_fallback.
- **CLI detection via `collector_type=cli_wrapper`** — forward-compatible with future CLI collector. Surface field not used for this check.
- **Interaction count = sum of `interaction_count` + `copy_events` + `paste_events`** from `collector_metrics` JSONB, default 0 for missing keys. Richer signal for confidence.
- **Unknown feedback task types fall through** to next priority in inference chain. Only known types (writing/coding/research/planning/communication/other) produce one-hot.
- **Profile loaded via JPA `UserProfileRepository`** — reuse existing repo, simpler than adding JDBC query.
- **Collector signal quality uses interaction proxy** — browser + interactions≥1 = 0.8, browser + 0 = 0.5, cli = 0.3.
- **`@TransactionalEventListener` for feedback rescoring** — ensures feedback is committed before async rescoring reads it. Replaces direct `@Async` call from `saveFeedback()`.
