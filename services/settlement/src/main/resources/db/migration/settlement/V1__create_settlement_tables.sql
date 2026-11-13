CREATE TABLE settlement.settlements (
    id                       UUID            PRIMARY KEY,
    tenant_id                UUID            NOT NULL,
    trip_id                  UUID            NOT NULL,
    driver_id                UUID            NOT NULL,
    status                   VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    advance_amount           DECIMAL(10,2),
    advance_currency         CHAR(3),
    expense_total_amount     DECIMAL(10,2),
    expense_total_currency   CHAR(3),
    advance_balance          DECIMAL(10,2),
    closed_by                UUID,
    closed_at                TIMESTAMP,
    created_at               TIMESTAMP       NOT NULL,
    updated_at               TIMESTAMP       NOT NULL,
    CONSTRAINT uk_settlements_trip_id UNIQUE (trip_id)
);

CREATE INDEX idx_settlements_tenant_trip ON settlement.settlements (tenant_id, trip_id);
