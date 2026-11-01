# PetTravel（携宠游）后端

基于 Spring Boot 3 的携宠出游路线预约管理系统后端，围绕路线预约、订单流转及并发库存一致性开展实现与测试。

## 技术栈

Java 17 · Spring Boot 3 · MyBatis-Plus · MySQL · Redis / Lua · JWT · WebSocket · Flyway · 阿里云 OSS · Apache POI · JUnit 5 / Testcontainers

## 已实现功能

- **用户与宠物**：注册、登录、JWT 认证、角色权限校验；宠物信息维护与预约资格校验。
- **路线与订单**：热门路线查询、管理员创建和修改路线、预约下单、订单支付、主动取消、超时取消与导出超时订单。
- **库存处理**：Redis Lua 原子预扣，MySQL 事务扣减与订单落库；数据库失败触发补偿，提交后确认 Redis 预扣。
- **一致性恢复**：持久化库存恢复任务、幂等释放、定时重试与 Redis/MySQL 库存对账。
- **通知与上传**：WebSocket 管理端订单通知、图片上传至阿里云 OSS。

> 当前实现的是**按路线维度的库存与预约**，尚未实现同一路线按日期/场次独立库存；目前也未实现令牌版本号驱动的动态权限撤销。仓库以实际代码为准。

## 关键实现

1. **预约事务**：`BookingOrchestrator` 串联 Redis 预扣、MySQL 事务扣库、确认与异常补偿。
2. **防重复与取消**：数据库唯一约束限制活跃重复预约；取消通过条件更新与库存恢复保证幂等。
3. **恢复与对账**：恢复任务记录待处理状态；路线级 Redis 锁与 MySQL 行锁协调库存重建。
4. **接口安全**：JWT 拦截 `/api/**` 的受保护接口，管理员操作单独校验角色；WebSocket 握手校验管理员身份。

库存实现与数据库迁移细节见 [库存一致性说明](docs/inventory-consistency-runbook.md)。

## 本地运行

要求：JDK 17、MySQL 8.x、Redis 7.x、Maven（可使用仓库自带的 `mvnw.cmd`）。涉及 OSS 上传时需要有效的 OSS 配置。

1. 新建数据库 `pet_travel_db`，导入 [基础结构](docs/mysql-baseline.sql)。该基础结构用于**全新测试/开发数据库**，不适用于覆盖已有业务库。
2. 通过环境变量设置本地数据库凭据、JWT 密钥和 OSS 配置。`src/main/resources/application.yml` 只读取环境变量，不存储真实凭据。
3. 在项目根目录运行：

```powershell
.\mvnw.cmd spring-boot:run
```

主要环境变量如下（请使用自己的本地配置，不要提交真实值）：

| 环境变量 | 说明 |
| --- | --- |
| `DB_URL`、`DB_USER`、`DB_PASSWORD` | MySQL 连接地址、用户和密码 |
| `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` | Redis 连接配置，密码可为空 |
| `JWT_SECRET` | 至少 32 字节的随机密钥，必须设置 |
| `ALIYUN_OSS_ENDPOINT`、`ALIYUN_OSS_BUCKET_NAME` | OSS 服务地址及 Bucket |
| `ALIYUN_OSS_ACCESS_KEY_ID`、`ALIYUN_OSS_ACCESS_KEY_SECRET` | OSS 身份凭据；运行时必须设置，不要公开 |
| `FLYWAY_ENABLED` | 默认 `true`，由 Flyway 管理迁移 |

新建开发库先导入基础结构，再让 Flyway 执行后续迁移。**对已有数据库进行升级前**，先备份并执行只读 [数据库预检查](src/main/resources/db/predeploy/PRECHECK_V20260822.sql)，不要直接导入基础结构覆盖现有表。

## 测试与构建

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
```

`src/test/resources/application.yml` 为 H2 测试提供隔离配置。`BookingConcurrencyIntegrationTest` 使用内存库存网关测试并发预约和事务行为；`ExternalRedisMysqlInventoryIntegrationTest` 使用 Testcontainers 启动真实 MySQL 与 Redis，Docker 不可用时会跳过。测试用例存在不等于当前环境已通过全部测试，请以本地实际执行结果为准。

## 接口说明

- 登录：`POST /api/user/login`；注册：`POST /api/user/register`。
- 热门路线：`GET /api/route/hot`；预约：`POST /api/route/book/{routeId}`。
- 支付与取消：`POST /api/order/{orderId}/pay`、`POST /api/order/{orderId}/cancel`。
- 管理员 WebSocket：`/ws/admin`，需要有效的管理员 JWT。

接口详情以 Controller 为准。本仓库主要展示后端服务、数据库迁移、库存一致性及测试实现，不包含完整的用户端页面。
