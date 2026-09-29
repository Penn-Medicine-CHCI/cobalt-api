BEGIN;
SELECT _v.register_patch(
  '273-provider-search-display-order',
  ARRAY['272-screening-destination-content'],
  NULL
);

-- Zero retains the existing alphabetical provider search order. Higher values
-- appear after lower values, with names used within each priority group.
ALTER TABLE provider ADD COLUMN search_display_order INTEGER NOT NULL DEFAULT 0;

COMMIT;
