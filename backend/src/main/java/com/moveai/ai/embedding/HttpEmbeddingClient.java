package com.moveai.ai.embedding;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.moveai.common.ApiException;

/** ai-service POST /embed HTTP 구현. */
@Component
public class HttpEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final int expectedDimension;

    public HttpEmbeddingClient(
            @Value("${moveai.ai-service.url}") String baseUrl,
            @Value("${moveai.embedding.dimension:${EMBEDDING_DIMENSION:1536}}") int expectedDimension) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.expectedDimension = expectedDimension;
    }

    @Override
    public List<double[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty() || texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("임베딩할 texts는 비어 있지 않아야 합니다.");
        }
        try {
            EmbedResponse response = restClient.post()
                    .uri("/embed")
                    .body(Map.of("texts", texts))
                    .retrieve()
                    .body(EmbedResponse.class);
            if (response == null || response.vectors() == null || response.vectors().size() != texts.size()) {
                throw providerError("임베딩 응답 개수가 입력과 다릅니다.");
            }
            for (double[] vector : response.vectors()) {
                if (vector == null || vector.length != expectedDimension) {
                    throw providerError("임베딩 응답 차원이 " + expectedDimension + "이 아닙니다.");
                }
            }
            return response.vectors();
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw providerError("임베딩 서비스 호출에 실패했습니다.");
        }
    }

    private ApiException providerError(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "EMBEDDING_PROVIDER_ERROR", message);
    }

    private record EmbedResponse(String model, int dimension, List<double[]> vectors) {
    }
}
