# 外卖平台前后端分离升级 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Spring Boot + Thymeleaf 全栈外卖平台升级为 Vue 3 + Spring Boot REST API + Redis + ES + MQ + Docker 的微服务架构

**Architecture:** 前端 Vue 3 + Element Plus (Vite)，后端 Spring Boot 3.2 REST API (JWT 认证)，中间件 Redis (缓存) + Elasticsearch (搜索) + RabbitMQ (消息队列) + MinIO (对象存储)，数据库 MySQL 8.0 (ShardingSphere 分库分表 + 读写分离)，全部容器化 Docker Compose 部署

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Security 6 + JWT, JPA/Hibernate, MySQL 8.0, Vue 3, Element Plus, Vite, Axios, Pinia, Redis 7, Elasticsearch 8, RabbitMQ 3, WebSocket, MinIO, ShardingSphere-JDBC 5, Seata 2, Docker Compose

---

## 项目结构概览

```
外卖前后端分离/
├── backend/                          # Spring Boot REST API
│   ├── pom.xml
│   └── src/main/java/com/waimai/
│       ├── WaimaiApplication.java
│       ├── config/
│       │   ├── SecurityConfig.java        # JWT + Spring Security
│       │   ├── JwtAuthFilter.java         # JWT 认证过滤器
│       │   ├── JwtUtil.java               # JWT Token 工具
│       │   ├── CorsConfig.java            # 跨域配置
│       │   ├── RedisConfig.java           # Redis 配置
│       │   ├── RabbitMQConfig.java        # RabbitMQ 配置
│       │   ├── WebSocketConfig.java       # WebSocket 配置
│       │   ├── ElasticsearchConfig.java   # ES 配置
│       │   ├── MinioConfig.java           # MinIO 配置
│       │   └── SwaggerConfig.java         # OpenAPI 文档
│       ├── controller/
│       │   ├── AuthController.java        # 登录/注册（REST JSON）
│       │   ├── ConsumerController.java    # 消费者端 REST API
│       │   ├── MerchantController.java    # 商家端 REST API
│       │   ├── UploadController.java      # 文件上传
│       │   └── WebSocketController.java   # WebSocket 推送
│       ├── dto/                            # 数据传输对象
│       │   ├── request/                   # 请求 DTO
│       │   └── response/                  # 响应 DTO
│       ├── entity/                         # JPA 实体（同原项目）
│       ├── repository/                     # JPA 仓库（同原项目）
│       ├── service/                        # 业务逻辑（同原项目）
│       ├── es/                             # Elasticsearch
│       │   ├── MerchantDocument.java      # 商家索引文档
│       │   └── MerchantSearchRepository.java
│       └── mq/                             # 消息队列
│           ├── OrderStatusProducer.java
│           └── OrderStatusConsumer.java
├── frontend/                          # Vue 3 + Element Plus
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── router/index.ts
│       ├── stores/                    # Pinia stores
│       │   ├── user.ts
│       │   ├── cart.ts
│       │   └── order.ts
│       ├── api/                       # Axios 请求封装
│       │   ├── request.ts            # Axios 实例 + 拦截器
│       │   ├── auth.ts
│       │   ├── merchant.ts
│       │   ├── product.ts
│       │   ├── order.ts
│       │   ├── review.ts
│       │   ├── coupon.ts
│       │   ├── address.ts
│       │   └── upload.ts
│       ├── views/                     # 页面组件
│       │   ├── auth/
│       │   │   ├── LoginView.vue
│       │   │   └── RegisterView.vue
│       │   ├── consumer/
│       │   │   ├── HomeView.vue       # 首页
│       │   │   ├── MerchantDetail.vue # 商家详情
│       │   │   ├── OrdersView.vue     # 订单列表
│       │   │   ├── OrderDetail.vue    # 订单详情
│       │   │   ├── ReviewView.vue     # 写评价
│       │   │   ├── CouponsView.vue    # 神券中心
│       │   │   ├── SearchView.vue     # 搜索
│       │   │   ├── ProfileView.vue    # 个人中心
│       │   │   ├── AddressView.vue    # 地址管理
│       │   │   └── BalanceView.vue    # 余额
│       │   └── merchant/
│       │       ├── DashboardView.vue  # 订单仪表盘
│       │       ├── OrderDetail.vue    # 订单详情
│       │       ├── ProductsView.vue   # 商品+分类管理
│       │       ├── ReviewsView.vue    # 评价管理
│       │       ├── EarningsView.vue   # 收益统计
│       │       └── ShopEdit.vue       # 店铺设置
│       ├── components/                # 通用组件
│       │   ├── AppLayout.vue          # 消费者布局
│       │   ├── MerchantLayout.vue     # 商家布局
│       │   ├── CartDrawer.vue         # 购物车抽屉
│       │   ├── CouponSelector.vue     # 优惠券选择器
│       │   └── OrderStatusSteps.vue   # 订单进度步骤条
│       └── utils/
│           └── websocket.ts           # WebSocket 客户端
├── docker/
│   ├── docker-compose.yml             # 一键部署
│   ├── mysql/
│   │   └── init.sql                   # 初始化 SQL
│   ├── redis/
│   │   └── redis.conf
│   └── elasticsearch/
│       └── elasticsearch.yml
└── docs/
    └── implementation-plan.md
```

---

## 阶段一：后端核心 — REST API + JWT + Swagger

### 关键技术决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 认证方案 | JWT (jjwt 0.12) | 无状态，适合前后端分离 |
| Token 刷新 | 双 Token (Access 30min + Refresh 7d) | 安全与体验平衡 |
| API 文档 | SpringDoc OpenAPI (Swagger UI) | 标准 REST API 文档 |
| 响应格式 | 统一 `Result<T>` 包装 | 前端统一处理 |
| 异常处理 | `@RestControllerAdvice` 全局异常 | 统一错误 JSON 响应 |
| 参数校验 | Jakarta Validation + Spring Validated | 声明式校验 |
| 密码加密 | BCrypt（保持不变） | 已有方案安全可靠 |
| ORM | JPA/Hibernate（保持不变） | 减少迁移成本 |
| 包结构 | 新增 `dto/` 包 | 前后端分离需要请求/响应 DTO |

### 数据库变更

```
无表结构变更，仅移除 Thymeleaf/Session 相关依赖，添加 JWT 库
```

### Task 1: 创建项目骨架

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/waimai/WaimaiApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: 创建 Maven 项目目录结构**

```bash
mkdir -p backend/src/main/java/com/waimai/{config,controller,dto/request,dto/response,entity,repository,service,es,mq}
mkdir -p backend/src/main/resources
mkdir -p backend/src/test/java/com/waimai
```

- [ ] **Step 2: 编写 pom.xml（含所有依赖）**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.waimai</groupId>
    <artifactId>waimai-backend</artifactId>
    <version>2.0.0</version>
    <packaging>jar</packaging>
    <name>waimai-backend</name>
    <description>外卖平台后端 REST API</description>

    <properties>
        <java.version>17</java.version>
        <jjwt.version>0.12.5</jjwt.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- SpringDoc OpenAPI (Swagger) -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.5.0</version>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- MySQL Driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.38</version>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Starter Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.38</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: 编写 application.yml**

```yaml
spring:
  application:
    name: waimai-backend

  datasource:
    url: jdbc:mysql://localhost:3306/waimai?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

server:
  port: 8080

# JWT 配置
jwt:
  secret: waimai-platform-jwt-secret-key-2024-must-be-at-least-256-bits-long
  access-token-expiration: 1800000     # 30 分钟
  refresh-token-expiration: 604800000  # 7 天

# Swagger
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha

logging:
  level:
    com.waimai: debug
    org.springframework.security: info
```

- [ ] **Step 4: 编写 WaimaiApplication.java 启动类**

```java
package com.waimai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WaimaiApplication {
    public static void main(String[] args) {
        SpringApplication.run(WaimaiApplication.class, args);
    }
}
```

- [ ] **Step 5: 验证编译**

```bash
cd backend && mvn clean compile -q
```
Expected: BUILD SUCCESS

---

### Task 2: 迁移实体类和仓库层

**Files:**
- Create: `backend/src/main/java/com/waimai/entity/User.java`
- Create: `backend/src/main/java/com/waimai/entity/Merchant.java`
- Create: `backend/src/main/java/com/waimai/entity/Category.java`
- Create: `backend/src/main/java/com/waimai/entity/Product.java`
- Create: `backend/src/main/java/com/waimai/entity/Order.java`
- Create: `backend/src/main/java/com/waimai/entity/OrderItem.java`
- Create: `backend/src/main/java/com/waimai/entity/Review.java`
- Create: `backend/src/main/java/com/waimai/entity/Coupon.java`
- Create: `backend/src/main/java/com/waimai/entity/Address.java`
- Create: `backend/src/main/java/com/waimai/entity/BalanceRecord.java`

- [ ] **Step 1: 从原项目复制所有 entity 文件**

所有实体类从 `d:/code/html5/外卖/src/main/java/com/waimai/entity/` 复制到 `backend/src/main/java/com/waimai/entity/`，包名不变，代码不变。

- [ ] **Step 2: 从原项目复制所有 repository 文件**

所有仓库接口从 `d:/code/html5/外卖/src/main/java/com/waimai/repository/` 复制到 `backend/src/main/java/com/waimai/repository/`，代码不变。

