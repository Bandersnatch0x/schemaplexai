-- V2026_05_02__add_execution_skill_role_columns.sql
-- Adds skill_name and role_name columns to sf_agent_execution for skill/role tracking

ALTER TABLE sf_agent_execution ADD COLUMN skill_name VARCHAR(64);
ALTER TABLE sf_agent_execution ADD COLUMN role_name VARCHAR(64);
