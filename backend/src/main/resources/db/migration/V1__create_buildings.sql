CREATE TABLE buildings (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    address TEXT NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT buildings_name_not_blank
        CHECK (length(trim(name)) > 0),
    CONSTRAINT buildings_address_not_blank
        CHECK (length(trim(address)) > 0)
);