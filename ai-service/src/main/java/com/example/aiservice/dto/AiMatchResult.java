package com.example.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for parsing AI/LLM response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMatchResult {

    private Integer matchScore;
    private List<String> missingSkills;
    private String suggestions;
    private List<String> interviewQuestions;
}
