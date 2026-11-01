package com.nfu.pettravel.mapper;

import com.nfu.pettravel.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
* @author 11707
* @description 针对表【sys_user(平台用户表)】的数据库操作Mapper
* @createDate 2026-05-07 08:13:11
* @Entity com.nfu.pettravel.entity.SysUser
*/
public interface SysUserMapper extends BaseMapper<SysUser> {
    int upgradeLegacyPassword(@Param("userId") long userId,
                              @Param("expectedUpdateTime") LocalDateTime expectedUpdateTime,
                              @Param("newPassword") String newPassword,
                              @Param("updateTime") LocalDateTime updateTime);

}




