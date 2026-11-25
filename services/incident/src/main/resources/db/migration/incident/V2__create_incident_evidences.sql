CREATE TABLE incident.incident_evidences (
    id              UUID            PRIMARY KEY,
    tenant_id       UUID            NOT NULL,
    incident_id     UUID            NOT NULL UNIQUE REFERENCES incident.incidents (id) ON DELETE CASCADE,
    file_name       VARCHAR(255)    NOT NULL,
    content_type    VARCHAR(100)    NOT NULL,
    file_size_bytes BIGINT          NOT NULL,
    sha256          VARCHAR(64)     NOT NULL,
    storage_key     VARCHAR(255)    NOT NULL,
    uploaded_by     UUID            NOT NULL,
    uploaded_at     TIMESTAMP       NOT NULL
);

CREATE INDEX idx_incident_evidences_tenant_incident
    ON incident.incident_evidences (tenant_id, incident_id);
