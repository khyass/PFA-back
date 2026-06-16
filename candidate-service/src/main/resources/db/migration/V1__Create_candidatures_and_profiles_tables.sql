-- V1__Create_candidatures_and_profiles_tables.sql
-- Initial schema for candidate-service

-- Create candidatures table
CREATE TABLE candidatures (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    job_offer_id UUID NOT NULL,
    job_offer_title VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    candidate_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'REVIEWING', 'ACCEPTED', 'REJECTED')),
    applied_date DATE NOT NULL,
    cover_letter TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_candidature_job_candidate UNIQUE (job_offer_id, candidate_id)
);

-- Create candidature_status_history table
CREATE TABLE candidature_status_history (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    candidature_id UUID NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL CHECK (new_status IN ('PENDING', 'REVIEWING', 'ACCEPTED', 'REJECTED')),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(500),
    CONSTRAINT fk_status_history_candidature FOREIGN KEY (candidature_id) REFERENCES candidatures(id) ON DELETE CASCADE
);

-- Create candidate_profiles table
CREATE TABLE candidate_profiles (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    phone VARCHAR(50),
    bio TEXT,
    linkedin_url VARCHAR(500),
    resume_file_name VARCHAR(255),
    resume_storage_path VARCHAR(500),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX idx_candidatures_candidate_id ON candidatures(candidate_id);
CREATE INDEX idx_candidatures_job_offer_id ON candidatures(job_offer_id);
CREATE INDEX idx_candidatures_status ON candidatures(status);
CREATE INDEX idx_candidatures_applied_date ON candidatures(applied_date DESC);
CREATE INDEX idx_status_history_candidature_id ON candidature_status_history(candidature_id);
CREATE INDEX idx_status_history_changed_at ON candidature_status_history(changed_at);
CREATE INDEX idx_profiles_user_id ON candidate_profiles(user_id);
