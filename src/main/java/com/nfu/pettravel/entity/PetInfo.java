package com.nfu.pettravel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nfu.pettravel.enums.VaccineStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 宠物信息档案表
 * @TableName pet_info
 */
@TableName(value ="pet_info", autoResultMap = true)
@Data
public class PetInfo {
    /**
     * 宠物ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 归属主人的用户ID
     */
    private Long userId;

    /**
     * 宠物昵称
     */
    private String name;

    /**
     * 品种 (如: 金毛, 柯基)
     */
    private String breed;

    /**
     * 年龄 (月)
     */
    private Integer age;

    /**
     * 宠物全貌照片URL (后续对接OSS)
     */
    private String avatarUrl;

    /**
     * 疫苗状态: 0-未齐全, 1-已齐全 (影响预订资格)
     */
    private VaccineStatus vaccineStatus;

    /**
     * 性格标签 (JSON数组格式，如: ["亲人", "护食"])
     */
    private String characterTags;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    private Integer isDeleted;
}
