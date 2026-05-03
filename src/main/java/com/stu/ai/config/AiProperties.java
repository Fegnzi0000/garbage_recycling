package com.stu.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 相关配置。
 */
@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 是否启用助手。
     */
    private boolean enabled = true;

    /**
     * openai / ollama
     */
    private String provider = "openai";

    private OpenAi openai = new OpenAi();

    private Ollama ollama = new Ollama();

    private Rag rag = new Rag();

    private Memory memory = new Memory();

    @Data
    public static class OpenAi {
        /**
         * 建议通过环境变量注入：OPENAI_API_KEY
         */
        private String apiKey;

        /**
         * 例如：gpt-4o-mini
         */
        private String model = "gpt-4o-mini";

        private Double temperature = 0.2;
    }

    @Data
    public static class Ollama {
        /**
         * 例如：http://localhost:11434
         */
        private String baseUrl = "http://localhost:11434";

        /**
         * 例如：qwen2.5:7b / llama3.1
         */
        private String model = "qwen2.5:7b";

        private Double temperature = 0.2;
    }

    @Data
    public static class Rag {
        private boolean enabled = true;

        /**
         * 取回文档片段数量
         */
        private int maxResults = 4;

        /**
         * 相似度阈值（不同 embedding 模型语义有差异，可按实际调优）
         */
        private double minScore = 0.6;

        /**
         * 知识库资源路径（classpath）
         */
        private String knowledgeResource = "rag/recycling_knowledge.md";
    }

    @Data
    public static class Memory {
        /**
         * 每个 session 保存的最大消息数。
         */
        private int maxMessages = 20;
    }
}

