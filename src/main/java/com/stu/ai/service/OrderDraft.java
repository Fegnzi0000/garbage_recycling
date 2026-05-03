package com.stu.ai.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 下单草稿（存储在 Redis）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDraft {

    /**
     * 草稿归属用户
     */
    private Long userId;

    /**
     * 草稿类型：personal / campus / enterprise ...
     */
    private String draftType;

    /**
     * 下单参数（最终会传给对应 service 创建订单）
     */
    private Map<String, Object> orderData;

    private LocalDateTime createdAt;
}

