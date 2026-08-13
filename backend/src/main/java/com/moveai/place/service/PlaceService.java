package com.moveai.place.service;

<<<<<<< HEAD
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
=======
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moveai.common.ApiException;
import com.moveai.place.dto.PlaceResponse;
import com.moveai.place.entity.Place;
import com.moveai.place.entity.PlaceNode;
import com.moveai.place.repository.PlaceNodeRepository;
import com.moveai.place.repository.PlaceRepository;
import com.moveai.route.entity.Route;
import com.moveai.route.repository.RouteRepository;

@Service
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceNodeRepository placeNodeRepository;
    private final RouteRepository routeRepository;

    public PlaceService(
            PlaceRepository placeRepository,
            PlaceNodeRepository placeNodeRepository,
            RouteRepository routeRepository) {
        this.placeRepository = placeRepository;
        this.placeNodeRepository = placeNodeRepository;
        this.routeRepository = routeRepository;
    }

    public List<PlaceResponse.Summary> findAll() {
        return placeRepository.findAllByOrderByPlaceCodeAsc().stream()
                .map(place -> new PlaceResponse.Summary(
                        place.getId(),
                        place.getPlaceCode(),
                        place.getName(),
                        place.getPlaceType(),
                        place.getDescription()))
                .toList();
    }

    public PlaceResponse.Detail findDetail(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> ApiException.notFound("PLACE_NOT_FOUND", "장소를 찾을 수 없습니다."));

        List<PlaceResponse.Node> nodes = placeNodeRepository.findByPlaceIdOrderByNodeCodeAsc(placeId)
                .stream()
                .map(PlaceService::toNode)
                .toList();
        List<PlaceResponse.Route> routes = routeRepository.findByPlaceIdOrderByRouteCodeAsc(placeId)
                .stream()
                .map(PlaceService::toRoute)
                .toList();

        return new PlaceResponse.Detail(
                place.getId(), place.getName(), place.getPlaceType(), place.getDescription(),
                nodes, routes);
    }

    private static PlaceResponse.Node toNode(PlaceNode node) {
        return new PlaceResponse.Node(
                node.getId(), node.getNodeCode(), node.getNodeType(),
                node.getName(), node.getFloorLabel(), node.isIndoor());
    }

    private static PlaceResponse.Route toRoute(Route route) {
        return new PlaceResponse.Route(
                route.getId(), route.getRouteCode(), route.getName(),
                route.isDefaultRoute(), route.getDestinationNodeId(),
                new PlaceResponse.Constraints(
                        route.getVehicleClass(), route.getMinTonnage(), route.getMaxTonnage(),
                        route.getMaxVehicleHeightM(), route.getMaxVehicleWidthM()));
>>>>>>> 062d1fac691c4e6b28d78b4ea555ea009cd527bf
    }
}
