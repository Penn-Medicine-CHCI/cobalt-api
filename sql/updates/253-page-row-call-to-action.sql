BEGIN;
SELECT _v.register_patch('253-page-row-call-to-action', NULL, NULL);

INSERT INTO row_type (row_type_id, description) VALUES ('CALL_TO_ACTION_BLOCK', 'Call-to-Action (Block)');
INSERT INTO row_type (row_type_id, description) VALUES ('CALL_TO_ACTION_FULL_WIDTH', 'Call-to-Action (Full Width)');

CREATE TABLE page_row_call_to_action (
  page_row_call_to_action_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  page_row_id UUID NOT NULL REFERENCES page_row,
  headline TEXT NOT NULL,
  description TEXT NOT NULL,
  button_text TEXT NOT NULL,
  button_url TEXT NOT NULL,
  image_file_upload_id UUID REFERENCES file_upload,
  created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_page_row_call_to_action_row ON page_row_call_to_action(page_row_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_row_call_to_action FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE VIEW v_page_row_call_to_action AS
SELECT
  prcta.page_row_call_to_action_id,
  pr.page_row_id,
  pr.page_section_id,
  pr.display_order,
  pr.row_type_id,
  prcta.headline,
  prcta.description,
  prcta.button_text,
  prcta.button_url,
  prcta.image_file_upload_id,
  fu.url AS image_url,
  pr.created,
  pr.last_updated
FROM
  page_row_call_to_action prcta
  JOIN page_row pr ON prcta.page_row_id = pr.page_row_id
  LEFT OUTER JOIN file_upload fu ON prcta.image_file_upload_id = fu.file_upload_id
WHERE
  pr.deleted_flag = FALSE;

COMMIT;
