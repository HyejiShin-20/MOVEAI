package com.moveai.ai.stt;
import org.springframework.stereotype.Component;
@Component
public class MockSttClient implements SttClient {
    @Override public String transcribe(byte[] audio) { return ""; }
}
