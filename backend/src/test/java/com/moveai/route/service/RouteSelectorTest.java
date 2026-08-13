package com.moveai.route.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moveai.route.service.RouteSelector.RouteOption;

class RouteSelectorTest {

    private final RouteSelector selector = new RouteSelector();
    private final RouteOption basement = new RouteOption(
            3, "ROUTE_B_01", "후문-지하2층", "TRUCK", null,
            new BigDecimal("1.0"), new BigDecimal("2.3"), null, true);
    private final RouteOption lobby = new RouteOption(
            4, "ROUTE_B_02", "정문-지상로비", "TRUCK", new BigDecimal("1.0"),
            null, null, null, false);

    @Test
    void exactOneTonSelectsBasementOnly() {
        assertThat(selector.select(
                        List.of(basement, lobby), new VehicleContext("TRUCK", 1.0, 2.3, null)))
                .get().extracting(RouteOption::routeCode).isEqualTo("ROUTE_B_01");
    }

    @Test
    void twoPointFiveTonSelectsLobbyOnly() {
        assertThat(selector.select(
                        List.of(basement, lobby), new VehicleContext("TRUCK", 2.5, 2.7, null)))
                .get().extracting(RouteOption::routeCode).isEqualTo("ROUTE_B_02");
    }

    @Test
    void returnsEmptyWhenNoRegisteredRouteFits() {
        assertThat(selector.select(
                List.of(basement), new VehicleContext("TRUCK", 1.0, 2.31, null))).isEmpty();
    }
}
