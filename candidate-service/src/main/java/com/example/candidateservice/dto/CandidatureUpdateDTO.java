package com.example.candidateservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a candidature (cover letter only).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatureUpdateDTO {

    private String coverLetter;
}
