BEGIN;
SELECT _v.register_patch('007-way2health', NULL, NULL);

ALTER TABLE institution ADD COLUMN metadata JSONB NULL;

UPDATE institution
SET metadata = '{"way2HealthIncidentTrackingConfigs": []}'::jsonb
WHERE institution_id != 'COBALT';

UPDATE institution
SET metadata = '{"way2HealthIncidentTrackingConfigs": [{"enabled": true, "studyId": 715, "type": "Medical Emergency: Suicide Ideation", "interactionId": "45f4082c-4d16-400e-aecd-38e87726f6d9"}]}'::jsonb
WHERE institution_id = 'COBALT';

ALTER TABLE institution ALTER COLUMN metadata SET NOT NULL;

CREATE TABLE way2health_incident (
  way2health_incident_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id VARCHAR NOT NULL REFERENCES institution,
  incident_id BIGINT NOT NULL,
  study_id BIGINT NOT NULL,
  raw_json JSONB NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON way2health_incident FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

END;