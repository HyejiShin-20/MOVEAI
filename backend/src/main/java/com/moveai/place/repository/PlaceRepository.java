package com.moveai.place.repository;

<<<<<<< HEAD
import com.moveai.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
=======
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moveai.place.entity.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findAllByOrderByPlaceCodeAsc();
>>>>>>> 062d1fac691c4e6b28d78b4ea555ea009cd527bf
}
