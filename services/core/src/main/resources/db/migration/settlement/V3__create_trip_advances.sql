CREATE TABLE settlement.trip_advances (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL,
    trip_id         UUID          NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    currency        CHAR(3)       NOT NULL,
    delivery_method VARCHAR(20)   NOT NULL,
    registered_by   UUID          NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    CONSTRAINT ck_trip_advances_amount CHECK (amount > 0),
    CONSTRAINT ck_trip_advances_delivery_method CHECK (delivery_method IN ('CASH', 'TRANSFER'))
);

CREATE INDEX idx_trip_advances_tenant_trip ON settlement.trip_advances (tenant_id, trip_id);