- [ ] **Step 3: 验证编译**

```bash
cd backend && mvn clean compile -q
```
Expected: BUILD SUCCESS

---

### Task 3: 创建统一响应和全局异常处理

**Files:**
- Create: `backend/src/main/java/com/waimai/dto/response/Result.java`
- Create: `backend/src/main/java/com/waimai/dto/response/ResultCode.java`
- Create: `backend/src/main/java/com/waimai/exception/BusinessException.java`
- Create: `backend/src/main/java/com/waimai/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: 创建 ResultCode 枚举**

```java
package com.waimai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    BUSINESS_ERROR(1000, "业务异常");

    private final int code;
    private final String message;
}
```

- [ ] **Step 2: 创建统一响应类 Result<T>**

```java
package com.waimai.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(ResultCode code) {
        return new Result<>(code.getCode(), code.getMessage(), null);
    }

    public static <T> Result<T> error(ResultCode code, String message) {
        return new Result<>(code.getCode(), message, null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
```

- [ ] **Step 3: 创建 BusinessException**

```java
package com.waimai.exception;

import com.waimai.dto.response.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(ResultCode code) {
        super(code.getMessage());
        this.code = code.getCode();
    }

    public BusinessException(ResultCode code, String message) {
        super(message);
        this.code = code.getCode();
    }
}
```

- [ ] **Step 4: 创建 GlobalExceptionHandler**

```java
package com.waimai.exception;

import com.waimai.dto.response.Result;
import com.waimai.dto.response.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("参数校验失败");
        return Result.error(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        return Result.error(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(ResultCode.INTERNAL_ERROR);
    }
}
```

- [ ] **Step 5: 验证编译**

```bash
cd backend && mvn clean compile -q
```
Expected: BUILD SUCCESS

---

### Task 4: JWT 认证体系

**Files:**
- Create: `backend/src/main/java/com/waimai/config/JwtUtil.java`
- Create: `backend/src/main/java/com/waimai/config/JwtAuthFilter.java`
- Create: `backend/src/main/java/com/waimai/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/waimai/config/CorsConfig.java`

- [ ] **Step 1: 创建 JwtUtil 工具类**

```java
package com.waimai.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(secret.getBytes())));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /** 生成 Access Token */
    public String generateAccessToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    /** 生成 Refresh Token */
    public String generateRefreshToken(Long userId, String username) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    /** 解析 Token */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 验证 Token 是否有效 */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** 从 Token 获取用户名 */
    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    /** 从 Token 获取用户 ID */
    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    /** 从 Token 获取角色 */
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }
}
```

- [ ] **Step 2: 创建 JwtAuthFilter 过滤器**

```java
package com.waimai.config;

import com.waimai.entity.User;
import com.waimai.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            String username = jwtUtil.getUsername(token);
            User user = userRepository.findByUsername(username).orElse(null);

            if (user != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user, null,
                                List.of(new SimpleGrantedAuthority(user.getRole())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 3: 重写 SecurityConfig（JWT + 无 Session）**

```java
package com.waimai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers("/api/auth/**").permitAll()
                // Swagger
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                // 文件访问
                .requestMatchers("/uploads/**").permitAll()
                // 消费者端
                .requestMatchers("/api/consumer/**").hasRole("CONSUMER")
                // 商家端
                .requestMatchers("/api/merchant/**").hasRole("MERCHANT")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- [ ] **Step 4: 创建 CorsConfig**

```java
package com.waimai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 5: 创建 SwaggerConfig**

```java
package com.waimai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("外卖平台 API")
                        .version("2.0.0")
                        .description("前后端分离外卖平台 REST API 文档"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .name("Bearer")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
```

- [ ] **Step 6: 验证编译和启动**

```bash
cd backend && mvn clean compile -q
```
Expected: BUILD SUCCESS

---

### Task 5: 迁移 Service 层（改为抛 BusinessException）

**Files:**
- Create: `backend/src/main/java/com/waimai/service/UserService.java`
- Create: `backend/src/main/java/com/waimai/service/MerchantService.java`
- Create: `backend/src/main/java/com/waimai/service/CategoryService.java`
- Create: `backend/src/main/java/com/waimai/service/ProductService.java`
- Create: `backend/src/main/java/com/waimai/service/OrderService.java`
- Create: `backend/src/main/java/com/waimai/service/ReviewService.java`
- Create: `backend/src/main/java/com/waimai/service/CouponService.java`
- Create: `backend/src/main/java/com/waimai/service/AddressService.java`
- Create: `backend/src/main/java/com/waimai/service/BalanceService.java`
- Create: `backend/src/main/java/com/waimai/service/CustomUserDetailsService.java`

- [ ] **Step 1: 复制所有 Service 类并修改异常抛出方式**

将所有原项目 Service 从 `d:/code/html5/外卖/src/main/java/com/waimai/service/` 复制到 `backend/src/main/java/com/waimai/service/`。

**关键修改：将所有 `throw new RuntimeException(...)` 替换为 `throw new BusinessException(...)`**

示例 — UserService 中：
```java
// 改前
throw new RuntimeException("用户名已存在");

// 改后
throw new BusinessException("用户名已存在");
```

在所有 Service 文件头部添加 import：
```java
import com.waimai.exception.BusinessException;
```

`CustomUserDetailsService.java` 保持不变（Spring Security 需要它来加载用户）：
```java
package com.waimai.service;

import com.waimai.entity.User;
import com.waimai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().replace("ROLE_", ""))
                .build();
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd backend && mvn clean compile -q
```
Expected: BUILD SUCCESS

---

### Task 6: 创建请求 DTO

**Files:**
- Create: `backend/src/main/java/com/waimai/dto/request/LoginRequest.java`
- Create: `backend/src/main/java/com/waimai/dto/request/RegisterConsumerRequest.java`
- Create: `backend/src/main/java/com/waimai/dto/request/RegisterMerchantRequest.java`
- Create: `backend/src/main/java/com/waimai/dto/request/SubmitOrderRequest.java`
- Create: `backend/src/main/java/com/waimai/dto/request/SubmitReviewRequest.java`
- Create: `backend/src/main/java/com/waimai/dto/request/UpdateAddressRequest.java`
- Create: `backend/src/main/java/com/waimai/dto/request/UpdateShopRequest.java`
- Create: `backend/src/main/java/com/waimai/dto/request/AddProductRequest.java`
- Create: `backend/src/main/java/com/waimai/dto/request/ClaimCouponRequest.java`
- Create: `backend/src/main/java/com/waimai/dto/request/RechargeRequest.java`
- Create: `backend/src/main/java/com/waimai/dto/request/ChangePasswordRequest.java`

- [ ] **Step 1: 创建 LoginRequest**

```java
package com.waimai.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 2: 创建 RegisterConsumerRequest**

```java
package com.waimai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterConsumerRequest {
    @NotBlank @Size(min = 3, max = 50)
    private String username;

    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank @Size(min = 6, max = 50)
    private String password;

    private String email;
}
```

- [ ] **Step 3: 创建 RegisterMerchantRequest**

```java
package com.waimai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterMerchantRequest {
    @NotBlank @Size(min = 3, max = 50)
    private String username;

    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank @Size(min = 6, max = 50)
    private String password;

    @NotBlank @Size(max = 100)
    private String shopName;

    @NotBlank @Size(max = 255)
    private String shopAddress;

    @NotBlank @Size(max = 50)
    private String businessLicense;

    private String description;
}
```

- [ ] **Step 4: 创建 SubmitOrderRequest**

```java
package com.waimai.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SubmitOrderRequest {
    @NotNull
    private Long merchantId;

    @NotNull
    private Long addressId;

    @NotEmpty
    private List<CartItemRequest> items;

    @NotNull
    private BigDecimal totalAmount;

    private Long couponId;  // 可选

    @Data
    public static class CartItemRequest {
        @NotNull
        private Long productId;
        @NotNull
        private Integer quantity;
    }
}
```

- [ ] **Step 5: 创建 SubmitReviewRequest**

```java
package com.waimai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitReviewRequest {
    @NotNull @Min(1) @Max(5)
    private Integer tasteRating;

    @NotNull @Min(1) @Max(5)
    private Integer packagingRating;

    @NotNull @Min(1) @Max(5)
    private Integer deliveryRating;

    private String comment;
}
```

- [ ] **Step 6: 创建 UpdateAddressRequest**

```java
package com.waimai.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAddressRequest {
    @NotBlank
    private String receiverName;

    @NotBlank
    private String receiverPhone;

    private String province;
    private String city;
    private String district;

    @NotBlank
    private String detailAddress;

    private Boolean isDefault;
}
```

- [ ] **Step 7: 创建 UpdateShopRequest**

```java
package com.waimai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateShopRequest {
    @NotBlank
    private String shopName;

    private String shopAvatar;

    @NotBlank
    private String description;

    @NotBlank
    private String businessHours;

    @NotNull
    private BigDecimal deliveryFee;

    @NotNull
    private BigDecimal minOrderAmount;
}
```

- [ ] **Step 8: 创建 AddProductRequest**

```java
package com.waimai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddProductRequest {
    @NotNull
    private Long categoryId;

    @NotBlank
    private String name;

    @NotNull
    private BigDecimal price;

    @NotNull
    private Integer stock;

    private String image;
    private String description;
}
```

- [ ] **Step 9: 创建 ClaimCouponRequest**

```java
package com.waimai.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClaimCouponRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String amount;

    @NotBlank
    private String minOrder;
}
```

- [ ] **Step 10: 创建 RechargeRequest**

```java
package com.waimai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RechargeRequest {
    @NotNull
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    private BigDecimal amount;
}
```

- [ ] **Step 11: 创建 ChangePasswordRequest**

```java
package com.waimai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    private String oldPassword;

    @NotBlank @Size(min = 6, max = 50)
    private String newPassword;
}
```

- [ ] **Step 12: 验证编译**

```bash
cd backend && mvn clean compile -q
```
Expected: BUILD SUCCESS

---

### Task 7: 重写 AuthController（REST JSON）

**Files:**
- Create: `backend/src/main/java/com/waimai/controller/AuthController.java`
- Modify: 移除 `backend/src/main/java/com/waimai/controller/CustomErrorController.java` (如果存在)

- [ ] **Step 1: 创建 AuthController（REST 风格）**

```java
package com.waimai.controller;

