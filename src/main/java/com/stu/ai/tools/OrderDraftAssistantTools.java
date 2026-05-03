package com.stu.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stu.ai.service.OrderDraftCreateResult;
import com.stu.ai.service.OrderDraftService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.util.StringUtils;

/**
 * 助手工具：仅生成“下单草稿”，不直接创建真实订单。
 */
public class OrderDraftAssistantTools {

    private final Long userId;
    private final OrderDraftService orderDraftService;
    private final ObjectMapper objectMapper;

    public OrderDraftAssistantTools(Long userId, OrderDraftService orderDraftService, ObjectMapper objectMapper) {
        this.userId = userId;
        this.orderDraftService = orderDraftService;
        this.objectMapper = objectMapper;
    }

    @Tool("create_personal_order_draft")
    public String createPersonalOrderDraft(Long addressId, String scheduledTime, String itemsJson) {
        try {
            Object items = null;
            if (StringUtils.hasText(itemsJson)) {
                // itemsJson 期望为 JSON 数组/对象
                items = objectMapper.readValue(itemsJson, Object.class);
            }
            OrderDraftCreateResult r = orderDraftService.createPersonalDraft(userId, addressId, scheduledTime, items);
            return "草稿已生成。draftId=" + r.getDraftId() + "\n" + r.getSummary() +
                    "\n二次确认：请调用 POST /api/assistant/order/draft/confirm 提交该 draftId。";
        } catch (Exception e) {
            return "生成草稿失败：" + e.getMessage();
        }
    }
}

