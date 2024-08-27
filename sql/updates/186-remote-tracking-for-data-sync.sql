BEGIN;
SELECT _v.register_patch('186-remote-tracking-for-data-sync', NULL, NULL);

ALTER TABLE institution ADD COLUMN remote_data_flag BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE file_upload ADD COLUMN remote_data_flag BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tag_content ADD COLUMN remote_data_flag BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tag ADD COLUMN remote_data_flag BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE institution ADD COLUMN sync_data BOOLEAN NOT NULL DEFAULT false;

CREATE OR REPLACE VIEW v_remote_content 
AS
SELECT rc.* 
FROM remote_content rc, remote_institution ri 
WHERE rc.owner_institution_id = ri.institution_id
AND rc.shared_flag=TRUE
AND rc.published=TRUE
AND rc.deleted_flag=FALSE
AND ri.sync_data = TRUE;

CREATE OR REPLACE VIEW v_remote_tag_content
AS
SELECT rtc.* 
FROM remote_tag_content rtc, v_remote_content vrct
WHERE rtc.content_id = vrct.content_id;

CREATE OR REPLACE VIEW v_remote_tag
AS
SELECT rt.*
FROM remote_tag rt, v_remote_tag_content vrct
WHERE rt.tag_id = vrct.tag_id;

CREATE OR REPLACE VIEW v_remote_institution
AS
SELECT ri.* 
FROM remote_institution ri
WHERE ri.sync_data = TRUE;

CREATE OR REPLACE VIEW v_remote_institution_content
AS
SELECT ric.*
FROM remote_institution_content ric, v_remote_institution vri, v_remote_content vrc
WHERE ric.institution_id = vri.institution_id
AND ric.content_id = vrc.content_id;

COMMIT;