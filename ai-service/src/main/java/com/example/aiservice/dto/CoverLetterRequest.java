package com.example.aiservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for AI cover letter generation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverLetterRequest {

    @NotNull(message = "Offer ID is required")
    private UUID offerId;

    /** Optional: candidate's key skills to highlight */
    private String candidateSkills;

    /** Optional: tone preference (formal, casual, enthusiastic) */
    private String tone;
}
