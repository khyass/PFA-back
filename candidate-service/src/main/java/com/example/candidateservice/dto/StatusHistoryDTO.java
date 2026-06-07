package com.example.candidateservice.dto;

import com.example.candidateservice.entity.CandidatureStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for candidature status history entry.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistoryDTO {

    private UUID id;
    private CandidatureStatus oldStatus;
    private CandidatureStatus newStatus;
    private LocalDateTime changedAt;
    private String note;
}
