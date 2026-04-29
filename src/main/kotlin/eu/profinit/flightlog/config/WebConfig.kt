package eu.profinit.flightlog.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.concurrent.TimeUnit

@Configuration
class WebConfig(
    @Value("\${flightlog.cors.allowed-origins:}") private val allowedOriginsProp: String
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        val origins = allowedOriginsProp
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val mapping = registry.addMapping("/**")
            .allowedMethods("*")
            .allowedHeaders("*")

        if (origins.isEmpty()) {
            mapping.allowedOriginPatterns("*")
        } else {
            mapping.allowedOrigins(*origins.toTypedArray())
        }
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Service worker MUST be served from root scope without caching.
        registry.addResourceHandler("/service-worker.js")
            .addResourceLocations("classpath:/static/service-worker.js")
            .setCacheControl(CacheControl.noStore())

        registry.addResourceHandler("/manifest.webmanifest")
            .addResourceLocations("classpath:/static/manifest.webmanifest")
            .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))

        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
    }
}
