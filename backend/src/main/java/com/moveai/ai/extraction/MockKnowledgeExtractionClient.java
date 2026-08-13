package com.moveai.ai.extraction;
import org.springframework.stereotype.Component;
@Component
public class MockKnowledgeExtractionClient implements KnowledgeExtractionClient {
    @Override public ExtractionResult extract(String transcript) { return new ExtractionResult(transcript); }
}
