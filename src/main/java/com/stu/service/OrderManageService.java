package com.stu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import com.stu.entity.EvaluationDTO;
import com.stu.vo.OrderDetailVO;
import com.stu.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Map;

@Tag(name = "OrderManageService", description = "订单核心服务接口，处理订单全生命周期操作")
public interface OrderManageService {

    /**
     * 查询订单详情
     * @param orderId 订单ID
     * @param userId 当前用户ID（用于权限校验）
     * @return 订单详情VO（含状态描述和允许的操作）
     */
    @Operation(summary = "查询订单详情", description = "返回订单完整信息，包括物品、地址、状态及允许的操作")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = OrderDetailVO.class))),
            @ApiResponse(responseCode = "500", description = "订单不存在或无权限")
    })
    OrderDetailVO getOrderDetail(
            @Parameter(description = "订单ID", required = true, example = "1001") Long orderId,
            @Parameter(description = "当前用户ID", required = true, example = "2001") Long userId);

    /**
     * 修改订单信息
     * @param orderId 订单ID
     * @param userId 当前用户ID
     * @param params 修改参数（支持addressId和scheduledTime）
     * @return 操作结果
     */
    @Operation(summary = "修改订单", description = "仅允许修改状态为“待上门”的订单，支持更新地址和预约时间")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "500", description = "修改失败（状态不允许或参数错误）")
    })
    Result modifyOrder(
            @Parameter(description = "订单ID", required = true, example = "1001") Long orderId,
            @Parameter(description = "当前用户ID", required = true, example = "2001") Long userId,
            @Parameter(description = "修改参数，格式：{\"addressId\":3001, \"scheduledTime\":\"2025-10-16 10:00:00\"}",
                    required = true) Map<String, Object> params);

    /**
     * 取消订单
     * @param orderId 订单ID
     * @param userId 当前用户ID
     * @return 操作结果
     */
    @Operation(summary = "取消订单", description = "仅允许取消状态为“待上门”的订单，取消后状态变为“已取消”")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "500", description = "取消失败（状态不允许或无权限）")
    })
    Result cancelOrder(
            @Parameter(description = "订单ID", required = true, example = "1001") Long orderId,
            @Parameter(description = "当前用户ID", required = true, example = "2001") Long userId);

//    /**
//     * 评价订单服务
//     * @param orderId 订单ID
//     * @param userId 当前用户ID
//     * @param evaluation 评价内容（评分+文字）
//     * @return 操作结果
//     */
//    @Operation(summary = "评价订单", description = "仅允许评价状态为“已完成”的订单，评分范围1-5分")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "评价成功"),
//            @ApiResponse(responseCode = "500", description = "评价失败（状态不允许或参数错误）")
//    })
//    Result evaluateOrder(
//            @Parameter(description = "订单ID", required = true, example = "1001") Long orderId,
//            @Parameter(description = "当前用户ID", required = true, example = "2001") Long userId,
//            @Parameter(description = "评价内容（包含评分和文字）", required = true) EvaluationDTO evaluation);

    /**
     * 再来一单（复制原订单创建新订单）
     * @param orderId 原订单ID
     * @param userId 当前用户ID
     * @return 新订单信息（ID和编号）
     */
    @Operation(summary = "再来一单", description = "复制原订单信息创建新订单，新订单状态为“待上门”")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功",
                    content = @Content(schema = @Schema(example = "{\"newOrderId\":1002, \"newOrderNo\":\"ORD20251012002\"}"))),
            @ApiResponse(responseCode = "500", description = "创建失败（状态不允许或原订单不存在）")
    })
    Map<String, Object> repeatOrder(
            @Parameter(description = "原订单ID", required = true, example = "1001") Long orderId,
            @Parameter(description = "当前用户ID", required = true, example = "2001") Long userId);

    /**
     * 分页查询用户订单列表
     * @param userId 当前用户ID
     * @param page 页码
     * @param size 每页条数
     * @param status 订单状态（可选，为空则查询所有状态）
     * @return 分页订单列表（VO）
     */
    @Operation(summary = "查询用户订单列表", description = "分页返回当前用户的订单，支持按状态筛选")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = IPage.class)))
    IPage<OrderDetailVO> getUserOrders(
            @Parameter(description = "当前用户ID", required = true, example = "2001") Long userId,
            @Parameter(description = "页码（默认1）", example = "1") Integer page,
            @Parameter(description = "每页条数（默认10）", example = "10") Integer size,
            @Parameter(description = "订单状态（0-已取消，1-待上门，2-回收中，3-已完成）", example = "1") Integer status);
}