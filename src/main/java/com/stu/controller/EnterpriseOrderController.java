package com.stu.controller;

import com.stu.entity.*;
import com.stu.service.*;
import com.stu.vo.ApiResponse;
import com.stu.vo.OrderResponse;
import com.stu.vo.RecyclingPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.xml.transform.Result;
import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/enterprise")
@Tag(name = "企业订单接口", description = "企业批量回收下单与定期回收计划管理")
@SecurityRequirement(name = "bearerAuth")
public class EnterpriseOrderController {

    @Autowired
    private EnterpriseService enterpriseService;
    @Autowired
    private EnterpriseOrderService enterpriseOrderService;
    @Autowired
    private EnterprisePriceService enterprisePriceService;
    @Autowired
    private OperationLogService operationLogService;
    @Autowired
    private RecyclingPlanService recyclingPlanService;
    @Autowired
    private PlanService planService;
    @Autowired
    private PlanValidator planValidator;

    @PostMapping("/orders/bulk")
    @Operation(summary = "企业批量回收下单", description = "企业用户提交批量回收订单，支持预约时间与发票信息；金额可由运营后续填写")
    public ApiResponse<OrderResponse> createBulkOrder(
            @Parameter(description = "批量回收下单请求体", required = true)
            @RequestBody @Valid BulkOrderRequest request,
            @Parameter(description = "请求头中的Authorization，格式为Bearer {token}", required = true)
            @RequestHeader("Authorization") String token) {

        EnterpriseUser enterprise = enterpriseService.validateEnterpriseQualification(token);

        // 按需关闭系统预估金额，交由运营后续填写
        BigDecimal estimatedAmount = null;
        // 如需启用系统预估计算，改用：
        // BigDecimal estimatedAmount = enterprisePriceService.calculateEnterpriseAmount(
        //         request.getItems(), request.getTotalWeight(), request.getWeightUnit());

        var order = enterpriseOrderService.createBulkOrder(enterprise, request, estimatedAmount);

        operationLogService.logOrderCreation(enterprise.getId(), order.getId(), "BULK");

        return ApiResponse.success(OrderResponse.fromOrder(order));
    }

    @PostMapping("/plans")
    @Operation(summary = "创建定期回收计划", description = "企业用户创建定期回收计划，支持立即开始并生成首单")
    public ApiResponse<RecyclingPlanResponse> createRecyclingPlan(
            @Parameter(description = "定期回收计划创建请求体", required = true)
            @RequestBody @Valid RecyclingPlanRequest request,
            @Parameter(description = "请求头中的Authorization，格式为Bearer {token}", required = true)
            @RequestHeader("Authorization") String token) {

        EnterpriseUser enterprise = enterpriseService.validateEnterpriseQualification(token);

        planValidator.validatePlanRequest(request);

        RecyclingPlan plan = recyclingPlanService.createPlan(enterprise, request);

        if (Boolean.TRUE.equals(request.getStartImmediately())) {
            planService.generateNextOrder(plan);
            plan.setNextScheduleDate(java.sql.Date.valueOf(calculateNextDate(plan.getCycleType())));
        }

        return ApiResponse.success(RecyclingPlanResponse.fromPlan(plan));
    }
    @PostMapping("/invoice/{orderId}")
    @Operation(summary = "企业申请发票", description = "仅允许为状态为“已完成”的企业订单申请发票")
    @Parameters({
            @Parameter(name = "orderId", description = "订单ID", in = ParameterIn.PATH, required = true),
            @Parameter(name = "applyDTO", description = "发票申请信息（抬头+税号）", in = ParameterIn.DEFAULT, required = true),
            @Parameter(name = "Authorization", description = "Bearer Token", in = ParameterIn.HEADER, required = true)
    })
//    public Result applyInvoice(
//            @PathVariable Long orderId,
//            @Valid @RequestBody InvoiceApplyDTO applyDTO,
//            @RequestHeader("Authorization") String authHeader) {
//        String token = authHeader.substring(7);
//        Long userId = jwtUtil.getUserIdFromToken(token);
//
//        User enterprise = userService.getById(userId);
//        if (enterprise == null || !"enterprise".equals(enterprise.getUserType())) {
//            return Result.error("非企业用户，无法申请发票");
//        }
//
//        Invoice invoice = enterpriseOrderService.applyInvoice(orderId, userId, applyDTO);
//        return Result.success(invoice);
//    }

    private LocalDate calculateNextDate(String cycleType) {
        LocalDate base = LocalDate.now();
        return switch (cycleType) {
            case "WEEKLY" -> base.plusWeeks(1);
            case "BIWEEKLY" -> base.plusWeeks(2);
            case "MONTHLY" -> base.plusMonths(1);
            default -> base.plusWeeks(1);
        };
    }
}
