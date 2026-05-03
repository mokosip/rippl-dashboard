package app.rippl

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@Controller
class SpaForwardController {

    @RequestMapping(
        value = ["/", "/login", "/trends", "/mirror", "/settings"],
        method = [RequestMethod.GET]
    )
    fun forward() = "forward:/index.html"
}
