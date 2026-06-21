package com.example.jobofferservice.service;

import com.example.jobofferservice.dto.CandidatureDTO;
import com.example.jobofferservice.entity.CandidatureStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Client service for communicating with the candidate-service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.candidate-service.url}")
    private String candidateServiceUrl;

    /**
     * Fetches all candidatures for a job offer from the candidate-service.
     */
    public List<CandidatureDTO> getCandidaturesForJobOffer(UUID jobOfferId) {
        try {
            String url = candidateServiceUrl + "/internal/candidatures/by-job-offer/" + jobOfferId;
            log.debug("Fetching candidatures for job offer {} from: {}", jobOfferId, url);

            HttpHeaders headers = new HttpHeaders();
            String token = getCurrentToken();
            if (token != null) {
                headers.setBearerAuth(token);
            }

            ResponseEntity<List<CandidatureFromCandidateService>> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {});

            List<CandidatureFromCandidateService> body = response.getBody();
            if (body == null) {
                return Collections.emptyList();
            }

            return body.stream().map(c -> CandidatureDTO.builder()
                    .id(c.id())
                    .candidateId(c.candidateId())
                    .applicantName(c.candidateName())
                    .applicantPhone(c.candidatePhone())
                    .applicantBio(c.candidateBio())
                    .applicantLinkedinUrl(c.candidateLinkedinUrl())
                    .resumeFileName(c.resumeFileName())
                    .status(CandidatureStatus.valueOf(c.status()))
                    .appliedDate(c.appliedDate())
                    .coverLetter(c.coverLetter())
                    .build()
            ).toList();
        } catch (Exception e) {
            log.error("Failed to fetch candidatures for job offer {}: {}", jobOfferId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String getCurrentToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        return null;
    }

    /**
     * Internal record to deserialize the response from candidate-service.
     */
    private record CandidatureFromCandidateService(
            UUID id,
            String candidateId,
            String candidateName,
            String candidateEmail,
            String candidatePhone,
            String candidateBio,
            String candidateLinkedinUrl,
            String resumeFileName,
            String status,
            LocalDate appliedDate,
            String coverLetter
    ) {}
}
