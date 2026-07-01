-- V1__Create_resume_analysis_and_job_matches_tables.sql
-- Initial schema for ai-service

-- Create resume_analysis table
CREATE TABLE resume_analysis (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    candidate_id VARCHAR(255) NOT NULL UNIQUE,
    extracted_text TEXT NOT NULL,
    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create job_offer_matches table
CREATE TABLE job_offer_matches (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    candidate_id VARCHAR(255) NOT NULL,
    job_offer_id UUID NOT NULL,
    job_title VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    match_score INTEGER NOT NULL CHECK (match_score >= 0 AND match_score <= 100),
    missing_skills TEXT,
    suggestions TEXT,
    interview_questions TEXT,
    computed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_match_candidate_joboffer UNIQUE (candidate_id, job_offer_id)
);

-- Create indexes for common queries
CREATE INDEX idx_resume_analysis_candidate ON resume_analysis(candidate_id);
CREATE INDEX idx_job_matches_candidate ON job_offer_matches(candidate_id);
CREATE INDEX idx_job_matches_job_offer ON job_offer_matches(job_offer_id);
CREATE INDEX idx_job_matches_score ON job_offer_matches(match_score DESC);
CREATE INDEX idx_job_matches_candidate_score ON job_offer_matches(candidate_id, match_score DESC);
