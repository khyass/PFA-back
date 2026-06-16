package com.example.jobofferservice.dto;

import com.example.jobofferservice.entity.JobOfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a job offer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOfferResponseDTO {

    private UUID id;
    private String title;
    private JobOfferStatus status;
    private LocalDate publishedDate;
    private String notes;
    private String companyName;
    private int candidatureCount;
    private String ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
