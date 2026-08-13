package com.moveai.user.repository;

import com.moveai.user.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {
}
