package com.nfu.pettravel.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfu.pettravel.common.ApiResponse;
import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.enums.UserRole;
import com.nfu.pettravel.utils.JwtUtils;
import com.nfu.pettravel.utils.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenInterceptor implements HandlerInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    public JwtTokenInterceptor(JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response);
            return false;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            writeUnauthorized(response);
            return false;
        }
        try {
            Claims claims = jwtUtils.parseToken(token);
            Number userId = claims.get("userId", Number.class);
            Number role = claims.get("role", Number.class);
            if (userId == null || role == null) {
                writeUnauthorized(response);
                return false;
            }
            UserContext.setUser(userId.longValue(), UserRole.fromCode(role.intValue()));
            return true;
        } catch (RuntimeException invalidToken) {
            writeUnauthorized(response);
            return false;
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        UserContext.removeUserId();
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(ErrorCode.UNAUTHORIZED));
    }
}
