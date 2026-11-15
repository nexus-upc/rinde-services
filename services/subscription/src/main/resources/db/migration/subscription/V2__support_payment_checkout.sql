ALTER TABLE subscription.payments
    ALTER COLUMN provider_event_id DROP NOT NULL;

ALTER TABLE subscription.payments
    ADD COLUMN amount DECIMAL(10,2);

UPDATE subscription.payments payment
SET amount = plan.monthly_price
FROM subscription.subscriptions subscription
JOIN subscription.plans plan ON plan.plan_id = subscription.plan_id
WHERE payment.subscription_id = subscription.subscription_id
  AND payment.amount IS NULL;

ALTER TABLE subscription.payments
    ALTER COLUMN amount SET NOT NULL;

ALTER TABLE subscription.payments
    ADD COLUMN failure_reason VARCHAR(500);

ALTER TABLE subscription.payments
    ADD CONSTRAINT uk_payments_provider_reference UNIQUE (provider_reference);

ALTER TABLE subscription.payments
    ADD CONSTRAINT ck_payments_amount_positive CHECK (amount > 0);
