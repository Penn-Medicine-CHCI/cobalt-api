BEGIN;
SELECT _v.register_patch('027-capitalization-updates', NULL, NULL);

UPDATE content_type SET call_to_action = 'Watch the Video' WHERE content_type_id='VIDEO';
UPDATE content_type SET call_to_action = 'Listen' WHERE content_type_id='AUDIO';
UPDATE content_type SET call_to_action = 'Listen' WHERE content_type_id='PODCAST';
UPDATE content_type SET call_to_action = 'Read the Article' WHERE content_type_id='ARTICLE';
UPDATE content_type SET call_to_action = 'Complete the Worksheet' WHERE content_type_id='WORKSHEET';
UPDATE content_type SET call_to_action = 'Read Blog Post' WHERE content_type_id='INT_BLOG';
UPDATE content_type SET call_to_action = 'Read Blog Post' WHERE content_type_id='EXT_BLOG';

COMMIT;