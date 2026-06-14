package com.waimai.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartRequest {
    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "数量不能为空")
    private Integer quantity;
}
