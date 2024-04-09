BEGIN;
SELECT _v.register_patch('162-study-reporting', NULL, NULL);

CREATE OR REPLACE VIEW v_account_check_in_action_file_upload AS
SELECT
  s.institution_id,
  sci.study_check_in_id,
  sci.check_in_number AS study_check_in_number,
  aci.account_check_in_id,
  aci.account_study_id,
  aci.check_in_start_date_time AS account_check_in_start_date_time,
  aci.check_in_end_date_time AS account_check_in_end_date_time,
  aci.check_in_status_id AS account_check_in_status_id,
  aci.completed_date AS account_check_in_completed_date,
  ast.study_id,
  ast.account_id,
  a.username AS account_username,
  s.name AS study_name,
  s.url_name AS study_url_name,
  acia.account_check_in_action_id,
  acia.study_check_in_action_id,
  acia.check_in_action_status_id AS account_check_in_action_status_id,
  acia.created AS account_check_in_action_created,
  acia.last_updated AS account_check_in_action_last_updated,
  aciafu.file_upload_id,
  fu.file_upload_type_id,
  fu.url AS file_upload_url,
  fu.storage_key AS file_upload_storage_key,
  fu.filename AS file_upload_filename,
  fu.content_type AS file_upload_content_type,
  fu.filesize AS file_upload_filesize,
  fu.created AS file_upload_created
FROM
  study_check_in sci,
  account_check_in aci,
  account_study ast,
  study s,
  account a,
  account_check_in_action acia,
  account_check_in_action_file_upload aciafu,
  file_upload fu
WHERE
  sci.study_check_in_id=aci.study_check_in_id
  AND aci.account_study_id=ast.account_study_id
  AND ast.study_id=s.study_id
  AND ast.account_id=a.account_id
  AND aci.account_check_in_id=acia.account_check_in_id
  AND acia.account_check_in_action_id=aciafu.account_check_in_action_id
  AND aciafu.file_upload_id=fu.file_upload_id;

CREATE OR REPLACE VIEW v_study_file_upload AS
SELECT
  s.institution_id,
  ast.study_id,
  ast.account_id,
  a.username AS account_username,
  s.name AS study_name,
  s.url_name AS study_url_name,
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