package com.example.jobofferservice.dto;

import com.example.jobofferservice.entity.JobOfferStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating only the status of a job offer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOfferStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private JobOfferStatus status;
}
