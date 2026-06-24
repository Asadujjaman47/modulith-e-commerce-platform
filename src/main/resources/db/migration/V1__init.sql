-- V1__init.sql
-- Foundation baseline migration.
--
-- Establishes shared database setup. Module-specific schemas are added in later phases via
-- dedicated migrations (V2__catalog.sql, V3__inventory.sql, ...). Existing migrations are never
-- modified.

-- UUID generation support (UUID primary keys are used across all aggregates).
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Spring Modulith event publication registry. Stores externalized application events so they can be
-- republished if a transactional event listener fails. Mirrors the schema shipped by
-- spring-modulith-events-jpa.
CREATE TABLE IF NOT EXISTS event_publication (
    id               UUID         NOT NULL,
    listener_id      TEXT         NOT NULL,
    event_type       TEXT         NOT NULL,
    serialized_event TEXT         NOT NULL,
    publication_date TIMESTAMP    WITH TIME ZONE NOT NULL,
    completion_date  TIMESTAMP    WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_event_publication_completion_date
    ON event_publication (completion_date);