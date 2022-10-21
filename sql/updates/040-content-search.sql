BEGIN;
SELECT _v.register_patch('40-content-search', NULL, NULL);

-- Cannot use TSVECTOR GENERATED ALWAYS in RDS (yet...)
ALTER TABLE content ADD COLUMN en_search_vector TSVECTOR;

CREATE INDEX content_en_search_vector_idx ON content USING GIN (en_search_vector);

CREATE FUNCTION content_en_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    NEW.en_search_vector = TO_TSVECTOR('pg_catalog.english', COALESCE(NEW.title, '') || ' ' || COALESCE(NEW.description, '') || ' ' || COALESCE(NEW.author, ''));
    RETURN NEW;
END
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER content_en_search_vector_update_tg BEFORE INSERT OR UPDATE ON content
FOR EACH ROW EXECUTE PROCEDURE content_en_search_vector_update();

-- Force trigger to run
UPDATE content SET title=title;

ALTER TABLE content ALTER COLUMN en_search_vector SET NOT NULL;

-- Recreate affected view to include en_search_vector
DROP VIEW v_admin_content;

CREATE VIEW v_admin_content AS
 SELECT c.content_id,
    c.content_type_id,
    c.title,
    c.url,
    c.date_created,
    c.duration_in_minutes,
    c.image_url,
    c.description,
    c.author,
    c.created,
    c.last_updated,
    c.owner_institution_id,
    c.en_search_vector,
    ctl.description AS content_type_label,
    ct.description AS content_type_description,
    ct.call_to_action,
    it.institution_id,
    i.name AS owner_institution,
    it.approved_flag,
    c.archived_flag,
    c.deleted_flag,
    c.content_type_label_id,
    c.visibility_id,
    c.other_institution_approval_status_id,
    c.owner_institution_approval_status_id
   FROM content c,
    content_type ct,
    institution_content it,
    institution i,
    content_type_label ctl
  WHERE c.content_type_id::text = ct.content_type_id::text AND c.content_id = it.content_id AND c.owner_institution_id::text = i.institution_id::text AND c.deleted_flag = false AND c.content_type_label_id::text = ctl.content_type_label_id::text;

COMMIT;