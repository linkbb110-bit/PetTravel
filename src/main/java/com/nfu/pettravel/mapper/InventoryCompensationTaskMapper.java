package com.nfu.pettravel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nfu.pettravel.entity.InventoryCompensationTask;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryCompensationTaskMapper extends BaseMapper<InventoryCompensationTask> {
    int markSucceeded(@Param("orderId") long orderId, @Param("taskType") String taskType);

    List<InventoryCompensationTask> findDueTasks(
            @Param("now") LocalDateTime now, @Param("limit") int limit);

    int claim(@Param("id") long id);

    int complete(@Param("id") long id);

    int retry(@Param("id") long id,
              @Param("nextRetryTime") LocalDateTime nextRetryTime,
              @Param("lastError") String lastError,
              @Param("maxRetries") int maxRetries);

    int requeueStale(@Param("staleBefore") LocalDateTime staleBefore);

    long countBlockingByRoute(@Param("routeId") long routeId);

    int resetRouteReconciliation(@Param("routeId") long routeId,
                                 @Param("nextRetryTime") LocalDateTime nextRetryTime);

    List<Long> findNeedsReconcileRouteIds(@Param("limit") int limit);

    int markNeedsReconcileSucceeded(@Param("routeId") long routeId);
}
