CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'INVITED',
    email_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT users_email_unique
        UNIQUE (email),

    CONSTRAINT users_email_normalized
        CHECK (
            length(email) > 0
            AND email = lower(trim(email))
        ),

    CONSTRAINT users_display_name_not_blank
        CHECK (length(trim(display_name)) > 0),

    CONSTRAINT users_status_valid
        CHECK (status IN ('INVITED', 'ACTIVE', 'DISABLED')),

    CONSTRAINT users_password_hash_not_blank
        CHECK (
            password_hash IS NULL
            OR length(trim(password_hash)) > 0
        ),

    CONSTRAINT users_active_requires_credentials
        CHECK (
            status <> 'ACTIVE'
            OR (
                password_hash IS NOT NULL
                AND email_verified_at IS NOT NULL
            )
        )
);