package com.moveai.route.entity;

import com.moveai.place.entity.Place;
import jakarta.persistence.*;

@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(nullable = false)
    private String name;

    @Column
    private String summary;

    protected Route() {}

    public Route(Place place, String name, String summary) {
        this.place = place;
        this.name = name;
        this.summary = summary;
    }

    public Long getId() { return id; }
    public Place getPlace() { return place; }
    public void setPlace(Place place) { this.place = place; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
