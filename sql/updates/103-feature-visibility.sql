BEGIN;
SELECT _v.register_patch('103-feature-visibility', NULL, NULL);

INSERT INTO feature (feature_id, navigation_header_id, name, url_name) VALUES
	('PSYCHOLOGIST', 'CONNECT_WITH_SUPPORT', 'Psychologist', '/connect-with-support/psychologist'),
	('PSYCHIATRIST', 'CONNECT_WITH_SUPPORT', 'Psychiatrist', '/connect-with-support/psychiatrist'),
	('LCSW', 'CONNECT_WITH_SUPPORT', 'LCSW', '/connect-with-support/lcsw');

INSERT INTO feature_support_role(feature_id, support_role_id) VALUES
	('PSYCHOLOGIST', 'PSYCHOLOGIST'),
	('PSYCHIATRIST', 'PSYCHIATRIST'),
	('LCSW', 'LCSW');

INSERT INTO feature_filter(feature_id, filter_id) VALUES
	('PSYCHOLOGIST', 'DATE'),
	('PSYCHOLOGIST', 'TIME_OF_DAY'),
	('PSYCHIATRIST', 'DATE'),
	('PSYCHIATRIST', 'TIME_OF_DAY'),
	('LCSW', 'DATE'),
	('LCSW', 'TIME_OF_DAY');

ALTER TABLE institution_feature ALTER COLUMN nav_description DROP NOT NULL;
ALTER TABLE institution_feature ADD COLUMN nav_visible BOOLEAN NOT NULL DEFAULT TRUE;

COMMIT;