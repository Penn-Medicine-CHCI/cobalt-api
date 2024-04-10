BEGIN;
SELECT _v.register_patch('164-study-reporting', NULL, NULL);

DROP VIEW v_study_file_upload;

CREATE VIEW v_study_file_upload AS
SELECT
  s.institution_id,
  ast.study_id,
  ast.account_id,
  a.username AS account_username,
  s.name AS study_name,
  s.url_name AS study_url_name,
  ast.account_study_id,
  sfu.file_upload_id,
  fu.file_upload_type_id,
  fu.url AS file_upload_url,
  fu.storage_key AS file_upload_storage_key,
  fu.filename AS file_upload_filename,
  fu.content_type AS file_upload_content_type,
  fu.filesize AS file_upload_filesize,
  fu.created AS file_upload_created
FROM
  account_study ast,
  study s,
  account a,
  study_file_upload sfu,
  file_upload fu
WHERE
  ast.study_id=s.study_id
  AND ast.account_id=a.account_id
  AND sfu.study_id=s.study_id
  AND sfu.file_upload_id=fu.file_upload_id
  AND fu.account_id=ast.account_id;

COMMIT;