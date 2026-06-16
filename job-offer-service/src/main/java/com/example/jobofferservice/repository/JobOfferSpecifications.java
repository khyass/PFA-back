package com.example.jobofferservice.repository;

import com.example.jobofferservice.entity.JobOffer;
import com.example.jobofferservice.entity.JobOfferStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for filtering JobOffer queries.
 */
public final class JobOfferSpecifications {

    private JobOfferSpecifications() {
        // Utility class - prevent instantiation
    }

    /**
     * Filter by status.
     */
    public static Specification<JobOffer> hasStatus(JobOfferStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    /**
     * Filter by company name (case-insensitive partial match).
     */
    public static Specification<JobOffer> hasCompanyNameLike(String company) {
        return (root, query, criteriaBuilder) -> {
            if (company == null || company.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("companyName")),
                    "%" + company.toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter by owner ID.
     */
    public static Specification<JobOffer> hasOwnerId(String ownerId) {
        return (root, query, criteriaBuilder) -> {
            if (ownerId == null || ownerId.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("ownerId"), ownerId);
        };
    }

    /**
     * Combine multiple filters.
     */
    public static Specification<JobOffer> withFilters(JobOfferStatus status, String company) {
        return Specification.where(hasStatus(status))
                .and(hasCompanyNameLike(company));
    }
}
