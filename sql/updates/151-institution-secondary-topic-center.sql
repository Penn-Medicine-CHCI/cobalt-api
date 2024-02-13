BEGIN;
SELECT _v.register_patch('151-institution-secondary-topic-center', NULL, NULL);

ALTER TABLE institution ADD COLUMN featured_secondary_topic_center_id UUID REFERENCES topic_center;

ALTER TABLE topic_center ADD COLUMN url_override TEXT;

COMMIT;