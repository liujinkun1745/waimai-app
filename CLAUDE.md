# CLAUDE.md — 外卖平台前后端分离升级

1. 使用中文。

2. 善用掌握的 skills。

3. 项目概述：
- **原项目** (`../外卖/`): Spring Boot + Thymeleaf + Bootstrap 5 全栈外卖平台，见原项目 CLAUDE.md
- **新项目** (当前目录): 前后端分离架构升级，详见 `docs/implementation-plan.md`

4. 新项目技术栈：
| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2 REST API + JWT (jjwt) + Spring Security 6 |
| 前端 | Vue 3 + Element Plus + Vite + Pinia + Axios |
| 数据库 | MySQL 8.0 + JPA/Hibernate |
| 缓存 | Redis 7 (购物车/优惠券/热门商家) |
| 搜索 | Elasticsearch 8 (商家搜索) |
| 消息队列 | RabbitMQ 3 (订单状态变更通知) |
| 实时推送 | WebSocket + STOMP |
| 对象存储 | MinIO (商品图片) |
| 分库分表 | ShardingSphere-JDBC 5 (订单表按 user_id) |
| 分布式事务 | Seata AT 模式 |
| 容器化 | Docker Compose |

5. 项目结构：
```
backend/     — Spring Boot REST API (JWT 认证, Swagger 文档)
frontend/    — Vue 3 + Element Plus (Vite 构建, Axios 请求)
docker/      — Docker Compose (MySQL + Redis + ES + RabbitMQ + MinIO + App)
docs/        — 实施计划文档
```

6. 实施计划（4 个阶段）：
- [ ] 计划1: 后端核心 — REST API + JWT + Swagger (Task 1-10)
- [ ] 计划2: 前端 — Vue 3 + Element Plus 全部页面 (Task 11-22)
- [ ] 计划3: 基础设施 — Redis + ES + RabbitMQ + WebSocket + MinIO + Docker (Task 23-30)
- [ ] 计划4: 数据架构 — ShardingSphere + 读写分离 + Seata (Task 31-34)

7. 规范（同原项目）：
- 编码前思考: 明确假设, 呈现权衡, 适时异议
- 简洁优先: 用最少代码解决问题, 不过度工程
- 精准修改: 只碰必须碰的, 匹配现有风格
- 目标驱动: 定义成功标准, 循环验证直到达成