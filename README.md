# 🛵 美味外卖 — 仿美团外卖平台

前后端分离架构，Vue 3 + Element Plus 移动端 + Spring Boot 3.4 REST API。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus + Vite + Pinia + Axios + Chart.js |
| 后端 | Spring Boot 3.4 + Spring Security 6 + JWT (jjwt) |
| 数据库 | MySQL 8.0 + JPA/Hibernate |
| 缓存 | Redis 7（购物车 / 优惠券 / 热门商家） |
| 搜索 | Elasticsearch 8（商家搜索） |
| 消息队列 | RabbitMQ 3（订单状态变更通知） |
| 实时推送 | WebSocket + STOMP |
| 对象存储 | MinIO（商品图片） |
| 分库分表 | ShardingSphere-JDBC 5（订单表按 user_id） |
| 分布式事务 | Seata AT 模式 |
| 容器化 | Docker Compose |

## 项目结构

```
├── backend/                 # Spring Boot REST API
│   ├── src/main/java/com/waimai/
│   │   ├── config/          # Security / JWT / CORS / Swagger / 中间件
│   │   ├── controller/      # REST 控制器
│   │   ├── dto/             # 请求/响应 DTO
│   │   ├── entity/          # JPA 实体
│   │   ├── repository/      # Spring Data 仓库
│   │   └── service/         # 业务逻辑
│   └── src/main/resources/  # 配置文件（含 ShardingSphere / Seata）
├── frontend/                # Vue 3 移动端
│   └── src/
│       ├── api/             # Axios 请求封装 + 拦截器
│       ├── components/      # 布局组件（底部导航栏）
│       ├── router/          # Vue Router + 路由守卫
│       ├── stores/          # Pinia 状态管理
│       ├── views/           # 页面组件
│       │   ├── auth/        # 登录 / 注册
│       │   ├── consumer/    # 消费者页面
│       │   └── merchant/    # 商家页面
│       └── style.css        # 全局样式
├── docker/                  # Docker Compose 编排
│   ├── docker-compose.yml
│   └── mysql/ redis/ elasticsearch/ seata/  # 各中间件配置
└── docs/                    # 实施计划文档
```

## 页面一览（19 个）

| # | 页面 | 路由 | 角色 |
|---|------|------|------|
| 1 | 登录 | `/login` | 游客 |
| 2 | 注册 | `/register` | 游客 |
| 3 | 首页 | `/consumer/home` | 消费者 |
| 4 | 商家详情 | `/consumer/merchant/:id` | 消费者 |
| 5 | 结算页 | `/consumer/checkout/:merchantId` | 消费者 |
| 6 | 订单列表 | `/consumer/orders` | 消费者 |
| 7 | 订单详情 | `/consumer/order/:id` | 消费者 |
| 8 | 写评价 | `/consumer/order/:id/review` | 消费者 |
| 9 | 神券中心 | `/consumer/coupons` | 消费者 |
| 10 | 搜索 | `/consumer/search` | 消费者 |
| 11 | 个人中心 | `/consumer/profile` | 消费者 |
| 12 | 地址管理 | `/consumer/address` | 消费者 |
| 13 | 余额 | `/consumer/balance` | 消费者 |
| 14 | 商家订单管理 | `/merchant/orders` | 商家 |
| 15 | 商家订单详情 | `/merchant/order/:id` | 商家 |
| 16 | 商品管理 | `/merchant/products` | 商家 |
| 17 | 评价管理 | `/merchant/reviews` | 商家 |
| 18 | 收益统计 | `/merchant/earnings` | 商家 |
| 19 | 店铺设置 | `/merchant/shop` | 商家 |

## 设计规范

- **纯移动端**：固定 480px 宽度居中，不做响应式
- **美团配色**：主色 `#FFD101`（黄）、价格 `#FF6B35`（橙红）、背景 `#F5F5F5`
- **卡片式布局**：白色卡片 + 8px 圆角 + 轻微阴影
- **全局 CSS**：`style.css` 非 scoped，组件用 `<style scoped>`

---

## 本地开发

### 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 21+ |
| Node.js | 20+ |
| MySQL | 8.0 |
| Maven | 3.9+（或用 `./mvnw` wrapper） |

### 1. 准备数据库

确保 MySQL 运行在 `localhost:3306`，root 密码 `123456`。首次启动 JPA 会自动建表和插入测试数据。

或者只启动 Docker 中的 MySQL：

```bash
cd docker
docker compose up -d mysql
```

### 2. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

