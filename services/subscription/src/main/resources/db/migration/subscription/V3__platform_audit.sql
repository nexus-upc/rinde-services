CREATE TABLE subscription.platform_audit (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    actor_email VARCHAR(150) NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id UUID NOT NULL,
    reason VARCHAR(500) NOT NULL,
    before_state TEXT NOT NULL,
    after_state TEXT NOT NULL,
    occurred_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_platform_audit_time ON subscription.platform_audit (occurred_at DESC, id);
