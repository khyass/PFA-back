package com.example.aiservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewPrepResponse {

    private String jobTitle;
    private String companyName;
    private List<QuestionAnswer> technicalQuestions;
    private List<QuestionAnswer> behavioralQuestions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionAnswer {
        private String question;
        private String answerOutline;
    }
}
