package com.moveai.job.dto;

/** 05B §4-2. */
public final class DeliveryJobResponse {

    private DeliveryJobResponse() {}

    /** GET /api/delivery-jobs */
    public record Summary(
            Long id,
            String jobCode,
            Long placeId,
            String placeName,
            String recipientLabel,
            String addressText,
            String itemSummary,
            String status) {}

    /** GET /api/delivery-jobs/{id} */
    public record Detail(
            Long id,
            String jobCode,
            Ref place,
            Ref destinationNode,
            String recipientLabel,
            String addressText,
            String itemSummary,
            String status) {}

    public record Ref(Long id, String name) {}
}
