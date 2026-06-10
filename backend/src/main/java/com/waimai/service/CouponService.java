package com.waimai.service;

import com.waimai.entity.Coupon;
import com.waimai.entity.User;
import com.waimai.exception.BusinessException;
import com.waimai.repository.CouponRepository;
import com.waimai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    public List<Coupon> getAvailable(Long userId) {
        return couponRepository.findByUserIdAndUsedFalse(userId);
    }

    public long countAvailable(Long userId) {
        return couponRepository.countByUserIdAndUsedFalse(userId);
    }

    @Transactional
    public void useCoupon(Long couponId, Long userId) {
        Coupon c = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException("优惠券不存在"));
        if (!c.getUser().getId().equals(userId))
            throw new BusinessException("无权使用");
        if (c.getUsed())
            throw new BusinessException("已使用");
        c.setUsed(true);
        couponRepository.save(c);
    }

    /** 创建优惠券（领取神券用） */
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
}
