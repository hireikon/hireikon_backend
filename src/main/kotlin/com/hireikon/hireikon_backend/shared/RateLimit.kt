package com.hireikon.hireikon_backend.shared

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// Annotation — put on a controller method to pick its bucket.
// Methods without it fall under the GENERAL bucket automatically.
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimit(val bucket: RateLimitBucket)

// Interceptor — runs after Spring Security, so the authenticated user is known
@Component
class RateLimitInterceptor(
    private val rateLimitService: RateLimitService,
    @Value("\${app.rate-limit.enabled}") private val rateLimitEnabled: Boolean
): HandlerInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (!rateLimitEnabled || handler !is HandlerMethod) return true

        val bucket = handler.getMethodAnnotation(RateLimit::class.java)?.bucket
            ?: RateLimitBucket.GENERAL
        val key = "${bucket.name}:${resolveIdentity(request)}"

        val result = rateLimitService.check(key, bucket)
        log.debug("rate-limit key={} allowed={} remaining={}", key, result.allowed, result.remaining)

        response.setHeader("X-RateLimit-Limit", bucket.maxRequests.toString())
        response.setHeader("X-RateLimit-Remaining", result.remaining.toString())
        response.setHeader("X-RateLimit-Reset", result.resetAfterSeconds.toString())

        if (!result.allowed) {
            // GlobalExceptionHandler turns this into a 429 with a Retry-After header
            throw RateLimitExceededException(result.message, result.resetAfterSeconds)
        }
        return true
    }

    // Logged-in users are limited per account (can't be dodged by changing network).
    // Anonymous callers are limited per IP.
    private fun resolveIdentity(request: HttpServletRequest): String {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth != null && auth.isAuthenticated && auth !is AnonymousAuthenticationToken) {
            return "user:${auth.principal}"
        }
        // remoteAddr is already resolved from trusted proxies when
        // server.forward-headers-strategy=native — never read X-Forwarded-For by hand,
        // clients can send any value they like in that header.
        return "ip:${request.remoteAddr}"
    }
}

@Configuration
class RateLimitWebConfig(
    private val interceptor: RateLimitInterceptor
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/**")
    }
}