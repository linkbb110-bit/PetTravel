package com.nfu.pettravel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nfu.pettravel.enums.UserRole;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 平台用户表
 * @TableName sys_user
 */
@TableName(value ="sys_user", autoResultMap = true)
@Data
public class SysUser {
    /**
     * 主键ID (雪花算法)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名/账号
     */
    private String username;

    /**
     * 密码 (BCrypt加密)
     */
    private String password;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 角色: 0-普通铲屎官, 1-商家/管理员
     */
    private UserRole roleType;

    /**
     * 状态: 0-禁用, 1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除: 0-未删除, 1-已删除
     */
    private Integer isDeleted;
}
