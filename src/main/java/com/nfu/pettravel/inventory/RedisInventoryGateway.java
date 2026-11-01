package com.nfu.pettravel.inventory;

import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RedisInventoryGateway implements InventoryGateway {
    private static final int DEFAULT_BATCH_SIZE = 200;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> reserveScript = script("lua/inventory_reserve.lua");
    private final DefaultRedisScript<Long> confirmScript = script("lua/inventory_confirm.lua");
    private final DefaultRedisScript<Long> compensateScript = script("lua/inventory_compensate.lua");
    private final DefaultRedisScript<Long> releaseScript = script("lua/inventory_release.lua");
    private final DefaultRedisScript<Long> reconcileScript = script("lua/inventory_reconcile.lua");
    private final DefaultRedisScript<Long> unlockScript = script("lua/unlock.lua");
    private final long reservationRetentionSeconds;
    private final long reservationTimeoutMillis;

    public RedisInventoryGateway(
            StringRedisTemplate redisTemplate,
            @Value("${pettravel.inventory.reservation-retention:7d}") Duration reservationRetention,
            @Value("${pettravel.inventory.reservation-timeout:2m}") Duration reservationTimeout) {
        this.redisTemplate = redisTemplate;
        this.reservationRetentionSeconds = reservationRetention.toSeconds();
        this.reservationTimeoutMillis = reservationTimeout.toMillis();
    }

    @Override
    public InventoryReservationResult reserve(long routeId, long orderId, int quantity) {
        long now = System.currentTimeMillis();
        Long result = redisTemplate.execute(
                reserveScript,
                List.of(stockKey(routeId), pendingCountKey(routeId), pendingSetKey(routeId),
                        reservationKey(routeId, orderId), reconcileLockKey(routeId)),
                Long.toString(orderId), Integer.toString(quantity),
                Long.toString(now + reservationTimeoutMillis));
        return InventoryReservationResult.fromLuaResult(requireLuaResult(result));
    }

    @Override
    public boolean confirm(long routeId, long orderId) {
        Long result = redisTemplate.execute(
                confirmScript,
                List.of(pendingCountKey(routeId), pendingSetKey(routeId), reservationKey(routeId, orderId)),
                Long.toString(orderId), Long.toString(reservationRetentionSeconds));
        long value = requireLuaResult(result);
        if (value < 0) {
            throw new BusinessException(ErrorCode.INVENTORY_OPERATION_FAILED);
        }
        return value == 1;
    }

    @Override
    public boolean compensateReservation(long routeId, long orderId) {
        Long result = redisTemplate.execute(
                compensateScript,
                List.of(stockKey(routeId), pendingCountKey(routeId), pendingSetKey(routeId),
                        reservationKey(routeId, orderId)),
                Long.toString(orderId), Long.toString(reservationRetentionSeconds));
        long value = requireLuaResult(result);
        if (value < 0) {
            throw new BusinessException(ErrorCode.INVENTORY_OPERATION_FAILED);
        }
        return value == 1;
    }

    @Override
    public boolean releaseOrder(long routeId, long orderId, int quantity) {
        Long result = redisTemplate.execute(
                releaseScript,
                List.of(stockKey(routeId), releasedOrderKey(routeId, orderId), reconcileLockKey(routeId)),
                Integer.toString(quantity));
        long value = requireLuaResult(result);
        if (value == -2) {
            throw new BusinessException(ErrorCode.INVENTORY_BUSY);
        }
        if (value < 0) {
            throw new BusinessException(ErrorCode.INVENTORY_NOT_INITIALIZED);
        }
        return value == 1;
    }

    @Override
    public String tryAcquireReconciliationLock(long routeId) {
        String lockKey = reconcileLockKey(routeId);
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, token, Duration.ofSeconds(15));
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    @Override
    public boolean synchronizeStockUnderLock(long routeId, String lockToken, int databaseStock) {
        Long result = redisTemplate.execute(reconcileScript,
                List.of(stockKey(routeId), pendingCountKey(routeId), reconcileLockKey(routeId)),
                lockToken, Integer.toString(databaseStock));
        return requireLuaResult(result) >= 0;
    }

    @Override
    public void releaseReconciliationLock(long routeId, String lockToken) {
        redisTemplate.execute(unlockScript, List.of(reconcileLockKey(routeId)), lockToken);
    }

    @Override
    public Long getStock(long routeId) {
        String value = redisTemplate.opsForValue().get(stockKey(routeId));
        return value == null ? null : Long.valueOf(value);
    }

    @Override
    public Set<Long> findExpiredReservations(long routeId, long deadlineEpochMillis, int limit) {
        int batchSize = limit <= 0 ? DEFAULT_BATCH_SIZE : limit;
        Set<String> ids = redisTemplate.opsForZSet()
                .rangeByScore(pendingSetKey(routeId), 0, deadlineEpochMillis, 0, batchSize);
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream().filter(Objects::nonNull).map(Long::valueOf).collect(Collectors.toSet());
    }

    private long requireLuaResult(Long result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.INVENTORY_OPERATION_FAILED);
        }
        return result;
    }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        script.setResultType(Long.class);
        return script;
    }

    private String stockKey(long routeId) {
        return prefix(routeId) + "stock";
    }

    private String pendingCountKey(long routeId) {
        return prefix(routeId) + "pending-count";
    }

    private String pendingSetKey(long routeId) {
        return prefix(routeId) + "pending-reservations";
    }

    private String reservationKey(long routeId, long orderId) {
        return prefix(routeId) + "reservation:" + orderId;
    }

    private String releasedOrderKey(long routeId, long orderId) {
        return prefix(routeId) + "released:" + orderId;
    }

    private String reconcileLockKey(long routeId) {
        return prefix(routeId) + "reconcile-lock";
    }

    private String prefix(long routeId) {
        return "pettravel:inventory:{" + routeId + "}:";
    }
}
