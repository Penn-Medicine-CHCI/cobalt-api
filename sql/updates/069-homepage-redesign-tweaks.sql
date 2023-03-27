BEGIN;
SELECT _v.register_patch('069-homepage-redesign-tweaks', NULL, NULL);

DELETE FROM feature_filter WHERE feature_id='COACHING' AND filter_id='LOCATION';

INSERT INTO feature_filter
  (feature_id, filter_id)
VALUES
  ('COACHING', 'DATE');

UPDATE filter SET name='Employer' where filter_id='LOCATION';

CREATE TABLE screening_flow_skip_type (
  screening_flow_skip_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO screening_flow_skip_type VALUES ('SKIP', 'Skip');
INSERT INTO screening_flow_skip_type VALUES ('EXIT', 'Exit');

ALTER TABLE screening_flow_version ADD COLUMN screening_flow_skip_type_id VARCHAR NOT NULL REFERENCES screening_flow_skip_type DEFAULT 'SKIP';

COMMIT;