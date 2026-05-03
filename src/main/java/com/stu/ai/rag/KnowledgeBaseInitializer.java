package com.stu.ai.rag;

import com.stu.ai.config.AiProperties;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 启动时将知识库文档切分、向量化并写入 EmbeddingStore，用于 RAG 检索。
 * <p>
 * 当前使用 InMemoryEmbeddingStore，适合本地开发/演示。
 */
@Component
@ConditionalOnProperty(prefix = "ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeBaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseInitializer.class);

    private final AiProperties props;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public KnowledgeBaseInitializer(AiProperties props,
                                   EmbeddingModel embeddingModel,
                                   EmbeddingStore<TextSegment> embeddingStore) {
        this.props = props;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String resourcePath = props.getRag().getKnowledgeResource();
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            log.warn("RAG knowledge resource not found: {}", resourcePath);
            return;
        }

        String text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        List<String> chunks = splitToChunks(text);

        int added = 0;
        for (String chunk : chunks) {
            if (chunk == null || chunk.isBlank()) {
                continue;
            }
            TextSegment segment = TextSegment.from(chunk.trim());
            Embedding embedding = embeddingModel.embed(segment.text()).content();
            embeddingStore.add(embedding, segment);
            added++;
        }

        log.info("RAG knowledge loaded, segments={}, resource={}", added, resourcePath);
    }

    /**
     * 简单切分：按空行切分为段落。
     * 后续可替换为更智能的 splitter（按 token / 标题层级等）。
     */
    private List<String> splitToChunks(String text) {
        String[] parts = text.split("\\R\\s*\\R");
        List<String> chunks = new ArrayList<>();
        for (String part : parts) {
            String p = part.trim();
            if (!p.isEmpty()) {
                chunks.add(p);
            }
        }
        return chunks;
    }
}


