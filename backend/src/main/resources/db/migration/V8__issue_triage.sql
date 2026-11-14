ALTER TABLE issues ADD COLUMN priority VARCHAR(12) NOT NULL DEFAULT 'MEDIUM'
    CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT'));
ALTER TABLE issues ADD COLUMN question VARCHAR(1000);
ALTER TABLE issue_events ADD COLUMN visibility VARCHAR(12) NOT NULL DEFAULT 'PUBLIC'
    CHECK (visibility IN ('PUBLIC','INTERNAL'));
ALTER TABLE issue_events ADD COLUMN event_order BIGSERIAL;

CREATE TABLE issue_assignments (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL REFERENCES issues(id),
    technician_id UUID NOT NULL REFERENCES users(id),
    assigned_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX issue_one_active_assignment ON issue_assignments(issue_id) WHERE ended_at IS NULL;
CREATE INDEX assignments_technician ON issue_assignments(technician_id,issue_id) WHERE ended_at IS NULL;

CREATE TABLE issue_commands (
    issue_id UUID NOT NULL REFERENCES issues(id),
    actor_id UUID NOT NULL REFERENCES users(id),
    idempotency_key UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    result_version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (issue_id,actor_id,idempotency_key)
);
CREATE TABLE issue_notifications (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES users(id),
    building_id UUID NOT NULL REFERENCES buildings(id),
    issue_id UUID NOT NULL REFERENCES issues(id),
    event_id UUID NOT NULL REFERENCES issue_events(id),
    message VARCHAR(1200) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(event_id,recipient_id)
);
CREATE INDEX issue_notifications_inbox ON issue_notifications(recipient_id,building_id,created_at DESC);
