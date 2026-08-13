package com.moveai.place.repository;
import com.moveai.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PlaceRepository extends JpaRepository<Place, Long> {
    Optional<Place> findByPlaceCode(String placeCode);
}
