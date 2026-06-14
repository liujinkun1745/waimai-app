package com.waimai.controller;

import com.waimai.config.JwtUtil;
import com.waimai.dto.request.*;
import com.waimai.dto.response.Result;
import com.waimai.entity.*;
import com.waimai.service.*;
import com.waimai.service.CartCacheService.CartItemDTO;
import com.waimai.service.OrderService.CartItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Autowired(required = false)
    private CartCacheService cartCacheService;
    private User currentUser(String token) {
        String jwt = token.replace("Bearer ", "");
        String username = jwtUtil.getUsername(jwt);
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

        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        Map<Long, Integer> monthlyOrderCounts = new HashMap<>();
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

    // ========== 购物车 ==========

    @GetMapping("/cart")
    @Operation(summary = "获取购物车")
    public Result<List<CartItemDTO>> getCart(
            @RequestHeader("Authorization") String token,
            @RequestParam Long merchantId) {
        User user = currentUser(token);
        if (cartCacheService == null) {
            return Result.success(List.of());
        }
        return Result.success(cartCacheService.getCart(user.getId(), merchantId));
    }

    @PostMapping("/cart/add")
    @Operation(summary = "加入购物车")
    public Result<Void> addToCart(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AddToCartRequest request) {
        User user = currentUser(token);
        if (cartCacheService != null) {
            Product product = productService.findById(request.getProductId());
            cartCacheService.addItem(user.getId(), request.getMerchantId(),
                    product.getId(), product.getName(), product.getImage(),
                    product.getPrice(), request.getQuantity());
        }
        return Result.success();
    }

    @PutMapping("/cart/{productId}")
    @Operation(summary = "更新购物车商品数量")
    public Result<Void> updateCartItem(
            @RequestHeader("Authorization") String token,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartRequest request) {
        User user = currentUser(token);
        if (cartCacheService != null) {
            cartCacheService.updateQuantity(user.getId(), request.getMerchantId(),
                    productId, request.getQuantity());
        }
        return Result.success();
    }

    @DeleteMapping("/cart/{productId}")
    @Operation(summary = "删除购物车单个商品")
    public Result<Void> removeCartItem(
            @RequestHeader("Authorization") String token,
            @PathVariable Long productId,
            @RequestParam Long merchantId) {
        User user = currentUser(token);
        if (cartCacheService != null) {
            cartCacheService.removeItem(user.getId(), merchantId, productId);
        }
        return Result.success();
    }

    @DeleteMapping("/cart")
    @Operation(summary = "清空购物车")
    public Result<Void> clearCart(
            @RequestHeader("Authorization") String token,
            @RequestParam Long merchantId) {
        User user = currentUser(token);
        if (cartCacheService != null) {
            cartCacheService.clearCart(user.getId(), merchantId);
        }
        return Result.success();
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
            couponService.useCoupon(request.getCouponId(), user.getId());
        }
        orderService.submitOrder(user.getId(), request.getMerchantId(),
                request.getAddressId(), cartItems, finalAmount);
        if (cartCacheService != null) {
            for (SubmitOrderRequest.CartItemRequest item : request.getItems()) {
                cartCacheService.removeItem(user.getId(), request.getMerchantId(), item.getProductId());
            }
        }
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

    @PostMapping("/address")
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
    @Operation(summary = "每日神券（三选一）")
    public Result<Map<String, Object>> dailyCoupons(@RequestHeader("Authorization") String token) {
        User user = currentUser(token);
        LocalDate today = LocalDate.now();
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
            String tplName = templates[idx][0];
            boolean thisClaimed = couponService.getAvailable(user.getId()).stream()
                    .anyMatch(c -> c.getCreatedAt() != null
                            && c.getCreatedAt().toLocalDate().equals(today)
                            && c.getName().equals(tplName));
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
        LocalDate today = LocalDate.now();
        boolean claimedToday = couponService.getAvailable(user.getId()).stream()
                .anyMatch(c -> c.getCreatedAt() != null
                        && c.getCreatedAt().toLocalDate().equals(today)
                        && DAILY_COUPON_NAMES.contains(c.getName()));
        if (claimedToday) {
            return Result.error(1000, "今日已领取过神券，明天再来吧！");
        }
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
    @Operation(summary = "余额充值")
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
        currentUser(token);
        Map<String, Object> data = new HashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            data.put("merchants", merchantService.searchOpenMerchants(keyword));
            data.put("keyword", keyword);
        } else {
            data.put("hotMerchants", merchantService.listBySales());
        }
        return Result.success(data);
    }
}
