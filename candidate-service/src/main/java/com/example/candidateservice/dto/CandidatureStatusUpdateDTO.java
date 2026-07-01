package com.example.candidateservice.dto;

import com.example.candidateservice.entity.CandidatureStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatureStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private CandidatureStatus status;

    private String note;

    private LocalDateTime interviewDate;

    private String interviewNotes;
}
