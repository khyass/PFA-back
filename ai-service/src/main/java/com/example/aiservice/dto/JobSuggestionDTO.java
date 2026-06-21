package com.example.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for job suggestion (ranked list item).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSuggestionDTO {

    private UUID jobOfferId;
    private String jobTitle;
    private String companyName;
    private Integer matchScore;
    private boolean cached;
}
