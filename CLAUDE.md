# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 工作语言

使用中文交流，代码注释和文档用中文。

## 项目概述

仿美团外卖平台 — Vue 3 + Spring Boot 3.4 前后端分离架构。从原 Spring Boot + Thymeleaf 全栈项目重构而来（原项目在 `../外卖/`）。

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
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=redis,rabbitmq,elasticsearch

# Swagger UI: http://localhost:8081/swagger-ui.html
```

### 前端（Vue 3 + Vite）

```bash
cd frontend && npm install
npx vite --port 4000 --strictPort
# 访问: http://localhost:4000
```

### Docker 基础设施

```bash
cd docker
docker compose up -d          # 启动 MySQL + Redis + ES + RabbitMQ + MinIO + Seata
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
| `entity/` | JPA 实体（复制自原项目，未修改） |
| `repository/` | Spring Data JPA 仓库 |
| `service/` | 业务逻辑，含 CustomUserDetailsService（Spring Security 需要） |
| `exception/` | BusinessException、GlobalExceptionHandler |

**中间件默认禁用**: `application.yml` 排除了 Redis、ES、RabbitMQ 的自动配置。在不需要这些中间件时，后端只需 MySQL 即可启动。通过 Spring profiles 按需激活：`redis`、`rabbitmq`、`elasticsearch`。

**ShardingSphere / Seata**: 配置文件 `application-sharding.yml` 和 `application-seata.yml` 已就绪，通过独立 profile `sharding` 和 `seata` 激活。

**注意**: `application.yml` 中 `server.port=8081`，但前端 vite proxy 和 README 中写的是 9091 — 两者不一致，如果前后端联调不通，先检查端口。

### 前端 (`frontend/`)

**路由设计**: 按角色分为两组：
- `/login`、`/register` — 游客页（`meta.guest`）
- `/consumer/*` — 消费者端（路由守卫检查 `ROLE_CONSUMER`），外层 `AppLayout.vue`（底部导航栏）
- `/merchant/*` — 商家端（路由守卫检查 `ROLE_MERCHANT`），外层 `MerchantLayout.vue`

**路由守卫** (`router/index.ts`): 检查 JWT 是否过期（解析 payload.exp），过期则清除 localStorage 并跳转登录。已登录用户访问游客页自动按角色跳转。

**API 层**: `api/request.ts` 创建 Axios 实例，请求拦截器自动附加 `Authorization: Bearer <token>`。响应拦截器处理 401 → 尝试使用 refreshToken 静默刷新 → 失败则清空登录态跳转 `/login`。

**状态管理 (Pinia)**:
- `stores/user.ts` — 登录/登出/角色判断，Token 存 localStorage
- `stores/cart.ts` — 购物车按商家隔离（切换商家清空），持久化到 localStorage

**CSS 设计规范**:
- 纯移动端：固定 480px 宽度居中，不做响应式
- 美团配色：主色 `#FFD101`（黄）、价格 `#FF6B35`（橙红）、背景 `#F5F5F5`
- 卡片式布局：白色卡片 + 8px 圆角 + 轻微阴影
- `style.css` 为全局样式（非 scoped），组件用 `<style scoped>`

### 数据库

数据库名 `waimai`，JPA `ddl-auto: update` 自动建表。初始数据由 `DataInitializer.java` (`CommandLineRunner`) 在首次启动时插入，含示例商家、商品、分类和测试用户。

## 实施进度

- [x] 阶段一：后端核心 — REST API + JWT + Swagger（全部完成）
- [x] 阶段二：前端 — 全部 19 个页面（全部完成）
- [x] 阶段三：基础设施 — 依赖和配置已就绪，Docker Compose 编排完成，中间件按需通过 profile 激活
- [x] 阶段四：ShardingSphere + Seata 配置文件已就绪，待验证
- [ ] 无测试用例（尚未编写单元测试或集成测试）

## 编码规范

- 简洁优先：用最少代码解决问题，不过度工程
- 精准修改：只碰必须碰的，匹配现有代码风格
- 目标驱动：定义成功标准，循环验证直到达成
- 后端 Service 抛出 `BusinessException` 而非 `RuntimeException`，由全局异常处理器统一返回 JSON
- 前端 API 文件命名与后端 Controller 对应，返回值通过响应拦截器自动解包 `data` 字段
