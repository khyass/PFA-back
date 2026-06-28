package com.example.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for AI-generated cover letter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverLetterResponse {

    private String coverLetter;
    private String jobTitle;
    private String companyName;
    private String tone;
}
