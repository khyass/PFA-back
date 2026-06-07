package com.example.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for interview preparation questions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewPrepDTO {

    private UUID jobOfferId;
    private String jobTitle;
    private String companyName;
    private List<String> interviewQuestions;
    private Integer matchScore;
}
