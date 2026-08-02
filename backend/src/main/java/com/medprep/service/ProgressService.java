package com.medprep.service;

import com.medprep.dto.response.ProgressResponse;
import com.medprep.model.UserSpecialtyProgress;
import com.medprep.repository.UserSpecialtyProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserSpecialtyProgressRepository progressRepository;

    @Transactional(readOnly = true)
    public List<ProgressResponse> getUserProgress(UUID userId) {
        return progressRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> getRedAlerts(UUID userId) {
        return progressRepository
                .findByUserIdAndTrafficLight(userId, UserSpecialtyProgress.TrafficLight.ROJO)
                .stream().map(this::toResponse).toList();
    }

    private ProgressResponse toResponse(UserSpecialtyProgress p) {
        return ProgressResponse.builder()
                .specialtyId(p.getSpecialty().getId())
                .specialtyName(p.getSpecialty().getName())
                .totalAttempts(p.getTotalAttempts())
                .correctAttempts(p.getCorrectAttempts())
                .accuracyPercentage(p.getAccuracyPercentage())
                .trafficLight(p.getTrafficLight())
                .lastUpdated(p.getLastUpdated())
                .sprintRequired(p.getTrafficLight() == UserSpecialtyProgress.TrafficLight.ROJO)
                .build();
    }
}
