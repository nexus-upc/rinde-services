CREATE TABLE incident.incidents (
    id                      UUID            PRIMARY KEY,
    tenant_id               UUID            NOT NULL,
    trip_id                 UUID            NOT NULL,
    vehicle_id              UUID            NOT NULL,
    reported_by             UUID            NOT NULL,
    type                    VARCHAR(30)     NOT NULL,
    description             VARCHAR(500)    NOT NULL,
    evidence_url            VARCHAR(500),
    status                  VARCHAR(20)     NOT NULL,
    idempotency_key         VARCHAR(64),
    reported_at             TIMESTAMP       NOT NULL,
    resolution_action       VARCHAR(500),
    resolution_closed_by    UUID,
    resolution_closed_at    TIMESTAMP,
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP       NOT NULL,
    CONSTRAINT uk_incidents_tenant_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_incidents_tenant_status ON incident.incidents (tenant_id, status);
CREATE INDEX idx_incidents_tenant_trip ON incident.incidents (tenant_id, trip_id);
CREATE INDEX idx_incidents_tenant_vehicle ON incident.incidents (tenant_id, vehicle_id);
