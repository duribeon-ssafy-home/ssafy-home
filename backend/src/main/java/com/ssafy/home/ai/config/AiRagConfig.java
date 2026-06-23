package com.ssafy.home.ai.config;

import com.ssafy.home.ai.rag.LocalKeywordEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiRagConfig {

    @Bean
    @ConditionalOnMissingBean
    public EmbeddingModel embeddingModel() {
        return new LocalKeywordEmbeddingModel();
    }

    @Bean
    @ConditionalOnMissingBean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
