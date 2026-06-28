package com.example.aiservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for interview preparation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewPrepRequest {

    @NotNull(message = "Offer ID is required")
    private UUID offerId;

    private boolean forceRefresh;
}
