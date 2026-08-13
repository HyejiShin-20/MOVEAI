package com.moveai.route.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moveai.route.entity.Route;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByPlaceIdOrderByRouteCodeAsc(Long placeId);

    /** 경로 선택 1단계 — 목적지가 같은 경로만 후보로 남긴다 (04 §11-3). */
    List<Route> findByPlaceIdAndDestinationNodeIdOrderByRouteCodeAsc(Long placeId, Long destinationNodeId);
}
