package com.moveai.guidance.dto;

import java.time.LocalDateTime;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record GuidanceCreateRequest(
        @NotNull Long deliveryJobId,
        @NotNull @Valid VehicleInput vehicle,
        LocalDateTime contextTime) {

    public record VehicleInput(
            @NotBlank String vehicleClass,
            @Positive double tonnage,
            @Positive double heightM,
            @Positive Double widthM) {
    }
}
