package com.nfu.pettravel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nfu.pettravel.enums.InventoryTaskStatus;
import com.nfu.pettravel.enums.InventoryTaskType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "inventory_compensation_task", autoResultMap = true)
public class InventoryCompensationTask {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private Long routeId;
    private InventoryTaskType taskType;
    private InventoryTaskStatus taskStatus;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
