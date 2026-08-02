package com.medprep.repository;

import com.medprep.model.UserSpecialtyProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSpecialtyProgressRepository extends JpaRepository<UserSpecialtyProgress, UUID> {

    List<UserSpecialtyProgress> findByUserId(UUID userId);

    Optional<UserSpecialtyProgress> findByUserIdAndSpecialtyId(UUID userId, UUID specialtyId);

    List<UserSpecialtyProgress> findByUserIdAndTrafficLight(
            UUID userId,
            UserSpecialtyProgress.TrafficLight trafficLight);
}
