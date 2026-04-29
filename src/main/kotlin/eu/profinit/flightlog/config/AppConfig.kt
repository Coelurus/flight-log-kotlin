package eu.profinit.flightlog.config

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@Configuration
@EnableScheduling
@EnableAsync
@EnableRetry
@EnableCaching
@EnableMethodSecurity
class AppConfig
