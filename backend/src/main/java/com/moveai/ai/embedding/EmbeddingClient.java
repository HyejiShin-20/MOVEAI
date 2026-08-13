package com.moveai.ai.embedding;

import java.util.List;

/** Python /embed 경계. DB 저장과 코사인 계산은 Spring이 담당한다. */
public interface EmbeddingClient {
    List<double[]> embed(List<String> texts);
}
