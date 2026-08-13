package com.moveai.report.storage;

public record StoredAudio(
    String filePath,
    String originalName,
    long fileSize,
    String contentType
) {}
