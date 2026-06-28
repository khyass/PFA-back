package com.example.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for a single offer suggestion.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferSuggestionResponse {

    private UUID offerId;
    private String offerTitle;
    private String companyName;
    private Integer score;
    private String justification;
}
