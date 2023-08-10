BEGIN;
SELECT _v.register_patch('110-interaction-scheduling-updates', NULL, NULL);

CREATE TABLE interaction_send_method (
  interaction_send_method_id VARCHAR NOT NULL,
  description VARCHAR NOT NULL,
  CONSTRAINT interaction_send_method_pkey PRIMARY KEY (interaction_send_method_id)
);

INSERT INTO interaction_send_method VALUES
('MINUTE_OFFSET', 'Send interaction based on number of minutes.'),
('DAY_AND_TIME_OFFSET', 'Send interaction based on day and time offset.');

ALTER TABLE interaction
ADD COLUMN send_time_of_day TIME NOT NULL DEFAULT '11:30';

ALTER TABLE interaction
ADD COLUMN send_day_offset INTEGER NOT NULL DEFAULT 1;

ALTER TABLE interaction
ADD COLUMN interaction_send_method_id VARCHAR REFERENCES interaction_send_method;

ALTER TABLE interaction
ADD COLUMN email_subject VARCHAR;

UPDATE interaction SET interaction_send_method_id = 'MINUTE_OFFSET';

ALTER TABLE interaction ALTER COLUMN interaction_send_method_id SET NOT NULL;

COMMIT;