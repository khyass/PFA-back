package com.example.candidateservice.dto;

import com.example.candidateservice.entity.CandidatureStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for a candidature.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatureResponseDTO {

    private UUID id;
    private String jobOfferTitle;
    private String companyName;
    private CandidatureStatus status;
    private LocalDate appliedDate;
    private String coverLetter;
}
