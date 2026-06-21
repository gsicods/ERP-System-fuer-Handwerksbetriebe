package org.example.kalkulationsprogramm.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class ZeiterfassungController {
    @GetMapping("/zeiterfassung")
    fun root(): String = "redirect:/zeiterfassung/"

    @GetMapping(
        value = [
            "/zeiterfassung/",
            "/zeiterfassung/{path:[^\\.]*}",
            "/zeiterfassung/{p1:[^\\.]*}/{p2:[^\\.]*}",
            "/zeiterfassung/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}",
            "/zeiterfassung/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}/{p4:[^\\.]*}",
            "/zeiterfassung/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}/{p4:[^\\.]*}/{p5:[^\\.]*}"
        ]
    )
    fun forwardAppRoutes(): String = FORWARD_TO_INDEX

    companion object {
        private const val FORWARD_TO_INDEX = "forward:/zeiterfassung/index.html"
    }
}