import com.waimai.config.JwtUtil;
import com.waimai.dto.request.*;
import com.waimai.dto.response.Result;
import com.waimai.entity.User;
import com.waimai.service.UserService;
import com.waimai.service.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、注册、Token 刷新")
public class AuthController {

    private final UserService userService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        // Spring Security 认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        Map<String, Object> data = Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "userId", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole(),
                "phone", user.getPhone()
        );
        return Result.success(data);
    }

    @PostMapping("/register/consumer")
    @Operation(summary = "消费者注册")
    public Result<Void> registerConsumer(@Valid @RequestBody RegisterConsumerRequest request) {
        userService.registerConsumer(
                request.getUsername(),
                request.getPhone(),
                request.getPassword(),
                request.getEmail() != null ? request.getEmail() : "");
        return Result.success();
    }

    @PostMapping("/register/merchant")
    @Operation(summary = "商家注册")
    public Result<Void> registerMerchant(@Valid @RequestBody RegisterMerchantRequest request) {
        userService.registerMerchant(
                request.getUsername(),
                request.getPhone(),
                request.getPassword(),
                request.getShopName(),
                request.getShopAddress(),
                request.getBusinessLicense(),
                request.getDescription() != null ? request.getDescription() : "");
        return Result.success();
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token")
    public Result<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String bearerToken) {
        String refreshToken = bearerToken.replace("Bearer ", "");
        if (!jwtUtil.validateToken(refreshToken)) {
            return Result.error(401, "Token 已过期，请重新登录");
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userService.findById(userId);

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        return Result.success(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        ));
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd backend && mvn clean compile -q
```
Expected: BUILD SUCCESS

---

### Task 8: 重写 ConsumerController（REST JSON）

**Files:**
- Create: `backend/src/main/java/com/waimai/controller/ConsumerController.java`

- [ ] **Step 1: 创建 ConsumerController（完整 REST API）**

```java
package com.waimai.controller;

import com.waimai.config.JwtUtil;
import com.waimai.dto.request.*;
import com.waimai.dto.response.Result;
import com.waimai.entity.*;
import com.waimai.service.*;
import com.waimai.service.OrderService.CartItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/consumer")
@RequiredArgsConstructor
@Tag(name = "消费者端", description = "首页浏览、下单、评价、优惠券等")
public class ConsumerController {

    private static final List<String> DAILY_COUPON_NAMES = List.of(
        "满50减20","满30减10","满20减5","满100减30","满15减3","无门槛红包","满40减15","满60减25");

    private final UserService userService;
    private final MerchantService merchantService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final AddressService addressService;
    private final OrderService orderService;
    private final BalanceService balanceService;
    private final ReviewService reviewService;
    private final CouponService couponService;
    private final JwtUtil jwtUtil;

    /** 从 JWT 获取当前用户 */
    private User currentUser(String token) {
        String username = jwtUtil.getUsername(token);
        return userService.findByUsername(username);
    }

    // ========== 首页 ==========

    @GetMapping("/index")
    @Operation(summary = "首页 — 商家列表")
    public Result<Map<String, Object>> index(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "sales") String sort) {
        User user = currentUser(token);
        List<Merchant> merchants;
        if ("rating".equals(sort)) {
            merchants = merchantService.listByRating();
        } else {
            merchants = merchantService.listBySales();
        }
        if (keyword != null && !keyword.isBlank()) {
            merchants = merchantService.searchOpenMerchants(keyword);
        }

        // 计算月销量
        Map<Long, Integer> monthlyOrderCounts = new HashMap<>();
        java.time.LocalDate firstOfMonth = java.time.LocalDate.now().withDayOfMonth(1);
        for (Merchant m : merchants) {
            List<Order> orders = orderService.listByMerchant(m.getId(), null);
            long count = orders.stream()
                    .filter(o -> "已完成".equals(o.getStatus())
                            && !o.getCreatedAt().toLocalDate().isBefore(firstOfMonth))
                    .count();
            monthlyOrderCounts.put(m.getId(), (int) count);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("merchants", merchants);
        data.put("monthlyOrderCounts", monthlyOrderCounts);
        data.put("couponCount", couponService.countAvailable(user.getId()));
        return Result.success(data);
    }

    // ========== 商家详情 ==========

    @GetMapping("/merchant/{id}")
    @Operation(summary = "商家详情")
    public Result<Map<String, Object>> merchantDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        User user = currentUser(token);
        Merchant merchant = merchantService.findById(id);
        List<Category> categories = categoryService.listByMerchant(id);
        List<Product> products = new ArrayList<>();
        Long firstCategoryId = null;
        if (!categories.isEmpty()) {
            firstCategoryId = categories.get(0).getId();
            products = productService.listByCategory(id, firstCategoryId);
        }
        List<Review> reviews = reviewService.listByMerchant(id);
        long reviewCount = reviewService.countByMerchant(id);

        int[] ratingDist = new int[5];
        for (Review r : reviews) {
            int overall = (int) Math.round(r.getOverallRating());
            if (overall >= 5) ratingDist[0]++;
            else if (overall >= 4) ratingDist[1]++;
            else if (overall >= 3) ratingDist[2]++;
            else if (overall >= 2) ratingDist[3]++;
            else ratingDist[4]++;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("merchant", merchant);
        data.put("categories", categories);
        data.put("products", products);
        data.put("currentCategoryId", firstCategoryId);
        data.put("reviews", reviews);
        data.put("reviewCount", reviewCount);
        data.put("ratingDist", ratingDist);
        data.put("couponCount", couponService.countAvailable(user.getId()));
        return Result.success(data);
    }

    @GetMapping("/merchant/{merchantId}/category/{categoryId}")
    @Operation(summary = "按分类加载商品")
    public Result<List<Product>> loadProducts(
            @PathVariable Long merchantId, @PathVariable Long categoryId) {
        return Result.success(productService.listByCategory(merchantId, categoryId));
    }

    // ========== 订单 ==========

    @GetMapping("/orders")
    @Operation(summary = "订单列表")
    public Result<Map<String, Object>> orders(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status) {
        User user = currentUser(token);
        List<Order> orders = orderService.listByConsumer(user.getId(), status);
        Map<Long, List<OrderItem>> itemsMap =
                orderService.getOrderItemsBatch(orders.stream().map(Order::getId).toList());
        Map<Long, Boolean> reviewedMap = new HashMap<>();
        for (Order o : orders) {
            reviewedMap.put(o.getId(), reviewService.existsByOrder(o.getId()));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("orders", orders);
        data.put("itemsMap", itemsMap);
        data.put("reviewedMap", reviewedMap);
        return Result.success(data);
    }

    @GetMapping("/order/{id}")
    @Operation(summary = "订单详情")
    public Result<Map<String, Object>> orderDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        User user = currentUser(token);
        Order order = orderService.findById(id);
        List<OrderItem> items = orderService.getOrderItems(id);
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);
        data.put("reviewed", reviewService.existsByOrder(id));
        return Result.success(data);
    }

    @PostMapping("/order/cancel/{id}")
    @Operation(summary = "取消订单")
    public Result<Void> cancelOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        User user = currentUser(token);
        orderService.cancelByConsumer(id, user.getId());
        return Result.success();
    }

    @PostMapping("/order/confirm/{id}")
    @Operation(summary = "确认收货")
    public Result<Void> confirmReceived(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        User user = currentUser(token);
        orderService.confirmReceived(id, user.getId());
        return Result.success();
    }

    @PostMapping("/order/submit")
    @Operation(summary = "提交订单")
    public Result<Void> submitOrder(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody SubmitOrderRequest request) {
        User user = currentUser(token);
        List<CartItem> cartItems = request.getItems().stream().map(item -> {
            Product product = productService.findById(item.getProductId());
            return new CartItem(product.getId(), product.getName(),
                    product.getImage(), product.getPrice(), item.getQuantity());
        }).toList();

        BigDecimal finalAmount = request.getTotalAmount();
        if (request.getCouponId() != null && request.getCouponId() > 0) {
            var couponOpt = java.util.Optional.empty();
            // 由 CouponService 处理
            couponService.useCoupon(request.getCouponId(), user.getId());
        }
        orderService.submitOrder(user.getId(), request.getMerchantId(),
                request.getAddressId(), cartItems, finalAmount);
        return Result.success();
    }

    // ========== 评价 ==========

    @PostMapping("/order/{id}/review")
    @Operation(summary = "提交评价")
    public Result<Void> submitReview(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @Valid @RequestBody SubmitReviewRequest request) {
        User user = currentUser(token);
        reviewService.submit(id, user.getId(), request.getTasteRating(),
                request.getPackagingRating(), request.getDeliveryRating(),
                request.getComment());
        return Result.success();
    }

    // ========== 地址管理 ==========

    @GetMapping("/address")
    @Operation(summary = "地址列表")
    public Result<List<Address>> addressList(@RequestHeader("Authorization") String token) {
        User user = currentUser(token);
        return Result.success(addressService.listByConsumer(user.getId()));
    }

    @PostMapping("/address/add")
    @Operation(summary = "添加地址")
    public Result<Void> addAddress(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateAddressRequest request) {
        User user = currentUser(token);
        addressService.add(user.getId(), request.getReceiverName(),
                request.getReceiverPhone(), request.getProvince(), request.getCity(),
                request.getDistrict(), request.getDetailAddress(), request.getIsDefault());
        return Result.success();
    }

    @PutMapping("/address/{id}")
    @Operation(summary = "编辑地址")
    public Result<Void> editAddress(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddressRequest request) {
        User user = currentUser(token);
        addressService.update(id, user.getId(), request.getReceiverName(),
                request.getReceiverPhone(), request.getProvince(), request.getCity(),
                request.getDistrict(), request.getDetailAddress(), request.getIsDefault());
        return Result.success();
    }

    @DeleteMapping("/address/{id}")
    @Operation(summary = "删除地址")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        addressService.delete(id);
        return Result.success();
    }

    // ========== 优惠券 ==========

    @GetMapping("/coupons")
    @Operation(summary = "可用优惠券列表")
    public Result<List<Map<String, Object>>> coupons(@RequestHeader("Authorization") String token) {
        User user = currentUser(token);
        List<Map<String, Object>> list = couponService.getAvailable(user.getId()).stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("amount", c.getAmount());
            m.put("minOrder", c.getMinOrder());
            return m;
        }).toList();
        return Result.success(list);
    }

    @GetMapping("/coupons/daily")
    @Operation(summary = "每日神券")
    public Result<Map<String, Object>> dailyCoupons(@RequestHeader("Authorization") String token) {
        User user = currentUser(token);
        java.time.LocalDate today = java.time.LocalDate.now();
        String[][] templates = {{"满50减20","20.00","50.00"},{"满30减10","10.00","30.00"},
                {"满20减5","5.00","20.00"},{"满100减30","30.00","100.00"},
                {"满15减3","3.00","15.00"},{"无门槛红包","6.00","0"},
                {"满40减15","15.00","40.00"},{"满60减25","25.00","60.00"}};
        long seed = today.toEpochDay() * 1000 + user.getId();
        Random rand = new Random(seed);
        List<Map<String, Object>> picks = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            int idx;
            do { idx = rand.nextInt(templates.length); } while (used.contains(idx));
            used.add(idx);
            Map<String, Object> m = new HashMap<>();
            m.put("name", templates[idx][0]);
            m.put("amount", templates[idx][1]);
            m.put("minOrder", templates[idx][2]);
            // 使用 repository 检查是否已领取
            boolean thisClaimed = false;
            for (var c : couponService.getAvailable(user.getId())) {
                if (c.getCreatedAt() != null && c.getCreatedAt().toLocalDate().equals(today)
                        && c.getName().equals(templates[idx][0])) {
                    thisClaimed = true;
                    break;
                }
            }
            m.put("claimed", thisClaimed);
            picks.add(m);
        }
        return Result.success(Map.of("coupons", picks));
    }

    @PostMapping("/coupons/claim")
    @Operation(summary = "领取神券")
    public Result<Map<String, Object>> claimCoupon(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ClaimCouponRequest request) {
        User user = currentUser(token);
        java.time.LocalDate today = java.time.LocalDate.now();
        var allCoupons = couponService.getAvailable(user.getId());
        boolean claimedToday = allCoupons.stream()
                .anyMatch(c -> c.getCreatedAt() != null
                        && c.getCreatedAt().toLocalDate().equals(today)
                        && DAILY_COUPON_NAMES.contains(c.getName()));
        if (claimedToday) {
            return Result.error(1000, "今日已领取过神券，明天再来吧！");
        }

        // 创建优惠券（此处需要 CouponRepository，通过 CouponService 暴露方法）
        couponService.createCoupon(user.getId(), request.getName(),
                new BigDecimal(request.getAmount()), new BigDecimal(request.getMinOrder()));

        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("count", couponService.countAvailable(user.getId()));
        return Result.success(result);
    }

    // ========== 个人中心 ==========

    @GetMapping("/profile")
    @Operation(summary = "获取个人信息")
    public Result<User> profile(@RequestHeader("Authorization") String token) {
        return Result.success(currentUser(token));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人信息")
    public Result<Void> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {
        User user = currentUser(token);
        userService.updateProfile(user.getId(), body.get("email"));
        return Result.success();
    }

    @PutMapping("/profile/password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangePasswordRequest request) {
        User user = currentUser(token);
        userService.changePassword(user.getId(), request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    // ========== 余额 ==========

    @GetMapping("/balance")
    @Operation(summary = "余额和记录")
    public Result<Map<String, Object>> balance(@RequestHeader("Authorization") String token) {
        User user = currentUser(token);
        List<BalanceRecord> records = balanceService.listRecords(user.getId());
        return Result.success(Map.of("balance", user.getBalance(), "records", records));
    }

    @PostMapping("/balance/recharge")
    @Operation(summary = "充值")
    public Result<Void> recharge(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody RechargeRequest request) {
        User user = currentUser(token);
        userService.recharge(user.getId(), request.getAmount());
        return Result.success();
    }

    // ========== 搜索 ==========

    @GetMapping("/search")
    @Operation(summary = "商家搜索")
    public Result<Map<String, Object>> search(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String keyword) {
        User user = currentUser(token);
        Map<String, Object> data = new HashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            data.put("merchants", merchantService.searchOpenMerchants(keyword));
            data.put("keyword", keyword);
        } else {
            data.put("merchants", merchantService.listBySales());
        }
        return Result.success(data);
    }
}
```

- [ ] **Step 2: 在 CouponService 中添加 createCoupon 方法**

由于以上 ConsumerController 调用了 `couponService.createCoupon()`，需要补充：

```java
// 在 CouponService.java 中添加
@Transactional
public Coupon createCoupon(Long userId, String name, BigDecimal amount, BigDecimal minOrder) {
    User user = userRepository.getReferenceById(userId);
    Coupon coupon = Coupon.builder()
            .user(user)
            .name(name)
            .amount(amount)
            .minOrder(minOrder)
            .used(false)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();
    return couponRepository.save(coupon);
}
```

同时需要在 CouponService 中注入 UserRepository：
```java
private final UserRepository userRepository;
```

- [ ] **Step 3: 验证编译**

```bash
cd backend && mvn clean compile -q
```
Expected: BUILD SUCCESS

---

### Task 9: 重写 MerchantController（REST JSON）

**Files:**
- Create: `backend/src/main/java/com/waimai/controller/MerchantController.java`

- [ ] **Step 1: 创建 MerchantController（完整 REST API）**

```java
package com.waimai.controller;

import com.waimai.config.JwtUtil;
import com.waimai.dto.request.*;
import com.waimai.dto.response.Result;
import com.waimai.entity.*;
import com.waimai.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
@Tag(name = "商家端", description = "订单管理、商品管理、评价回复、收益统计")
public class MerchantController {

    private final UserService userService;
    private final MerchantService merchantService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final OrderService orderService;
    private final ReviewService reviewService;
    private final JwtUtil jwtUtil;

    private Merchant currentMerchant(String token) {
        String username = jwtUtil.getUsername(token);
        User user = userService.findByUsername(username);
        return merchantService.findByUserId(user.getId());
    }

    // ========== 店铺管理 ==========

    @GetMapping("/shop")
    @Operation(summary = "获取店铺信息")
    public Result<Merchant> shopInfo(@RequestHeader("Authorization") String token) {
        return Result.success(currentMerchant(token));
    }

    @PutMapping("/shop")
    @Operation(summary = "更新店铺信息")
    public Result<Void> updateShop(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateShopRequest request) {
        Merchant merchant = currentMerchant(token);
        merchantService.updateShopInfo(merchant.getId(), request.getShopName(),
                request.getShopAvatar(), request.getDescription(),
                request.getBusinessHours(), request.getDeliveryFee(),
                request.getMinOrderAmount());
        return Result.success();
    }

    @PostMapping("/shop/toggle-status")
    @Operation(summary = "切换营业状态")
    public Result<Void> toggleStatus(@RequestHeader("Authorization") String token) {
        Merchant merchant = currentMerchant(token);
        merchantService.toggleStatus(merchant.getId());
        return Result.success();
    }

    // ========== 商品+分类管理 ==========

    @GetMapping("/products")
    @Operation(summary = "商品+分类列表")
    public Result<Map<String, Object>> productsAll(@RequestHeader("Authorization") String token) {
        Merchant merchant = currentMerchant(token);
        List<Product> products = productService.listAll(merchant.getId());
        List<Category> categories = categoryService.listByMerchant(merchant.getId());
        Map<Long, Long> categoryProductCounts = new HashMap<>();
        for (Product p : products) {
            categoryProductCounts.merge(p.getCategory().getId(), 1L, Long::sum);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("merchant", merchant);
        data.put("products", products);
        data.put("categories", categories);
        data.put("categoryProductCounts", categoryProductCounts);
        return Result.success(data);
    }

    @PostMapping("/product/move-category")
    @Operation(summary = "移动商品分类")
    public Result<Void> moveProduct(
            @RequestParam Long productId, @RequestParam Long categoryId) {
        productService.moveCategory(productId, categoryId);
        return Result.success();
    }

    @PostMapping("/product")
    @Operation(summary = "添加商品")
    public Result<Void> addProduct(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AddProductRequest request) {
        Merchant merchant = currentMerchant(token);
        productService.add(merchant.getId(), request.getCategoryId(),
                request.getName(), request.getPrice(), request.getStock(),
                request.getImage() != null ? request.getImage() : "/images/food1.svg",
                request.getDescription() != null ? request.getDescription() : "");
        return Result.success();
    }

    @PutMapping("/product/{id}")
    @Operation(summary = "编辑商品")
    public Result<Void> editProduct(
            @PathVariable Long id,
            @Valid @RequestBody AddProductRequest request) {
        productService.update(id, request.getCategoryId(), request.getName(),
                request.getPrice(), request.getStock(),
                request.getImage(), request.getDescription());
        return Result.success();
    }

    @PostMapping("/product/toggle/{id}")
    @Operation(summary = "商品上下架")
    public Result<Void> toggleProduct(@PathVariable Long id) {
        productService.toggleStatus(id);
        return Result.success();
    }

    @DeleteMapping("/product/{id}")
    @Operation(summary = "删除商品")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }

    // ========== 分类管理 ==========

    @PostMapping("/category")
    @Operation(summary = "添加分类")
    public Result<Void> addCategory(
            @RequestHeader("Authorization") String token,
            @RequestParam String name,
            @RequestParam Integer sortOrder) {
        Merchant merchant = currentMerchant(token);
        categoryService.add(merchant.getId(), name, sortOrder);
        return Result.success();
    }

    @PutMapping("/category/{id}")
    @Operation(summary = "编辑分类")
    public Result<Void> editCategory(
            @PathVariable Long id, @RequestParam String name, @RequestParam Integer sortOrder) {
        categoryService.update(id, name, sortOrder);
        return Result.success();
    }

    @DeleteMapping("/category/{id}")
    @Operation(summary = "删除分类")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }

    // ========== 订单管理 ==========

    @GetMapping("/orders")
    @Operation(summary = "订单列表+仪表盘")
    public Result<Map<String, Object>> orders(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status) {
        Merchant merchant = currentMerchant(token);
        List<Order> orders = orderService.listByMerchant(merchant.getId(), status);
        Map<Long, List<OrderItem>> itemsMap =
                orderService.getOrderItemsBatch(orders.stream().map(Order::getId).toList());

        // 仪表盘
        List<Order> allOrders = orderService.listByMerchant(merchant.getId(), null);
        LocalDate today = LocalDate.now();
        long pendingCount = allOrders.stream().filter(o -> "待接单".equals(o.getStatus())).count();
        long todayCount = allOrders.stream().filter(o -> o.getCreatedAt().toLocalDate().equals(today)).count();
        BigDecimal todayEarnings = allOrders.stream()
                .filter(o -> "已完成".equals(o.getStatus()) && o.getCreatedAt().toLocalDate().equals(today))
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new HashMap<>();
        data.put("merchant", merchant);
        data.put("orders", orders);
        data.put("itemsMap", itemsMap);
        data.put("pendingCount", pendingCount);
        data.put("todayCount", todayCount);
        data.put("todayEarnings", todayEarnings);
        return Result.success(data);
    }

    @GetMapping("/order/{id}")
    @Operation(summary = "订单详情")
    public Result<Map<String, Object>> orderDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        Merchant merchant = currentMerchant(token);
        Order order = orderService.findById(id);
        List<OrderItem> items = orderService.getOrderItems(id);
        return Result.success(Map.of("merchant", merchant, "order", order, "items", items));
    }

    @PostMapping("/order/accept/{id}")
    @Operation(summary = "接单")
    public Result<Void> acceptOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        Merchant merchant = currentMerchant(token);
        orderService.acceptOrder(id, merchant.getId());
        return Result.success();
    }

    @PostMapping("/order/deliver/{id}")
    @Operation(summary = "开始配送")
    public Result<Void> startDelivery(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        Merchant merchant = currentMerchant(token);
        orderService.startDelivery(id, merchant.getId());
        return Result.success();
    }

    @PostMapping("/order/complete/{id}")
    @Operation(summary = "完成订单")
    public Result<Void> completeOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        Merchant merchant = currentMerchant(token);
        orderService.completeOrder(id, merchant.getId());
        return Result.success();
    }

    // ========== 评价管理 ==========

    @GetMapping("/reviews")
    @Operation(summary = "评价列表+分布")
    public Result<Map<String, Object>> reviews(@RequestHeader("Authorization") String token) {
        Merchant merchant = currentMerchant(token);
        List<Review> reviews = reviewService.listByMerchant(merchant.getId());
        int[] ratingDist = new int[6];
        Map<Long, Integer> roundedStars = new HashMap<>();
        for (Review r : reviews) {
            int star = (int) Math.round(r.getOverallRating());
            if (star >= 1 && star <= 5) ratingDist[star]++;
            roundedStars.put(r.getId(), star);
        }
        return Result.success(Map.of(
                "merchant", merchant,
                "reviews", reviews,
                "ratingDist", ratingDist,
                "roundedStars", roundedStars));
    }

    @PostMapping("/review/reply/{id}")
    @Operation(summary = "回复评价")
    public Result<Void> replyReview(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reviewService.reply(id, body.get("reply"));
        return Result.success();
    }

    // ========== 收益统计 ==========

    @GetMapping("/earnings")
    @Operation(summary = "收益统计数据")
    public Result<Map<String, Object>> earnings(@RequestHeader("Authorization") String token) {
        Merchant merchant = currentMerchant(token);
        List<Order> allOrders = orderService.listByMerchant(merchant.getId(), null);
        List<Product> products = productService.listAll(merchant.getId());

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfLastWeek = startOfWeek.minusWeeks(1);

        BigDecimal todayEarnings = BigDecimal.ZERO;
        BigDecimal yesterdayEarnings = BigDecimal.ZERO;
        BigDecimal weekEarnings = BigDecimal.ZERO;
        BigDecimal monthEarnings = BigDecimal.ZERO;
        BigDecimal lastWeekEarnings = BigDecimal.ZERO;
        BigDecimal totalEarnings = BigDecimal.ZERO;
        long todayOrders = 0, weekOrders = 0, monthOrders = 0, totalOrders = 0;

        Map<LocalDate, BigDecimal> dailyRevenue = new LinkedHashMap<>();
        Map<LocalDate, Integer> dailyCount = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dailyRevenue.put(d, BigDecimal.ZERO);
            dailyCount.put(d, 0);
        }

        Map<LocalDate, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) {
            monthlyRevenue.put(today.minusDays(i), BigDecimal.ZERO);
        }

        long pendingCount = 0, deliveringCount = 0, completedCount = 0, cancelledCount = 0;

        for (Order order : allOrders) {
            LocalDate orderDate = order.getCreatedAt().toLocalDate();
            BigDecimal amount = order.getTotalAmount();
            switch (order.getStatus()) {
                case "待接单", "待配送" -> pendingCount++;
                case "配送中" -> deliveringCount++;
                case "已完成" -> completedCount++;
                case "已取消" -> cancelledCount++;
            }
            if (!"已完成".equals(order.getStatus())) continue;
            totalEarnings = totalEarnings.add(amount);
            totalOrders++;
            if (orderDate.equals(today)) { todayEarnings = todayEarnings.add(amount); todayOrders++; }
            if (orderDate.equals(today.minusDays(1))) yesterdayEarnings = yesterdayEarnings.add(amount);
            if (!orderDate.isBefore(startOfWeek)) { weekEarnings = weekEarnings.add(amount); weekOrders++; }
            if (!orderDate.isBefore(startOfMonth)) { monthEarnings = monthEarnings.add(amount); monthOrders++; }
            if (!orderDate.isBefore(startOfLastWeek) && orderDate.isBefore(startOfWeek))
                lastWeekEarnings = lastWeekEarnings.add(amount);
            if (!orderDate.isBefore(today.minusDays(6))) {
                dailyRevenue.merge(orderDate, amount, BigDecimal::add);
                dailyCount.merge(orderDate, 1, Integer::sum);
            }
            if (!orderDate.isBefore(today.minusDays(29)))
                monthlyRevenue.merge(orderDate, amount, BigDecimal::add);
        }

        List<String> chart7Labels = new ArrayList<>();
        List<String> chart7Revenue = new ArrayList<>();
        List<Integer> chart7Orders = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : dailyRevenue.entrySet()) {
            chart7Labels.add(e.getKey().toString().substring(5));
            chart7Revenue.add(e.getValue().toString());
            chart7Orders.add(dailyCount.get(e.getKey()));
        }

        List<String> chart30Labels = new ArrayList<>();
        List<String> chart30Revenue = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : monthlyRevenue.entrySet()) {
            chart30Labels.add(e.getKey().toString().substring(5));
            chart30Revenue.add(e.getValue().toString());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("merchant", merchant);
        data.put("todayEarnings", todayEarnings);
        data.put("yesterdayEarnings", yesterdayEarnings);
        data.put("weekEarnings", weekEarnings);
        data.put("monthEarnings", monthEarnings);
        data.put("lastWeekEarnings", lastWeekEarnings);
        data.put("totalEarnings", totalEarnings);
        data.put("todayOrders", todayOrders);
        data.put("weekOrders", weekOrders);
        data.put("monthOrders", monthOrders);
        data.put("totalOrders", totalOrders);
        data.put("pendingCount", pendingCount);
        data.put("deliveringCount", deliveringCount);
        data.put("completedCount", completedCount);
        data.put("cancelledCount", cancelledCount);
        data.put("chart7Labels", chart7Labels);
        data.put("chart7Revenue", chart7Revenue);
        data.put("chart7Orders", chart7Orders);
        data.put("chart30Labels", chart30Labels);
        data.put("chart30Revenue", chart30Revenue);
        return Result.success(data);
    }
}
```

- [ ] **Step 2: 补充 Service 中缺失的方法**

在 ProductService 中添加 `moveCategory`:
```java
@Transactional
public void moveCategory(Long productId, Long categoryId) {
    Product product = findById(productId);
    product.setCategory(categoryRepository.getReferenceById(categoryId));
    productRepository.save(product);
}
```

在 ReviewService 中添加 `reply`:
```java
@Transactional
public void reply(Long reviewId, String reply) {
    Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new BusinessException("评价不存在"));
    review.setReply(reply);
    reviewRepository.save(review);
}
```

- [ ] **Step 3: 验证编译**

```bash
cd backend && mvn clean compile -q
```
Expected: BUILD SUCCESS

---

### Task 10: 迁移 DataInitializer

**Files:**
- Create: `backend/src/main/java/com/waimai/config/DataInitializer.java`

直接从原项目复制 `DataInitializer.java`，代码完全不变。

- [ ] **Step 1: 复制 DataInitializer**

```bash
cp "d:/code/html5/外卖/src/main/java/com/waimai/config/DataInitializer.java" \
   "backend/src/main/java/com/waimai/config/DataInitializer.java"
