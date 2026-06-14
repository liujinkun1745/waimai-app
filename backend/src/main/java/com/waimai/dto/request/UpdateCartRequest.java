package com.waimai.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCartRequest {
    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @NotNull(message = "数量不能为空")
    private Integer quantity;
}
