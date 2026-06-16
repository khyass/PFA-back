package com.example.candidateservice.service;

import com.example.candidateservice.dto.JobOfferDTO;
import com.example.candidateservice.entity.JobOfferStatus;
import com.example.candidateservice.exception.JobOfferNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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
     * Fetches a job offer by ID from the job-offer-service.
     *
     * @param jobOfferId The job offer ID
     * @return The job offer DTO
     * @throws JobOfferNotFoundException if the job offer is not found
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
     * Checks if a job offer is accepting applications (status is OPEN).
     *
     * @param jobOffer The job offer DTO
     * @return true if accepting applications
     */
    public boolean isAcceptingApplications(JobOfferDTO jobOffer) {
        return jobOffer.getStatus() == JobOfferStatus.OPEN;
    }
}
