BEGIN;
SELECT _v.register_patch('037-app-content-type', NULL, NULL);

INSERT INTO content_type (content_type_id, description, call_to_action)
VALUES ('APP', 'App', 'Get the App');

INSERT INTO content_type_label (content_type_label_id, description) VALUES ('APP', 'App');

COMMIT;