BEGIN;
SELECT _v.register_patch('070-institution-location-display-order', NULL, NULL);

ALTER TABLE institution_location ADD COLUMN display_order INTEGER;

UPDATE institution_location SET display_order = 1 WHERE institution_id='COBALT' AND name='Cobalt Health System';
UPDATE institution_location SET display_order = 2 WHERE institution_id='COBALT' AND name='Cobalt General';

UPDATE institution_location SET display_order = 1 WHERE institution_id != 'COBALT';

ALTER TABLE institution_location ALTER COLUMN display_order SET NOT NULL;

COMMIT;