```

- [ ] **Step 2: 验证编译并启动测试**

```bash
cd backend && mvn clean spring-boot:run
```
Expected: 应用启动成功，Swagger 可访问 http://localhost:8080/swagger-ui.html

---

### 阶段一验证标准

- [ ] `mvn clean compile` 编译通过
- [ ] 应用启动后 Swagger UI 可访问
- [ ] POST `/api/auth/login` 返回 JWT Token
- [ ] 带 Token 访问 `/api/consumer/index` 返回商家列表 JSON
- [ ] 带 Token 访问 `/api/merchant/orders` 返回订单列表 JSON
- [ ] 所有原项目业务逻辑完整保留

---

## 阶段二：前端 — Vue 3 + Element Plus

### 关键技术决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 构建工具 | Vite 5 | Vue 3 官方推荐 |
| UI 框架 | Element Plus 2.x | 成熟的企业级组件库 |
| HTTP 客户端 | Axios | 拦截器支持 Token 自动刷新 |
| 状态管理 | Pinia | Vue 3 官方推荐 |
| 路由 | Vue Router 4 | 路由守卫实现权限控制 |
| 图表 | Chart.js 4 (via vue-chartjs) | 与原项目一致，商家收益图表 |
| CSS 主题 | 美团黄 #FFD101 CSS 变量 | 保持原项目视觉风格 |

### Task 11: 创建 Vue 3 项目骨架

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/index.html`
- Create: `frontend/tsconfig.json`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`

- [ ] **Step 1: 初始化项目**

```bash
cd "d:/code/html5/外卖前后端分离"
npm create vite@latest frontend -- --template vue-ts
cd frontend && npm install
```

- [ ] **Step 2: 安装依赖**

```bash
cd frontend && npm install element-plus @element-plus/icons-vue vue-router@4 pinia axios vue-chartjs chart.js
```

- [ ] **Step 3: 配置 vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 4: 配置 main.ts**

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
```

