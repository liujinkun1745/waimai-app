package com.waimai.controller;

import com.waimai.config.JwtUtil;
import com.waimai.dto.request.*;
import com.waimai.dto.response.Result;
import com.waimai.entity.User;
import com.waimai.service.CustomUserDetailsService;
import com.waimai.service.UserService;
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
    @Operation(summary = "用户登录", description = "通过用户名+密码登录，返回 JWT Token")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
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
    @Operation(summary = "刷新 Token", description = "用 Refresh Token 换取新的 Access Token")
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
