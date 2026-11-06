CREATE TABLE apartments (
    id UUID PRIMARY KEY,
    building_id UUID NOT NULL,
    unit_number VARCHAR(32) NOT NULL,
    floor VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT apartments_building_fk
        FOREIGN KEY (building_id)
        REFERENCES buildings(id)
        ON DELETE RESTRICT,

    CONSTRAINT apartments_building_unit_unique
        UNIQUE (building_id, unit_number),

    CONSTRAINT apartments_unit_number_valid
        CHECK (
            length(unit_number) > 0
            AND unit_number = trim(unit_number)
        )
);