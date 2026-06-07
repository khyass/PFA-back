package com.example.aiservice.service;

import com.example.aiservice.dto.JobOfferDTO;
import com.example.aiservice.exception.JobOfferNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client service for communicating with the job-offer-service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobOfferClient {

    private final RestTemplate restTemplate;

    @Value("${app.job-offer-service.url}")
    private String jobOfferServiceUrl;

    /**
     * Fetches a job offer by ID.
     */
    public JobOfferDTO getJobOffer(UUID jobOfferId) {
        try {
            String url = jobOfferServiceUrl + "/api/job-offers/" + jobOfferId;
            log.debug("Fetching job offer from: {}", url);

            JobOfferDTO jobOffer = restTemplate.getForObject(url, JobOfferDTO.class);

            if (jobOffer == null) {
                throw new JobOfferNotFoundException(jobOfferId);
            }

            return jobOffer;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new JobOfferNotFoundException(jobOfferId);
            }
            log.error("Error fetching job offer {}: {}", jobOfferId, e.getMessage());
            throw new RuntimeException("Error communicating with job-offer-service", e);
        }
    }

    /**
     * Fetches all OPEN job offers.
     */
    @SuppressWarnings("unchecked")
    public List<JobOfferDTO> getAllOpenJobOffers() {
        try {
            String url = jobOfferServiceUrl + "/api/job-offers?status=OPEN&size=100";
            log.debug("Fetching all open job offers from: {}", url);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("content")) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
            return content.stream()
                    .map(this::mapToJobOfferDTO)
                    .toList();

        } catch (Exception e) {
            log.error("Error fetching open job offers: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private JobOfferDTO mapToJobOfferDTO(Map<String, Object> map) {
        return JobOfferDTO.builder()
                .id(UUID.fromString((String) map.get("id")))
                .title((String) map.get("title"))
                .status((String) map.get("status"))
                .companyName((String) map.get("companyName"))
                .notes((String) map.get("notes"))
                .build();
    }
}
