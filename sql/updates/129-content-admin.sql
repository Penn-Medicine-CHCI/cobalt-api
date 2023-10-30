BEGIN;
SELECT _v.register_patch('128-content-admin', NULL, NULL);

CREATE TABLE content_status
(content_status_id VARCHAR NOT NULL PRIMARY KEY,
 description VARCHAR NOT NULL,
 display_order INTEGER NOT NULL);

INSERT INTO content_status
(content_status_id, description, display_order)
VALUES
('DRAFT', 'Draft',1),
('SCHEDULED', 'Scheduled', 2),
('LIVE', 'Live', 3),
('EXPIRED', 'Expired', 4);

ALTER TABLE content ADD COLUMN content_status_id VARCHAR REFERENCES content_status;
ALTER TABLE content ADD COLUMN shared_flag BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE content ADD COLUMN search_terms VARCHAR NULL;
ALTER TABLE content ADD COLUMN publish_start_date timestamptz NULL;
ALTER TABLE content ADD COLUMN publish_end_date timestamptz NULL;
ALTER TABLE content ADD COLUMN publish_recurring BOOLEAN NOT NULL DEFAULT false;

UPDATE content SET content_status_id = 'EXPIRED' where archived_flag = true;
UPDATE content SET content_status_id = 'DRAFT' where owner_institution_approval_status_id='PENDING' and content_status_id IS NULL;
UPDATE content SET content_status_id = 'LIVE' where owner_institution_approval_status_id='APPROVED' and content_status_id IS NULL;
UPDATE content SET content_status_id = 'EXPIRED' where owner_institution_approval_status_id='REJECTED' and content_status_id IS NULL;
ALTER TABLE content ALTER COLUMN content_status_id SET NOT NULL;

UPDATE content SET shared_flag = TRUE WHERE visibility_id = 'PUBLIC';

UPDATE content SET publish_start_date = date_created;
UPDATE content SET publish_start_date = created WHERE publish_start_date IS NULL;
ALTER TABLE content ALTER COLUMN publish_start_date SET NOT NULL;

UPDATE content SET date_created = created WHERE date_created IS NULL;
ALTER TABLE content ALTER COLUMN date_created SET NOT NULL;

DROP VIEW v_admin_content;

ALTER TABLE content DROP COLUMN archived_flag;
ALTER TABLE content DROP COLUMN owner_institution_approval_status_id;
ALTER TABLE content DROP COLUMN other_institution_approval_status_id;
ALTER TABLE content DROP COLUMN visibility_id;
ALTER TABLE content DROP COLUMN content_type_label_id;

DROP TABLE visibility;
DROP TABLE approval_status;
DROP TABLE available_status;

ALTER TABLE institution_content DROP COLUMN approved_flag;

CREATE OR REPLACE VIEW v_admin_content
AS SELECT c.content_id,
    c.content_type_id,
    c.title,
    c.url,
    c.publish_start_date,
    c.publish_end_date,
    c.publish_recurring,
    c.content_status_id,
    cs.description content_status_description,
    c.shared_flag, 
    c.search_terms,
    c.duration_in_minutes,
    c.image_url,
    c.description,
    c.author,
    c.created,
    c.last_updated,
    c.en_search_vector,
    ct.description AS content_type_description,
    ct.call_to_action,
    it.institution_id,
    c.owner_institution_id,
    i.name AS owner_institution,
    c.date_created
   FROM content c,
    content_type ct,
    institution_content it,
    institution i,
    content_status cs
  WHERE c.content_type_id::text = ct.content_type_id::text 
  AND c.content_id = it.content_id 
  AND c.owner_institution_id::text = i.institution_id::text
  AND c.content_status_id = cs.content_status_id
  AND c.deleted_flag = false;


COMMIT;