package com.moveai.report.dto;

import jakarta.validation.constraints.NotBlank;

public record TranscriptUpdateRequest(@NotBlank String correctedText) {}
