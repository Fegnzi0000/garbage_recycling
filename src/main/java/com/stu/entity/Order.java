package com.stu.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stu.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@TableName("order")
@Schema(description = "订单实体类")
public class Order {
    @TableId(type = IdType.AUTO)
    @Schema(description = "订单ID", example = "1001")
    private Long id;

    @TableField("order_no")
    @Schema(description = "订单编号", example = "ORD20251012001")
    private String orderNo;

    @TableField("user_id")
    @Schema(description = "用户ID", example = "2001")
    private Long userId;

    @TableField("address_id")
    @Schema(description = "地址ID", example = "3001")
    private Long addressId;

    @TableField("order_type")
    @Schema(description = "订单类型（1-个人，2-高校，3-企业）", example = "1")
    private Integer orderType;

    @TableField("campus_type")
    @Schema(description = "校园订单类型（仅高校订单使用）", example = "1")
    private Integer campusType;

    @TableField("status")
    @Schema(description = "订单状态（0-已取消，1-待上门，2-回收中，3-已完成）", example = "1")
    private Integer status;

    @TableField("items")
    @Schema(description = "物品信息（JSON字符串）", example = "[{\"category\":\"废纸\",\"weight\":2.5}]")
    private String items;

    @TableField("images")
    @Schema(description = "图片URL（JSON字符串）", example = "[\"https://example.com/img1.jpg\"]")
    private String images;

    @TableField("campus_info")
    @Schema(description = "校园订单特有信息（JSON字符串）", example = "{\"school\":\"清华大学\"}")
    private String campusInfo;

    @TableField("estimated_amount")
    @Schema(description = "预估金额（元）", example = "15.50")
    private BigDecimal estimatedAmount;

    @TableField("scheduled_time")
    @Schema(description = "预约时间", example = "2025-10-15 14:30:00")
    private Date scheduledTime;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "创建时间", example = "2025-10-12 10:00:00")
    private Date createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间", example = "2025-10-12 10:00:00")
    private Date updatedAt;

    @TableField(exist = false)
    @Schema(description = "状态描述", example = "待上门")
    private String statusDesc;

    @TableField(exist = false)
    @Schema(description = "允许的操作列表", example = "[\"modify\",\"cancel\",\"detail\"]")
    private List<String> allowOperations;

    @JsonIgnore
    @Schema(hidden = true)
    public OrderStatus getStatusEnum() {
        return OrderStatus.getByCode(this.status);
    }

    @Schema(description = "校验操作是否允许")
    public boolean checkOperation(String operation) {
        OrderStatus status = getStatusEnum();
        return status != null && status.isAllowed(operation);
    }
}