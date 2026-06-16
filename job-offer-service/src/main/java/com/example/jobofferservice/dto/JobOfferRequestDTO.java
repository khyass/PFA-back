package com.example.jobofferservice.dto;

import com.example.jobofferservice.entity.JobOfferStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating or updating a job offer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOfferRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Status is required")
    private JobOfferStatus status;

    private LocalDate publishedDate;

    private String notes;

    @NotBlank(message = "Company name is required")
    private String companyName;
}