- [ ] **Step 5: 创建 App.vue**

```vue
<template>
  <router-view />
</template>

<style>
:root {
  --yellow: #FFD101;
  --yellow-dark: #FFB800;
}
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #f5f5f5;
}
</style>
```

---

### Task 12: 搭建路由和请求层

**Files:**
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/api/request.ts`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/stores/user.ts`
- Create: `frontend/src/stores/cart.ts`

- [ ] **Step 1: 创建 Axios 实例（request.ts）**

```typescript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器 — 添加 Token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 — 处理错误和 Token 刷新
request.interceptors.response.use(
  response => {
    const { code, message, data } = response.data
    if (code === 200) return data
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  async error => {
    if (error.response?.status === 401) {
      // Token 过期，尝试刷新
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const res = await axios.post('/api/auth/refresh', {}, {
            headers: { Authorization: `Bearer ${refreshToken}` }
          })
          if (res.data.code === 200) {
            const { accessToken, refreshToken: newRefresh } = res.data.data
            localStorage.setItem('accessToken', accessToken)
            localStorage.setItem('refreshToken', newRefresh)
            error.config.headers.Authorization = `Bearer ${accessToken}`
            return request(error.config)
          }
        } catch { /* ignore */ }
      }
      localStorage.clear()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    }
    return Promise.reject(error)
  }
)

export default request
```

