BEGIN;
SELECT _v.register_patch('079-ic-updates', NULL, NULL);

-- Optional ability to express "here are all the possible screening types for this flow".
-- Requested by IC team - they want the ability to show which screening types were _not_ taken
-- during a screening session, e.g. if the session ends quickly based on how a patient answers.
CREATE TABLE screening_flow_version_screening_type (
	screening_flow_version_id UUID NOT NULL REFERENCES screening_flow_version,
	screening_type_id VARCHAR NOT NULL REFERENCES screening_type,
	PRIMARY KEY (screening_flow_version_id, screening_type_id)
);

COMMIT;