BEGIN;
SELECT _v.register_patch('244-page-duplicate-group-fix', NULL, NULL);

-- Standalone duplicates should be roots of new page groups.
-- Older duplicate flows could create rows with parent_page_id = NULL that inherited another page's group.
UPDATE page
SET page_group_id = page_id
WHERE parent_page_id IS NULL
  AND page_group_id <> page_id;

COMMIT;
