CREATE TABLE trip.trip_locations (
    id               UUID          PRIMARY KEY,
    tenant_id        UUID          NOT NULL,
    trip_id          UUID          NOT NULL REFERENCES trip.trips (id),
    latitude         DECIMAL(9,6)  NOT NULL,
    longitude        DECIMAL(9,6)  NOT NULL,
    accuracy_meters  DECIMAL(10,2),
    recorded_at      TIMESTAMP     NOT NULL,
    received_at      TIMESTAMP     NOT NULL,
    CONSTRAINT uk_trip_locations_trip_recorded_at UNIQUE (trip_id, recorded_at),
    CONSTRAINT ck_trip_locations_latitude CHECK (latitude >= -90 AND latitude <= 90),
    CONSTRAINT ck_trip_locations_longitude CHECK (longitude >= -180 AND longitude <= 180),
    CONSTRAINT ck_trip_locations_accuracy CHECK (accuracy_meters IS NULL OR accuracy_meters >= 0)
);

CREATE INDEX idx_trip_locations_tenant_trip_recorded_at
    ON trip.trip_locations (tenant_id, trip_id, recorded_at);