- [ ] **Step 2: 创建 auth API**

```typescript
import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterConsumerParams {
  username: string
  phone: string
  password: string
  email?: string
}

export interface RegisterMerchantParams {
  username: string
  phone: string
  password: string
  shopName: string
  shopAddress: string
  businessLicense: string
  description?: string
}

export const authApi = {
  login: (params: LoginParams) =>
    request.post('/auth/login', params),

  registerConsumer: (params: RegisterConsumerParams) =>
    request.post('/auth/register/consumer', params),

  registerMerchant: (params: RegisterMerchantParams) =>
    request.post('/auth/register/merchant', params),

  refreshToken: (refreshToken: string) =>
    request.post('/auth/refresh', {}, {
      headers: { Authorization: `Bearer ${refreshToken}` }
    })
}
```

- [ ] **Step 3: 创建 User Store（Pinia）**

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi, type LoginParams } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const user = ref<any>(null)
  const token = ref(localStorage.getItem('accessToken') || '')

  async function login(params: LoginParams) {
    const res: any = await authApi.login(params)
    localStorage.setItem('accessToken', res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
    token.value = res.accessToken
    user.value = {
      id: res.userId,
      username: res.username,
      role: res.role,
      phone: res.phone
    }
    return res
  }

  function logout() {
    localStorage.clear()
    user.value = null
    token.value = ''
  }

  function isLoggedIn() {
    return !!token.value
  }

  function isConsumer() {
    return user.value?.role === 'ROLE_CONSUMER'
  }

  function isMerchant() {
    return user.value?.role === 'ROLE_MERCHANT'
  }

  return { user, token, login, logout, isLoggedIn, isConsumer, isMerchant }
})
```

- [ ] **Step 4: 创建 Cart Store（Pinia）**

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface CartItem {
  productId: number
  productName: string
  productImage: string
  price: number
  quantity: number
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const merchantId = ref<number | null>(null)
  const merchantName = ref('')

  const totalAmount = computed(() =>
    items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
  )

  const totalCount = computed(() =>
    items.value.reduce((sum, item) => sum + item.quantity, 0)
  )

  function setMerchant(id: number, name: string) {
    if (merchantId.value !== id) {
      items.value = []
      merchantId.value = id
      merchantName.value = name
    }
    loadFromStorage(id)
  }

  function addItem(product: any, quantity = 1) {
    const exist = items.value.find(i => i.productId === product.id)
    if (exist) {
      exist.quantity += quantity
    } else {
      items.value.push({
        productId: product.id,
        productName: product.name,
        productImage: product.image,
        price: product.price,
        quantity
      })
    }
    saveToStorage()
  }

  function removeItem(productId: number) {
    items.value = items.value.filter(i => i.productId !== productId)
    saveToStorage()
  }

  function updateQuantity(productId: number, quantity: number) {
    const item = items.value.find(i => i.productId === productId)
    if (item) {
      item.quantity = quantity
      if (item.quantity <= 0) removeItem(productId)
    }
    saveToStorage()
  }

  function clear() {
    items.value = []
    if (merchantId.value) {
      localStorage.removeItem(`waimai_cart_${merchantId.value}`)
    }
  }

  function saveToStorage() {
    if (merchantId.value) {
      localStorage.setItem(`waimai_cart_${merchantId.value}`, JSON.stringify(items.value))
    }
  }

  function loadFromStorage(id: number) {
    const saved = localStorage.getItem(`waimai_cart_${id}`)
    if (saved) {
      try { items.value = JSON.parse(saved) } catch { items.value = [] }
    }
  }

  return { items, merchantId, merchantName, totalAmount, totalCount,
           setMerchant, addItem, removeItem, updateQuantity, clear }
})
```

- [ ] **Step 5: 创建路由配置**

