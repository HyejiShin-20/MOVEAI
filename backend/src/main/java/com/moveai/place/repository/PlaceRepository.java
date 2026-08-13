package com.moveai.place.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moveai.place.entity.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findAllByOrderByPlaceCodeAsc();
}
