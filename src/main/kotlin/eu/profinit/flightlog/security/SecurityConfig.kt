package eu.profinit.flightlog.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.session.HttpSessionEventPublisher
import org.springframework.web.cors.CorsConfiguration

@Configuration
class SecurityConfig(
    private val mapper: ObjectMapper,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun httpSessionEventPublisher() = HttpSessionEventPublisher()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource {
                CorsConfiguration().applyPermitDefaultValues().also {
                    it.allowCredentials = true
                    it.addAllowedOriginPattern("*")
                    it.addAllowedMethod("*")
                    it.addAllowedHeader("*")
                }
            } }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .maximumSessions(5)
            }
            .authorizeHttpRequests { reg ->
                reg
                    // Public assets / PWA
                    .requestMatchers(
                        "/", "/index.html", "/manifest.webmanifest", "/service-worker.js",
                        "/pwa-register.js", "/icons/**", "/static/**", "/favicon.ico",
                        "/login.html", "/writer/**", "/admin/**",
                    ).permitAll()
                    // Public auth + actuator health
                    .requestMatchers("/api/auth/**", "/actuator/health", "/actuator/info").permitAll()
                    // Role-based API
                    .requestMatchers("/api/admin/**", "/actuator/**").hasRole("ADMIN")
                    .requestMatchers("/api/writer/**").hasAnyRole("WRITER", "ADMIN")
                    // Legacy endpoints (for the existing Aurelia frontend) - require auth
                    .requestMatchers("/flight/**", "/airplane/**", "/user/**").authenticated()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, resp, _ -> writeJson(resp, HttpStatus.UNAUTHORIZED, "unauthenticated") }
                    .accessDeniedHandler { _, resp, _ -> writeJson(resp, HttpStatus.FORBIDDEN, "forbidden") }
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() } // we expose /api/auth/logout instead
        return http.build()
    }

    private fun writeJson(resp: HttpServletResponse, status: HttpStatus, code: String) {
        resp.status = status.value()
        resp.contentType = MediaType.APPLICATION_JSON_VALUE
        resp.writer.write(mapper.writeValueAsString(mapOf("error" to code)))
    }
}
