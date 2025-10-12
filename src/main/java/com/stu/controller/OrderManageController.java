package com.stu.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.stu.entity.EvaluationDTO;
import com.stu.service.OrderManageService;
import com.stu.service.OrderService;
import com.stu.util.JwtUtil;
import com.stu.vo.OrderDetailVO;
import com.stu.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/manage/order")
@Tag(name = "订单管理接口", description = "提供订单查询、修改、取消、评价等功能")
public class OrderManageController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private OrderManageService orderManageService;

    private Long getUserId(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @GetMapping("/detail/{orderId}")
    @Operation(summary = "查看订单详情", description = "返回订单完整信息及允许的操作")
    @Parameters({
            @Parameter(name = "orderId", description = "订单ID", in = ParameterIn.PATH, required = true),
            @Parameter(name = "Authorization", description = "Bearer Token", in = ParameterIn.HEADER, required = true)
    })
    public Result getOrderDetail(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        OrderDetailVO detail = orderManageService.getOrderDetail(orderId, userId);
        return Result.success(detail);
    }

    @PutMapping("/modify/{orderId}")
    @Operation(summary = "修改订单信息", description = "仅允许修改状态为“待上门”的订单（支持修改地址和预约时间）")
    @Parameters({
            @Parameter(name = "orderId", description = "订单ID", in = ParameterIn.PATH, required = true),
            @Parameter(name = "params", description = "修改参数（addressId/scheduledTime）", in = ParameterIn.DEFAULT, required = true),
            @Parameter(name = "Authorization", description = "Bearer Token", in = ParameterIn.HEADER, required = true)
    })
    public Result modifyOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> params,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        return orderManageService.modifyOrder(orderId, userId, params);
    }

    @PostMapping("/manage/cancel/{orderId}")
    @Operation(summary = "取消订单", description = "仅允许取消状态为“待上门”的订单")
    @Parameters({
            @Parameter(name = "orderId", description = "订单ID", in = ParameterIn.PATH, required = true),
            @Parameter(name = "Authorization", description = "Bearer Token", in = ParameterIn.HEADER, required = true)
    })
    public Result cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        return orderManageService.cancelOrder(orderId, userId);
    }

//    @PostMapping("/evaluate/{orderId}")
//    @Operation(summary = "评价回收服务", description = "仅允许评价状态为“已完成”的订单")
//    @Parameters({
//            @Parameter(name = "orderId", description = "订单ID", in = ParameterIn.PATH, required = true),
//            @Parameter(name = "evaluation", description = "评价内容（评分1-5分+评价文字）", in = ParameterIn.DEFAULT, required = true),
//            @Parameter(name = "Authorization", description = "Bearer Token", in = ParameterIn.HEADER, required = true)
//    })
//    public Result evaluateOrder(
//            @PathVariable Long orderId,
//            @Valid @RequestBody EvaluationDTO evaluation,
//            @RequestHeader("Authorization") String authHeader) {
//        Long userId = getUserId(authHeader);
//        return orderService.evaluateOrder(orderId, userId, evaluation);
//    }

    @PostMapping("/repeat/{orderId}")
    @Operation(summary = "再来一单", description = "复制原订单信息创建新订单（状态重置为待上门）")
    @Parameters({
            @Parameter(name = "orderId", description = "原订单ID", in = ParameterIn.PATH, required = true),
            @Parameter(name = "Authorization", description = "Bearer Token", in = ParameterIn.HEADER, required = true)
    })
    public Result repeatOrder(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        Map<String, Object> newOrder = orderManageService.repeatOrder(orderId, userId);
        return Result.success(newOrder);
    }

    @GetMapping("/manage/list")
    @Operation(summary = "查询用户订单列表", description = "分页查询当前用户的订单，支持按状态筛选")
    @Parameters({
            @Parameter(name = "page", description = "页码（默认1）", in = ParameterIn.QUERY, required = false),
            @Parameter(name = "size", description = "每页条数（默认10）", in = ParameterIn.QUERY, required = false),
            @Parameter(name = "status", description = "订单状态（0-已取消，1-待上门，2-回收中，3-已完成）", in = ParameterIn.QUERY, required = false),
            @Parameter(name = "Authorization", description = "Bearer Token", in = ParameterIn.HEADER, required = true)
    })
    public Result getOrderList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        IPage<OrderDetailVO> orderPage = orderManageService.getUserOrders(userId, page, size, status);
        return Result.success(orderPage);
    }
}
