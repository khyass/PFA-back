-- V2__Add_recruitment_pipeline_stages.sql
-- Add new recruitment stages and interview scheduling fields

-- Add interview_date column
ALTER TABLE candidatures ADD COLUMN IF NOT EXISTS interview_date TIMESTAMP;

-- Add interview_notes column for enterprise to add interview-specific notes
ALTER TABLE candidatures ADD COLUMN IF NOT EXISTS interview_notes TEXT;

-- Drop existing check constraints on status columns and recreate with new values
ALTER TABLE candidatures ALTER COLUMN status VARCHAR(50) NOT NULL;
ALTER TABLE candidature_status_history ALTER COLUMN new_status VARCHAR(50) NOT NULL;
