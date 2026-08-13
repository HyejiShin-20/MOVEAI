package com.moveai.user.repository;

import com.moveai.user.entity.MemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {
}
