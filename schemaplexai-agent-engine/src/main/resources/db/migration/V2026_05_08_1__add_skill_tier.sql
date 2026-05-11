-- V2026_05_08__add_skill_tier.sql
-- Adds tier column to sf_agent_skill for progressive skill disclosure

ALTER TABLE sf_agent_skill ADD COLUMN tier SMALLINT NOT NULL DEFAULT 1;
