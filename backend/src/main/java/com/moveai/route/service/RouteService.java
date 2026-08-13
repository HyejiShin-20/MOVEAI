package com.moveai.route.service;

<<<<<<< HEAD
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
=======
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moveai.common.ApiException;
import com.moveai.route.dto.RouteResponse;
import com.moveai.route.entity.Route;
import com.moveai.route.entity.RouteSegment;
import com.moveai.route.repository.RouteRepository;
import com.moveai.route.repository.RouteSegmentRepository;

@Service
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteSegmentRepository routeSegmentRepository;

    public RouteService(RouteRepository routeRepository, RouteSegmentRepository routeSegmentRepository) {
        this.routeRepository = routeRepository;
        this.routeSegmentRepository = routeSegmentRepository;
    }

    public RouteResponse.Detail findDetail(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> ApiException.notFound("ROUTE_NOT_FOUND", "경로를 찾을 수 없습니다."));

        List<RouteResponse.Segment> segments =
                routeSegmentRepository.findByRouteIdOrderBySequence(routeId).stream()
                        .map(RouteService::toSegment)
                        .toList();

        return new RouteResponse.Detail(route.getId(), route.getName(), segments.size(), segments);
    }

    private static RouteResponse.Segment toSegment(RouteSegment segment) {
        return new RouteResponse.Segment(
                segment.getId(),
                segment.getSequenceNo(),
                segment.getFromNode().getName(),
                segment.getToNode().getName(),
                segment.getMovementMode(),
                segment.getTraversalMethod(),
                segment.getInstruction(),
                segment.isIndoor());
>>>>>>> 062d1fac691c4e6b28d78b4ea555ea009cd527bf
    }
}