后端默认启动在 **8081** 端口，Swagger 文档：http://localhost:8081/swagger-ui.html

> 中间件（Redis / Elasticsearch / RabbitMQ）**默认不启用**，后端仅需 MySQL 即可运行。
> 如需启用中间件，通过 Docker 启动对应服务后，加 profile 参数：
> ```bash
> ./mvnw spring-boot:run -Dspring-boot.run.profiles=redis,rabbitmq,elasticsearch
> ```

### 3. 启动前端

```bash
cd frontend
npm install
npx vite --port 4000 --strictPort
```

前端启动后访问：http://localhost:4000

Vite 会自动将 `/api` 请求代理到 `localhost:9091`，如果后端跑在 8081，修改 `frontend/vite.config.ts` 中的 proxy target。

### 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 消费者 | consumer1 | 123456 |
| 商家 | merchant1 | 123456 |

---

## 核心特性

- **JWT 双 Token 认证**：Access Token 30 分钟 + Refresh Token 7 天，Axios 拦截器自动刷新
- **购物车**：Pinia + localStorage 持久化，按商家隔离（切换商家清空）
- **路由守卫**：解析 JWT exp 判过期，按角色自动跳转
- **飞入动画**：商品加购抛物线飞入购物车
- **骨架屏**：列表加载 shimmer 占位
- **搜索历史**：最近 8 条 localStorage 存储
- **订单进度条**：4 段可视化状态追踪
- **Chart.js 图表**：商家 7 天 / 30 天收益趋势

---

## Docker 完整部署

### 0. 配置环境变量

首次部署需要从模板创建 `.env` 文件：

```bash
cp .env.example .env
# 编辑 .env，将密码改为你自己的强密码（生产环境务必修改）
```

`.env.example` 中的占位值仅供本地开发使用，**生产部署请务必修改所有密码**。

### 1. 启动全部服务

```bash
cd docker
docker compose up -d
```

这会启动 9 个容器：MySQL（主 + 从）、Redis、Elasticsearch、RabbitMQ、MinIO、Seata Server、后端应用。

### 2. 查看服务状态

```bash
docker compose ps
```

所有服务的 State 应显示 `Up`。

### 3. 服务端口一览

| 服务 | 端口 | 管理界面 |
|------|------|---------|
| 后端 API | 8080 | Swagger: `/swagger-ui.html` |
| MySQL 主 | 3306 | — |
| MySQL 从 | 3307 | — |
| Redis | 6379 | — |
| Elasticsearch | 9200 | — |
| RabbitMQ | 5672 / 15672 | http://localhost:15672 (guest/guest) |
| MinIO | 9000 / 9001 | http://localhost:9001 (minioadmin/minioadmin) |
| Seata | 8091 | — |

---

## 云服务器部署

### 服务器要求

| 配置 | 最低 | 推荐 |
|------|------|------|
| 系统 | Ubuntu 22.04/24.04 LTS | 同左 |
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB（ES 吃内存） |
| 磁盘 | 40 GB | 60 GB+ |

### 1. 安装 Docker

```bash
curl -fsSL https://get.docker.com | bash
apt install -y docker-compose-v2
usermod -aG docker $USER
# 退出重新登录
```

### 2. 拉取项目并配置环境变量

```bash
git clone https://github.com/liujunkun1745/waimai-app.git /opt/waimai
cd /opt/waimai
cp .env.example .env
# 然后用 vim/nano 编辑 .env，务必改成强密码
```

### 3. 启动服务

```bash
cd /opt/waimai/docker
docker compose up -d
```

### 4. 安装 Nginx 反代

```bash
apt install -y nginx
```

创建 `/etc/nginx/sites-available/waimai`：

```nginx
server {
    listen 80;
    server_name _;

    # 前端静态页
    location / {
        root /opt/waimai/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 反代
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

启用配置：

```bash
ln -s /etc/nginx/sites-available/waimai /etc/nginx/sites-enabled/
rm /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx
```

### 5. 构建前端

```bash
cd /opt/waimai/frontend
npm install && npm run build
```

### 6. 阿里云安全组开放端口

| 端口 | 用途 |
|------|------|
| 80 | 前端页面 + API |
| 9001 | MinIO 控制台（建议仅开放自己 IP） |
| 15672 | RabbitMQ 管理（建议仅开放自己 IP） |

> 在阿里云控制台 → 安全组 → 入方向规则 中添加。

### 7. 验证

浏览器访问 `http://你的公网IP`，用测试账号 consumer1 / 123456 登录。

---

## License

MIT
