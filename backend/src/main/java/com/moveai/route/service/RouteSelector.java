package com.moveai.route.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

/** 목적지 일치 후보에서 차량 제약을 hard filter하고 기본 Route를 고른다. */
@Component
public class RouteSelector {

    public Optional<RouteOption> select(List<RouteOption> routes, VehicleContext vehicle) {
        List<RouteOption> eligible = routes.stream()
                .filter(route -> vehicleFits(route, vehicle))
                .sorted(Comparator.comparing(RouteOption::routeCode))
                .toList();
        return eligible.stream().filter(RouteOption::defaultRoute).findFirst()
                .or(() -> eligible.stream().findFirst());
    }

    public boolean vehicleFits(RouteOption route, VehicleContext vehicle) {
        if (route.vehicleClass() != null && !route.vehicleClass().equals(vehicle.vehicleClass())) {
            return false;
        }
        if (route.maxTonnage() != null && vehicle.tonnage() > route.maxTonnage().doubleValue()) {
            return false;
        }
        // min_tonnage는 문서 확정대로 '초과'만 허용한다. 정확히 1.0톤은 B_02가 아니다.
        if (route.minTonnage() != null && vehicle.tonnage() <= route.minTonnage().doubleValue()) {
            return false;
        }
        if (route.maxVehicleHeightM() != null
                && vehicle.heightM() > route.maxVehicleHeightM().doubleValue()) {
            return false;
        }
        return route.maxVehicleWidthM() == null || vehicle.widthM() == null
                || vehicle.widthM() <= route.maxVehicleWidthM().doubleValue();
    }

    public record RouteOption(
            long id,
            String routeCode,
            String name,
            String vehicleClass,
            BigDecimal minTonnage,
            BigDecimal maxTonnage,
            BigDecimal maxVehicleHeightM,
            BigDecimal maxVehicleWidthM,
            boolean defaultRoute) {
    }
}
