-- Check that this name is unused before executing. Never run a reset on a shared database.
CREATE DATABASE smart_care CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- Create a dedicated application account separately using your chosen password.
-- The application needs SELECT, INSERT, UPDATE, DELETE on smart_care.*.
-- Execute 01-schema.sql using a migration account with DDL permissions.
