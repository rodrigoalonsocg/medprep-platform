package com.medprep.service;

import com.medprep.dto.request.UpdateProfileRequest;
import com.medprep.dto.response.UserProfileResponse;
import com.medprep.exception.MedPrepException;
import com.medprep.model.UserProfile;
import com.medprep.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return toResponse(findProfile(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        UserProfile profile = findProfile(userId);
        if (req.getFullName() != null) profile.setFullName(req.getFullName());
        if (req.getUniversity() != null) profile.setUniversity(req.getUniversity());
        return toResponse(userProfileRepository.save(profile));
    }

    private UserProfile findProfile(UUID userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> MedPrepException.notFound("Perfil", userId.toString()));
    }

    private UserProfileResponse toResponse(UserProfile p) {
        return UserProfileResponse.builder()
                .id(p.getId())
                .fullName(p.getFullName())
                .role(p.getRole())
                .university(p.getUniversity())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
