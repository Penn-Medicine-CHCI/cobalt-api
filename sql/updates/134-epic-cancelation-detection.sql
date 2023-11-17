BEGIN;
SELECT _v.register_patch('134-epic-cancelation-detection', NULL, NULL);

CREATE TABLE appointment_cancelation_reason (
	appointment_cancelation_reason_id VARCHAR PRIMARY KEY,
	description VARCHAR NOT NULL
);

INSERT INTO appointment_cancelation_reason VALUES ('UNSPECIFIED', 'Unspecified');
INSERT INTO appointment_cancelation_reason VALUES ('EXTERNALLY_CANCELED', 'Externally Canceled');

ALTER TABLE appointment ADD COLUMN appointment_cancelation_reason_id TEXT NOT NULL REFERENCES appointment_cancelation_reason DEFAULT 'UNSPECIFIED';

-- e.g. urn:oid:1.1.111.111111.1.11.111.1.1.1.111111.1
ALTER TABLE appointment ADD COLUMN epic_appointment_fhir_identifier_system TEXT;

-- e.g. 12345678
ALTER TABLE appointment ADD COLUMN epic_appointment_fhir_identifier_value TEXT;

-- Keep the booking response around in case it's needed later
ALTER TABLE appointment ADD COLUMN epic_appointment_fhir_stu3_response JSONB;

COMMIT;