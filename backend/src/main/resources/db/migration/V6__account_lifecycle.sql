ALTER TABLE users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE account_invitations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    building_id UUID NOT NULL REFERENCES buildings(id),
    apartment_id UUID NOT NULL REFERENCES apartments(id),
    membership_id UUID NOT NULL REFERENCES building_memberships(id),
    invited_by UUID NOT NULL REFERENCES users(id),
    verification_source VARCHAR(160) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','ACCEPTED','EXPIRED','REVOKED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    incorrect_details BOOLEAN NOT NULL DEFAULT FALSE,
    reason VARCHAR(500)
);
CREATE UNIQUE INDEX account_invitations_one_pending
    ON account_invitations(user_id, building_id) WHERE status = 'PENDING';

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE account_audit_events (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id),
    building_id UUID REFERENCES buildings(id),
    action VARCHAR(64) NOT NULL,
    target_id UUID,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE FUNCTION reject_account_audit_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Account audit events are append-only';
END;
$$;
CREATE TRIGGER account_audit_append_only
    BEFORE UPDATE OR DELETE ON account_audit_events
    FOR EACH ROW EXECUTE FUNCTION reject_account_audit_change();
