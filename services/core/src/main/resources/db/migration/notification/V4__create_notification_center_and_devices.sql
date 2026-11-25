CREATE TABLE notification.company_notifications (
    id          UUID         PRIMARY KEY,
    event_id    UUID         NOT NULL,
    tenant_id   UUID         NOT NULL,
    type        VARCHAR(40)  NOT NULL,
    title       VARCHAR(150) NOT NULL,
    message     VARCHAR(500) NOT NULL,
    link        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT uk_company_notifications_event_id UNIQUE (event_id)
);

CREATE INDEX idx_company_notifications_tenant_created
    ON notification.company_notifications (tenant_id, created_at DESC);

CREATE TABLE notification.notification_reads (
    notification_id UUID      NOT NULL REFERENCES notification.company_notifications (id) ON DELETE CASCADE,
    user_id         UUID      NOT NULL,
    tenant_id       UUID      NOT NULL,
    read_at         TIMESTAMP NOT NULL,
    PRIMARY KEY (notification_id, user_id)
);

CREATE INDEX idx_notification_reads_tenant_user
    ON notification.notification_reads (tenant_id, user_id);

CREATE TABLE notification.driver_devices (
    id          UUID         PRIMARY KEY,
    tenant_id   UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    token       VARCHAR(512) NOT NULL,
    platform    VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT uk_driver_devices_token UNIQUE (token)
);

CREATE INDEX idx_driver_devices_tenant_user
    ON notification.driver_devices (tenant_id, user_id);
