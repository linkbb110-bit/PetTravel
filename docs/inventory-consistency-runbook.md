# PetTravel 库存一致性与发布手册

## 预约提交链路

HTTP 成功只会在 MySQL 订单事务提交后返回：

1. MyBatis-Plus `IdWorker` 生成雪花订单 ID。
2. `inventory_reserve.lua` 原子检查并预扣 Redis 库存，同时写入待确认预约、数量和超时分数。
3. 当前请求线程开启 MySQL 事务，校验宠物归属、疫苗资格、路线状态和重复预约，原子扣减 `camp_route.available_stock`，插入 `WAIT_PAY` 订单及 `RESERVATION_CONFIRM` 恢复任务。
4. MySQL 失败时立即执行 `inventory_compensate.lua`；Redis 也不可用时，持久 `BOOKING_ROLLBACK` 任务和 Redis 待确认集合均可触发恢复。
5. MySQL 提交后执行 `inventory_confirm.lua`。确认失败不会反向恢复已经提交的 MySQL 库存，由持久任务重试。
6. 只有 WebSocket 通知使用 `@Async`。线程池拒绝通知时会丢弃并记录告警，不阻塞或失败已经提交的预约。

`RESERVED` 元数据在确认或补偿前不设置 TTL，避免恢复时丢失扣减数量；`CONFIRMED`、`COMPENSATED` 终态标记使用 TTL。Redis 键使用 `{routeId}` hash tag，同一路线的 Lua 多键操作可运行于 Redis Cluster 同一 slot。订单释放幂等标记保持持久，清理必须与订单归档和不可再重试的生命周期绑定。

## 超时与主动取消

所有实例都可以扫描到同一个候选订单，但只有下列 CAS 能成功一次：

```sql
UPDATE appointment_order
SET order_status = 2, update_time = CURRENT_TIMESTAMP
WHERE id = ? AND order_status = 0 AND is_deleted = 0;
```

只有 `affected rows = 1` 的事务会原子恢复 MySQL 库存并插入 `ORDER_RELEASE` 任务。提交后 Redis Lua 使用永久 order marker 防止重复加库存。主动取消接口在统一响应的 `data` 中返回布尔值，明确本次请求是否赢得 CAS；超时任务将 `false` 当作正常幂等 no-op。

## 安全对账协议

MySQL 是库存最终事实来源。所有定时对账、管理员初始化以及路线容量修改后的同步都走同一个协议：

1. 先取得路线级 Redis 对账锁；新的预约预扣和未执行的订单释放会返回 `INVENTORY_BUSY`。
2. 再通过 `SELECT available_stock ... FOR UPDATE` 锁定读取 MySQL 路线行，序列化预约扣减、取消恢复和管理员改库存。
3. 确认该路线没有 `PENDING/PROCESSING` 的订单型库存任务。
4. Lua 最终再次校验锁 token 和 Redis 待确认预约数为 0，才以 MySQL 值覆盖 Redis。

普通 Redis 重试达到上限后，任务进入 `NEEDS_RECONCILE`，不再永久冻结路线。恢复任务会按上述协议执行路线级修复，成功后统一标记完成。路线创建/修改事务会持久写入 `ROUTE_RECONCILE` 任务；提交后的即时 Redis 调用失败不会把已经提交的 HTTP 操作误报为失败。

## 数据库发布

项目使用 Flyway 跟踪生产迁移，每个 MySQL DDL 都拆成独立版本，避免一个大脚本部分成功后无法判断进度。

1. 备份数据库。
2. 只读执行 [`PRECHECK_V20260822.sql`](../src/main/resources/db/predeploy/PRECHECK_V20260822.sql)，处理活跃重复订单、非法库存和明文密码统计；任何返回的异常行都必须先解决。
3. 确认当前数据库是项目既有基线结构。首次接管非空库时，`baseline-on-migrate=true` 会写入版本 `0`，然后顺序执行 `V20260822.01` 至 `V20260822.07`。
4. 先在数据库副本或预发布环境启动并检查 `flyway_schema_history` 七个版本均为成功，再进入生产维护窗口。不要对已部分人工执行过同名改动的库直接启动；应先核对 `information_schema` 并修复 Flyway 基线。
5. 生产保持 `FLYWAY_ENABLED=true`。只有由独立发布系统执行迁移时才设置为 `false`，且必须由该系统维护等价的版本历史和校验。

关键环境变量：

- `JWT_SECRET`：至少 32 字节的随机值；
- `DB_URL`、`DB_USER`、`DB_PASSWORD`；
- `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`；
- `ALIYUN_OSS_ACCESS_KEY_ID`、`ALIYUN_OSS_ACCESS_KEY_SECRET`、endpoint、bucket 和可选 public base URL。

存量明文密码默认拒绝登录。只有经过审批的迁移窗口才临时设置 `LEGACY_PLAINTEXT_UPGRADE_ENABLED=true`；升级使用窄字段 CAS，随后重新读取账号禁用状态和角色。迁移完成后立即关闭。

## WebSocket 鉴权

管理员通知端点为 `/ws/admin`，只接受 ADMIN JWT，不接受 URL 查询参数。非浏览器客户端使用 `Authorization: Bearer <jwt>`；浏览器使用 `new WebSocket(url, ["bearer", jwt])`，服务端只协商 `bearer` 子协议。应用日志不会输出完整 JWT 或消息正文。

## 验证与监控

```powershell
mvn test
mvn -DskipTests package
```

`BookingConcurrencyIntegrationTest` 在 H2 MySQL 模式使用真实 Spring 事务和 Mapper SQL，覆盖 1000 请求/100 库存、100 路重复取消、唯一键插入失败后的库存回滚。`ExternalRedisMysqlInventoryIntegrationTest` 使用 MySQL 8.4 + Redis 7.4，先从旧基线运行真实 Flyway 迁移，再执行 Lua/SQL 并发测试；有 Docker 的 CI 必须要求该测试零跳过。

生产监控至少包括：

- 按 `task_type`、`task_status` 统计数量和最老任务年龄；`NEEDS_RECONCILE > 0` 持续超过一个调度周期立即告警；
- Redis 预扣失败码（库存不足、未初始化、对账繁忙）分布；
- Redis/MySQL 库存抽样差异；发现差异时禁止人工直接 `SET`，先检查待确认预约和恢复任务；
- `released:{orderId}` 幂等键数量、通知线程池拒绝数、Flyway 校验失败和 OSS 上传失败率。
