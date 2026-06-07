package com.example.aiservice.service;

import com.example.aiservice.entity.ResumeAnalysis;
import com.example.aiservice.exception.ResumeExtractionException;
import com.example.aiservice.repository.ResumeAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * Service for extracting text from resumes using Apache Tika.
 * Called from the candidate-service when a resume is uploaded.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ResumeExtractionService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final Tika tika = new Tika();

    /**
     * Extracts text from a resume file and saves/updates the analysis.
     *
     * @param candidateId The candidate's user ID
     * @param inputStream The resume file input stream
     * @param filename    The original filename (for logging)
     * @return The saved ResumeAnalysis entity
     */
    public ResumeAnalysis extractAndSave(String candidateId, InputStream inputStream, String filename) {
        log.info("Extracting text from resume for candidate: {}, file: {}", candidateId, filename);

        String extractedText = extractText(inputStream, filename);

        // Find existing or create new
        ResumeAnalysis analysis = resumeAnalysisRepository.findByCandidateId(candidateId)
                .orElse(ResumeAnalysis.builder()
                        .candidateId(candidateId)
                        .build());

        analysis.setExtractedText(extractedText);
        analysis.setAnalyzedAt(LocalDateTime.now());

        ResumeAnalysis saved = resumeAnalysisRepository.save(analysis);
        log.info("Saved resume analysis for candidate: {}, extracted {} characters", candidateId, extractedText.length());

        return saved;
    }

    /**
     * Extracts plain text from the uploaded file using Apache Tika.
     */
    private String extractText(InputStream inputStream, String filename) {
        try {
            String extractedText = tika.parseToString(inputStream);

            if (extractedText == null || extractedText.isBlank()) {
                log.warn("Tika extracted blank text from file: {}", filename);
                throw new ResumeExtractionException();
            }

            // Clean up the text
            extractedText = extractedText.trim()
                    .replaceAll("\\s+", " ")  // Normalize whitespace
                    .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");  // Remove control characters

            log.debug("Extracted {} characters from file: {}", extractedText.length(), filename);
            return extractedText;

        } catch (IOException e) {
            log.error("IO error extracting text from {}: {}", filename, e.getMessage());
            throw new ResumeExtractionException("Could not read the uploaded file. Please try again.", e);
        } catch (TikaException e) {
            log.error("Tika error extracting text from {}: {}", filename, e.getMessage());
            throw new ResumeExtractionException();
        }
    }

    /**
     * Gets the extracted text for a candidate.
     *
     * @param candidateId The candidate's user ID
     * @return The extracted text, or null if not found
     */
    @Transactional(readOnly = true)
    public String getExtractedText(String candidateId) {
        return resumeAnalysisRepository.findByCandidateId(candidateId)
                .map(ResumeAnalysis::getExtractedText)
                .orElse(null);
    }

    /**
     * Checks if a resume analysis exists for a candidate.
     */
    @Transactional(readOnly = true)
    public boolean hasResumeAnalysis(String candidateId) {
        return resumeAnalysisRepository.existsByCandidateId(candidateId);
    }
}
