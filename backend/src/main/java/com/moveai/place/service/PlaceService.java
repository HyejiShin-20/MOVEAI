package com.moveai.place.service;

import com.moveai.place.entity.Place;
import com.moveai.place.repository.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlaceService {

    private final PlaceRepository placeRepository;

    public PlaceService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    public List<Place> findAll() {
        return placeRepository.findAll();
    }

    public Place findById(Long id) {
        return placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다: " + id));
    }

    @Transactional
    public Place create(String name, String address, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("장소 이름은 필수입니다.");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("주소는 필수입니다.");
        }
        return placeRepository.save(new Place(name, address, description));
    }

    @Transactional
    public Place update(Long id, String name, String address, String description) {
        Place place = findById(id);
        if (name != null && !name.isBlank()) place.setName(name);
        if (address != null && !address.isBlank()) place.setAddress(address);
        place.setDescription(description);
        return placeRepository.save(place);
    }

    @Transactional
    public void delete(Long id) {
        Place place = findById(id);
        placeRepository.delete(place);
    }
}
