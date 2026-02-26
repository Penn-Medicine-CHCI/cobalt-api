BEGIN;
SELECT _v.register_patch('244-page-duplicate-group-fix', NULL, NULL);

-- Standalone duplicates should be roots of new page groups.
-- Older duplicate flows could create rows with parent_page_id = NULL that inherited another page's group.
CREATE TABLE page_duplicate_group_fix_244_backup (
	page_id UUID PRIMARY KEY,
	previous_page_group_id UUID NOT NULL,
	new_page_group_id UUID NOT NULL,
	parent_page_id UUID,
	institution_id TEXT NOT NULL,
	name TEXT NOT NULL,
	url_name TEXT NOT NULL,
	page_status_id TEXT NOT NULL,
	backed_up_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO page_duplicate_group_fix_244_backup (
	page_id,
	previous_page_group_id,
	new_page_group_id,
	parent_page_id,
	institution_id,
	name,
	url_name,
	page_status_id
)
SELECT
	p.page_id,
	p.page_group_id AS previous_page_group_id,
	p.page_id AS new_page_group_id,
	p.parent_page_id,
	p.institution_id,
	p.name,
	p.url_name,
	p.page_status_id
FROM page p
WHERE p.parent_page_id IS NULL
	AND p.page_group_id <> p.page_id;

UPDATE page p
SET page_group_id = b.new_page_group_id
FROM page_duplicate_group_fix_244_backup b
WHERE p.page_id = b.page_id;

-- Rollback helper:
-- UPDATE page p
-- SET page_group_id = b.previous_page_group_id
-- FROM page_duplicate_group_fix_244_backup b
-- WHERE p.page_id = b.page_id;

COMMIT;
