package com.stu.ai.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
@ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(AiProperties props) {
        String provider = props.getProvider() == null ? "openai" : props.getProvider().trim().toLowerCase();

        return switch (provider) {
            case "ollama" -> OllamaChatModel.builder()
                    .baseUrl(props.getOllama().getBaseUrl())
                    .modelName(props.getOllama().getModel())
                    .temperature(props.getOllama().getTemperature())
                    .build();
            case "openai" -> {
                if (!StringUtils.hasText(props.getOpenai().getApiKey())) {
                    throw new IllegalStateException("ai.provider=openai but ai.openai.api-key is empty. Please set OPENAI_API_KEY or ai.openai.api-key");
                }
                yield OpenAiChatModel.builder()
                        .apiKey(props.getOpenai().getApiKey())
                        .modelName(props.getOpenai().getModel())
                        .temperature(props.getOpenai().getTemperature())
                        .build();
            }
            default -> throw new IllegalArgumentException("Unsupported ai.provider: " + provider);
        };
    }

    /**
     * 本地 Embedding 模型：用于 RAG 向量化（不依赖外部 key）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ContentRetriever contentRetriever(AiProperties props,
                                             EmbeddingStore<TextSegment> embeddingStore,
                                             EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(props.getRag().getMaxResults())
                .minScore(props.getRag().getMinScore())
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider(AiProperties props) {
        Map<Object, ChatMemory> memories = new ConcurrentHashMap<>();
        return memoryId -> memories.computeIfAbsent(memoryId, id -> MessageWindowChatMemory.withMaxMessages(props.getMemory().getMaxMessages()));
    }
}


