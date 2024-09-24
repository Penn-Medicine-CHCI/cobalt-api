BEGIN;
SELECT _v.register_patch('188-data-sync', NULL, NULL);

CREATE OR REPLACE VIEW v_remote_content
AS
SELECT rc.*
FROM remote_content rc, remote_institution ri
WHERE rc.owner_institution_id = ri.institution_id
AND rc.shared_flag=TRUE
AND rc.published=TRUE
AND rc.deleted_flag=FALSE
AND ri.sync_data = TRUE;

CREATE OR REPLACE VIEW v_remote_tag
AS
SELECT rt.*
FROM remote_tag rt, v_remote_tag_content vrct
WHERE rt.tag_id = vrct.tag_id;

COMMIT;