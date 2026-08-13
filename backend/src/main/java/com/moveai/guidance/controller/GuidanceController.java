package com.moveai.guidance.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.moveai.guidance.dto.GuidanceCompleteResponse;
import com.moveai.guidance.dto.GuidanceCreateRequest;
import com.moveai.guidance.dto.GuidanceSessionResponse;
import com.moveai.guidance.service.GuidanceService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/guidance")
public class GuidanceController {

    private final GuidanceService guidanceService;

    public GuidanceController(GuidanceService guidanceService) {
        this.guidanceService = guidanceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuidanceSessionResponse create(@Valid @RequestBody GuidanceCreateRequest request) {
        return guidanceService.create(request);
    }

    @GetMapping("/{id}")
    public GuidanceSessionResponse get(@PathVariable long id) {
        return guidanceService.get(id);
    }

    @PostMapping("/{id}/next")
    public GuidanceSessionResponse next(@PathVariable long id) {
        return guidanceService.next(id);
    }

    @PostMapping("/{id}/complete")
    public GuidanceCompleteResponse complete(@PathVariable long id) {
        return guidanceService.complete(id);
    }
}
