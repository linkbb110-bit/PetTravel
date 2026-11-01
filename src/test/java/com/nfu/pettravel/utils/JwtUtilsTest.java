package com.nfu.pettravel.utils;

import com.nfu.pettravel.enums.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    @Test
    void shouldIssueAndVerifyTokenWithRoleClaim() {
        JwtUtils jwtUtils = new JwtUtils("0123456789abcdef0123456789abcdef", Duration.ofHours(1));

        String token = jwtUtils.generateToken(7L, "alice", UserRole.ADMIN);
        Claims claims = jwtUtils.parseToken(token);

        assertThat(claims.get("userId", Number.class).longValue()).isEqualTo(7L);
        assertThat(claims.get("role", Number.class).intValue()).isEqualTo(UserRole.ADMIN.getCode());
        assertThat(claims.getSubject()).isEqualTo("alice");
    }

    @Test
    void shouldRejectSecretShorterThanHs256Minimum() {
        assertThatThrownBy(() -> new JwtUtils("short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
