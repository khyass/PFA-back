package com.example.candidateservice.mapper;

import com.example.candidateservice.dto.*;
import com.example.candidateservice.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct mapper for Candidature, CandidatureStatusHistory, and CandidateProfile entities.
 */
@Mapper(componentModel = "spring")
public interface CandidateMapper {

    // ===== Candidature mappings =====

    /**
     * Maps a Candidature entity to a CandidatureResponseDTO.
     */
    CandidatureResponseDTO toCandidatureResponseDTO(Candidature candidature);

    /**
     * Maps a list of Candidature entities to a list of CandidatureResponseDTOs.
     */
    List<CandidatureResponseDTO> toCandidatureResponseDTOList(List<Candidature> candidatures);

    // ===== Status History mappings =====

    /**
     * Maps a CandidatureStatusHistory entity to a StatusHistoryDTO.
     */
    StatusHistoryDTO toStatusHistoryDTO(CandidatureStatusHistory history);

    /**
     * Maps a list of CandidatureStatusHistory entities to a list of StatusHistoryDTOs.
     */
    List<StatusHistoryDTO> toStatusHistoryDTOList(List<CandidatureStatusHistory> histories);

    // ===== CandidateProfile mappings =====

    /**
     * Maps a CandidateProfile entity to a CandidateProfileDTO.
     * Note: resumeStoragePath is intentionally excluded from the DTO.
     */
    CandidateProfileDTO toCandidateProfileDTO(CandidateProfile profile);

    /**
     * Updates a CandidateProfile entity from a ProfileUpdateRequestDTO.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "resumeFileName", ignore = true)
    @Mapping(target = "resumeStoragePath", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateProfileFromDTO(ProfileUpdateRequestDTO dto, @MappingTarget CandidateProfile profile);
}
