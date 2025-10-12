package com.stu.entity;
// filePath：garbage_recycling/src/main/java/com/stu/dto/InvoiceApplyDTO.java


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
@Schema(description = "发票申请DTO")
public class InvoiceApplyDTO {
    @NotBlank(message = "发票抬头不能为空")
    @Schema(description = "发票抬头", example = "北京科技有限公司")
    private String title;

    @NotBlank(message = "税号不能为空")
    @Schema(description = "纳税人识别号", example = "91110108MA01234567")
    private String taxNumber;
}