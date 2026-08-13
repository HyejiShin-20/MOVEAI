package com.moveai.retrieval;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.moveai.knowledge.embedding.EmbeddingTextBuilder;

@Configuration
public class RetrievalConfiguration {

    @Bean
    CandidateCollector candidateCollector() {
        return new CandidateCollector();
    }

    @Bean
    ConditionEvaluator conditionEvaluator() {
        return new ConditionEvaluator();
    }

    @Bean
    QueryTextBuilder queryTextBuilder() {
        return new QueryTextBuilder();
    }

    @Bean
    CosineCalculator cosineCalculator() {
        return new CosineCalculator();
    }

    @Bean
    RankingService rankingService(CosineCalculator cosineCalculator) {
        return new RankingService(cosineCalculator);
    }

    @Bean
    HybridSearchService hybridSearchService(
            CandidateCollector collector,
            ConditionEvaluator evaluator,
            RankingService rankingService) {
        return new HybridSearchService(collector, evaluator, rankingService);
    }

    @Bean
    EmbeddingTextBuilder embeddingTextBuilder() {
        return new EmbeddingTextBuilder();
    }
}
