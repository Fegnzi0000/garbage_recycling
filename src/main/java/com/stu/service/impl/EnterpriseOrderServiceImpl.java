package com.stu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.stu.entity.*;
import com.stu.mapper.EnterpriseOrderMapper;
import com.stu.service.EnterpriseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Date;

@Service
public class EnterpriseOrderServiceImpl implements EnterpriseOrderService {

    @Autowired
    private EnterpriseOrderMapper enterpriseOrderMapper;

    @Override
    public EnterpriseOrder createBulkOrder(EnterpriseUser enterprise, BulkOrderRequest request, BigDecimal estimatedAmount) {
        EnterpriseOrder order = new EnterpriseOrder();
        order.setOrderNo(generateOrderNo());
        order.setEnterpriseId(enterprise.getId());
        order.setOrderType("BULK");
        order.setTotalWeight(request.getTotalWeight());
        order.setWeightUnit(request.getWeightUnit().name());
        order.setEstimatedAmount(estimatedAmount);
        order.setInvoiceRequired(Boolean.TRUE.equals(request.getInvoiceRequired()));
        if (request.getInvoiceInfo() != null) {
            order.setInvoiceTitle(request.getInvoiceInfo().getInvoiceTitle());
            order.setTaxNumber(request.getInvoiceInfo().getTaxNumber());
        } else {
            order.setInvoiceTitle(enterprise.getInvoiceTitle());
            order.setTaxNumber(enterprise.getTaxNumber());
        }
        order.setInvoiceStatus("NOT_APPLIED");
        if (request.getScheduleTime() != null) {
            order.setScheduleTime(Date.from(request.getScheduleTime().atZone(ZoneId.systemDefault()).toInstant()));
        }
        order.setStatus("PENDING");
        order.setPickupAddress(request.getPickupAddress());
        order.setContactPerson(request.getContactPerson());
        order.setContactPhone(request.getContactPhone());
        enterpriseOrderMapper.insert(order);
        return order;
    }

//    /**
//     * 企业申请发票
//     */
//    @Override
//    @Transactional
//    public Invoice applyInvoice(Long orderId, Long enterpriseId, InvoiceApplyDTO applyDTO) {
//        // 校验订单
//        Order order = orderMapper.selectById(orderId);
//        if (order == null || !order.getUserId().equals(enterpriseId) || order.getOrderType() != 3) {
//            throw new RuntimeException("企业订单不存在或无权限");
//        }
//        if (!order.checkOperation("invoice")) {
//            throw new RuntimeException("当前订单状态不允许申请发票");
//        }
//
//        // 校验是否已申请过发票
//        QueryWrapper<Invoice> query = new QueryWrapper<>();
//        query.eq("order_id", orderId);
//        if (invoiceMapper.selectCount(query) > 0) {
//            throw new RuntimeException("该订单已申请过发票");
//        }
//
//        // 创建发票记录
//        Invoice invoice = new Invoice();
//        invoice.setOrderId(orderId);
//        invoice.setEnterpriseId(enterpriseId);
//        invoice.setTitle(applyDTO.getTitle());
//        invoice.setTaxNumber(applyDTO.getTaxNumber());
//        invoice.setAmount(order.getEstimatedAmount());
//        invoice.setStatus("APPLIED"); // 申请中
//        invoice.setAppliedAt(new Date());
//        invoiceMapper.insert(invoice);
//
//        return invoice;
//    }

    private String generateOrderNo() {
        return "E" + IdWorker.getIdStr();
    }
}

