BEGIN;
SELECT _v.register_patch(
	'267-institution-location-group',
	ARRAY['266-provider-booking-analytics'],
	NULL
);

-- Groups are presentation metadata only. Provider eligibility continues to
-- match institution_location_id exactly and does not inherit through a group.
CREATE TABLE institution_location_group (
	institution_location_group_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id VARCHAR NOT NULL REFERENCES institution,
	name TEXT NOT NULL CHECK (BTRIM(name) <> ''),
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	UNIQUE (institution_location_group_id, institution_id)
);

CREATE TRIGGER set_last_updated
BEFORE INSERT OR UPDATE ON institution_location_group
FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE UNIQUE INDEX institution_location_group_name_unique_idx
ON institution_location_group (institution_id, LOWER(name));

CREATE INDEX institution_location_group_display_order_idx
ON institution_location_group (institution_id, display_order, institution_location_group_id);

ALTER TABLE institution_location
ADD COLUMN institution_location_group_id UUID;

ALTER TABLE institution_location
ADD CONSTRAINT institution_location_institution_location_group_fk
FOREIGN KEY (institution_location_group_id, institution_id)
REFERENCES institution_location_group (institution_location_group_id, institution_id);

CREATE INDEX institution_location_group_location_display_order_idx
ON institution_location (institution_id, institution_location_group_id, display_order, institution_location_id)
WHERE institution_location_group_id IS NOT NULL;

COMMENT ON TABLE institution_location_group IS
'Optional client-facing groups for institution locations. Groups do not define eligibility inheritance.';

COMMENT ON COLUMN institution_location.institution_location_group_id IS
'Optional presentation group. Provider eligibility continues to match institution_location_id exactly.';

COMMIT;
