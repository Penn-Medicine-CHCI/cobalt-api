BEGIN;
SELECT _v.register_patch('119-counseling-feature', NULL, NULL);

INSERT INTO feature
  (feature_id, name, url_name, navigation_header_id)
VALUES
  ('COUNSELING_SERVICES', 'Counseling Services', '/counseling-services', 'CONNECT_WITH_SUPPORT');

COMMIT;