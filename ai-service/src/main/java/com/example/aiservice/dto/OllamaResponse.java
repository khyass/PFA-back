package com.example.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Ollama API response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaResponse {

    private String model;
    private String response;
    private boolean done;
}
