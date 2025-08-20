package com.chatgpt.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final String apiKeyHeader;
    private final String apiKeyValue;

    public ApiKeyAuthFilter(String apiKeyHeader, String apiKeyValue) {
        this.apiKeyHeader = apiKeyHeader;
        this.apiKeyValue = apiKeyValue;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Solo proteger el endpoint /api/chat/send
        if ("/api/chat/send".equals(request.getRequestURI())) {
            String key = request.getHeader(apiKeyHeader);
            if (key == null || !key.equals(apiKeyValue)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized: Invalid API Key");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
