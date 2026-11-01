package com.nfu.pettravel.service;

import com.nfu.pettravel.dto.UserLoginDTO;
import com.nfu.pettravel.entity.SysUser;
import com.nfu.pettravel.enums.UserRole;
import com.nfu.pettravel.mapper.SysUserMapper;
import com.nfu.pettravel.service.impl.SysUserServiceImpl;
import com.nfu.pettravel.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserServiceImplTest {

    @Test
    void shouldAuthenticateBcryptPasswordAndIssueTokenWithoutLoggingCredentials() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        JwtUtils jwtUtils = new JwtUtils("0123456789abcdef0123456789abcdef", Duration.ofHours(1));
        SysUser user = new SysUser();
        user.setId(7L);
        user.setUsername("alice");
        user.setPassword(passwordEncoder.encode("correct horse battery staple"));
        user.setRoleType(UserRole.ADMIN);
        user.setStatus(1);
        when(mapper.selectOne(any())).thenReturn(user);
        when(mapper.selectById(7L)).thenReturn(user);
        SysUserServiceImpl service = new SysUserServiceImpl(mapper, passwordEncoder, jwtUtils, false);

        UserLoginDTO request = new UserLoginDTO();
        request.setUsername("alice");
        request.setPassword("correct horse battery staple");

        assertThat(service.login(request)).isNotBlank();
    }

    @Test
    void shouldUpgradeLegacyPasswordWithNarrowCasAndRefreshSecurityState() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        JwtUtils jwtUtils = new JwtUtils("0123456789abcdef0123456789abcdef", Duration.ofHours(1));
        SysUser legacy = new SysUser();
        legacy.setId(8L);
        legacy.setUsername("legacy");
        legacy.setPassword("old-password");
        legacy.setRoleType(UserRole.ADMIN);
        legacy.setStatus(1);
        legacy.setUpdateTime(LocalDateTime.now().minusDays(1));
        SysUser refreshed = new SysUser();
        refreshed.setId(8L);
        refreshed.setUsername("legacy");
        refreshed.setPassword(passwordEncoder.encode("old-password"));
        refreshed.setRoleType(UserRole.USER);
        refreshed.setStatus(1);
        when(mapper.selectOne(any())).thenReturn(legacy);
        when(mapper.upgradeLegacyPassword(
                any(Long.class), any(LocalDateTime.class), any(String.class), any(LocalDateTime.class)))
                .thenReturn(1);
        when(mapper.selectById(8L)).thenReturn(refreshed);
        SysUserServiceImpl service = new SysUserServiceImpl(mapper, passwordEncoder, jwtUtils, true);

        UserLoginDTO request = new UserLoginDTO();
        request.setUsername("legacy");
        request.setPassword("old-password");

        String token = service.login(request);

        assertThat(jwtUtils.parseToken(token).get("role", Number.class).intValue())
                .isEqualTo(UserRole.USER.getCode());
        verify(mapper).upgradeLegacyPassword(
                any(Long.class), any(LocalDateTime.class), any(String.class), any(LocalDateTime.class));
        verify(mapper, never()).updateById(any());
    }
}
