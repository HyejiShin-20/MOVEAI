package com.moveai.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_code", nullable = false)
    private String placeCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "place_type", nullable = false)
    private String placeType;

    @Column(name = "custom_place_type")
    private String customPlaceType;

    private String description;

    protected Place() {}

    public Long getId() {
        return id;
    }

    public String getPlaceCode() {
        return placeCode;
    }

    public String getName() {
        return name;
    }

    public String getPlaceType() {
        return placeType;
    }

    public String getCustomPlaceType() {
        return customPlaceType;
    }

    public String getDescription() {
        return description;
    }
}
