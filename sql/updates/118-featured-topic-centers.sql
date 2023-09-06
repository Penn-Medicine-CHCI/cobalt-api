BEGIN;
SELECT _v.register_patch('118-featured-topic-centers', NULL, NULL);

ALTER TABLE institution ADD COLUMN featured_topic_center_id UUID REFERENCES topic_center;

CREATE TABLE topic_center_display_style (
  topic_center_display_style_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO topic_center_display_style VALUES ('DEFAULT', 'Default');
INSERT INTO topic_center_display_style VALUES ('FEATURED', 'Featured');

ALTER TABLE topic_center ADD COLUMN topic_center_display_style_id VARCHAR REFERENCES topic_center_display_style NOT NULL DEFAULT 'DEFAULT';
ALTER TABLE topic_center ADD COLUMN featured_title TEXT;
ALTER TABLE topic_center ADD COLUMN featured_description TEXT;
ALTER TABLE topic_center ADD COLUMN featured_call_to_action TEXT;
ALTER TABLE topic_center ADD COLUMN image_url TEXT;

COMMIT;