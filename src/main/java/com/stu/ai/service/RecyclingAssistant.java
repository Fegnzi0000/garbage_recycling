package com.stu.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 回收助手（LangChain4j AI Service）。
 * <p>
 * 说明：本助手使用 RAG（若启用）以及 Tool 调用（仅下单草稿）来辅助用户。
 */
public interface RecyclingAssistant {

    @SystemMessage("""
            你是一个垃圾回收系统的智能助手（Recycling Assistant）。
            你的目标：帮助用户理解垃圾分类、下单流程，并在需要时调用工具生成“下单草稿”。

            安全规则：
            1) 你只能生成下单草稿（draft），不要直接创建真实订单。
            2) 如果用户希望提交订单，请提示用户进行二次确认：调用后端确认接口提交草稿。
            3) 不要在回答中输出用户手机号、完整地址等隐私信息；如必须提及，请做脱敏。
            4) 如果用户的问题超出知识库或无法确定，请明确说明不确定，并给出建议的下一步（例如查询分类接口或联系人工）。
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}

