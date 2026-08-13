package com.moveai.job.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moveai.job.dto.DeliveryJobResponse;
import com.moveai.job.service.DeliveryJobService;

@RestController
@RequestMapping("/api/delivery-jobs")
public class DeliveryJobController {

    private final DeliveryJobService deliveryJobService;

    public DeliveryJobController(DeliveryJobService deliveryJobService) {
        this.deliveryJobService = deliveryJobService;
    }

    @GetMapping
    public List<DeliveryJobResponse.Summary> list(@RequestParam(required = false) String status) {
        return deliveryJobService.findAll(status);
    }

    @GetMapping("/{id}")
    public DeliveryJobResponse.Detail detail(@PathVariable Long id) {
        return deliveryJobService.findDetail(id);
    }
}
