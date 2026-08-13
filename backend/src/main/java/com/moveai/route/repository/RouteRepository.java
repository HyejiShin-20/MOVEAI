package com.moveai.route.repository;

<<<<<<< HEAD
import com.moveai.route.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByPlaceId(Long placeId);
=======
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moveai.route.entity.Route;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByPlaceIdOrderByRouteCodeAsc(Long placeId);

    /** 경로 선택 1단계 — 목적지가 같은 경로만 후보로 남긴다 (04 §11-3). */
    List<Route> findByPlaceIdAndDestinationNodeIdOrderByRouteCodeAsc(Long placeId, Long destinationNodeId);
>>>>>>> 062d1fac691c4e6b28d78b4ea555ea009cd527bf
}
