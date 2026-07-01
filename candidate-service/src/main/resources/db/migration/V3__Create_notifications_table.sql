-- V3__Create_notifications_table.sql
-- Create notifications table for candidate notifications

CREATE TABLE IF NOT EXISTS notifications (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    candidate_id VARCHAR(255) NOT NULL,
    title VARCHAR(500) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_candidate_id ON notifications(candidate_id);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(candidate_id, is_read);
