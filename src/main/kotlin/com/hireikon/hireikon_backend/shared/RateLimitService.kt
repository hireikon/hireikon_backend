package com.hireikon.hireikon_backend.shared

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID
import kotlin.math.ceil

enum class RateLimitBucket(
    val maxRequests: Int,
    val windowSeconds: Long,
    val message: String
) {
    // Auth — brute force / email bombing protection
    AUTH_LOGIN(5, 60, "Too many login attempts. Please wait a minute before trying again."),
    AUTH_REGISTER(5, 3600, "Too many register attempts. Please trying again in an hour."),
    AUTH_FORGOT_PASSWORD(3, 300, "Too many forgot password request. Please try again in 5 minutes."),
    AUTH_RESET_PASSWORD(3, 300, "Too many password reset request. Please try again in 5 minutes."),

    // Anything that calls Gemini — protects API cost
    AI_RESUME_PARSE(5, 3600, "Resume parsing limit reached. You can parse up to 5 resumes per hour."),
    AI_SKILL_GAP(10, 3600, "Skill gap analysis limit reached. You can run up to 10 analyses per hour."),
    AI_QUIZ_GENERATE(20, 3600, "Quiz generation limit reached. You can generate up to 20 quizzes per hour."),
    APPLICATION_SUBMIT(30, 3600, "Application limit reached. You can submit up to 30 applications per hour."),

    // Baseline for every other /api endpoint
    GENERAL(120, 60, "Too many requests. Please slow down.")
}

data class RateLimitResult(
    val allowed: Boolean,
    val remaining: Int,
    val resetAfterSeconds: Long,
    val message: String = ""
)

// Orchestrator: Redis first, in-memory fallback
@Service
class RateLimitService(
    private val redis: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val local = LocalRateLimiter()

    // After a Redis failure, skip Redis for a short cooldown so a Redis outage
    // doesn't add a connection timeout to every single request.
    @Volatile private var redisRetryAtMs = 0L

    fun check(key: String, bucket: RateLimitBucket): RateLimitResult {
        if (System.currentTimeMillis() >= redisRetryAtMs) {
            try {
                return checkRedis(key, bucket)
            } catch (ex: Exception) {
                redisRetryAtMs = System.currentTimeMillis() + REDIS_COOLDOWN_MS
                log.warn(
                    "Redis rate limiter unavailable — using per-instance in-memory fallback for {}s. Cause: {}",
                    REDIS_COOLDOWN_MS / 1000, ex.message
                )
            }
        }
        return local.check(key, bucket)
    }

    private fun checkRedis(key: String, bucket: RateLimitBucket): RateLimitResult {
        val reply = redis.execute(
            SLIDING_WINDOW_SCRIPT,
            listOf("$KEY_PREFIX$key"),
            (bucket.windowSeconds * 1000).toString(),
            bucket.maxRequests.toString(),
            UUID.randomUUID().toString()
        ) ?: throw IllegalStateException("Empty reply from Redis rate limit script")

        val allowed = (reply[0] as Number).toLong() == 1L
        val remaining = (reply[1] as Number).toInt()
        val retryAfterMs = (reply[2] as Number).toLong()

        return RateLimitResult(
            allowed = allowed,
            remaining = remaining,
            resetAfterSeconds = ceil(retryAfterMs / 1000.0).toLong().coerceAtLeast(1),
            message = if (allowed) "" else bucket.message
        )
    }

    private companion object {
        const val KEY_PREFIX = "hireikon:rl:"
        const val REDIS_COOLDOWN_MS = 10_000L

        // Sliding-window log on a sorted set. Runs atomically inside Redis, uses Redis TIME
        // (not app clocks) so multiple instances can't disagree, and PEXPIREs the key so
        // idle keys clean themselves up.
        val SLIDING_WINDOW_SCRIPT = DefaultRedisScript<List<*>>().apply {
            setScriptText(
                """
                local t = redis.call('TIME')
                local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
                local window = tonumber(ARGV[1])
                local limit = tonumber(ARGV[2])
 
                redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now - window)
                local count = redis.call('ZCARD', KEYS[1])
 
                if count >= limit then
                  local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
                  local retryAfter = window
                  if oldest[2] then
                    retryAfter = tonumber(oldest[2]) + window - now
                  end
                  return {0, 0, retryAfter}
                end
 
                redis.call('ZADD', KEYS[1], now, ARGV[3])
                redis.call('PEXPIRE', KEYS[1], window)
                return {1, limit - count - 1, window}
                """.trimIndent()
            )
            resultType = List::class.java
        }
    }
}

// Fallback: per-instance sliding window (Caffeine)
// Only used while Redis is down — limits are per instance, not global.
private class LocalRateLimiter {
    private val caches = RateLimitBucket.entries.associateWith { bucket ->
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(bucket.windowSeconds))
            .maximumSize(100_000)
            .build<String, MutableList<Long>>()
    }

    fun check(key: String, bucket: RateLimitBucket): RateLimitResult {
        val cache = caches.getValue(bucket)
        val now = System.currentTimeMillis()
        val windowMs = bucket.windowSeconds * 1000
        val stamps = cache.get(key) { mutableListOf() }

        synchronized(stamps) {
            stamps.removeIf { it < now - windowMs }

            if (stamps.size >= bucket.maxRequests) {
                val retryMs = (stamps.min() + windowMs) - now
                return RateLimitResult(
                    allowed = false,
                    remaining = 0,
                    resetAfterSeconds = ceil(retryMs / 1000.0).toLong().coerceAtLeast(1),
                    message = bucket.message
                )
            }

            stamps.add(now)
            cache.put(key, stamps)
            return RateLimitResult(
                allowed = true,
                remaining = bucket.maxRequests - stamps.size,
                resetAfterSeconds = bucket.windowSeconds
            )
        }
    }
}