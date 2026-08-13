package com.moveai.place.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moveai.place.dto.PlaceResponse;
import com.moveai.place.service.PlaceService;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    public List<PlaceResponse.Summary> list() {
        return placeService.findAll();
    }

    @GetMapping("/{id}")
    public PlaceResponse.Detail detail(@PathVariable Long id) {
        return placeService.findDetail(id);
    }
}
