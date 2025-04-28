BEGIN;
SELECT _v.register_patch('210-page-migration', NULL, NULL);

-- If TRUE, drive nav items from Topic Centers instead of Pages.
ALTER TABLE institution ADD COLUMN prefer_legacy_topic_centers BOOLEAN NOT NULL DEFAULT FALSE;

-- Alternative "short" description, usable when displaying a Featured page in the nav (for example)
ALTER TABLE page_site_location ADD COLUMN short_description TEXT;

COMMIT;