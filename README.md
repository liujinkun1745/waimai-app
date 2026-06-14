# 🛵 仿美团外卖平台

全栈移动端外卖平台，Vue 3 + Spring Boot 3.4 前后端分离架构。

<p align="center">
  <img src="https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vue.js" alt="Vue">
  <img src="https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?logo=springboot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-7-DC382D?logo=redis" alt="Redis">
  <img src="https://img.shields.io/badge/Docker-✔-2496ED?logo=docker" alt="Docker">
</p>

## 功能

| 消费者端 (11 页) | 商家端 (6 页) |
|---|---|
| 首页浏览 / 商家详情 / 购物车 / 结算 / 订单 / 评价 | 订单管理 / 商品管理 / 评价管理 |
| 神券中心 / 搜索 / 个人中心 / 地址管理 / 余额充值 | 收益统计 / 店铺设置 |

## 技术栈

**后端**：Spring Boot 3.4 + Spring Security 6 + JWT + Spring Data JPA + MySQL 8.0

**可选中间件**（通过 Spring Profile 按需激活）：
- Redis 7 — 购物车缓存 + 热门商家缓存
- RabbitMQ 3 — 订单状态异步通知
- Elasticsearch 8 — 商家搜索
- MinIO — 商品图片对象存储
- ShardingSphere-JDBC 5 — 订单分表
- Seata AT — 分布式事务

**前端**：Vue 3 + Element Plus + Vite + Pinia + Axios + Chart.js

**部署**：Docker Compose 一键编排 9 容器

## 快速开始

### 环境要求

JDK 21+ · Node.js 20+ · MySQL 8.0

### 1. 初始化

```bash
cp .env.example .env
cd frontend && npm install
```

### 2. 启动后端（端口 8081）

```bash
cd backend
./mvnw spring-boot:run
```

仅需 MySQL 即可运行，中间件默认不启用。需要时加 profile：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=redis,rabbitmq
```

Swagger 文档：http://localhost:8081/swagger-ui.html

### 3. 启动前端（端口 4000）

```bash
cd frontend
npm run dev
```

访问 http://localhost:4000

### 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 消费者 | `consumer1` | `123456` |
| 商家 | `merchant1` | `123456` |

## 架构

```
浏览器（480px 移动端）
      │  :4000
Vue 3 + Element Plus
      │  /api → :8081
Spring Boot REST API + JWT
      │
      ├── MySQL（持久数据）
      ├── Redis（购物车 / 热门商家缓存）
      ├── RabbitMQ（订单通知）
      └── MinIO（商品图片）
```

### 后端包结构

```
controller/     Auth / Consumer / Merchant / Upload
service/        业务逻辑 + 缓存服务
repository/     Spring Data JPA（11 个仓库）
entity/         JPA 实体（12 张表，含订单闭环所需全部关联）
dto/            请求/响应 DTO + Jakarta Validation
config/         Security · JWT · CORS · Swagger · 中间件
exception/      BusinessException + 全局异常处理
```

### API 权限

| 路径前缀 | 权限 |
|----------|------|
| `/api/auth/**` | 公开 |
| `/api/consumer/**` | ROLE_CONSUMER |
| `/api/merchant/**` | ROLE_MERCHANT |

### 核心特性

- **JWT 双 Token** — Access 30 分钟 + Refresh 7 天，Axios 拦截器自动刷新
- **统一响应格式** — `Result<T>` {code, message, data}，全局异常拦截器兜底
- **Redis 购物车** — Hash 结构，Key 按用户+商家隔离，7 天 TTL，手动 JSON 序列化
- **热门商家缓存** — 30 分钟 TTL，商家信息变更主动清除
- **订单闭环** — 下单 → 接单 → 配送 → 完成 → 评价，RabbitMQ 异步通知
- **声明式校验** — Jakarta Validation 注解校验请求参数

## Docker 部署

```bash
cp .env.example .env
cd docker
docker compose up -d
```

| 服务 | 端口 |
|------|------|
| 后端 API | 8080 |
| MySQL（主/从）| 3306 / 3307 |
| Redis | 6379 |
| Elasticsearch | 9200 |
| RabbitMQ | 5672 / 15672 |
| MinIO | 9000 / 9001 |

## License

MIT
