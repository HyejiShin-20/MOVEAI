package com.moveai.ai.embedding;
import org.springframework.stereotype.Component;
@Component
public class MockEmbeddingClient implements EmbeddingClient {
    @Override public float[] embed(String text) { return new float[0]; }
}
