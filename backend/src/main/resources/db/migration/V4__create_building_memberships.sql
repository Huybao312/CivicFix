CREATE TABLE building_memberships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    building_id UUID NOT NULL,
    role VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    valid_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT memberships_user_fk
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT memberships_building_fk
        FOREIGN KEY (building_id)
        REFERENCES buildings(id)
        ON DELETE RESTRICT,

    CONSTRAINT memberships_role_valid
        CHECK (
            role IN ('RESIDENT', 'MANAGER', 'TECHNICIAN', 'ADMIN')
        ),

    CONSTRAINT memberships_status_valid
        CHECK (status IN ('PENDING', 'ACTIVE', 'ENDED')),

    CONSTRAINT memberships_period_valid
        CHECK (valid_to IS NULL OR valid_to > valid_from)
);

CREATE UNIQUE INDEX memberships_one_open_role
    ON building_memberships (user_id, building_id, role)
    WHERE status IN ('PENDING', 'ACTIVE');