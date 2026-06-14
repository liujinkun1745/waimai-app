# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 工作语言

使用中文交流，代码注释和文档用中文。

## 项目概述

仿美团外卖平台 — Vue 3 + Spring Boot 3.4 前后端分离架构，JDK 21。从原 Spring Boot + Thymeleaf 全栈项目重构而来（原项目在 `../外卖/`）。

## 首次初始化

```bash
cp .env.example .env          # 创建环境变量文件（JWT secret、数据库密码等）
cd frontend && npm install     # 安装前端依赖
```

## 常用命令

### 后端（Spring Boot）

```bash
# 编译
cd backend && ./mvnw clean compile -q

# 打包
cd backend && ./mvnw clean package -DskipTests

# 启动（默认端口 8081，仅需要 MySQL）
cd backend && ./mvnw spring-boot:run

# 启动并启用中间件（需要 Docker 基础设施运行）
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=redis,rabbitmq,elasticsearch,ws

# Swagger UI: http://localhost:8081/swagger-ui.html
```

### 前端（Vue 3 + Vite）

```bash
cd frontend
npm run dev                   # 开发模式，端口 4000（strictPort，不会回退到其他端口）
npm run build                 # 生产构建（type-check + vite build）
npm run preview               # 预览生产构建
# 访问: http://localhost:4000
```

### Docker 基础设施

```bash
cd docker
docker compose up -d          # 启动 MySQL + Redis + ES + RabbitMQ + MinIO + Seata
docker compose up -d mysql    # 仅启动 MySQL
docker compose ps              # 查看服务状态
docker compose down -v         # 停止并清除数据卷
```

### 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 消费者 | consumer1 | 123456 |
| 商家 | merchant1 | 123456 |

## 架构

### 后端 (`backend/`)

**启动入口**: `WaimaiApplication.java`，包路径 `com.waimai`。

**认证流程**: Spring Security 6 无状态模式 → `JwtAuthFilter` 从 `Authorization: Bearer <token>` 提取 JWT → 查询 `UserRepository` 加载用户 → 注入 SecurityContext。双 Token 机制：Access Token 30 分钟 + Refresh Token 7 天。

**请求处理链**: Controller (`@RestController`) → Service → Repository (Spring Data JPA) → MySQL

**统一响应格式**: 所有接口返回 `Result<T>`，字段为 `code`、`message`、`data`。异常由 `GlobalExceptionHandler` (`@RestControllerAdvice`) 统一拦截。

**包结构**:
| 包 | 职责 |
|----|------|
| `config/` | Security、JWT、CORS、Swagger、中间件配置和 DataInitializer |
| `controller/` | AuthController、ConsumerController、MerchantController、UploadController |
| `dto/request/` | 请求 DTO（含 Jakarta Validation 校验注解） |
| `dto/response/` | Result、ResultCode |
| `entity/` | JPA 实体（12 个：User、Order、OrderItem、Merchant、Product、Category、Review、Address、Coupon、BalanceRecord、UndoLog、LocalMessage）。UndoLog 用于 Seata AT 分布式事务回滚，LocalMessage 用于本地消息表 |
| `repository/` | Spring Data JPA 仓库（11 个，与实体一一对应） |
| `service/` | 业务逻辑。UserService、MerchantService、CategoryService、ProductService、OrderService、ReviewService、CouponService、AddressService、BalanceService、MerchantCacheService（Redis 热门商家缓存）、CustomUserDetailsService |
| `exception/` | BusinessException、GlobalExceptionHandler |

**核心实体关系**: User → Order（一对多），Order → OrderItem（一对多），Merchant → Product（一对多），Product → Category（多对一），Order → Review（一对一），User → Address（一对多）。

**中间件与 Profile**: `application.yml` 默认排除了 Redis、ES、RabbitMQ 的自动配置，后端只需 MySQL 即可启动。通过 Spring profiles 按需激活：

| Profile | 对应中间件 | 配置类 |
|---------|-----------|--------|
| `redis` | Redis 7（购物车/优惠券缓存） | `RedisConfig` |
| `rabbitmq` | RabbitMQ 3（订单状态通知） | `RabbitMQConfig` |
| `elasticsearch` | ES 8（商家搜索） | 自动配置 |
| `ws` | WebSocket + STOMP（实时推送） | `WebSocketConfig` |
| `sharding` | ShardingSphere-JDBC 5（订单分表） | `application-sharding.yml` |
| `seata` | Seata AT 模式（分布式事务） | `application-seata.yml` |

**环境变量**: `application.yml` 中敏感配置使用 `${ENV_VAR:default}` 占位符，通过系统环境变量注入。`.env` 文件供 Docker Compose 读取（`docker compose` 自动加载 `.env`），本地开发时需手动 export 或使用 IDE 环境变量设置。主要变量：`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY` 等。详见 `.env.example`。

