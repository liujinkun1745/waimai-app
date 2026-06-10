# 🛵 美味外卖 — 仿美团外卖平台

前后端分离架构，Vue 3 + Element Plus 移动端 + Spring Boot 3.2 REST API。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus + Vite + Pinia + Axios + Chart.js |
| 后端 | Spring Boot 3.2 + Spring Security 6 + JWT (jjwt) |
| 数据库 | MySQL 8.0 + JPA/Hibernate |
| 缓存 | Redis 7（购物车 / 优惠券 / 热门商家） |
| 搜索 | Elasticsearch 8（商家搜索） |
| 消息队列 | RabbitMQ 3（订单状态变更通知） |
| 实时推送 | WebSocket + STOMP |
| 对象存储 | MinIO（商品图片） |
| 容器化 | Docker Compose |

## 项目结构

```
├── backend/                 # Spring Boot REST API
│   ├── src/main/java/com/waimai/
│   │   ├── config/          # Security / JWT / CORS / Swagger
│   │   ├── controller/      # REST 控制器
│   │   ├── dto/             # 请求/响应 DTO
│   │   ├── entity/          # JPA 实体
│   │   ├── repository/      # Spring Data 仓库
│   │   └── service/         # 业务逻辑
│   └── src/main/resources/  # 配置文件
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
└── docs/                    # 实施计划文档
```

## 页面一览（10 个）

| # | 页面 | 路由 | 角色 |
|---|------|------|------|
| 1 | 登录 | `/login` | 游客 |
| 2 | 首页 | `/consumer/home` | 消费者 |
| 3 | 商家详情 | `/consumer/merchant/:id` | 消费者 |
| 4 | 结算页 | `/consumer/checkout/:merchantId` | 消费者 |
| 5 | 订单列表 | `/consumer/orders` | 消费者 |
| 6 | 神券中心 | `/consumer/coupons` | 消费者 |
| 7 | 个人中心 | `/consumer/profile` | 消费者 |
| 8 | 商家订单管理 | `/merchant/orders` | 商家 |
| 9 | 商品管理 | `/merchant/products` | 商家 |
| 10 | 收益统计 | `/merchant/earnings` | 商家 |

## 设计规范

- **纯移动端**：固定 480px 宽度居中，不做响应式
- **美团配色**：主色 `#FFD101`（黄）、价格 `#FF6B35`（橙红）、背景 `#F5F5F5`
- **卡片式布局**：白色卡片 + 8px 圆角 + 轻微阴影
- **全局 CSS**：`style.css` 非 scoped，组件用 `<style scoped>`

## 快速开始

### 1. 启动基础设施（Docker）

```bash
cd docker
docker-compose up -d
```

### 2. 启动后端

```bash
cd backend
./mvnw spring-boot:run
# 或者
java -jar target/waimai-backend-2.0.0.jar
```

后端启动后 Swagger 文档：`http://localhost:9091/swagger-ui.html`

### 3. 启动前端

```bash
cd frontend
npm install
npx vite --port 4000 --strictPort
```

前端启动后访问：`http://localhost:4000`

### 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 消费者 | consumer1 | 123456 |
| 商家 | merchant1 | 123456 |

## 核心特性

- **JWT 认证**：Axios 拦截器自动附加 Token，过期自动刷新
- **购物车**：Pinia + localStorage 持久化，按商家隔离
- **飞入动画**：商品加购抛物线飞入购物车
- **骨架屏**：列表加载 shimmer 占位
- **搜索历史**：最近 8 条 localStorage 存储
- **订单进度条**：4 段可视化状态追踪
- **Chart.js 图表**：商家 7 天 / 30 天收益趋势

## License

MIT
