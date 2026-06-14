package com.waimai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 购物车缓存服务 — Redis Hash 存储，按用户 + 商家隔离
 *
 * Key:   waimai:cart:{userId}:{merchantId}
 * Field: productId  Value: CartItemDTO JSON 字符串
 */
@Slf4j
@Service
@Profile("redis")
@RequiredArgsConstructor
public class CartCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    private static final String CART_KEY_PREFIX = "waimai:cart:";
    private static final long CART_TTL_DAYS = 7;

    private String cartKey(Long userId, Long merchantId) {
        return CART_KEY_PREFIX + userId + ":" + merchantId;
    }

    /** 添加商品到购物车（已存在则叠加数量） */
    public void addItem(Long userId, Long merchantId, Long productId,
                        String productName, String productImage,
                        BigDecimal price, int quantity) {
        String key = cartKey(userId, merchantId);
        String field = String.valueOf(productId);

        String existJson = (String) redisTemplate.opsForHash().get(key, field);
        if (existJson != null) {
            try {
                CartItemDTO exist = redisObjectMapper.readValue(existJson, CartItemDTO.class);
                exist.setQuantity(exist.getQuantity() + quantity);
                redisTemplate.opsForHash().put(key, field, redisObjectMapper.writeValueAsString(exist));
            } catch (Exception e) {
                log.error("购物车反序列化失败: {}", e.getMessage());
            }
        } else {
            CartItemDTO item = new CartItemDTO(productId, productName, productImage, price, quantity);
            try {
                redisTemplate.opsForHash().put(key, field, redisObjectMapper.writeValueAsString(item));
            } catch (JsonProcessingException e) {
                log.error("购物车序列化失败: {}", e.getMessage());
            }
        }
        redisTemplate.expire(key, CART_TTL_DAYS, TimeUnit.DAYS);
        log.debug("购物车添加: userId={}, merchantId={}, productId={}, qty={}", userId, merchantId, productId, quantity);
    }

    /** 获取购物车列表 */
    public List<CartItemDTO> getCart(Long userId, Long merchantId) {
        String key = cartKey(userId, merchantId);
        List<Object> values = redisTemplate.opsForHash().values(key);
        List<CartItemDTO> items = new ArrayList<>();
        for (Object v : values) {
            try {
                items.add(redisObjectMapper.readValue((String) v, CartItemDTO.class));
            } catch (Exception e) {
                log.error("购物车条目反序列化失败: {}", e.getMessage());
            }
        }
        log.debug("购物车查询: userId={}, merchantId={}, size={}", userId, merchantId, items.size());
        return items;
    }

    /** 更新商品数量（≤0 则删除） */
    public void updateQuantity(Long userId, Long merchantId, Long productId, int quantity) {
        String key = cartKey(userId, merchantId);
        String field = String.valueOf(productId);
        if (quantity <= 0) {
            redisTemplate.opsForHash().delete(key, field);
            log.debug("购物车删除: userId={}, merchantId={}, productId={}", userId, merchantId, productId);
        } else {
            String existJson = (String) redisTemplate.opsForHash().get(key, field);
            if (existJson != null) {
                try {
                    CartItemDTO item = redisObjectMapper.readValue(existJson, CartItemDTO.class);
                    item.setQuantity(quantity);
                    redisTemplate.opsForHash().put(key, field, redisObjectMapper.writeValueAsString(item));
                    log.debug("购物车更新数量: userId={}, merchantId={}, productId={}, qty={}", userId, merchantId, productId, quantity);
                } catch (Exception e) {
                    log.error("购物车更新失败: {}", e.getMessage());
                }
            }
        }
    }

    /** 删除单个商品 */
    public void removeItem(Long userId, Long merchantId, Long productId) {
        String key = cartKey(userId, merchantId);
        redisTemplate.opsForHash().delete(key, String.valueOf(productId));
        log.debug("购物车删除: userId={}, merchantId={}, productId={}", userId, merchantId, productId);
    }

    /** 清空购物车 */
    public void clearCart(Long userId, Long merchantId) {
        redisTemplate.delete(cartKey(userId, merchantId));
        log.debug("购物车清空: userId={}, merchantId={}", userId, merchantId);
    }

    /**
     * 购物车商品 DTO
     */
    public static class CartItemDTO {
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;

        public CartItemDTO() {}

        public CartItemDTO(Long productId, String productName, String productImage,
                           BigDecimal price, Integer quantity) {
            this.productId = productId;
            this.productName = productName;
            this.productImage = productImage;
            this.price = price;
            this.quantity = quantity;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductImage() { return productImage; }
        public void setProductImage(String productImage) { this.productImage = productImage; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
