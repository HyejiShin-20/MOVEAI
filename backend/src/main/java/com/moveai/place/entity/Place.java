package com.moveai.place.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
}
