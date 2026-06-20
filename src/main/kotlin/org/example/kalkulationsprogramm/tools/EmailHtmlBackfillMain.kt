package org.example.kalkulationsprogramm.tools

import org.example.kalkulationsprogramm.KalkulationsprogrammApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder

object EmailHtmlBackfillMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val context = SpringApplicationBuilder(KalkulationsprogrammApplication::class.java)
            .web(WebApplicationType.NONE)
            .run(*args)
        try {
            context.getBean(EmailHtmlBackfillRunner::class.java).run()
        } finally {
            context.close()
        }
    }
}
