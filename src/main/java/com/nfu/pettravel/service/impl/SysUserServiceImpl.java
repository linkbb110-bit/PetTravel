package com.nfu.pettravel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.dto.UserLoginDTO;
import com.nfu.pettravel.dto.UserRegisterDTO;
import com.nfu.pettravel.entity.SysUser;
import com.nfu.pettravel.enums.UserRole;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.mapper.SysUserMapper;
import com.nfu.pettravel.service.SysUserService;
import com.nfu.pettravel.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final boolean legacyPlaintextUpgradeEnabled;

    public SysUserServiceImpl(
            SysUserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            @Value("${pettravel.security.legacy-plaintext-upgrade-enabled:false}")
            boolean legacyPlaintextUpgradeEnabled) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.legacyPlaintextUpgradeEnabled = legacyPlaintextUpgradeEnabled;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String login(UserLoginDTO loginDTO) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDTO.getUsername())
                .eq(SysUser::getIsDeleted, 0));
        if (user == null || !passwordMatches(loginDTO.getPassword(), user)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        SysUser current = userMapper.selectById(user.getId());
        if (current == null || Integer.valueOf(1).equals(current.getIsDeleted())
                || !isBcryptHash(current.getPassword())
                || !passwordEncoder.matches(loginDTO.getPassword(), current.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!Integer.valueOf(1).equals(current.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        UserRole role = current.getRoleType() == null ? UserRole.USER : current.getRoleType();
        return jwtUtils.generateToken(current.getId(), current.getUsername(), role);
    }

    @Override
    public long register(UserRegisterDTO registerDTO) {
        SysUser user = new SysUser();
        user.setUsername(registerDTO.username());
        user.setPassword(passwordEncoder.encode(registerDTO.password()));
        user.setPhone(registerDTO.phone());
        user.setRoleType(UserRole.USER);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setIsDeleted(0);
        try {
            if (userMapper.insert(user) != 1 || user.getId() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        } catch (DuplicateKeyException duplicate) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        return user.getId();
    }

    private boolean passwordMatches(String rawPassword, SysUser user) {
        String stored = user.getPassword();
        if (isBcryptHash(stored)) {
            return passwordEncoder.matches(rawPassword, stored);
        }
        if (!legacyPlaintextUpgradeEnabled || stored == null || !constantTimeEquals(rawPassword, stored)) {
            return false;
        }
        if (user.getUpdateTime() == null) {
            return false;
        }
        userMapper.upgradeLegacyPassword(
                user.getId(), user.getUpdateTime(), passwordEncoder.encode(rawPassword), LocalDateTime.now());
        return true;
    }

    private boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$")
                || value.startsWith("$2y$"));
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }
}
