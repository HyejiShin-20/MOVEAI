package com.moveai.place.repository;
import com.moveai.place.entity.PlaceNode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PlaceNodeRepository extends JpaRepository<PlaceNode, Long> {
    Optional<PlaceNode> findByNodeCode(String nodeCode);
}
