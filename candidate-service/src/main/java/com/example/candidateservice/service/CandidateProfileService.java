package com.example.candidateservice.service;

import com.example.candidateservice.dto.CandidateProfileDTO;
import com.example.candidateservice.dto.ProfileUpdateRequestDTO;
import com.example.candidateservice.entity.CandidateProfile;
import com.example.candidateservice.exception.FileStorageException;
import com.example.candidateservice.mapper.CandidateMapper;
import com.example.candidateservice.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for managing candidate profiles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepository;
    private final ResumeStorageService resumeStorageService;
    private final CandidateMapper candidateMapper;

    /**
     * Gets the profile for the authenticated candidate.
     * Creates an empty profile if one does not exist (first login).
     */
    @Transactional
    public CandidateProfileDTO getProfile(String userId) {
        log.debug("Getting profile for user: {}", userId);

        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyProfile(userId));

        return candidateMapper.toCandidateProfileDTO(profile);
    }

    /**
     * Updates the candidate's profile.
     */
    public CandidateProfileDTO updateProfile(String userId, ProfileUpdateRequestDTO request) {
        log.info("Updating profile for user: {}", userId);

        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyProfile(userId));

        candidateMapper.updateProfileFromDTO(request, profile);

        CandidateProfile updatedProfile = profileRepository.save(profile);
        log.info("Updated profile for user: {}", userId);

        return candidateMapper.toCandidateProfileDTO(updatedProfile);
    }

    /**
     * Uploads a resume for the candidate.
     */
    public CandidateProfileDTO uploadResume(String userId, MultipartFile file) {
        log.info("Uploading resume for user: {}", userId);

        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyProfile(userId));

        // Delete old resume if exists
        if (profile.getResumeStoragePath() != null) {
            resumeStorageService.deleteResume(profile.getResumeStoragePath());
        }

        // Store new resume
        String storagePath = resumeStorageService.storeResume(file, userId);

        profile.setResumeFileName(file.getOriginalFilename());
        profile.setResumeStoragePath(storagePath);

        CandidateProfile updatedProfile = profileRepository.save(profile);
        log.info("Uploaded resume for user: {} at {}", userId, storagePath);

        return candidateMapper.toCandidateProfileDTO(updatedProfile);
    }

    /**
     * Downloads the candidate's resume.
     */
    @Transactional(readOnly = true)
    public ResumeDownload downloadResume(String userId) {
        log.debug("Downloading resume for user: {}", userId);

        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new FileStorageException("Profile not found"));

        if (profile.getResumeStoragePath() == null || profile.getResumeFileName() == null) {
            throw new FileStorageException("No resume uploaded");
        }

        Resource resource = resumeStorageService.loadResume(profile.getResumeStoragePath());
        String contentType = resumeStorageService.getContentType(profile.getResumeFileName());

        return new ResumeDownload(resource, profile.getResumeFileName(), contentType);
    }

    /**
     * Creates an empty profile for a new user.
     */
    private CandidateProfile createEmptyProfile(String userId) {
        log.info("Creating empty profile for user: {}", userId);

        CandidateProfile profile = CandidateProfile.builder()
                .userId(userId)
                .build();

        return profileRepository.save(profile);
    }

    /**
     * DTO for resume download response.
     */
    public record ResumeDownload(Resource resource, String filename, String contentType) {
    }
}
