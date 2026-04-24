package com.hireikon.hireikon_backend.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    @Value("\${app.cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private val allowedOriginsRaw: String
) {
    private val allowedOrigins: List<String>
        get() = allowedOriginsRaw.split(",").map { it.trim() }

    @Bean
    fun securityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        return httpSecurity
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public auth endpoints (no token needed)
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout-all").authenticated()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").permitAll()

                    // /me requires a valid token
                    .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()

                    // Candidate only
                    .requestMatchers("/api/v1/candidate/**").hasRole("CANDIDATE")
                    .requestMatchers("/api/v1/applications/**").hasRole("CANDIDATE")
                    .requestMatchers("/api/v1/quiz/**").hasRole("CANDIDATE")

                    // Recruiter only
                    .requestMatchers(HttpMethod.POST, "/api/v1/jobs/**").hasRole("RECRUITER")
                    .requestMatchers(HttpMethod.PUT,  "/api/v1/jobs/**").hasRole("RECRUITER")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/jobs/**").hasRole("RECRUITER")
                    .requestMatchers("/api/v1/recruiter/**").hasRole("RECRUITER")

                    // Admin only
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                    // Everything else requires authentication
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            this.allowedOrigins = allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager
}