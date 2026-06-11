# 🛵 美味外卖 — 仿美团外卖平台

Vue 3 + Spring Boot 3.4 前后端分离，移动端外卖平台。支持消费者下单、商家管理、实时推送，全套 Docker 部署。

<p align="center">
  <img src="https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vue.js" alt="Vue">
  <img src="https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?logo=springboot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql" alt="MySQL">
  <img src="https://img.shields.io/badge/Docker-✔-2496ED?logo=docker" alt="Docker">
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License">
</p>

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 · Element Plus · Vite · Pinia · Axios · Chart.js |
| 后端 | Spring Boot 3.4 · Spring Security 6 · JWT (jjwt) · Spring Data JPA |
| 数据库 | MySQL 8.0 + 读写分离 |
| 缓存 | Redis 7（购物车 / 优惠券 / 热门商家） |
| 搜索 | Elasticsearch 8（商家搜索） |
| 消息队列 | RabbitMQ 3（订单状态通知） |
| 实时推送 | WebSocket + STOMP |
| 对象存储 | MinIO（商品图片） |
| 分库分表 | ShardingSphere-JDBC 5（订单表按 user_id 分片） |
| 分布式事务 | Seata AT 模式 |
| 容器化 | Docker Compose 一键部署 9 个服务 |

## 页面

| 消费者端 (11) | 商家端 (6) | 游客 (2) |
|---|---|---|
| 首页 / 商家详情 / 结算 / 订单 / 评价 | 订单管理 / 商品管理 / 评价管理 | 登录 / 注册 |
| 神券中心 / 搜索 / 个人中心 / 地址 / 余额 | 收益统计 / 店铺设置 | |

## 快速开始

### 环境要求

- **JDK 21+** · **Node.js 20+** · **MySQL 8.0**

### 1. 初始化

```bash
cp .env.example .env        # 环境变量（JWT secret、数据库密码等）
cd frontend && npm install   # 前端依赖
```

### 2. 启动后端（端口 8081）

```bash
cd backend
./mvnw spring-boot:run
```

> 仅需 MySQL 即可运行。中间件默认不启用，需要时加 profile：`-Dspring-boot.run.profiles=redis,rabbitmq,elasticsearch,ws`

Swagger 文档：http://localhost:8081/swagger-ui.html

### 3. 启动前端（端口 4000）

```bash
cd frontend
npm run dev
```

访问 http://localhost:4000，Vite 自动代理 `/api` 到后端。

### 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 消费者 | `consumer1` | `123456` |
| 商家 | `merchant1` | `123456` |

## 核心特性

- **JWT 双 Token** — Access Token 30 分钟 + Refresh Token 7 天，Axios 拦截器自动续期
- **购物车** — Pinia + localStorage 持久化，按商家隔离，加购抛物线飞入动画
- **路由守卫** — 解析 JWT exp 判断过期，按角色自动跳转
- **骨架屏** — 列表加载 shimmer 占位
- **搜索历史** — 最近 8 条 localStorage 存储
- **订单进度条** — 4 段可视化状态追踪
- **收益图表** — Chart.js 7 天 / 30 天趋势

## 架构

```
                    ┌─────────────┐
                    │   浏览器     │  (480px 移动端)
                    └──────┬──────┘
                           │  :4000 (Vite dev)
                    ┌──────▼──────┐
                    │  Vue 3 前端  │  Element Plus
                    └──────┬──────┘
                           │  /api → :8081
                    ┌──────▼──────┐
                    │  Spring Boot │  REST API + JWT
                    └──┬───┬───┬──┘
                       │   │   │
              ┌────────┼───┼───┼────────┐
              ▼        ▼   ▼   ▼        ▼
           MySQL    Redis  ES  RabbitMQ  MinIO
```

### 后端包结构

| 包 | 职责 |
|----|------|
| `config/` | Security · JWT · CORS · Swagger · 中间件 · DataInitializer |
| `controller/` | AuthController · ConsumerController · MerchantController · UploadController |
| `service/` | 业务逻辑 + CustomUserDetailsService |
| `repository/` | Spring Data JPA 仓库（10 个） |
| `entity/` | JPA 实体（12 个，含 UndoLog、LocalMessage） |
| `dto/` | 请求/响应 DTO + Jakarta Validation |
| `exception/` | BusinessException + 全局异常处理 |

### API 权限

| 路径 | 权限 |
|------|------|
| `/api/auth/**` | 公开 |
| `/api/consumer/**` | `ROLE_CONSUMER` |
| `/api/merchant/**` | `ROLE_MERCHANT` |

### 前端结构

| 目录 | 职责 |
|------|------|
| `api/` | Axios 封装，响应拦截器自动解包 `Result.data`，错误返回 `null` |
| `router/` | 19 条懒加载路由 + JWT 过期守卫 |
| `stores/` | Pinia — user（登录态）+ cart（购物车隔离） |
| `views/` | `auth/` · `consumer/` · `merchant/` 三组页面 |
| `components/` | AppLayout（消费者底部导航栏）· MerchantLayout |

## Docker 部署

```bash
cp .env.example .env          # 编辑 .env，生产环境务必修改所有密码
cd docker
docker compose up -d           # 启动全部 9 个容器
```

| 服务 | 端口 | 管理界面 |
|------|------|---------|
| 后端 API | 8080 | `/swagger-ui.html` |
| MySQL (主/从) | 3306 / 3307 | — |
| Redis | 6379 | — |
| Elasticsearch | 9200 | — |
| RabbitMQ | 5672 / 15672 | http://localhost:15672 |
| MinIO | 9000 / 9001 | http://localhost:9001 |
| Seata | 8091 | — |

## 云服务器部署

```bash
# 1. 安装 Docker
curl -fsSL https://get.docker.com | bash
apt install -y docker-compose-v2

# 2. 拉取项目
git clone https://github.com/liujunkun1745/waimai-app.git /opt/waimai
cd /opt/waimai
cp .env.example .env    # 编辑密码

# 3. 启动服务
cd docker && docker compose up -d

# 4. 构建前端 + Nginx 反代
cd /opt/waimai/frontend && npm install && npm run build
```

Nginx 配置参考：

```nginx
server {
    listen 80;
    location / {
        root /opt/waimai/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## License

MIT
