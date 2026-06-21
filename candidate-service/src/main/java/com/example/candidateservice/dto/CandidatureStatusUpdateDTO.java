package com.example.candidateservice.dto;

import com.example.candidateservice.entity.CandidatureStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatureStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private CandidatureStatus status;

    private String note;
}
