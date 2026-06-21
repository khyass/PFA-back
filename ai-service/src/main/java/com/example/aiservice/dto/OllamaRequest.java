package com.example.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Ollama API request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaRequest {

    private String model;
    private String prompt;
    private boolean stream;
}
