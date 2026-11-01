package com.nfu.pettravel.websocket;

import com.nfu.pettravel.enums.UserRole;
import com.nfu.pettravel.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class AdminWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;

    public AdminWebSocketHandshakeInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String authorization = request.getHeaders().getFirst("Authorization");
        String token = extractToken(request, authorization);
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            Claims claims = jwtUtils.parseToken(token);
            Number role = claims.get("role", Number.class);
            Number userId = claims.get("userId", Number.class);
            if (role == null || userId == null || UserRole.fromCode(role.intValue()) != UserRole.ADMIN) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
            attributes.put("userId", userId.longValue());
            return true;
        } catch (RuntimeException invalidToken) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No ThreadLocal state is created during WebSocket handshakes.
    }

    private String extractToken(ServerHttpRequest request, String authorization) {
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }
        // Browser WebSocket APIs cannot set Authorization. They can send a subprotocol list:
        // new WebSocket(url, ["bearer", jwt]). The server negotiates only the "bearer" protocol.
        String protocols = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (!StringUtils.hasText(protocols)) {
            return null;
        }
        String[] values = protocols.split(",");
        if (values.length == 2 && "bearer".equalsIgnoreCase(values[0].trim())) {
            return values[1].trim();
        }
        return null;
    }
}
