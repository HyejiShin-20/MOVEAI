package com.moveai.place.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moveai.place.entity.PlaceNode;

public interface PlaceNodeRepository extends JpaRepository<PlaceNode, Long> {

    List<PlaceNode> findByPlaceIdOrderByNodeCodeAsc(Long placeId);
}
