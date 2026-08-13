package com.moveai.route.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.moveai.route.entity.RouteSegment;

public interface RouteSegmentRepository extends JpaRepository<RouteSegment, Long> {

    /** 노드 이름을 함께 쓰므로 미리 조인해 가져온다. */
    @Query("""
            select s from RouteSegment s
            join fetch s.fromNode
            join fetch s.toNode
            where s.routeId = :routeId
            order by s.sequenceNo asc
            """)
    List<RouteSegment> findByRouteIdOrderBySequence(Long routeId);

    @Query("""
            select s from RouteSegment s
            join fetch s.fromNode
            join fetch s.toNode
            where s.routeId = :routeId and s.sequenceNo = :sequenceNo
            """)
    Optional<RouteSegment> findStep(Long routeId, int sequenceNo);

    int countByRouteId(Long routeId);
}
