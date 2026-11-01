# PetTravel 后端

PetTravel 是基于 Spring Boot 3、MyBatis-Plus、Redis Lua、JWT、WebSocket、OSS 和 POI 的携宠出游预约后端。

## 本地配置

运行前至少配置：

```text
JWT_SECRET=<至少 32 字节的随机值>
DB_URL=jdbc:mysql://localhost:3306/pet_travel_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
DB_USER=root
DB_PASSWORD=<数据库密码>
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=<Redis 密码，可为空>
ALIYUN_OSS_ACCESS_KEY_ID=<OSS AccessKey ID>
ALIYUN_OSS_ACCESS_KEY_SECRET=<OSS AccessKey Secret>
```

首次部署到既有数据库前，先只读执行 [`PRECHECK_V20260822.sql`](src/main/resources/db/predeploy/PRECHECK_V20260822.sql)。应用默认启用 Flyway，并从版本 0 接管既有非空基线库。完整的库存恢复、迁移和监控说明见 [`inventory-consistency-runbook.md`](docs/inventory-consistency-runbook.md)。

## 构建与测试

```powershell
mvn test
mvn -DskipTests package
```

核心 H2 并发测试无需外部服务。真实 MySQL 8.4 + Redis 7.4 测试使用 Testcontainers；本机没有 Docker 时会跳过，CI 应提供 Docker 并要求零跳过。

## 接口安全

- `/api/user/login`、`/api/user/register` 之外的 `/api/**` 请求都需要 `Authorization: Bearer <jwt>`。
- 管理员接口由 `SysUser.roleType=ADMIN` 保护。
- 管理员 WebSocket 为 `/ws/admin`；浏览器通过 `new WebSocket(url, ["bearer", jwt])` 携带身份，不接受 URL token。
