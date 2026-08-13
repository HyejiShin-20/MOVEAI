package com.moveai.ai.stt;
public interface SttClient {
    String transcribe(byte[] audio);
}
