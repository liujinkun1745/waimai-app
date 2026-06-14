package com.waimai.service;

import com.waimai.entity.Merchant;
import com.waimai.exception.BusinessException;
import com.waimai.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    @Autowired(required = false)
    private MerchantCacheService cacheService;

    public Merchant findByUserId(Long userId) {
        return merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("商家不存在"));
    }

    public Merchant findById(Long id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("商家不存在"));
    }

    public List<Merchant> searchOpenMerchants(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return listBySales();
        }
        return merchantRepository.findByShopNameContainingAndStatus(keyword, "营业中");
    }

    public List<Merchant> listBySales() {
        if (cacheService != null) {
            List<Merchant> cached = cacheService.getHotMerchants();
            if (cached != null) {
                return cached;
            }
        }
        List<Merchant> merchants = merchantRepository.findByStatusOrderByMonthlySalesDesc("营业中");
        if (cacheService != null) {
            cacheService.setHotMerchants(merchants);
        }
        return merchants;
    }

    public List<Merchant> listByRating() {
        return merchantRepository.findByStatusOrderByRatingDesc("营业中");
    }

    @Transactional
    public void updateShopInfo(Long merchantId, String shopName, String shopAvatar,
                                String description, String businessHours,
                                BigDecimal deliveryFee, BigDecimal minOrderAmount) {
        Merchant merchant = findById(merchantId);
        merchant.setShopName(shopName);
        if (shopAvatar != null && !shopAvatar.isBlank()) {
            merchant.setShopAvatar(shopAvatar);
        }
        merchant.setDescription(description);
        merchant.setBusinessHours(businessHours);
        merchant.setDeliveryFee(deliveryFee);
        merchant.setMinOrderAmount(minOrderAmount);
        merchantRepository.save(merchant);
        if (cacheService != null) {
            cacheService.evictHotMerchants();
            cacheService.evictMerchantDetail(merchantId);
        }
    }

    @Transactional
    public void toggleStatus(Long merchantId) {
        Merchant merchant = findById(merchantId);
        if ("营业中".equals(merchant.getStatus())) {
            merchant.setStatus("休息中");
        } else {
            merchant.setStatus("营业中");
        }
        merchantRepository.save(merchant);
        if (cacheService != null) {
            cacheService.evictHotMerchants();
            cacheService.evictMerchantDetail(merchantId);
        }
    }
}
