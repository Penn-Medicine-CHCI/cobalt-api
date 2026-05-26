BEGIN;
SELECT _v.register_patch('254-group-topic-archived', NULL, NULL);

ALTER TABLE group_topic ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX group_topic_institution_id_archived_display_order_idx
ON group_topic USING btree (institution_id, archived, display_order);

COMMIT;
