package com.moveai.place.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "places", uniqueConstraints = @UniqueConstraint(name = "uk_places_code", columnNames = "place_code"))
public class Place {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_code", nullable = false, length = 100)
    private String placeCode;

    @Column(nullable = false, length = 255)
    private String name;

    protected Place() {}
    public Place(String placeCode, String name) { this.placeCode = placeCode; this.name = name; }
    public Long getId() { return id; }
    public String getPlaceCode() { return placeCode; }
    public String getName() { return name; }
}
