-- V2__Create_offer_suggestions_and_interview_prep_tables.sql
-- Tables for keyword-based offer suggestions and interview preparation

-- Create ai_offer_suggestions table
CREATE TABLE ai_offer_suggestions (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    candidate_id VARCHAR(255) NOT NULL,
    keywords_hash VARCHAR(64) NOT NULL,
    offer_id UUID NOT NULL,
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    justification TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_suggestion_candidate_hash_offer UNIQUE (candidate_id, keywords_hash, offer_id)
);

CREATE INDEX idx_suggestions_candidate_hash ON ai_offer_suggestions(candidate_id, keywords_hash);
CREATE INDEX idx_suggestions_score ON ai_offer_suggestions(score DESC);

-- Create ai_interview_prep table
CREATE TABLE ai_interview_prep (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    candidate_id VARCHAR(255) NOT NULL,
    offer_id UUID NOT NULL,
    payload CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_interview_prep_candidate_offer UNIQUE (candidate_id, offer_id)
);

CREATE INDEX idx_interview_prep_candidate_offer ON ai_interview_prep(candidate_id, offer_id);
