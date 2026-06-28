-- V6: Add daily_chat_limit to tenants
ALTER TABLE tenants ADD COLUMN daily_chat_limit INT NOT NULL DEFAULT 1000;
