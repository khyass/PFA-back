package com.example.candidateservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating candidate profile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequestDTO {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;

    private String bio;

    private String linkedinUrl;
}
