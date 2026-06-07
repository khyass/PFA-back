package com.example.candidateservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a candidature (applying to a job).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatureRequestDTO {

    @NotNull(message = "Job offer ID is required")
    private UUID jobOfferId;

    private String coverLetter;
}
