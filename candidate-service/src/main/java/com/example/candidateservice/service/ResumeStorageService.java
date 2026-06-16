package com.example.candidateservice.service;

import com.example.candidateservice.exception.FileStorageException;
import com.example.candidateservice.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Service for handling file storage operations (resumes).
 * Isolated to make it easy to swap to S3 or other storage later.
 */
@Service
@Slf4j
public class ResumeStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // DOCX
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx");

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${app.upload.path}")
    private String uploadPath;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("Upload directory initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory", e);
        }
    }

    /**
     * Stores a resume file for a user.
     *
     * @param file   The uploaded file
     * @param userId The user ID
     * @return The storage path relative to the upload directory
     */
    public String storeResume(MultipartFile file, String userId) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + "." + extension;

        try {
            // Create user-specific directory
            Path userDir = rootLocation.resolve("resumes").resolve(userId);
            Files.createDirectories(userDir);

            // Delete any existing resume files for this user
            deleteExistingResumes(userDir);

            // Store the new file
            Path targetLocation = userDir.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String storagePath = "resumes/" + userId + "/" + newFilename;
            log.info("Stored resume for user {}: {}", userId, storagePath);

            return storagePath;

        } catch (IOException e) {
            throw new FileStorageException("Failed to store resume file", e);
        }
    }

    /**
     * Loads a resume file as a Resource.
     *
     * @param storagePath The storage path of the file
     * @return The file resource
     */
    public Resource loadResume(String storagePath) {
        try {
            Path filePath = rootLocation.resolve(storagePath).normalize();

            // Security check: ensure the path is within the upload directory
            if (!filePath.startsWith(rootLocation)) {
                throw new FileStorageException("Invalid file path");
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("Resume file not found");
            }
        } catch (MalformedURLException e) {
            throw new FileStorageException("Invalid file path", e);
        }
    }

    /**
     * Deletes a resume file.
     *
     * @param storagePath The storage path of the file
     */
    public void deleteResume(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        try {
            Path filePath = rootLocation.resolve(storagePath).normalize();

            // Security check
            if (!filePath.startsWith(rootLocation)) {
                throw new FileStorageException("Invalid file path");
            }

            Files.deleteIfExists(filePath);
            log.info("Deleted resume: {}", storagePath);
        } catch (IOException e) {
            log.warn("Failed to delete resume file: {}", storagePath, e);
        }
    }

    /**
     * Gets the content type based on file extension.
     */
    public String getContentType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("File size exceeds the maximum allowed (5MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileException("Only PDF and DOCX files are allowed");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileException("Invalid filename");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException("Only PDF and DOCX files are allowed");
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    private void deleteExistingResumes(Path userDir) throws IOException {
        if (Files.exists(userDir)) {
            Files.list(userDir)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                            log.debug("Deleted existing resume: {}", file);
                        } catch (IOException e) {
                            log.warn("Failed to delete existing resume: {}", file, e);
                        }
                    });
        }
    }
}
