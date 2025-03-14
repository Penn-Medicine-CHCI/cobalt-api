BEGIN;
SELECT _v.register_patch('205-page-tag', NULL, NULL);

CREATE TABLE page_row_tag_group (
    page_row_tag_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    page_row_id UUID NOT NULL REFERENCES page_row,
    tag_id VARCHAR NOT NULL REFERENCES tag,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_page_row_tag_row_tag ON page_row_tag(page_row_id, tag_group_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_row_tag FOR EACH ROW EXECUTE PROCEDURE set_last_updated();


INSERT INTO row_type
VALUES
('TAG', 'Tag');

CREATE VIEW v_page_row_tag
AS 
SELECT prt.page_row_tag_id, pr.page_row_id, pr.display_order, pr.page_section_id, pr.row_type_id, 
prt.tag_id, pr.created_by_account_id, pr.created, pr.last_updated
FROM page_row_tag prt, page_row pr
WHERE prt.page_row_id = pr.page_row_id
AND pr.deleted_flag = FALSE;


COMMIT;