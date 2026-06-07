package com.example.jobofferservice.dto;

import com.example.jobofferservice.entity.CandidatureStatus;
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
public class CandidatureDTO {

    private UUID id;
    private String applicantName;
    private String applicantEmail;
    private LocalDate appliedDate;
    private CandidatureStatus status;
}
