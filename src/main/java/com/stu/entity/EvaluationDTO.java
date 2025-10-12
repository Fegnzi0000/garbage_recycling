package com.stu.entity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
@Schema(description = "订单评价DTO")
public class EvaluationDTO {
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    @Schema(description = "评分（1-5分）", example = "5")
    private Integer score;

    @NotBlank(message = "评价内容不能为空")
    @Schema(description = "评价内容", example = "回收员服务很专业，效率高")
    private String content;
}