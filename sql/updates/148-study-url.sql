BEGIN;
SELECT _v.register_patch('148-study-url', NULL, NULL);

DROP INDEX study_unique_url_name_idx;

CREATE UNIQUE INDEX study_unique_url_name_idx ON study USING btree (url_name);

COMMIT;