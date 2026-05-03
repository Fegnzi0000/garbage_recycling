package com.stu.ai.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;

/**
 * 当未配置真实 LLM（如缺少 OpenAI Key）时的兜底模型，避免应用启动失败。
 * <p>
 * 该模型不会进行真实推理，仅返回固定提示。
 */
public class DisabledChatLanguageModel implements ChatLanguageModel {

    private final String reason;

    public DisabledChatLanguageModel(String reason) {
        this.reason = reason;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        String text = "AI 助手当前不可用：" + reason + "。\n" +
                "请在 application.yml / 环境变量中配置 ai.provider 与对应的 OPENAI_API_KEY 或 Ollama 参数，然后重启服务。";
        return Response.from(AiMessage.from(text));
    }
}