```typescript
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guest: true }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { guest: true }
    },
    // 消费者路由
    {
      path: '/consumer',
      component: () => import('@/components/AppLayout.vue'),
      meta: { requiresAuth: true, role: 'ROLE_CONSUMER' },
      children: [
        { path: '', redirect: '/consumer/home' },
        { path: 'home', name: 'home', component: () => import('@/views/consumer/HomeView.vue') },
        { path: 'merchant/:id', name: 'merchant-detail', component: () => import('@/views/consumer/MerchantDetail.vue') },
        { path: 'orders', name: 'orders', component: () => import('@/views/consumer/OrdersView.vue') },
        { path: 'order/:id', name: 'order-detail', component: () => import('@/views/consumer/OrderDetail.vue') },
        { path: 'order/:id/review', name: 'review', component: () => import('@/views/consumer/ReviewView.vue') },
        { path: 'coupons', name: 'coupons', component: () => import('@/views/consumer/CouponsView.vue') },
        { path: 'search', name: 'search', component: () => import('@/views/consumer/SearchView.vue') },
        { path: 'profile', name: 'profile', component: () => import('@/views/consumer/ProfileView.vue') },
        { path: 'address', name: 'address', component: () => import('@/views/consumer/AddressView.vue') },
        { path: 'balance', name: 'balance', component: () => import('@/views/consumer/BalanceView.vue') }
      ]
    },
    // 商家路由
    {
      path: '/merchant',
      component: () => import('@/components/MerchantLayout.vue'),
      meta: { requiresAuth: true, role: 'ROLE_MERCHANT' },
      children: [
        { path: '', redirect: '/merchant/orders' },
        { path: 'orders', name: 'merchant-orders', component: () => import('@/views/merchant/DashboardView.vue') },
        { path: 'order/:id', name: 'merchant-order-detail', component: () => import('@/views/merchant/OrderDetail.vue') },
        { path: 'products', name: 'merchant-products', component: () => import('@/views/merchant/ProductsView.vue') },
        { path: 'reviews', name: 'merchant-reviews', component: () => import('@/views/merchant/ReviewsView.vue') },
        { path: 'earnings', name: 'merchant-earnings', component: () => import('@/views/merchant/EarningsView.vue') },
        { path: 'shop', name: 'merchant-shop', component: () => import('@/views/merchant/ShopEdit.vue') }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/login' }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')
  const role = localStorage.getItem('role')

  if (to.meta.guest && token) {
    // 已登录用户访问登录页，按角色跳转
    return next(role === 'ROLE_CONSUMER' ? '/consumer/home' : '/merchant/orders')
  }

  if (to.meta.requiresAuth) {
    if (!token) return next('/login')
    if (to.meta.role && to.meta.role !== role) {
      return next(role === 'ROLE_CONSUMER' ? '/consumer/home' : '/merchant/orders')
    }
  }

  next()
})

export default router
```

---

### Task 13-22: 实现全部页面（由于篇幅，摘要说明）

以下页面需要逐个实现，每个页面对应原项目 Thymeleaf 模板：

| Task | 页面 | 对应原模板 | 核心 Element Plus 组件 |
|------|------|-----------|----------------------|
| 13 | LoginView.vue | login.html | el-form, el-input, el-button |
| 14 | RegisterView.vue | register.html | el-tabs, el-form, el-input |
| 15 | HomeView.vue | consumer/index.html | el-carousel, el-card, el-input (搜索) |
| 16 | MerchantDetail.vue | consumer/merchant-detail.html | el-tabs, el-card, el-drawer (购物车) |
| 17 | OrdersView.vue | consumer/orders.html | el-tabs, el-steps, el-tag |
| 18 | CouponsView.vue | consumer/coupons-page.html | el-card, el-button, el-countdown |
| 19 | AddressView.vue | consumer/address.html | el-form, el-dialog, el-radio |
| 20 | DashboardView.vue | merchant/orders.html | el-statistic, el-table, el-tag |
| 21 | ProductsView.vue | merchant/products-all.html | el-table, el-dialog, el-collapse |
| 22 | EarningsView.vue | merchant/earnings.html | el-card, LineChart (vue-chartjs) |

**每个页面的实现步骤模式：**

- [ ] Step 1: 创建对应的 API 模块（如 `frontend/src/api/merchant.ts`）
- [ ] Step 2: 创建 Vue SFC 组件，使用 Element Plus 组件
- [ ] Step 3: 实现数据获取和交互逻辑（`<script setup lang="ts">`）
- [ ] Step 4: 编写 `<style scoped>` 美团黄主题样式
- [ ] Step 5: 在浏览器中验证页面渲染和交互

**示例 — Task 15: HomeView.vue（首页）**

```vue
<template>
  <div class="home">
    <!-- 顶部搜索栏 -->
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索商家或商品" size="large"
                @keyup.enter="search" clearable>
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <!-- 广告轮播 -->
    <el-carousel height="160px" class="banner">
      <el-carousel-item v-for="b in banners" :key="b">
        <div class="banner-item" :style="{ background: b.color }">{{ b.text }}</div>
      </el-carousel-item>
    </el-carousel>

    <!-- 排序切换 -->
    <div class="sort-bar">
      <el-radio-group v-model="sort" @change="loadMerchants">
        <el-radio-button value="sales">按销量</el-radio-button>
        <el-radio-button value="rating">按评分</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 商家列表 -->
    <div class="merchant-list">
      <el-card v-for="m in merchants" :key="m.id" class="merchant-card"
               shadow="hover" @click="$router.push(`/consumer/merchant/${m.id}`)">
        <div class="card-header">
          <el-avatar :size="50" :src="m.shopAvatar" />
          <div class="shop-info">
            <div class="shop-name">{{ m.shopName }}</div>
            <el-rate :model-value="m.rating" disabled show-score size="small" />
            <div class="shop-meta">
              <span>月售{{ monthlyOrderCounts[m.id] || 0 }}</span>
              <span>¥{{ m.deliveryFee }} 配送</span>
            </div>
          </div>
        </div>
        <div class="card-footer">
          <el-tag size="small">{{ m.status }}</el-tag>
          <span class="time">{{ m.businessHours }}</span>
        </div>
      </el-card>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <div v-for="tab in tabs" :key="tab.key" class="nav-item"
           :class="{ active: $route.path === tab.path }"
           @click="$router.push(tab.path)">
        <el-icon><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, HomeFilled, ShoppingCart, Tickets, User } from '@element-plus/icons-vue'
import { consumerApi } from '@/api/consumer'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const keyword = ref('')
const sort = ref('sales')
const merchants = ref<any[]>([])
const monthlyOrderCounts = ref<Record<number, number>>({})

const banners = [
  { text: '🔥 每日神券限量领', color: 'linear-gradient(135deg, #FFD101, #FFB800)' },
  { text: '🚀 新用户首单立减', color: 'linear-gradient(135deg, #FF6B6B, #FF8E53)' },
  { text: '🎂 生日蛋糕全场8折', color: 'linear-gradient(135deg, #A18CD1, #FBC2EB)' }
]

const tabs = [
  { key: 'home', label: '首页', path: '/consumer/home', icon: 'HomeFilled' },
  { key: 'search', label: '搜索', path: '/consumer/search', icon: 'Search' },
  { key: 'orders', label: '订单', path: '/consumer/orders', icon: 'ShoppingCart' },
  { key: 'coupons', label: '优惠券', path: '/consumer/coupons', icon: 'Tickets' },
  { key: 'my', label: '我的', path: '/consumer/profile', icon: 'User' }
]

async function loadMerchants() {
  const res: any = await consumerApi.getHome(keyword.value, sort.value)
  merchants.value = res.merchants
  monthlyOrderCounts.value = res.monthlyOrderCounts
}

function search() { loadMerchants() }

onMounted(loadMerchants)
</script>

<style scoped>
.home { padding-bottom: 60px; }
.search-bar { padding: 12px 16px; background: var(--yellow); }
.banner { margin: 12px 16px; border-radius: 8px; overflow: hidden; }
.banner-item { height: 100%; display: flex; align-items: center; justify-content: center;
  color: white; font-size: 18px; font-weight: bold; }
.sort-bar { padding: 8px 16px; }
.merchant-list { padding: 0 16px; }
.merchant-card { margin-bottom: 10px; cursor: pointer; }
.card-header { display: flex; gap: 12px; align-items: center; }
.shop-name { font-size: 16px; font-weight: bold; }
.shop-meta { font-size: 12px; color: #999; margin-top: 4px; }
.shop-meta span { margin-right: 12px; }
.card-footer { margin-top: 8px; display: flex; justify-content: space-between; align-items: center; }
.time { font-size: 12px; color: #999; }
.bottom-nav { position: fixed; bottom: 0; left: 0; right: 0; height: 56px;
  background: white; display: flex; border-top: 1px solid #eee; z-index: 100; }
.nav-item { flex: 1; display: flex; flex-direction: column; align-items: center;
  justify-content: center; font-size: 12px; color: #999; }
.nav-item.active { color: var(--yellow-dark); }
</style>
```

其余页面遵循相同模式，在原项目 HTML 模板的功能基础上用 Element Plus 重写。

---

### 阶段二验证标准

