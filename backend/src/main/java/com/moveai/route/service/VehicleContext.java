package com.moveai.route.service;

/** 배송 시작 시점의 차량 정보. 기사 고정 프로필이 아니다. */
public record VehicleContext(
        String vehicleClass,
        double tonnage,
        double heightM,
        Double widthM) {
}
