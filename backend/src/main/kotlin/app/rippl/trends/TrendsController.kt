package app.rippl.trends

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/trends")
class TrendsController(private val trendsService: TrendsService) {

    @GetMapping("/weekly")
    fun weekly(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?
    ): List<WeeklyTrend> {
        val end = to ?: LocalDate.now()
        val start = from ?: end.minusWeeks(12)
        return trendsService.weekly(userId, start, end)
    }

    @GetMapping("/monthly")
    fun monthly(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?
    ): List<MonthlyTrend> {
        val end = to ?: LocalDate.now()
        val start = from ?: end.minusMonths(12)
        return trendsService.monthly(userId, start, end)
    }

    @GetMapping("/time-saved")
    fun timeSaved(@AuthenticationPrincipal userId: UUID): TimeSaved =
        trendsService.timeSaved(userId)
}
