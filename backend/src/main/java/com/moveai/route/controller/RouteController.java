package com.moveai.route.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moveai.route.dto.RouteResponse;
import com.moveai.route.service.RouteService;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/{id}")
    public RouteResponse.Detail detail(@PathVariable Long id) {
        return routeService.findDetail(id);
    }
}
