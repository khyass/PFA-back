package com.example.candidateservice.dto;

import com.example.candidateservice.entity.CandidatureStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatureForEnterpriseDTO {

    private UUID id;
    private String candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String candidateBio;
    private String candidateLinkedinUrl;
    private String resumeFileName;
    private CandidatureStatus status;
    private LocalDate appliedDate;
    private String coverLetter;
}
