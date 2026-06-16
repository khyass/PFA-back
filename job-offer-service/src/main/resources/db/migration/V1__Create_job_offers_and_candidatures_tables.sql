-- V1__Create_job_offers_and_candidatures_tables.sql
-- Initial schema for job-offer-service

-- Create job_offers table
CREATE TABLE job_offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('OPEN', 'CLOSED', 'DRAFT')),
    published_date DATE,
    notes TEXT,
    company_name VARCHAR(255) NOT NULL,
    candidature_count INTEGER NOT NULL DEFAULT 0,
    owner_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create candidatures table
CREATE TABLE candidatures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    applicant_name VARCHAR(255) NOT NULL,
    applicant_email VARCHAR(255) NOT NULL,
    applied_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'REVIEWING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    applicant_id VARCHAR(255) NOT NULL,
    job_offer_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_candidatures_job_offer FOREIGN KEY (job_offer_id) REFERENCES job_offers(id)
);

-- Create indexes for common queries
CREATE INDEX idx_job_offers_status ON job_offers(status);
CREATE INDEX idx_job_offers_company_name ON job_offers(company_name);
CREATE INDEX idx_job_offers_owner_id ON job_offers(owner_id);
CREATE INDEX idx_job_offers_published_date ON job_offers(published_date DESC);
CREATE INDEX idx_candidatures_job_offer_id ON candidatures(job_offer_id);
CREATE INDEX idx_candidatures_applicant_id ON candidatures(applicant_id);

-- Add comments for documentation
COMMENT ON TABLE job_offers IS 'Stores job offers posted by enterprise users';
COMMENT ON TABLE candidatures IS 'Stores candidatures/applications to job offers';
COMMENT ON COLUMN job_offers.owner_id IS 'Keycloak user ID of the enterprise owner';
COMMENT ON COLUMN candidatures.applicant_id IS 'Keycloak user ID of the candidate applicant';
