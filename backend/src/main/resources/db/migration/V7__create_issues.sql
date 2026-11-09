CREATE SEQUENCE issue_code_sequence;
CREATE TABLE issues (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE DEFAULT ('CF-' || lpad(nextval('issue_code_sequence')::text, 8, '0')),
    building_id UUID NOT NULL REFERENCES buildings(id),
    reported_by UUID NOT NULL REFERENCES users(id),
    reporter_name_at_report VARCHAR(160) NOT NULL,
    apartment_id_at_report UUID REFERENCES apartments(id),
    apartment_label_at_report VARCHAR(32),
    location_description VARCHAR(300) NOT NULL CHECK (length(trim(location_description)) BETWEEN 3 AND 300),
    description VARCHAR(3000) NOT NULL CHECK (length(trim(description)) BETWEEN 10 AND 3000),
    category VARCHAR(20) NOT NULL CHECK (category IN ('OTHER','ELECTRICAL','WATER','ELEVATOR')),
    scope VARCHAR(20) NOT NULL DEFAULT 'COMMON_AREA' CHECK (scope='COMMON_AREA'),
    source VARCHAR(20) NOT NULL DEFAULT 'WEB' CHECK (source='WEB'),
    status VARCHAR(24) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED','NEEDS_INFO','ACCEPTED','ASSIGNED','IN_PROGRESS','ON_HOLD','RESOLVED','CLOSED','REJECTED')),
    version BIGINT NOT NULL DEFAULT 0,
    idempotency_key UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (building_id,reported_by,idempotency_key),
    CHECK ((apartment_id_at_report IS NULL) = (apartment_label_at_report IS NULL))
);
CREATE INDEX issues_building_created ON issues(building_id,created_at DESC,id DESC);
CREATE INDEX issues_reporter_created ON issues(building_id,reported_by,created_at DESC,id DESC);

CREATE TABLE issue_events (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL REFERENCES issues(id),
    actor_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(40) NOT NULL,
    public_message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX issue_events_history ON issue_events(issue_id,created_at,id);
CREATE FUNCTION reject_issue_event_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Issue events are append-only';
END;
$$;
CREATE TRIGGER issue_events_append_only BEFORE UPDATE OR DELETE ON issue_events
    FOR EACH ROW EXECUTE FUNCTION reject_issue_event_change();
