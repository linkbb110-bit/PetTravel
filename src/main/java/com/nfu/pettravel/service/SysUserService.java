package com.nfu.pettravel.service;

import com.nfu.pettravel.dto.UserLoginDTO;
import com.nfu.pettravel.dto.UserRegisterDTO;
import com.nfu.pettravel.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 11707
* @description 针对表【sys_user(平台用户表)】的数据库操作Service
* @createDate 2026-05-07 08:13:11
*/
public interface SysUserService extends IService<SysUser> {

    String login(UserLoginDTO loginDTO);

    long register(UserRegisterDTO registerDTO);
}
