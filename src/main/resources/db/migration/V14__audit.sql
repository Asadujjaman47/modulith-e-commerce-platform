-- V14__audit.sql
-- Audit module schema: an append-only trail of business activity built from all published events.
--
-- One row is written per observed business event. entity_id and actor_id reference other modules by
-- value only (no cross-module FKs), per the module boundary rules. Rows are immutable once written.
-- Indexes back the admin search filters (category, event type, entity, actor, time window).

CREATE TABLE audit_logs (
    id          UUID         NOT NULL,
    category    VARCHAR(20)  NOT NULL,
    event_type  VARCHAR(64)  NOT NULL,
    action      VARCHAR(32)  NOT NULL,
    entity_type VARCHAR(64)  NOT NULL,
    entity_id   UUID,
    actor_id    UUID,
    description VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_logs_category ON audit_logs (category);
CREATE INDEX idx_audit_logs_event_type ON audit_logs (event_type);
CREATE INDEX idx_audit_logs_entity_id ON audit_logs (entity_id);
CREATE INDEX idx_audit_logs_actor_id ON audit_logs (actor_id);
CREATE INDEX idx_audit_logs_occurred_at ON audit_logs (occurred_at);
