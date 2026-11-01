package com.nfu.pettravel.websocket;

import com.nfu.pettravel.enums.UserRole;
import com.nfu.pettravel.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdminWebSocketHandshakeInterceptorTest {

    @Test
    void shouldAllowOnlyAdminJwtFromAuthorizationHeader() {
        JwtUtils jwtUtils = new JwtUtils("0123456789abcdef0123456789abcdef", Duration.ofHours(1));
        AdminWebSocketHandshakeInterceptor interceptor = new AdminWebSocketHandshakeInterceptor(jwtUtils);

        assertThat(handshake(interceptor, jwtUtils.generateToken(1L, "admin", UserRole.ADMIN))).isTrue();
        assertThat(handshake(interceptor, jwtUtils.generateToken(2L, "user", UserRole.USER))).isFalse();
        assertThat(handshake(interceptor, null)).isFalse();
        assertThat(browserHandshake(
                interceptor, jwtUtils.generateToken(3L, "browser-admin", UserRole.ADMIN))).isTrue();
    }

    private boolean handshake(AdminWebSocketHandshakeInterceptor interceptor, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        Map<String, Object> attributes = new HashMap<>();
        return interceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                new org.springframework.http.server.ServletServerHttpResponse(new MockHttpServletResponse()),
                null,
                attributes);
    }

    private boolean browserHandshake(AdminWebSocketHandshakeInterceptor interceptor, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Sec-WebSocket-Protocol", "bearer, " + token);
        return interceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                new org.springframework.http.server.ServletServerHttpResponse(new MockHttpServletResponse()),
                null,
                new HashMap<>());
    }
}
