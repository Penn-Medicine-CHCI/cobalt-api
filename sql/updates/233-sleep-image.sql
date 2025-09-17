BEGIN;
SELECT _v.register_patch('233-sleep-image', NULL, NULL);

INSERT INTO screening_image VALUES ('SLEEP', 'Sleep');

COMMIT;