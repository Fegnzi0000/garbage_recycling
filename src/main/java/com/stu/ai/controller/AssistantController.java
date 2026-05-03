package com.stu.ai.controller;

import com.stu.ai.controller.dto.AssistantChatRequest;
import com.stu.ai.controller.dto.AssistantChatResponse;
import com.stu.ai.controller.dto.ConfirmDraftRequest;
import com.stu.ai.service.OrderDraftService;
import com.stu.ai.service.RecyclingAssistantOrchestrator;
import com.stu.util.JwtUtil;
import com.stu.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/assistant")
@Tag(name = "回收助手（LangChain4j）", description = "RAG 问答 + 工具调用（下单草稿）+ 二次确认")
@SecurityRequirement(name = "bearerAuth")
public class AssistantController {

    private final RecyclingAssistantOrchestrator assistantOrchestrator;
    private final OrderDraftService orderDraftService;
    private final JwtUtil jwtUtil;

    public AssistantController(RecyclingAssistantOrchestrator assistantOrchestrator,
                               OrderDraftService orderDraftService,
                               JwtUtil jwtUtil) {
        this.assistantOrchestrator = assistantOrchestrator;
        this.orderDraftService = orderDraftService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/chat")
    @Operation(summary = "回收助手对话", description = "RAG 检索增强问答；如用户表达下单意图，助手可调用工具生成下单草稿（不会直接创建真实订单）。")
    public Result chat(
            @RequestBody AssistantChatRequest req,
            @Parameter(description = "请求头 Authorization: Bearer {token}", required = true)
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Long userId = jwtUtil.getUserIdFromToken(token);

        String sessionId = (req.getSessionId() == null || req.getSessionId().isBlank())
                ? UUID.randomUUID().toString().replace("-", "")
                : req.getSessionId();

        if (req.getMessage() == null || req.getMessage().isBlank()) {
            return Result.error("message 不能为空");
        }

        String answer = assistantOrchestrator.chat(userId, sessionId, req.getMessage());
        return Result.success(new AssistantChatResponse(sessionId, answer));
    }

    @PostMapping("/order/draft/confirm")
    @Operation(summary = "二次确认并提交草稿（创建真实订单）", description = "仅当用户显式确认并提交 draftId 后，才会创建真实订单。")
    public Result confirmDraft(
            @RequestBody ConfirmDraftRequest req,
            @Parameter(description = "请求头 Authorization: Bearer {token}", required = true)
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Long userId = jwtUtil.getUserIdFromToken(token);
        return orderDraftService.confirmAndSubmitPersonalDraft(userId, req.getDraftId());
    }
}

