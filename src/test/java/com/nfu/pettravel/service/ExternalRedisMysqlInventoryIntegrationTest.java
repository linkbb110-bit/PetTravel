package com.nfu.pettravel.service;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.nfu.pettravel.PetTravelApplication;
import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.inventory.InventoryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = PetTravelApplication.class)
class ExternalRedisMysqlInventoryIntegrationTest {
    private static final long ROUTE_ID = 88L;
    private static final int STOCK = 100;
    private static final int REQUESTS = 1000;

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("pettravel")
            .withUsername("pettravel")
            .withPassword("pettravel")
            .withInitScript("mysql-baseline-schema.sql");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "0");
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("pettravel.order.timeout-scan-initial-delay", () -> "3600000");
        registry.add("pettravel.inventory.recovery-initial-delay", () -> "3600000");
    }
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private BookingOrchestrator bookingOrchestrator;
    @Autowired
    private InventoryGateway inventoryGateway;
    @Autowired
    private InventoryReconciliationService reconciliationService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

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
                """, ROUTE_ID, "Testcontainers route", BigDecimal.TEN, STOCK, STOCK,
                Timestamp.valueOf(now), Timestamp.valueOf(now));
        List<Object[]> pets = new ArrayList<>(REQUESTS);
        for (int index = 0; index < REQUESTS; index++) {
            pets.add(new Object[]{200_000L + index, 100_000L + index, "pet-" + index,
                    "breed", 12, 1, Timestamp.valueOf(now), Timestamp.valueOf(now)});
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO pet_info
                    (id, user_id, name, breed, age, vaccine_status, create_time, update_time, is_deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
                """, pets);
        assertThat(reconciliationService.reconcile(ROUTE_ID)).isTrue();
    }

    @Test
    void shouldConserveStockWithActualRedisLuaAndMysqlTransactions() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(64);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>(REQUESTS);
        try {
            for (int index = 0; index < REQUESTS; index++) {
                long orderId = 300_000L + index;
                long userId = 100_000L + index;
                long petId = 200_000L + index;
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        bookingOrchestrator.bookWithOrderId(orderId, userId, ROUTE_ID, petId);
                        return true;
                    } catch (BusinessException rejected) {
                        assertThat(rejected.getErrorCode()).isEqualTo(ErrorCode.STOCK_INSUFFICIENT);
                        return false;
                    }
                }));
            }
            start.countDown();
            int succeeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get(60, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }
            Integer mysqlStock = jdbcTemplate.queryForObject(
                    "SELECT available_stock FROM camp_route WHERE id = ?", Integer.class, ROUTE_ID);
            Integer orders = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM appointment_order", Integer.class);

            assertThat(succeeded).isEqualTo(STOCK);
            assertThat(orders).isEqualTo(STOCK);
            assertThat(mysqlStock).isZero();
            assertThat(inventoryGateway.getStock(ROUTE_ID)).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldMigrateTheExistingMysqlBaselineWithFlyway() {
        Integer migratedVersions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version LIKE '20260822%'",
                Integer.class);
        Integer activeKeyColumn = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'appointment_order'
                  AND column_name = 'active_booking_key'
                """, Integer.class);
        Integer recoveryTable = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'inventory_compensation_task'
                """, Integer.class);

        assertThat(migratedVersions).isEqualTo(7);
        assertThat(activeKeyColumn).isOne();
        assertThat(recoveryTable).isOne();
    }

    @Test
    void shouldRejectOrderReleaseWhileRouteReconciliationLockIsHeld() {
        jdbcTemplate.update("UPDATE camp_route SET available_stock = 99 WHERE id = ?", ROUTE_ID);
        assertThat(reconciliationService.reconcile(ROUTE_ID)).isTrue();
        String token = inventoryGateway.tryAcquireReconciliationLock(ROUTE_ID);
        assertThat(token).isNotNull();
        try {
            assertThatThrownBy(() -> inventoryGateway.releaseOrder(ROUTE_ID, 991L, 1))
                    .isInstanceOfSatisfying(BusinessException.class,
                            exception -> assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.INVENTORY_BUSY));
            assertThat(inventoryGateway.getStock(ROUTE_ID)).isEqualTo(99L);
        } finally {
            inventoryGateway.releaseReconciliationLock(ROUTE_ID, token);
        }
        assertThat(inventoryGateway.releaseOrder(ROUTE_ID, 991L, 1)).isTrue();
        assertThat(inventoryGateway.getStock(ROUTE_ID)).isEqualTo(100L);
    }

  
@Test
void shouldAcquireRedisLockBeforeWaitingForMysqlRouteRowLock() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch mysqlRowLocked = new CountDownLatch(1);
    CountDownLatch releaseMysqlRow = new CountDownLatch(1);

    try {
        Future<?> holder = executor.submit(() ->
                new TransactionTemplate(transactionManager)
                        .executeWithoutResult(status -> {
                            jdbcTemplate.queryForObject(
                                    "SELECT available_stock FROM camp_route WHERE id = ? FOR UPDATE",
                                    Integer.class, ROUTE_ID);

                            mysqlRowLocked.countDown();

                            try {
                                if (!releaseMysqlRow.await(10, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException(
                                            "Timed out waiting to release MySQL row");
                                }
                            } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(interrupted);
                            }
                        }));

        assertThat(mysqlRowLocked.await(5, TimeUnit.SECONDS)).isTrue();

        Future<Boolean> reconciliation = executor.submit(
                () -> reconciliationService.reconcile(ROUTE_ID));

        // 只观察 Redis 锁，不参与锁竞争
        String lockKey =
                "pettravel:inventory:{" + ROUTE_ID + "}:reconcile-lock";

        boolean reconciliationLockObserved = false;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
                reconciliationLockObserved = true;
                break;
            }
            Thread.sleep(20);
        }

        assertThat(reconciliationLockObserved).isTrue();

        // 对账锁持有期间，新的库存预扣应被拒绝
        assertThatThrownBy(() -> inventoryGateway.reserve(ROUTE_ID, 992L, 1))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVENTORY_BUSY));

        releaseMysqlRow.countDown();

        holder.get(10, TimeUnit.SECONDS);
        assertThat(reconciliation.get(10, TimeUnit.SECONDS)).isTrue();

    } finally {
        releaseMysqlRow.countDown();
        executor.shutdownNow();
    }
}
}
