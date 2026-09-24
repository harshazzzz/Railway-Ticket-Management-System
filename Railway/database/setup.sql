-- Run using a MySQL administrator. Change the sample password before executing.
CREATE DATABASE IF NOT EXISTS railway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE railway;
SOURCE src/main/resources/schema.sql;
CREATE USER IF NOT EXISTS 'railway_app'@'localhost' IDENTIFIED BY 'REPLACE_WITH_A_LONG_RANDOM_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON railway.* TO 'railway_app'@'localhost';
-- Runtime account intentionally has no CREATE/DROP permissions.
