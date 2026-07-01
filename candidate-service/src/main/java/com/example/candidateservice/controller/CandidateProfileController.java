package com.example.candidateservice.controller;

import com.example.candidateservice.dto.CandidateProfileDTO;
import com.example.candidateservice.dto.ProfileUpdateRequestDTO;
import com.example.candidateservice.service.CandidateProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidate/profile")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Profil Candidat", description = "Gestion du profil candidat : informations personnelles et CV")
public class CandidateProfileController {

    private final CandidateProfileService profileService;

    /**
     * Get the authenticated candidate's profile.
     * Creates an empty profile if one doesn't exist (first login).
     *
     * @param jwt The authenticated user's JWT
     * @return The candidate's profile
     */
    @GetMapping
    public ResponseEntity<CandidateProfileDTO> getProfile(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.debug("GET /api/candidate/profile - userId={}", userId);

        CandidateProfileDTO profile = profileService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update the candidate's profile info.
     *
     * @param request The profile update request
     * @param jwt     The authenticated user's JWT
     * @return The updated profile
     */
    @PutMapping
    public ResponseEntity<CandidateProfileDTO> updateProfile(
            @Valid @RequestBody ProfileUpdateRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        log.info("PUT /api/candidate/profile - userId={}", userId);

        CandidateProfileDTO updatedProfile = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Upload a resume (PDF or DOCX, max 5MB).
     *
     * @param file The resume file
     * @param jwt  The authenticated user's JWT
     * @return The updated profile with resume info
     */
    @PostMapping("/resume")
    public ResponseEntity<CandidateProfileDTO> uploadResume(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        log.info("POST /api/candidate/profile/resume - userId={}, filename={}, size={}",
                userId, file.getOriginalFilename(), file.getSize());

        CandidateProfileDTO updatedProfile = profileService.uploadResume(userId, file);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Download the candidate's current resume file.
     *
     * @param jwt The authenticated user's JWT
     * @return The resume file with appropriate headers
     */
    @GetMapping("/resume")
    public ResponseEntity<Resource> downloadResume(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.debug("GET /api/candidate/profile/resume - userId={}", userId);

        CandidateProfileService.ResumeDownload download = profileService.downloadResume(userId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.filename() + "\"")
                .body(download.resource());
    }
}
