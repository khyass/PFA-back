package com.example.jobofferservice.mapper;

import com.example.jobofferservice.dto.CandidatureDTO;
import com.example.jobofferservice.dto.JobOfferRequestDTO;
import com.example.jobofferservice.dto.JobOfferResponseDTO;
import com.example.jobofferservice.entity.Candidature;
import com.example.jobofferservice.entity.JobOffer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct mapper for JobOffer and Candidature entities.
 */
@Mapper(componentModel = "spring")
public interface JobOfferMapper {

    /**
     * Maps a JobOffer entity to a JobOfferResponseDTO.
     */
    JobOfferResponseDTO toResponseDTO(JobOffer jobOffer);

    /**
     * Maps a list of JobOffer entities to a list of JobOfferResponseDTOs.
     */
    List<JobOfferResponseDTO> toResponseDTOList(List<JobOffer> jobOffers);

    /**
     * Maps a JobOfferRequestDTO to a JobOffer entity.
     * Does not map id, ownerId, candidatureCount, createdAt, updatedAt, or candidatures.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "candidatureCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "candidatures", ignore = true)
    JobOffer toEntity(JobOfferRequestDTO dto);

    /**
     * Updates an existing JobOffer entity from a JobOfferRequestDTO.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "candidatureCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "candidatures", ignore = true)
    void updateEntityFromDTO(JobOfferRequestDTO dto, @MappingTarget JobOffer jobOffer);

    /**
     * Maps a Candidature entity to a CandidatureDTO.
     */
    CandidatureDTO toCandidatureDTO(Candidature candidature);

    /**
     * Maps a list of Candidature entities to a list of CandidatureDTOs.
     */
    List<CandidatureDTO> toCandidatureDTOList(List<Candidature> candidatures);
}
