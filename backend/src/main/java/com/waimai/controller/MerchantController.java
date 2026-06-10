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
import java.util.ArrayList;
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
        String jwt = token.replace("Bearer ", "");
        String username = jwtUtil.getUsername(jwt);
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

        // 构建包含 category 信息的商品数据（避免 Jackson 序列化 Hibernate 代理问题）
        List<Map<String, Object>> productList = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("image", p.getImage());
            item.put("price", p.getPrice());
            item.put("stock", p.getStock());
            item.put("sales", p.getSales());
            item.put("description", p.getDescription());
            item.put("status", p.getStatus());
            item.put("createdAt", p.getCreatedAt());
            // 手动加载分类信息
            Category cat = p.getCategory();
            Map<String, Object> catInfo = new HashMap<>();
            catInfo.put("id", cat.getId());
            catInfo.put("name", cat.getName());
            catInfo.put("sortOrder", cat.getSortOrder());
            item.put("category", catInfo);
            productList.add(item);

            categoryProductCounts.merge(cat.getId(), 1L, Long::sum);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("merchant", merchant);
        data.put("products", productList);
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
            @RequestParam(defaultValue = "0") Integer sortOrder) {
        Merchant merchant = currentMerchant(token);
        categoryService.add(merchant.getId(), name, sortOrder);
        return Result.success();
    }

    @PutMapping("/category/{id}")
    @Operation(summary = "编辑分类")
    public Result<Void> editCategory(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(defaultValue = "0") Integer sortOrder) {
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
    @Operation(summary = "评价列表+评分分布")
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
