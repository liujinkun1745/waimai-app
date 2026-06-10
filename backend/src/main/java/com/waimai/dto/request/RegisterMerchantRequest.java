package com.waimai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterMerchantRequest {
    @NotBlank @Size(min = 3, max = 50, message = "用户名长度3-50")
    private String username;

    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank @Size(min = 6, max = 50, message = "密码长度6-50")
    private String password;

    @NotBlank @Size(max = 100, message = "店铺名称最长100字符")
    private String shopName;

    @NotBlank @Size(max = 255, message = "店铺地址最长255字符")
    private String shopAddress;

    @NotBlank @Size(max = 50, message = "营业执照号最长50字符")
    private String businessLicense;

    private String description;
}
