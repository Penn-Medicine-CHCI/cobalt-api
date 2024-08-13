BEGIN;
SELECT _v.register_patch('180-study-audio-video-tracking', NULL, NULL);

CREATE TABLE recording_preference
(recording_preference_id VARCHAR NOT NULL PRIMARY KEY,
 description VARCHAR NOT NULL);

INSERT INTO recording_preference
VALUES
('NO_PREFERENCE', 'No Preference'),
('VIDEO', 'Video'),
('AUDIO', 'Audio');

ALTER TABLE account_study ADD COLUMN recording_preference_id VARCHAR NOT NULL REFERENCES recording_preference DEFAULT 'NO_PREFERENCE';

 CREATE OR REPLACE VIEW v_account_study
 AS
 SELECT a.account_study_id,
    a.account_id,
    a.study_id,
    a.created,
    a.last_updated,
    a.encryption_keypair_id,
    a.deleted,
    a.study_started,
    ac.institution_id,
    ac.time_zone,
    ac.password_reset_required,
    a.recording_preference_id,
    ac.username
   FROM account_study a,
    study s,
    account ac
  WHERE a.study_id = s.study_id AND a.account_id = ac.account_id AND a.deleted = false;


 ALTER TABLE study ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

 INSERT INTO account_capability_type 
 VALUES
 ('STUDY_ADMIN', 'Study Administrator');
 
COMMIT;
