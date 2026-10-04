-- One database per service on a single PostgreSQL instance: services cannot read each other's tables,
-- and each can be migrated, backed up or moved to its own server independently.
-- Runs once, when the data volume is created (docker-entrypoint-initdb.d), as the POSTGRES_USER superuser.
-- Schemas, tables and the pgvector extension are created by each service's Flyway migrations.
CREATE DATABASE user_db;
CREATE DATABASE book_db;
CREATE DATABASE order_db;
CREATE DATABASE assistant_db;
