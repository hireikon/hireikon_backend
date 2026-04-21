package com.hireikon.hireikon_backend.security

import com.hireikon.hireikon_backend.database.model.enums.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Base64
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val encodedJwtSecret: String,
    @Value("\${app.jwt.access-token-expiration-ms}") val accessTokenExpirationMs: Long,
    @Value("\${app.jwt.refresh-token-expiration-ms}") val refreshTokenExpirationMs: Long
) {

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(encodedJwtSecret))
    }

    // Token Generation

    fun generateAccessToken(userId: String, email: String, role: UserRole): String =
        buildToken(userId, email, role, accessTokenExpirationMs)

    fun generateRefreshToken(userId: String, email: String, role: UserRole): String =
        buildToken(userId, email, role, refreshTokenExpirationMs, isRefresh = true)

    private fun buildToken(
        userId: String,
        email: String,
        role: UserRole,
        expiry: Long,
        isRefresh: Boolean = false
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry)

        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("role", role.name)
            .claim("type", if (isRefresh) "refresh" else "access")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(signingKey, Jwts.SIG.HS512)
            .compact()
    }

    // Token Validation

    fun validateToken(token: String): Boolean {
        return try {
            parseAllClaims(token)
            true
        } catch (ex: JwtException) {
            false
        } catch (ex: IllegalArgumentException) {
            false
        }
    }

    fun isAccessToken(token: String): Boolean =
        getClaim(token, "type") == "access"

    // Claims Extraction

    fun getUserId(token: String): String {
        val claims = parseAllClaims(token) ?: throw ResponseStatusException(
            HttpStatusCode.valueOf(401), "Invalid token."
        )
        return claims.subject
    }

    fun getEmail(token: String): String =
        getClaim(token, "email")

    fun getRole(token: String): UserRole =
        UserRole.valueOf(getClaim(token, "role"))

    fun getExpiration(token: String): Date {
        val claims = parseAllClaims(token) ?: throw ResponseStatusException(
            HttpStatusCode.valueOf(401), "Invalid token."
        )
        return claims.expiration
    }

    private fun getClaim(token: String, key: String): String =
        parseAllClaims(token)?.get(key) as String

    private fun parseAllClaims(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) { null }
    }
}