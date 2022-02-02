BEGIN;
SELECT _v.register_patch('011-provider-specialties', NULL, NULL);

ALTER TABLE provider ADD bio_url VARCHAR;

-- Tie specialties to institutions directly (as opposed to implicitly by provider)
-- because we prefer an institution's administrator to manage the set of possible specialties
-- as opposed to having providers define whatever they like and potentially filling up the provider search UI with
-- lots of (possibly redundant) options
CREATE TABLE specialty (
	specialty_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id VARCHAR NOT NULL REFERENCES institution,
	description VARCHAR NOT NULL,
	display_order SMALLINT NOT NULL
);

INSERT INTO specialty (specialty_id, institution_id, description, display_order)
VALUES ('dca84296-85a1-488c-8a6e-5462bd011fd3', 'COBALT', 'Patient Care', 1);

INSERT INTO specialty (specialty_id, institution_id, description, display_order)
VALUES ('58ccf1bb-8257-4a01-8847-c4d99e360ef6', 'COBALT', 'Academic', 2);

INSERT INTO specialty (specialty_id, institution_id, description, display_order)
VALUES ('413585b6-5eb6-4cee-92ff-f6974c7464e2', 'COBALT', 'Work/Life Balance', 3);

CREATE TABLE provider_specialty (
	provider_id UUID NOT NULL REFERENCES provider,
	specialty_id UUID NOT NULL REFERENCES specialty,
	PRIMARY KEY(provider_id, specialty_id)
);

END;