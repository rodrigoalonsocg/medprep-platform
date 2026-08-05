package com.medprep.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String fullName;

    @Size(max = 150, message = "La universidad no puede superar 150 caracteres")
    private String university;
}
