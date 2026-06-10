package com.waimai.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClaimCouponRequest {
    @NotBlank(message = "券名称不能为空")
    private String name;

    @NotBlank(message = "券金额不能为空")
    private String amount;

    @NotBlank(message = "最低消费不能为空")
    private String minOrder;
}
