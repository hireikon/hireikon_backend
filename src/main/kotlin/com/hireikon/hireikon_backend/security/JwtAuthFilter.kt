package com.hireikon.hireikon_backend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService
): OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)

        if (token != null && jwtService.validateToken(token) && jwtService.isAccessToken(token)) {
            val userId = jwtService.getUserId(token)
            val role = jwtService.getRole(token)
            val authority = SimpleGrantedAuthority("ROLE_${role.name}")

            val auth = UsernamePasswordAuthenticationToken(userId, null, listOf(authority))
            auth.details = WebAuthenticationDetailsSource().buildDetails(request)
            // retrieve auth object across the codebase
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.substring(7).takeIf { it.isNotBlank() }
    }
}