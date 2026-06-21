package com.example.aiservice.controller;

import com.example.aiservice.entity.ResumeAnalysis;
import com.example.aiservice.service.ResumeExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Internal controller for resume analysis.
 * Called by candidate-service when a resume is uploaded.
 * No authentication required (internal service-to-service communication).
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalController {

    private final ResumeExtractionService resumeExtractionService;

    /**
     * Analyze a resume and extract text.
     * Called internally by candidate-service after resume upload.
     *
     * @param candidateId The candidate's user ID
     * @param file        The resume file
     * @return Success response with analysis ID
     */
    @PostMapping("/resume/analyze")
    public ResponseEntity<Map<String, Object>> analyzeResume(
            @RequestParam("candidateId") String candidateId,
            @RequestParam("file") MultipartFile file) throws IOException {

        log.info("POST /internal/resume/analyze - candidateId={}, filename={}", candidateId, file.getOriginalFilename());

        ResumeAnalysis analysis = resumeExtractionService.extractAndSave(
                candidateId,
                file.getInputStream(),
                file.getOriginalFilename()
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "analysisId", analysis.getId().toString(),
                "textLength", analysis.getExtractedText().length(),
                "analyzedAt", analysis.getAnalyzedAt().toString()
        ));
    }

    /**
     * Check if a resume analysis exists for a candidate.
     *
     * @param candidateId The candidate's user ID
     * @return Whether analysis exists
     */
    @GetMapping("/resume/exists/{candidateId}")
    public ResponseEntity<Map<String, Boolean>> hasResumeAnalysis(@PathVariable String candidateId) {
        log.debug("GET /internal/resume/exists/{}", candidateId);

        boolean exists = resumeExtractionService.hasResumeAnalysis(candidateId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}
