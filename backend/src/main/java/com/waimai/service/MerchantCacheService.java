package com.waimai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waimai.entity.Merchant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商家缓存服务 — 热门商家 / 商家详情缓存到 Redis
 * 使用 JSON 字符串手动序列化，避免 DevTools 类加载器冲突
 */
@Slf4j
@Service
@Profile("redis")
@RequiredArgsConstructor
public class MerchantCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    private static final String HOT_MERCHANTS_KEY = "waimai:merchants:hot";
    private static final String MERCHANT_DETAIL_PREFIX = "waimai:merchant:detail:";
    private static final long CACHE_TTL_MINUTES = 30;

    /** 获取热门商家（缓存） */
    public List<Merchant> getHotMerchants() {
        String json = redisTemplate.opsForValue().get(HOT_MERCHANTS_KEY);
        if (json != null) {
            try {
                List<Merchant> list = redisObjectMapper.readValue(json,
                        new TypeReference<List<Merchant>>() {});
                log.debug("热门商家命中缓存, size={}", list.size());
                return list;
            } catch (Exception e) {
                log.error("热门商家缓存反序列化失败: {}", e.getMessage());
            }
        }
        return null;
    }

    /** 设置热门商家缓存 */
    public void setHotMerchants(List<Merchant> merchants) {
        try {
            String json = redisObjectMapper.writeValueAsString(merchants);
            redisTemplate.opsForValue().set(HOT_MERCHANTS_KEY, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("热门商家缓存已更新, size={}", merchants.size());
        } catch (Exception e) {
            log.error("热门商家缓存序列化失败: {}", e.getMessage());
        }
    }

    /** 删除热门商家缓存 */
    public void evictHotMerchants() {
        redisTemplate.delete(HOT_MERCHANTS_KEY);
        log.debug("热门商家缓存已清除");
    }

    /** 获取商家详情（缓存） */
    public Merchant getMerchantDetail(Long merchantId) {
        String json = redisTemplate.opsForValue().get(MERCHANT_DETAIL_PREFIX + merchantId);
        if (json != null) {
            try {
                return redisObjectMapper.readValue(json, Merchant.class);
            } catch (Exception e) {
                log.error("商家详情反序列化失败: {}", e.getMessage());
            }
        }
        return null;
    }

    /** 设置商家详情缓存 */
    public void setMerchantDetail(Long merchantId, Merchant merchant) {
        try {
            String json = redisObjectMapper.writeValueAsString(merchant);
            redisTemplate.opsForValue().set(MERCHANT_DETAIL_PREFIX + merchantId, json,
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("商家详情序列化失败: {}", e.getMessage());
        }
    }

    /** 删除商家详情缓存 */
    public void evictMerchantDetail(Long merchantId) {
        redisTemplate.delete(MERCHANT_DETAIL_PREFIX + merchantId);
    }
}
