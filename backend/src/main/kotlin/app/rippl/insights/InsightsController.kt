package app.rippl.insights

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/insights")
class InsightsController(private val insightsService: InsightsService) {

    @GetMapping("/mirror")
    fun mirror(@AuthenticationPrincipal userId: UUID): List<MirrorMoment> =
        insightsService.mirrorMoments(userId)
}
