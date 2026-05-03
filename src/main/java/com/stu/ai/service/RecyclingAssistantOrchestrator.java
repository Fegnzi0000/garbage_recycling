package com.stu.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stu.ai.tools.AddressAssistantTools;
import com.stu.ai.tools.OrderDraftAssistantTools;
import com.stu.service.UserAddressService;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * 构建并调用 LangChain4j Assistant。
 * <p>
 * 说明：这里按“每次请求创建 assistant 实例”的方式构建，便于注入 userId 绑定的工具对象。
 */
@Service
@ConditionalOnBean(ChatLanguageModel.class)
public class RecyclingAssistantOrchestrator {

    private final ChatLanguageModel chatLanguageModel;
    private final ChatMemoryProvider chatMemoryProvider;
    private final ObjectMapper objectMapper;

    private final UserAddressService userAddressService;
    private final OrderDraftService orderDraftService;

    /**
     * RAG 检索器（可选）。
     */
    private final ContentRetriever contentRetriever;

    public RecyclingAssistantOrchestrator(ChatLanguageModel chatLanguageModel,
                                          ChatMemoryProvider chatMemoryProvider,
                                          ObjectMapper objectMapper,
                                          UserAddressService userAddressService,
                                          OrderDraftService orderDraftService,
                                          @Nullable ContentRetriever contentRetriever) {
        this.chatLanguageModel = chatLanguageModel;
        this.chatMemoryProvider = chatMemoryProvider;
        this.objectMapper = objectMapper;
        this.userAddressService = userAddressService;
        this.orderDraftService = orderDraftService;
        this.contentRetriever = contentRetriever;
    }

    public String chat(Long userId, String sessionId, String message) {
        AddressAssistantTools addressTools = new AddressAssistantTools(userId, userAddressService);
        OrderDraftAssistantTools draftTools = new OrderDraftAssistantTools(userId, orderDraftService, objectMapper);

        var builder = AiServices.builder(RecyclingAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(addressTools, draftTools);

        // 如果 RAG 启用则注入检索器
        if (contentRetriever != null) {
            builder.contentRetriever(contentRetriever);
        }

        RecyclingAssistant assistant = builder.build();
        return assistant.chat(sessionId, message);
    }
}


