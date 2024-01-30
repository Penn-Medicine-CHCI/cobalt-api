BEGIN;
SELECT _v.register_patch('144-filesize', NULL, NULL);

ALTER TABLE file_upload ADD COLUMN filesize NUMERIC;

CREATE OR REPLACE VIEW v_admin_content
AS SELECT c.content_id,
    c.content_type_id,
    c.title,
    c.url,
    c.publish_start_date,
    c.publish_end_date,
    c.publish_recurring,
    CASE WHEN c.published =  false THEN 'DRAFT'
    WHEN NOW() BETWEEN c.publish_start_date AND COALESCE(c.publish_end_date, NOW() + INTERVAL '1 DAY') THEN 'LIVE'
    WHEN COALESCE(c.publish_end_date, NOW() + INTERVAL '1 DAY') < NOW() THEN 'EXPIRED'
    WHEN c.publish_start_date > NOW() THEN 'SCHEDULED'
    END as content_status_id,
    CASE WHEN c.published =  false THEN 'Draft'
    WHEN NOW() BETWEEN c.publish_start_date AND c.publish_end_date THEN 'Live'
    WHEN c.publish_end_date < NOW() THEN 'Expired'
    WHEN c.publish_start_date > NOW() THEN 'Scheduled'
    END as content_status_description,
    c.shared_flag, 
    c.search_terms,
    c.duration_in_minutes,
    c.description,
    c.author,
    c.created,
    c.last_updated,
    c.en_search_vector,
    ct.description AS content_type_description,
    ct.call_to_action,
    c.owner_institution_id,
    i.name AS owner_institution,
    c.date_created,
    c.file_upload_id,
    c.image_file_upload_id,
    fu.url as file_url,
    fu.filename,
    fu.content_type as file_content_type,
    fu2.url as image_url,
    fu.filesize
   FROM content_type ct,
    institution i,
    content c 
    LEFT OUTER JOIN file_upload fu ON c.file_upload_id = fu.file_upload_id
    LEFT OUTER JOIN file_upload fu2 ON c.image_file_upload_id = fu2.file_upload_id
  WHERE c.content_type_id::text = ct.content_type_id::text 
  AND c.owner_institution_id::text = i.institution_id::text
  AND c.deleted_flag = false;

  DROP VIEW v_institution_content;

  CREATE OR REPLACE VIEW v_institution_content
  AS SELECT vac.*,
    it.institution_id,
    it.created AS institution_created_date
   FROM v_admin_content vac,
    institution_content it
  WHERE vac.content_id = it.content_id;

COMMIT;
