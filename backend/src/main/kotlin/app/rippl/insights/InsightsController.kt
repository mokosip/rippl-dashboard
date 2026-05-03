package app.rippl.insights

import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/insights")
class InsightsController(private val insightsService: InsightsService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/mirror")
    fun mirror(@AuthenticationPrincipal userId: UUID): List<MirrorMoment> {
        log.debug("Mirror insights requested for userId: {}", userId)
        return insightsService.mirrorMoments(userId)
    }
}