**MinIO 文件上传**: `UploadController` 处理图片上传（商品图片等），存储到 MinIO bucket `waimai-products`。

**API 权限分界** (`SecurityConfig.java`):
- `/api/auth/**` — 公开（登录/注册/刷新 Token）
- `/api/consumer/**` — 需要 `ROLE_CONSUMER`
- `/api/merchant/**` — 需要 `ROLE_MERCHANT`

**Docker 部署**: 后端在多阶段 Dockerfile 中端口为 **8080**（`SERVER_PORT: 8080`），不同于本地开发的 8081。Docker Compose 会启动 9 个容器（含 MySQL 主从、Redis、ES、RabbitMQ、MinIO、Seata、后端应用）。

### 前端 (`frontend/`)

**技术栈**: Vue 3 + Element Plus + Vite + Pinia + Axios + Chart.js + vue-chartjs

**路由设计**: 按角色分为三组：
- `/login`、`/register` — 游客页（`meta.guest`），已登录用户自动按角色跳转
- `/consumer/*` — 消费者端（路由守卫检查 `ROLE_CONSUMER`），外层 `AppLayout.vue`（底部导航栏）
- `/merchant/*` — 商家端（路由守卫检查 `ROLE_MERCHANT`），外层 `MerchantLayout.vue`

共 19 个页面（消费者 11 个 + 商家 6 个 + 游客 2 个），全部使用懒加载 `() => import(...)`。

**路由守卫** (`router/index.ts`): 解析 JWT payload 中的 `exp` 字段判断过期，过期则清除 localStorage 并跳转登录。已登录用户访问游客页自动按角色跳转。

**API 层** (`api/request.ts`): Axios 实例，`baseURL: '/api'`，超时 15 秒。关键行为：
- **请求拦截器**: 自动附加 `Authorization: Bearer <token>`
- **响应拦截器**: `code === 200` 时自动解包 `Result<T>.data`，调用方直接拿到业务数据；非 200 则 `ElMessage.error` 提示
- **401 刷新**: 自动用 refreshToken 调 `/api/auth/refresh`，成功则更新 localStorage 并重试原请求；失败清空登录态跳转 `/login`
- **错误静默**: 401、403、网络错误均返回 `null` 而非抛异常 — API 调用方检查 `null` 判断失败，无需 try-catch
- API 文件命名与后端 Controller 对应（`auth.ts` → `AuthController`，`consumer.ts` → `ConsumerController`，`merchant.ts` → `MerchantController`）

**状态管理 (Pinia)**:
- `stores/user.ts` — 登录/登出/角色判断，Token 存 localStorage
- `stores/cart.ts` — 购物车按商家隔离（切换商家清空），持久化到 localStorage

**CSS 设计规范**:
- 纯移动端：固定 480px 宽度居中，不做响应式
- 美团配色：主色 `#FFD101`（黄）、价格 `#FF6B35`（橙红）、背景 `#F5F5F5`
- 卡片式布局：白色卡片 + 8px 圆角 + 轻微阴影
- `style.css` 为全局样式（非 scoped），组件用 `<style scoped>`

### 数据库

数据库名 `waimai`，JDBC URL 中 `createDatabaseIfNotExist=true` 自动创建数据库。JPA `ddl-auto: update` 自动建表，`open-in-view: true`（允许视图层懒加载关联实体）。初始数据由 `DataInitializer.java` (`CommandLineRunner`) 在首次启动时插入，含示例商家、商品、分类和测试用户。

`docs/` 目录包含原始实施计划 `implementation-plan.md`（注意其中部分版本号已过时，以实际 `pom.xml` 和 `package.json` 为准）。

## 实施进度

- [x] 阶段一：后端核心 — REST API + JWT + Swagger（全部完成）
- [x] 阶段二：前端 — 全部 19 个页面（全部完成）
- [x] 阶段三：基础设施 — Docker Compose 编排完成，中间件按需通过 profile 激活
- [x] 阶段四：ShardingSphere + Seata 配置文件已就绪，待验证
- [ ] 无测试用例（尚未编写单元测试或集成测试）

## 编码规范

- 简洁优先：用最少代码解决问题，不过度工程
- 精准修改：只碰必须碰的，匹配现有代码风格
- 目标驱动：定义成功标准，循环验证直到达成
- 后端 Service 抛出 `BusinessException` 而非 `RuntimeException`，由全局异常处理器统一返回 JSON
- 前端 API 文件命名与后端 Controller 对应，返回值通过响应拦截器自动解包 `data` 字段；检查 `null` 判断失败，无需 try-catch
