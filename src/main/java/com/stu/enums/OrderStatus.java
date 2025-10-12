// filePath：garbage_recycling/src/main/java/com/stu/enums/OrderStatus.java
package com.stu.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@Schema(description = "订单状态枚举（包含允许的操作）")
public enum OrderStatus {
    @Schema(description = "已取消订单，允许操作：再来一单、查看详情")
    CANCELED(0, "已取消", Arrays.asList("repeat", "detail")),

    @Schema(description = "待上门订单，允许操作：修改订单、取消订单、查看详情")
    PENDING_VISIT(1, "待上门", Arrays.asList("modify", "cancel", "detail")),

    @Schema(description = "回收中订单，允许操作：查看详情")
    RECOVERING(2, "回收中", Arrays.asList("detail")),

    @Schema(description = "已完成订单，允许操作：评价服务、申请发票、再来一单、查看详情")
    COMPLETED(3, "已完成", Arrays.asList("evaluate", "invoice", "repeat", "detail"));

    @Schema(description = "状态编码")
    private final Integer code;

    @Schema(description = "状态描述")
    private final String desc;

    @Schema(description = "允许的操作列表")
    private final List<String> allowOperations;

    @Schema(description = "校验操作是否允许")
    public boolean isAllowed(String operation) {
        return allowOperations.contains(operation);
    }

    @Schema(description = "根据状态码获取枚举")
    public static OrderStatus getByCode(Integer code) {
        if (code == null) return null;
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    @Schema(description = "获取所有状态码")
    public static List<Integer> getAllCodes() {
        return Arrays.stream(values()).map(OrderStatus::getCode).collect(Collectors.toList());
    }
}