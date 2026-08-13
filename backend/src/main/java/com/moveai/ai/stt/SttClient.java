package com.moveai.ai.stt;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.moveai.common.ApiException;

/**
 * ai-service {@code POST /stt} 호출 (05B §5-1).
 *
 * <p>음성 인식 자체는 Python이 한다. Spring은 파일을 넘기고 문장을 받을 뿐이다.
 */
@Component
public class SttClient {

    private static final Logger log = LoggerFactory.getLogger(SttClient.class);

    private final RestClient restClient;

    public SttClient(@Value("${moveai.ai-service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        // 음성 전사는 수십 초가 걸릴 수 있다. 짧게 끊으면 멀쩡한 요청이 실패로 보인다.
        requestFactory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public SttResult transcribe(byte[] audio, String fileName, String contentType) {
        HttpHeaders partHeaders = new HttpHeaders();
        // ai-service 가 형식을 허용목록으로 거른다. 추론에 맡기지 않고 명시한다.
        partHeaders.setContentType(MediaType.parseMediaType(contentType));
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("audio", new HttpEntity<>(new NamedByteArrayResource(audio, fileName, contentType), partHeaders));

        try {
            SttResult result = restClient.post()
                    .uri("/stt")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(SttResult.class);
            if (result == null || result.text() == null || result.text().isBlank()) {
                throw sttFailed("전사 결과가 비어 있습니다.");
            }
            return result;
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("stt call failed: error_type={}", exception.getClass().getSimpleName());
            throw sttFailed("음성 변환에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private static ApiException sttFailed(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "STT_FAILED", message);
    }

    public record SttResult(String text, Integer durationMs) {}

    /** multipart 파트에 파일명과 형식을 실어 보내기 위한 래퍼. */
    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String fileName;
        private final String contentType;

        private NamedByteArrayResource(byte[] bytes, String fileName, String contentType) {
            super(bytes);
            this.fileName = fileName;
            this.contentType = contentType;
        }

        @Override
        public String getFilename() {
            return fileName;
        }

        @Override
        public String getDescription() {
            return "audio(" + contentType + ")";
        }
    }
}
