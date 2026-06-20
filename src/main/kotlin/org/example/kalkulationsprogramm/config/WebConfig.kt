package org.example.kalkulationsprogramm.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/zeiterfassung").setViewName("forward:/zeiterfassung/index.html")
        registry.addViewController("/zeiterfassung/").setViewName("forward:/zeiterfassung/index.html")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/zeiterfassung/**")
            .addResourceLocations("classpath:/static/zeiterfassung/")

        registry.addResourceHandler("/**")
            .addResourceLocations(
                "classpath:/static/",
                "classpath:/public/",
                "classpath:/resources/",
                "classpath:/META-INF/resources/"
            )
    }
}
