package com.waimai.service;

import com.waimai.entity.*;
import com.waimai.exception.BusinessException;
import com.waimai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final BalanceRecordRepository balanceRecordRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerConsumer(String username, String phone, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException("手机号已被注册");
        }
        User user = User.builder()
                .username(username)
                .phone(phone)
                .password(passwordEncoder.encode(password))
                .email(email)
                .role("ROLE_CONSUMER")
                .balance(BigDecimal.ZERO)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User registerMerchant(String username, String phone, String password,
                                  String shopName, String shopAddress, String businessLicense,
                                  String description) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException("手机号已被注册");
        }
        User user = User.builder()
                .username(username)
                .phone(phone)
                .password(passwordEncoder.encode(password))
                .role("ROLE_MERCHANT")
                .balance(BigDecimal.ZERO)
                .build();
        user = userRepository.save(user);

        Merchant merchant = Merchant.builder()
                .user(user)
                .shopName(shopName)
                .shopAddress(shopAddress)
                .businessLicense(businessLicense)
                .description(description)
                .build();
        merchantRepository.save(merchant);

        return user;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    public void updateProfile(Long userId, String email) {
        User user = findById(userId);
        user.setEmail(email);
        userRepository.save(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = findById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void recharge(Long userId, BigDecimal amount) {
        User user = findById(userId);
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);

        BalanceRecord record = BalanceRecord.builder()
                .user(user)
                .amount(amount)
                .type("充值")
                .description("余额充值")
                .build();
        balanceRecordRepository.save(record);
    }
}
