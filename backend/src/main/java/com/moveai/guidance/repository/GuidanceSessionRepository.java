package com.moveai.guidance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.moveai.guidance.entity.GuidanceSession;

public interface GuidanceSessionRepository extends JpaRepository<GuidanceSession, Long> {

    @Modifying(clearAutomatically = true)
    @Query("update GuidanceSession s set s.status = 'ABANDONED'"
            + " where s.deliveryJobId = :jobId and s.status = 'ACTIVE'")
    int abandonActiveByJobId(Long jobId);
}
