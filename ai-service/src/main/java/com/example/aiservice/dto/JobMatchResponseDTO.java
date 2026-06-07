package com.example.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a job offer match result.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatchResponseDTO {

    private UUID jobOfferId;
    private String jobTitle;
    private String companyName;
    private Integer matchScore;
    private List<String> missingSkills;
    private String suggestions;
    private List<String> interviewQuestions;
    private LocalDateTime computedAt;
    private boolean cached;
}
