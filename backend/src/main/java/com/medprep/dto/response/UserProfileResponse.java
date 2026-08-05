package com.medprep.dto.response;

import com.medprep.model.UserProfile;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String fullName;
    private UserProfile.UserRole role;
    private String university;
    private Instant createdAt;
}
