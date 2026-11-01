package com.nfu.pettravel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nfu.pettravel.enums.OrderStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 携宠游订单记录表
 * @TableName appointment_order
 */
@TableName(value ="appointment_order", autoResultMap = true)
@Data
public class AppointmentOrder {
    /**
     * 订单ID (建议用雪花算法生成单号)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 下单人ID
     */
    private Long userId;

    /**
     * 出游宠物ID
     */
    private Long petId;

    /**
     * 预约的营地路线ID
     */
    private Long routeId;

    /**
     * 订单状态: 0-待支付, 1-已支付(预订成功), 2-已取消(超时或主动), 3-已核销/完成
     */
    private OrderStatus orderStatus;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 创建时间(下单时间)
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
