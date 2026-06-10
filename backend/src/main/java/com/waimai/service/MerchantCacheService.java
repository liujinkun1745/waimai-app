package com.waimai.service;

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
 */
@Slf4j
@Service
@Profile("redis")
@RequiredArgsConstructor
public class MerchantCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String HOT_MERCHANTS_KEY = "waimai:merchants:hot";
    private static final String MERCHANT_DETAIL_PREFIX = "waimai:merchant:detail:";
    private static final long CACHE_TTL_MINUTES = 30;

    /** 获取热门商家（缓存） */
    @SuppressWarnings("unchecked")
    public List<Merchant> getHotMerchants() {
        List<Merchant> cached = (List<Merchant>) redisTemplate.opsForValue().get(HOT_MERCHANTS_KEY);
        if (cached != null) {
            log.debug("热门商家命中缓存, size={}", cached.size());
            return cached;
        }
        return null;
    }

    /** 设置热门商家缓存 */
    public void setHotMerchants(List<Merchant> merchants) {
        redisTemplate.opsForValue().set(HOT_MERCHANTS_KEY, merchants, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("热门商家缓存已更新, size={}", merchants.size());
    }

    /** 删除热门商家缓存 */
    public void evictHotMerchants() {
        redisTemplate.delete(HOT_MERCHANTS_KEY);
        log.debug("热门商家缓存已清除");
    }

    /** 获取商家详情（缓存） */
    public Merchant getMerchantDetail(Long merchantId) {
        return (Merchant) redisTemplate.opsForValue().get(MERCHANT_DETAIL_PREFIX + merchantId);
    }

    /** 设置商家详情缓存 */
    public void setMerchantDetail(Long merchantId, Merchant merchant) {
        redisTemplate.opsForValue().set(MERCHANT_DETAIL_PREFIX + merchantId, merchant,
                CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /** 删除商家详情缓存 */
    public void evictMerchantDetail(Long merchantId) {
        redisTemplate.delete(MERCHANT_DETAIL_PREFIX + merchantId);
    }
}
