package com.example.candidateservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for candidate profile.
 * Note: resumeStoragePath is intentionally excluded for security.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateProfileDTO {

    private UUID id;
    private String userId;
    private String fullName;
    private String phone;
    private String bio;
    private String linkedinUrl;
    private String resumeFileName;
    private LocalDateTime updatedAt;
}
