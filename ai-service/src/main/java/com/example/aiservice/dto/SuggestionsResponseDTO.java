package com.example.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for async suggestions computation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestionsResponseDTO {

    private boolean computing;
    private String message;
    private List<JobSuggestionDTO> suggestions;
}