- [ ] `npm run dev` 启动前端开发服务器
- [ ] 登录页可正常登录，Token 存储到 localStorage
- [ ] 消费者首页可展示商家列表
- [ ] 商家详情页可展示商品、分类、评价
- [ ] 购物车可添加/删除商品
- [ ] 订单流程可完整走通（下单→支付→商家接单→配送→完成→评价）
- [ ] 商家端仪表盘显示正确统计数据
- [ ] 商品管理页 CRUD 操作正常
- [ ] 收益页 Chart.js 图表正常渲染
- [ ] 路由守卫正确拦截未登录和角色越权访问

---

## 阶段三：基础设施 — Redis + ES + MQ + WebSocket + MinIO + Docker Compose

### 关键技术决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| Redis 客户端 | Spring Data Redis + Lettuce | Spring 官方推荐 |
| Redis 序列化 | Jackson2JsonRedisSerializer | JSON 格式可读性好 |
| ES 客户端 | Spring Data Elasticsearch | 与 Spring 生态集成好 |
| MQ | Spring AMQP (RabbitMQ) | Spring 官方支持 |
| WebSocket | Spring WebSocket + STOMP | 浏览器原生支持 |
| 对象存储 | MinIO Java SDK | 兼容 S3，自部署 |
| 容器化 | Docker Compose | 开发环境一键启动 |

### Task 23: Docker Compose 基础设施

**Files:**
- Create: `docker/docker-compose.yml`
- Create: `docker/mysql/init.sql`
- Create: `docker/redis/redis.conf`
- Create: `docker/elasticsearch/elasticsearch.yml`

- [ ] **Step 1: 创建 docker-compose.yml**

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: waimai-mysql
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: waimai
    ports:
      - "3306:3306"
    volumes:
      - ./mysql/init.sql:/docker-entrypoint-initdb.d/init.sql
      - mysql-data:/var/lib/mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7-alpine
    container_name: waimai-redis
    ports:
      - "6379:6379"
    volumes:
      - ./redis/redis.conf:/usr/local/etc/redis/redis.conf
      - redis-data:/data
    command: redis-server /usr/local/etc/redis/redis.conf

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: waimai-es
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: waimai-rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  minio:
    image: minio/minio:latest
    container_name: waimai-minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio-data:/data
    command: server /data --console-address ":9001"

  backend:
    build:
      context: ../backend
      dockerfile: Dockerfile
    container_name: waimai-backend
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - elasticsearch
      - rabbitmq
      - minio
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/waimai?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 123456
      SPRING_DATA_REDIS_HOST: redis
      SPRING_ELASTICSEARCH_URIS: http://elasticsearch:9200
      SPRING_RABBITMQ_HOST: rabbitmq
      MINIO_ENDPOINT: http://minio:9000

volumes:
  mysql-data:
  redis-data:
  es-data:
  minio-data:
```

---

### Task 24-30: 集成中间件

| Task | 内容 | 关键文件 |
|------|------|---------|
| 24 | Redis 集成 | RedisConfig.java, 购物车/缓存改造 |
| 25 | Elasticsearch 集成 | MerchantDocument.java, 搜索改造 |
| 26 | RabbitMQ 集成 | RabbitMQConfig.java, 订单状态消息 |
| 27 | WebSocket 集成 | WebSocketConfig.java, 实时推送 |
| 28 | MinIO 集成 | MinioConfig.java, UploadController.java |
| 29 | Dockerfile | backend/Dockerfile |
| 30 | 配置整合 | application-docker.yml |

### 阶段三验证标准

- [ ] `docker-compose up` 所有服务启动正常
- [ ] Redis 缓存购物车和热门商家
- [ ] ES 搜索替代 MySQL LIKE
- [ ] 订单状态变更通过 RabbitMQ → WebSocket 推送前端
- [ ] 图片上传到 MinIO 并能访问

---

## 阶段四：数据架构 — 分库分表 + 读写分离 + 分布式事务

### 关键技术决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 分库分表 | ShardingSphere-JDBC 5.x | 代码侵入小，与 Spring Boot 集成好 |
| 分片策略 | order 表按 user_id 取模 | 消费者订单查询为主场景 |
| 读写分离 | 主从复制 + ShardingSphere 路由 | 商家后台读多，消费者写多 |
| 分布式事务 | Seata AT 模式 | 下单流程需要最终一致性 |

### Task 31-34: 数据层改造

| Task | 内容 | 关键配置 |
|------|------|---------|
| 31 | ShardingSphere-JDBC 依赖和配置 | 分片规则 4 库 8 表 |
| 32 | MySQL 主从复制配置 | docker-compose 添加从库 |
| 33 | Seata 集成 | Seata Server + 配置 |
| 34 | 下单流程改造 | @GlobalTransactional |

### 阶段四验证标准

- [ ] 订单数据按 user_id 正确路由到不同分片
- [ ] 写操作走主库，读操作走从库
- [ ] 模拟故障验证 Seata 事务回滚
- [ ] 性能压测验证分库分表效果

---

## 附录

### A. API 端点完整列表

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| POST | /api/auth/login | 登录 | 公开 |
| POST | /api/auth/register/consumer | 消费者注册 | 公开 |
| POST | /api/auth/register/merchant | 商家注册 | 公开 |
| POST | /api/auth/refresh | 刷新Token | 公开 |
| GET | /api/consumer/index | 首页商家列表 | CONSUMER |
| GET | /api/consumer/merchant/{id} | 商家详情 | CONSUMER |
| GET | /api/consumer/merchant/{mid}/category/{cid} | 按分类加载商品 | CONSUMER |
| GET | /api/consumer/orders | 订单列表 | CONSUMER |
| GET | /api/consumer/order/{id} | 订单详情 | CONSUMER |
| POST | /api/consumer/order/cancel/{id} | 取消订单 | CONSUMER |
| POST | /api/consumer/order/confirm/{id} | 确认收货 | CONSUMER |
| POST | /api/consumer/order/submit | 提交订单 | CONSUMER |
| POST | /api/consumer/order/{id}/review | 提交评价 | CONSUMER |
| GET | /api/consumer/address | 地址列表 | CONSUMER |
| POST | /api/consumer/address/add | 添加地址 | CONSUMER |
| PUT | /api/consumer/address/{id} | 编辑地址 | CONSUMER |
| DELETE | /api/consumer/address/{id} | 删除地址 | CONSUMER |
| GET | /api/consumer/coupons | 可用优惠券 | CONSUMER |
| GET | /api/consumer/coupons/daily | 每日神券 | CONSUMER |
| POST | /api/consumer/coupons/claim | 领取神券 | CONSUMER |
| GET | /api/consumer/search | 搜索 | CONSUMER |
| GET | /api/consumer/profile | 个人信息 | CONSUMER |
| PUT | /api/consumer/profile | 更新信息 | CONSUMER |
| PUT | /api/consumer/profile/password | 修改密码 | CONSUMER |
| GET | /api/consumer/balance | 余额 | CONSUMER |
| POST | /api/consumer/balance/recharge | 充值 | CONSUMER |
| GET | /api/merchant/shop | 店铺信息 | MERCHANT |
| PUT | /api/merchant/shop | 更新店铺 | MERCHANT |
| POST | /api/merchant/shop/toggle-status | 切换营业状态 | MERCHANT |
| GET | /api/merchant/products | 商品+分类列表 | MERCHANT |
| POST | /api/merchant/product | 添加商品 | MERCHANT |
| PUT | /api/merchant/product/{id} | 编辑商品 | MERCHANT |
| POST | /api/merchant/product/toggle/{id} | 上下架 | MERCHANT |
| DELETE | /api/merchant/product/{id} | 删除商品 | MERCHANT |
| POST | /api/merchant/product/move-category | 移动分类 | MERCHANT |
| POST | /api/merchant/category | 添加分类 | MERCHANT |
| PUT | /api/merchant/category/{id} | 编辑分类 | MERCHANT |
| DELETE | /api/merchant/category/{id} | 删除分类 | MERCHANT |
| GET | /api/merchant/orders | 订单列表+仪表盘 | MERCHANT |
| GET | /api/merchant/order/{id} | 订单详情 | MERCHANT |
| POST | /api/merchant/order/accept/{id} | 接单 | MERCHANT |
| POST | /api/merchant/order/deliver/{id} | 开始配送 | MERCHANT |
| POST | /api/merchant/order/complete/{id} | 完成订单 | MERCHANT |
| GET | /api/merchant/reviews | 评价列表 | MERCHANT |
| POST | /api/merchant/review/reply/{id} | 回复评价 | MERCHANT |
| GET | /api/merchant/earnings | 收益统计 | MERCHANT |

### B. 数据表关系图

```
User (1) ─── (1) Merchant
User (1) ─── (N) Address
User (1) ─── (N) Order (as consumer)
User (1) ─── (N) Coupon
User (1) ─── (N) BalanceRecord
User (1) ─── (N) Review (as consumer)
Merchant (1) ─── (N) Category
Merchant (1) ─── (N) Product
Merchant (1) ─── (N) Order
Merchant (1) ─── (N) Review
Category (1) ─── (N) Product
Order (1) ─── (N) OrderItem
Order (1) ─── (0..1) Review
```

### C. 订单状态流转

```
待付款 → (支付) → 待接单 → (商家接单) → 待配送 → (开始配送) → 配送中 → (完成) → 已完成
                    ↓                                    ↓
                 已取消                              (确认收货)
                                                         ↓
                                                      已完成
```
