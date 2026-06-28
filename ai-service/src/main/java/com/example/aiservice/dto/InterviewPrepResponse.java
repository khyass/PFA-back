package com.example.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for AI-generated interview preparation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewPrepResponse {

    private String jobTitle;
    private String companyName;
    private List<QuestionAnswer> technicalQuestions;
    private List<QuestionAnswer> behavioralQuestions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionAnswer {
        private String question;
        private String answerOutline;
    }
}
