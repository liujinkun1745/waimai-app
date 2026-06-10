package com.waimai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitReviewRequest {
    @NotNull @Min(1) @Max(5)
    private Integer tasteRating;

    @NotNull @Min(1) @Max(5)
    private Integer packagingRating;

    @NotNull @Min(1) @Max(5)
    private Integer deliveryRating;

    private String comment;
}
