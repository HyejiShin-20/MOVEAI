package com.moveai.route.service;

import com.moveai.place.entity.Place;
import com.moveai.place.repository.PlaceRepository;
import com.moveai.route.entity.Route;
import com.moveai.route.repository.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final PlaceRepository placeRepository;

    public RouteService(RouteRepository routeRepository, PlaceRepository placeRepository) {
        this.routeRepository = routeRepository;
        this.placeRepository = placeRepository;
    }

    public List<Route> findByPlaceId(Long placeId) {
        return routeRepository.findByPlaceId(placeId);
    }

    public Route findById(Long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("경로를 찾을 수 없습니다: " + routeId));
    }

    @Transactional
    public Route create(Long placeId, String name, String summary) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다: " + placeId));
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("경로 이름은 필수입니다.");
        }
        return routeRepository.save(new Route(place, name, summary));
    }
}
