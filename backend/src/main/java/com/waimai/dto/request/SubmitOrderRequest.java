package com.waimai.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SubmitOrderRequest {
    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @NotNull(message = "地址ID不能为空")
    private Long addressId;

    @NotEmpty(message = "商品不能为空")
    private List<CartItemRequest> items;

    @NotNull(message = "总金额不能为空")
    private BigDecimal totalAmount;

    private Long couponId;

    @Data
    public static class CartItemRequest {
        @NotNull
        private Long productId;
        @NotNull
        private Integer quantity;
    }
}
