package com.stu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.stu.entity.EvaluationDTO;
import com.stu.entity.Order;
import com.stu.entity.UserAddress;
import com.stu.enums.OrderStatus;

import com.stu.mapper.OrderMapper;
import com.stu.mapper.UserAddressMapper;
import com.stu.service.OrderManageService;
import com.stu.service.OrderService;
import com.stu.vo.OrderDetailVO;
import com.stu.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderManageServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderManageService {

    @Autowired
    private OrderMapper orderMapper;


    @Autowired
    private UserAddressMapper addressMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 查看订单详情
     */
    @Override
    public OrderDetailVO getOrderDetail(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在或无权限");
        }
        return OrderDetailVO.fromOrder(order);
    }

    /**
     * 修改订单
     */
    @Override
    @Transactional
    public Result modifyOrder(Long orderId, Long userId, Map<String, Object> params) {
        Order order = validateOrder(orderId, userId, "modify");

        // 修改地址（如果有）
        if (params.containsKey("addressId")) {
            Long addressId = Long.valueOf(params.get("addressId").toString());
            UserAddress address = addressMapper.selectById(addressId);
            if (address == null || !address.getUserId().equals(userId)) {
                return Result.error("地址无效或不属于当前用户");
            }
            order.setAddressId(addressId);
        }

        // 修改预约时间（如果有）
        if (params.containsKey("scheduledTime")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                order.setScheduledTime(sdf.parse(params.get("scheduledTime").toString()));
            } catch (Exception e) {
                return Result.error("预约时间格式错误");
            }
        }

        order.setUpdatedAt(new Date());
        orderMapper.updateById(order);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("message", "订单修改成功");
        return Result.success(resultMap);
    }

    /**
     * 取消订单
     */
    @Override
    @Transactional
    public Result cancelOrder(Long orderId, Long userId) {
        Order order = validateOrder(orderId, userId, "cancel");
        order.setStatus(OrderStatus.CANCELED.getCode());
        order.setUpdatedAt(new Date());
        orderMapper.updateById(order);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("message", "订单取消成功");
        return Result.success(resultMap);
    }

//    /**
//     * 评价订单
//     */
//    @Override
//    @Transactional
//    public Result evaluateOrder(Long orderId, Long userId, EvaluationDTO evaluation) {
//        Order order = validateOrder(orderId, userId, "evaluate");
//
//        Evaluation eval = new Evaluation();
//        eval.setOrderId(orderId);
//        eval.setUserId(userId);
//        eval.setScore(evaluation.getScore());
//        eval.setContent(evaluation.getContent());
//        eval.setCreatedAt(new Date());
//        evaluationMapper.insert(eval);
//
//        return Result.success("评价成功");
//    }

    /**
     * 再来一单
     */
    @Override
    @Transactional
    public Map<String, Object> repeatOrder(Long orderId, Long userId) {
        Order original = validateOrder(orderId, userId, "repeat");

        // 复制原订单信息创建新订单
        Order newOrder = new Order();
        newOrder.setOrderNo("ORD" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        newOrder.setUserId(userId);
        newOrder.setAddressId(original.getAddressId());
        newOrder.setOrderType(original.getOrderType());
        newOrder.setCampusType(original.getCampusType());
        newOrder.setStatus(OrderStatus.PENDING_VISIT.getCode()); // 重置为待上门
        newOrder.setItems(original.getItems());
        newOrder.setImages(original.getImages());
        newOrder.setCampusInfo(original.getCampusInfo());
        newOrder.setEstimatedAmount(original.getEstimatedAmount());
        newOrder.setCreatedAt(new Date());
        newOrder.setUpdatedAt(new Date());

        orderMapper.insert(newOrder);

        Map<String, Object> result = new HashMap<>();
        result.put("newOrderId", newOrder.getId());
        result.put("newOrderNo", newOrder.getOrderNo());
        return result;
    }

    /**
     * 查询用户订单列表
     */
    @Override
    public IPage<OrderDetailVO> getUserOrders(Long userId, Integer page, Integer size, Integer status) {
        Page<Order> orderPage = new Page<>(page, size);
        QueryWrapper<Order> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        if (status != null) {
            query.eq("status", status);
        }
        query.orderByDesc("created_at");

        IPage<Order> pageResult = orderMapper.selectPage(orderPage, query);

        // 转换为VO
        return pageResult.convert(OrderDetailVO::fromOrder);
    }

    /**
     * 通用订单校验（存在性、权限、操作权限）
     */
    private Order validateOrder(Long orderId, Long userId, String operation) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权限操作该订单");
        }
        if (!order.checkOperation(operation)) {
            OrderStatus status = order.getStatusEnum();
            throw new RuntimeException(String.format("当前订单状态（%s）不允许执行【%s】操作",
                    status.getDesc(), getOperationName(operation)));
        }
        return order;
    }

    /**
     * 操作代码转中文名称（用于错误提示）
     */
    private String getOperationName(String operation) {
        switch (operation) {
            case "modify": return "修改订单";
            case "cancel": return "取消订单";
            case "detail": return "查看详情";
            case "evaluate": return "评价服务";
            case "invoice": return "申请发票";
            case "repeat": return "再来一单";
            default: return operation;
        }
    }
}