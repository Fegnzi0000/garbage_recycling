package com.stu.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stu.service.PersonOrderService;
import com.stu.service.UserAddressService;
import com.stu.vo.Result;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 下单草稿服务：
 * <ul>
 *     <li>生成草稿并存入 Redis（绑定 userId + TTL）</li>
 *     <li>二次确认后提交草稿创建真实订单</li>
 * </ul>
 */
@Service
public class OrderDraftService {

    private static final String DRAFT_KEY_PREFIX = "draft:order:";

    /**
     * 草稿默认 TTL：30 分钟。
     */
    private static final long DRAFT_TTL_MINUTES = 30;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private final UserAddressService userAddressService;
    private final PersonOrderService personOrderService;

    public OrderDraftService(StringRedisTemplate stringRedisTemplate,
                             ObjectMapper objectMapper,
                             UserAddressService userAddressService,
                             PersonOrderService personOrderService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.userAddressService = userAddressService;
        this.personOrderService = personOrderService;
    }

    public OrderDraftCreateResult createPersonalDraft(Long userId, Long addressId, String scheduledTime, Object items) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }
        if (addressId == null) {
            throw new IllegalArgumentException("addressId is null");
        }

        // 地址归属校验（避免草稿里写入非法 addressId）
        var addr = userAddressService.getById(addressId);
        if (addr == null) {
            throw new IllegalArgumentException("地址不存在");
        }
        if (!userId.equals(addr.getUserId())) {
            throw new IllegalArgumentException("地址不属于当前用户");
        }

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("addressId", addressId);
        if (StringUtils.hasText(scheduledTime)) {
            orderData.put("scheduledTime", scheduledTime);
        }
        if (items != null) {
            orderData.put("items", items);
        }

        String draftId = UUID.randomUUID().toString().replace("-", "");
        OrderDraft draft = new OrderDraft(userId, "personal", orderData, LocalDateTime.now());

        String key = DRAFT_KEY_PREFIX + draftId;
        try {
            String json = objectMapper.writeValueAsString(draft);
            stringRedisTemplate.opsForValue().set(key, json, DRAFT_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new IllegalStateException("草稿保存失败", e);
        }

        String summary = buildPersonalDraftSummary(addressId, scheduledTime, items);
        return new OrderDraftCreateResult(draftId, summary);
    }

    public Result confirmAndSubmitPersonalDraft(Long userId, String draftId) {
        if (userId == null) {
            return Result.error("未获取到用户信息");
        }
        if (!StringUtils.hasText(draftId)) {
            return Result.error("draftId 不能为空");
        }

        String key = DRAFT_KEY_PREFIX + draftId;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return Result.error("草稿不存在或已过期");
        }

        try {
            OrderDraft draft = objectMapper.readValue(json, OrderDraft.class);
            if (draft == null) {
                return Result.error("草稿解析失败");
            }
            if (!userId.equals(draft.getUserId())) {
                return Result.error("草稿不属于当前用户");
            }
            if (!"personal".equalsIgnoreCase(draft.getDraftType())) {
                return Result.error("草稿类型不支持提交");
            }

            Map<String, Object> orderData = objectMapper.convertValue(draft.getOrderData(), new TypeReference<Map<String, Object>>() {
            });

            // 提交真实订单
            Result result = personOrderService.createPersonalOrder(orderData, userId);
            // 无论成功失败，都可选择删除草稿；这里选择成功后删除，失败保留方便用户修正后再提交
            if (result != null && result.getCode() != null && result.getCode() == 200) {
                stringRedisTemplate.delete(key);
            }
            return result;
        } catch (Exception e) {
            return Result.error("草稿提交失败: " + e.getMessage());
        }
    }

    private String buildPersonalDraftSummary(Long addressId, String scheduledTime, Object items) {
        StringBuilder sb = new StringBuilder();
        sb.append("已生成个人回收订单草稿：");
        sb.append("addressId=").append(addressId);
        if (StringUtils.hasText(scheduledTime)) {
            sb.append(", scheduledTime=").append(scheduledTime);
        }
        if (items != null) {
            sb.append(", items=已填写");
        } else {
            sb.append(", items=未填写");
        }
        sb.append("。\n");
        sb.append("请二次确认后再提交订单。");
        return sb.toString();
    }
}

