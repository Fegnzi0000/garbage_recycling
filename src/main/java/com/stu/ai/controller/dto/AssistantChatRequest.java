package com.stu.ai.controller.dto;

import lombok.Data;

@Data
public class AssistantChatRequest {

    /**
     * 前端传入的会话 id；不传则由后端生成并返回。
     */
    private String sessionId;

    /**
     * 用户输入
     */
    private String message;
}

