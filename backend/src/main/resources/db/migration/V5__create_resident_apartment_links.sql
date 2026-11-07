CREATE TABLE resident_apartment_links (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    apartment_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'VERIFIED',

    verified_by UUID NOT NULL,
    verified_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verification_source VARCHAR(160) NOT NULL,

    valid_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_to TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT resident_links_user_fk
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT resident_links_apartment_fk
        FOREIGN KEY (apartment_id)
        REFERENCES apartments(id)
        ON DELETE RESTRICT,

    CONSTRAINT resident_links_verifier_fk
        FOREIGN KEY (verified_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT resident_links_source_not_blank
        CHECK (length(trim(verification_source)) > 0),

    CONSTRAINT resident_links_period_valid
        CHECK (valid_to IS NULL OR valid_to > valid_from),

    CONSTRAINT resident_links_status_consistent
        CHECK (
            (status = 'VERIFIED' AND ended_at IS NULL)
            OR
            (status = 'ENDED' AND ended_at IS NOT NULL)
        )
);

CREATE UNIQUE INDEX resident_links_one_open_pair
    ON resident_apartment_links (user_id, apartment_id)
    WHERE status = 'VERIFIED';