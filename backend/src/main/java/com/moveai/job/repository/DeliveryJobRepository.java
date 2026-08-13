package com.moveai.job.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.moveai.job.entity.DeliveryJob;

public interface DeliveryJobRepository extends JpaRepository<DeliveryJob, Long> {

    @Query("""
            select j from DeliveryJob j
            join fetch j.place
            join fetch j.destinationNode
            where (:status is null or j.status = :status)
            order by j.jobCode asc
            """)
    List<DeliveryJob> findForList(String status);
}
