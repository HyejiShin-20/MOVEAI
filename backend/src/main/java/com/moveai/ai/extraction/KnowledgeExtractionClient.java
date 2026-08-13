package com.moveai.ai.extraction;
public interface KnowledgeExtractionClient {
    ExtractionResult extract(String transcript);
    record ExtractionResult(String statement) {}
}
