package com.stu.ai.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssistantChatResponse {

    private String sessionId;

    private String answer;
}

