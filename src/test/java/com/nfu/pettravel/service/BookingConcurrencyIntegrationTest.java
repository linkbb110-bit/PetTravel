package com.nfu.pettravel.service;

import com.nfu.pettravel.PetTravelApplication;
import com.nfu.pettravel.cache.RouteCacheService;
import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.inventory.InventoryGateway;
import com.nfu.pettravel.inventory.InventoryReservationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = {PetTravelApplication.class, BookingConcurrencyIntegrationTest.TestBeans.class})
class BookingConcurrencyIntegrationTest {
    private static final long ROUTE_ID = 8L;
    private static final int INITIAL_STOCK = 100;
    private static final int REQUESTS = 1000;

    @Autowired
    private BookingOrchestrator bookingOrchestrator;
    @Autowired
    private OrderCancellationService cancellationService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private InMemoryInventoryGateway inventoryGateway;

    @BeforeEach
    void prepareData() {
        jdbcTemplate.update("DELETE FROM inventory_compensation_task");
        jdbcTemplate.update("DELETE FROM appointment_order");
        jdbcTemplate.update("DELETE FROM pet_info");
        jdbcTemplate.update("DELETE FROM camp_route");
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO camp_route
                    (id, title, price, total_stock, available_stock, version, status,
                     create_time, update_time, is_deleted)
                VALUES (?, ?, ?, ?, ?, 0, 1, ?, ?, 0)
                """, ROUTE_ID, "并发测试路线", BigDecimal.TEN, INITIAL_STOCK, INITIAL_STOCK,
                Timestamp.valueOf(now), Timestamp.valueOf(now));
        List<Object[]> pets = new ArrayList<>(REQUESTS);
        for (int index = 0; index < REQUESTS; index++) {
            long userId = 10_000L + index;
            long petId = 20_000L + index;
            pets.add(new Object[]{petId, userId, "pet-" + index, "breed", 12, 1,
                    Timestamp.valueOf(now), Timestamp.valueOf(now)});
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO pet_info
                    (id, user_id, name, breed, age, vaccine_status, create_time, update_time, is_deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
                """, pets);
        inventoryGateway.reset(INITIAL_STOCK);
    }

    @Test
    void shouldNeverOversellAndShouldConserveRedisAndMysqlStock() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(64);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>(REQUESTS);
        try {
            for (int index = 0; index < REQUESTS; index++) {
                long orderId = 100_000L + index;
                long userId = 10_000L + index;
                long petId = 20_000L + index;
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        bookingOrchestrator.bookWithOrderId(orderId, userId, ROUTE_ID, petId);
                        return true;
                    } catch (BusinessException expectedRejection) {
                        assertThat(expectedRejection.getErrorCode())
                                .isIn(ErrorCode.STOCK_INSUFFICIENT, ErrorCode.INVENTORY_BUSY);
                        return false;
                    }
                }));
            }
            start.countDown();
            int succeeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }

            Integer mysqlStock = jdbcTemplate.queryForObject(
                    "SELECT available_stock FROM camp_route WHERE id = ?", Integer.class, ROUTE_ID);
            Integer orderCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM appointment_order", Integer.class);

            assertThat(succeeded).isEqualTo(INITIAL_STOCK);
            assertThat(succeeded).isLessThanOrEqualTo(INITIAL_STOCK);
            assertThat(orderCount).isEqualTo(succeeded);
            assertThat(mysqlStock).isNotNegative().isEqualTo(INITIAL_STOCK - succeeded);
            assertThat(inventoryGateway.stock()).isNotNegative().isEqualTo(mysqlStock);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldCancelSameOrderExactlyOnceAcrossConcurrentWorkers() throws Exception {
        long orderId = 909L;
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("UPDATE camp_route SET total_stock = 1, available_stock = 0 WHERE id = ?", ROUTE_ID);
        jdbcTemplate.update("""
                INSERT INTO appointment_order
                    (id, user_id, pet_id, route_id, order_status, create_time, update_time, is_deleted)
                VALUES (?, ?, ?, ?, 0, ?, ?, 0)
                """, orderId, 10_000L, 20_000L, ROUTE_ID, Timestamp.valueOf(now), Timestamp.valueOf(now));
        inventoryGateway.reset(0);

        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int index = 0; index < 100; index++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return cancellationService.cancel(orderId, null);
                }));
            }
            start.countDown();
            int cancelled = 0;
            for (Future<Boolean> result : results) {
                if (result.get(20, TimeUnit.SECONDS)) {
                    cancelled++;
                }
            }

            Integer mysqlStock = jdbcTemplate.queryForObject(
                    "SELECT available_stock FROM camp_route WHERE id = ?", Integer.class, ROUTE_ID);
            Integer status = jdbcTemplate.queryForObject(
                    "SELECT order_status FROM appointment_order WHERE id = ?", Integer.class, orderId);
            Integer releaseTasks = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM inventory_compensation_task
                    WHERE order_id = ? AND task_type = 'ORDER_RELEASE'
                    """, Integer.class, orderId);

            assertThat(cancelled).isOne();
            assertThat(status).isEqualTo(2);
            assertThat(mysqlStock).isOne();
            assertThat(inventoryGateway.stock()).isOne();
            assertThat(releaseTasks).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRollbackMysqlStockWhenOrderInsertViolatesUniqueKey() {
        long collidingOrderId = 707L;
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO appointment_order
                    (id, user_id, pet_id, route_id, order_status, create_time, update_time, is_deleted)
                VALUES (?, ?, ?, ?, 2, ?, ?, 0)
                """, collidingOrderId, 99_999L, 20_001L, ROUTE_ID,
                Timestamp.valueOf(now), Timestamp.valueOf(now));

        assertThatThrownBy(() -> bookingOrchestrator.bookWithOrderId(
                collidingOrderId, 10_000L, ROUTE_ID, 20_000L))
                .isInstanceOf(RuntimeException.class);

        Integer mysqlStock = jdbcTemplate.queryForObject(
                "SELECT available_stock FROM camp_route WHERE id = ?", Integer.class, ROUTE_ID);
        Integer tasks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_compensation_task WHERE order_id = ?",
                Integer.class, collidingOrderId);

        assertThat(mysqlStock).isEqualTo(INITIAL_STOCK);
        assertThat(inventoryGateway.stock()).isEqualTo(INITIAL_STOCK);
        assertThat(tasks).isZero();
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        InMemoryInventoryGateway inMemoryInventoryGateway() {
            return new InMemoryInventoryGateway();
        }

        @Bean
        @Primary
        RouteCacheService noOpRouteCacheService() {
            return mock(RouteCacheService.class);
        }
    }

    static final class InMemoryInventoryGateway implements InventoryGateway {
        private final AtomicInteger stock = new AtomicInteger();
        private final ConcurrentHashMap<Long, String> reservations = new ConcurrentHashMap<>();
        private final Set<Long> released = ConcurrentHashMap.newKeySet();

        void reset(int value) {
            stock.set(value);
            reservations.clear();
            released.clear();
        }

        int stock() {
            return stock.get();
        }

        @Override
        public InventoryReservationResult reserve(long routeId, long orderId, int quantity) {
            while (true) {
                int current = stock.get();
                if (current < quantity) {
                    throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
                }
                if (stock.compareAndSet(current, current - quantity)) {
                    reservations.put(orderId, "RESERVED");
                    return new InventoryReservationResult(current - quantity);
                }
            }
        }

        @Override
        public boolean confirm(long routeId, long orderId) {
            return reservations.replace(orderId, "RESERVED", "CONFIRMED");
        }

        @Override
        public boolean compensateReservation(long routeId, long orderId) {
            if (reservations.replace(orderId, "RESERVED", "COMPENSATED")) {
                stock.incrementAndGet();
                return true;
            }
            return false;
        }

        @Override
        public boolean releaseOrder(long routeId, long orderId, int quantity) {
            if (released.add(orderId)) {
                stock.addAndGet(quantity);
                return true;
            }
            return false;
        }

        @Override public String tryAcquireReconciliationLock(long routeId) { return "test-lock"; }
        @Override public boolean synchronizeStockUnderLock(long routeId, String token, int databaseStock) {
            stock.set(databaseStock);
            return true;
        }
        @Override public void releaseReconciliationLock(long routeId, String token) { }
        @Override public Long getStock(long routeId) { return (long) stock.get(); }
        @Override public Set<Long> findExpiredReservations(long routeId, long deadline, int limit) { return Set.of(); }
    }
}
