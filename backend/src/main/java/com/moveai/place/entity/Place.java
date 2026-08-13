package com.moveai.place.entity;

<<<<<<< HEAD
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
=======
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
>>>>>>> 062d1fac691c4e6b28d78b4ea555ea009cd527bf

@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

<<<<<<< HEAD
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column
    private String description;

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<com.moveai.route.entity.Route> routes = new ArrayList<>();

    protected Place() {}

    public Place(String name, String address, String description) {
        this.name = name;
        this.address = address;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<com.moveai.route.entity.Route> getRoutes() { return routes; }
    public void setRoutes(List<com.moveai.route.entity.Route> routes) { this.routes = routes; }
=======
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
>>>>>>> 062d1fac691c4e6b28d78b4ea555ea009cd527bf
}
