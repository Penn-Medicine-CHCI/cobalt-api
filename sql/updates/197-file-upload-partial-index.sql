BEGIN;
SELECT _v.register_patch('197-file-upload-partial-index', NULL, NULL);

--Running this maually in all envs
--CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_file_upload_file_upload_type_id_partial ON file_upload(file_upload_type_id)
--WHERE file_upload_type_id IN ('GROUP_SESSION_IMAGE','CONTENT_IMAGE', 'CONTENT');

CREATE FOREIGN TABLE IF NOT EXISTS remote_content_audience (
	content_id UUID NOT NULL,
	content_audience_type_id TEXT,
	created_by_account_id UUID,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW())
SERVER cobalt_remote
OPTIONS (schema_name 'cobalt', table_name 'content_audience');

ALTER TABLE content_audience ADD COLUMN remote_data_flag BOOLEAN NOT NULL DEFAULT false;

CREATE OR REPLACE VIEW v_remote_content_audience
AS
SELECT rca.* 
FROM remote_content_audience rca, v_remote_content vrc
WHERE rca.content_id = vrc.content_id;

COMMIT;
