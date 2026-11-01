package com.nfu.pettravel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("camp_route")
public class CampRoute {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // 严格对应你 DDL 里的字段
    private String title;           // 路线标题
    private BigDecimal price;       // 价格
    private Integer totalStock;     // 总名额
    private Integer availableStock; // 当前可用库存 (核心防超卖字段)
    private Integer version;        // 乐观锁版本号
    private Integer status;         // 状态
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Integer isDeleted;      // 逻辑删除
}
