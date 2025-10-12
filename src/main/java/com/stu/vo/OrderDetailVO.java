// filePath：garbage_recycling/src/main/java/com/stu/vo/OrderDetailVO.java
package com.stu.vo;

import com.stu.entity.Order;
import com.stu.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "订单详情VO")
public class OrderDetailVO {
    @Schema(description = "订单ID", example = "1001")
    private Long id;

    @Schema(description = "订单编号", example = "ORD20251012001")
    private String orderNo;

    @Schema(description = "订单类型（1-个人，2-高校，3-企业）", example = "1")
    private Integer orderType;

    @Schema(description = "订单状态编码", example = "1")
    private Integer status;

    @Schema(description = "订单状态描述", example = "待上门")
    private String statusDesc;

    @Schema(description = "允许的操作列表", example = "[\"modify\",\"cancel\",\"detail\"]")
    private List<String> allowOperations;

    @Schema(description = "物品信息（JSON字符串）", example = "[{\"category\":\"废纸\",\"weight\":2.5}]")
    private String items;

    @Schema(description = "图片URL（JSON字符串）", example = "[\"https://example.com/img1.jpg\"]")
    private String images;

    @Schema(description = "预估金额（元）", example = "15.50")
    private BigDecimal estimatedAmount;

    @Schema(description = "预约时间", example = "2025-10-15 14:30:00")
    private Date scheduledTime;

    @Schema(description = "创建时间", example = "2025-10-12 10:00:00")
    private Date createdAt;

    @Schema(description = "从Order实体转换为VO")
    public static OrderDetailVO fromOrder(Order order) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType(order.getOrderType());
        vo.setStatus(order.getStatus());

        OrderStatus statusEnum = order.getStatusEnum();
        if (statusEnum != null) {
            vo.setStatusDesc(statusEnum.getDesc());
            vo.setAllowOperations(statusEnum.getAllowOperations());
        }

        vo.setItems(order.getItems());
        vo.setImages(order.getImages());
        vo.setEstimatedAmount(order.getEstimatedAmount());
        vo.setScheduledTime(order.getScheduledTime());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }
}