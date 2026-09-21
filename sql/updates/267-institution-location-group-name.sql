BEGIN;
SELECT _v.register_patch(
	'267-institution-location-group-name',
	ARRAY['266-provider-booking-analytics'],
	NULL
);

-- Optional presentation metadata for clients that group institution locations
-- under an employer or another tenant-defined heading.  Location matching
-- continues to use institution_location_id; this field has no hierarchy or
-- inheritance semantics.
ALTER TABLE institution_location
ADD COLUMN group_name TEXT;

COMMENT ON COLUMN institution_location.group_name IS
'Optional client-facing option-group label. It does not define location hierarchy or eligibility inheritance.';

COMMIT;
