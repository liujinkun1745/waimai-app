package com.waimai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateShopRequest {
    @NotBlank(message = "店铺名称不能为空")
    private String shopName;

    private String shopAvatar;

    @NotBlank(message = "店铺描述不能为空")
    private String description;

    @NotBlank(message = "营业时间不能为空")
    private String businessHours;

    @NotNull(message = "配送费不能为空")
    private BigDecimal deliveryFee;

    @NotNull(message = "起送价不能为空")
    private BigDecimal minOrderAmount;
}
