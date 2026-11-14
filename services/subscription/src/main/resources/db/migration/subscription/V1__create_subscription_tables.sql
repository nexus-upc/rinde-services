CREATE TABLE subscription.plans (
    plan_id       UUID           PRIMARY KEY,
    name          VARCHAR(80)    NOT NULL,
    monthly_price DECIMAL(10,2)  NOT NULL CHECK (monthly_price > 0),
    unit_limit    INTEGER        NOT NULL CHECK (unit_limit > 0),
    active        BOOLEAN        NOT NULL
);

CREATE TABLE subscription.subscriptions (
    subscription_id UUID         PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    plan_id         UUID         NOT NULL REFERENCES subscription.plans (plan_id),
    status          VARCHAR(20)  NOT NULL,
    starts_at       TIMESTAMP,
    expires_at      TIMESTAMP
);

CREATE INDEX idx_subscriptions_tenant_status
    ON subscription.subscriptions (tenant_id, status);

CREATE TABLE subscription.payments (
    payment_id         UUID          PRIMARY KEY,
    tenant_id          UUID          NOT NULL,
    subscription_id    UUID          NOT NULL REFERENCES subscription.subscriptions (subscription_id),
    provider_event_id  VARCHAR(160)  NOT NULL UNIQUE,
    provider_reference VARCHAR(160)  NOT NULL,
    status             VARCHAR(20)   NOT NULL,
    created_at         TIMESTAMP     NOT NULL
);

CREATE INDEX idx_payments_tenant_subscription
    ON subscription.payments (tenant_id, subscription_id);
