-- V2026_05_03__extend_mcp_server.sql
-- Extends sf_mcp_server with command/args/envVars for stdio transport,
-- serverPublicKey for signature verification, protocolVersion, and toolWhitelist.

ALTER TABLE sf_mcp_server ADD COLUMN command VARCHAR(255);
ALTER TABLE sf_mcp_server ADD COLUMN args JSONB;
ALTER TABLE sf_mcp_server ADD COLUMN env_vars JSONB;
ALTER TABLE sf_mcp_server ADD COLUMN server_public_key TEXT;
ALTER TABLE sf_mcp_server ADD COLUMN protocol_version VARCHAR(32);
ALTER TABLE sf_mcp_server ADD COLUMN tool_whitelist JSONB;
