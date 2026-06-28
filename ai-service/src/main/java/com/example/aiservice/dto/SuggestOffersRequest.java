package com.example.aiservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for keyword-based offer suggestions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestOffersRequest {

    @NotNull(message = "Keywords list is required")
    @NotEmpty(message = "At least one keyword is required")
    private List<String> keywords;
}
