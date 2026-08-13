package com.moveai.route.repository;

import com.moveai.route.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByPlaceId(Long placeId);
}
