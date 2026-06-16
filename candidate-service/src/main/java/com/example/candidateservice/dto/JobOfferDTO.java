package com.example.candidateservice.dto;

import com.example.candidateservice.entity.JobOfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for job offer data retrieved from job-offer-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOfferDTO {

    private UUID id;
    private String title;
    private JobOfferStatus status;
    private String companyName;
}
