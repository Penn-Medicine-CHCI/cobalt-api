\set ON_ERROR_STOP on

\if :{?source_institution_id}
\else
  DO $$
  BEGIN
    RAISE EXCEPTION 'Pass -v source_institution_id=...';
  END
  $$;
\endif

SET search_path TO cobalt, public;

-- Create empty session-local work tables before entering the read-only
-- transaction. PostgreSQL permits inserting into temporary tables while the
-- transaction is read-only, but it does not permit CREATE TEMP TABLE AS then.
CREATE TEMPORARY TABLE page_rehearsal_export_settings (
  source_institution_id TEXT NOT NULL
) ON COMMIT PRESERVE ROWS;

CREATE TEMPORARY TABLE page_rehearsal_export_page (
  page_id UUID NOT NULL
) ON COMMIT PRESERVE ROWS;
CREATE UNIQUE INDEX ON page_rehearsal_export_page(page_id);

CREATE TEMPORARY TABLE page_rehearsal_export_section (
  page_section_id UUID NOT NULL,
  page_id UUID NOT NULL
) ON COMMIT PRESERVE ROWS;
CREATE UNIQUE INDEX ON page_rehearsal_export_section(page_section_id);

CREATE TEMPORARY TABLE page_rehearsal_export_row (
  page_row_id UUID NOT NULL,
  page_id UUID NOT NULL
) ON COMMIT PRESERVE ROWS;
CREATE UNIQUE INDEX ON page_rehearsal_export_row(page_row_id);

CREATE TEMPORARY TABLE page_rehearsal_export_file_upload (
  file_upload_id UUID NOT NULL PRIMARY KEY
) ON COMMIT PRESERVE ROWS;

CREATE TEMPORARY TABLE page_rehearsal_export_mailing_list (
  mailing_list_id UUID NOT NULL PRIMARY KEY
) ON COMMIT PRESERVE ROWS;

-- This is an editable identity-map template, not a replacement for source IDs.
-- The importer retains it unchanged and resolves configured/local substitutions
-- into a separate audited map.
CREATE TEMPORARY TABLE page_rehearsal_export_reference_map (
  reference_type TEXT NOT NULL,
  source_id TEXT NOT NULL,
  target_id TEXT NOT NULL,
  PRIMARY KEY (reference_type, source_id)
) ON COMMIT PRESERVE ROWS;

CREATE TEMPORARY TABLE page_rehearsal_export_manifest (
  table_name TEXT NOT NULL,
  row_count BIGINT NOT NULL
) ON COMMIT PRESERVE ROWS;

CREATE TEMPORARY TABLE page_rehearsal_export_column_anomaly (
  page_row_id UUID NOT NULL,
  column_count BIGINT NOT NULL,
  distinct_order_count BIGINT NOT NULL,
  minimum_order SMALLINT,
  maximum_order SMALLINT,
  over_four_columns BOOLEAN NOT NULL,
  order_requires_normalization BOOLEAN NOT NULL
) ON COMMIT PRESERVE ROWS;

CREATE TEMPORARY TABLE page_rehearsal_export_shape_metric (
  metric TEXT NOT NULL,
  value BIGINT NOT NULL
) ON COMMIT PRESERVE ROWS;

CREATE TEMPORARY TABLE page_rehearsal_export_sensitive_text_count (
  field_name TEXT NOT NULL,
  email_like BIGINT NOT NULL,
  phone_like BIGINT NOT NULL,
  link_like BIGINT NOT NULL
) ON COMMIT PRESERVE ROWS;

BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;

INSERT INTO page_rehearsal_export_settings (source_institution_id)
VALUES (:'source_institution_id');

INSERT INTO page_rehearsal_export_page (page_id)
SELECT p.page_id
FROM page p
JOIN page_rehearsal_export_settings s
  ON s.source_institution_id = p.institution_id;

INSERT INTO page_rehearsal_export_section (page_section_id, page_id)
SELECT ps.page_section_id, ps.page_id
FROM page_section ps
JOIN page_rehearsal_export_page p ON p.page_id = ps.page_id;

INSERT INTO page_rehearsal_export_row (page_row_id, page_id)
SELECT pr.page_row_id, ps.page_id
FROM page_row pr
JOIN page_rehearsal_export_section ps
  ON ps.page_section_id = pr.page_section_id;

INSERT INTO page_rehearsal_export_file_upload (file_upload_id)
SELECT p.image_file_upload_id
FROM page p
JOIN page_rehearsal_export_page selected USING (page_id)
WHERE p.image_file_upload_id IS NOT NULL
UNION
SELECT prc.image_file_upload_id
FROM page_row_column prc
JOIN page_rehearsal_export_row selected USING (page_row_id)
WHERE prc.image_file_upload_id IS NOT NULL;

INSERT INTO page_rehearsal_export_mailing_list (mailing_list_id)
SELECT DISTINCT association.mailing_list_id
FROM page_row_mailing_list association
JOIN page_rehearsal_export_row selected USING (page_row_id);

INSERT INTO page_rehearsal_export_reference_map (
  reference_type,
  source_id,
  target_id
)
SELECT reference_type, source_id, source_id
FROM (
  SELECT 'INSTITUTION'::TEXT AS reference_type, p.institution_id::TEXT AS source_id
  FROM page p
  JOIN page_rehearsal_export_page selected USING (page_id)
  UNION
  SELECT 'INSTITUTION', ml.institution_id::TEXT
  FROM mailing_list ml
  JOIN page_rehearsal_export_mailing_list selected USING (mailing_list_id)
  UNION
  SELECT 'ACCOUNT', p.created_by_account_id::TEXT
  FROM page p
  JOIN page_rehearsal_export_page selected USING (page_id)
  UNION
  SELECT 'ACCOUNT', psl.created_by_account_id::TEXT
  FROM page_site_location psl
  JOIN page_rehearsal_export_page selected USING (page_id)
  UNION
  SELECT 'ACCOUNT', ps.created_by_account_id::TEXT
  FROM page_section ps
  JOIN page_rehearsal_export_section selected USING (page_section_id)
  UNION
  SELECT 'ACCOUNT', pr.created_by_account_id::TEXT
  FROM page_row pr
  JOIN page_rehearsal_export_row selected USING (page_row_id)
  UNION
  SELECT 'ACCOUNT', fu.account_id::TEXT
  FROM file_upload fu
  JOIN page_rehearsal_export_file_upload selected USING (file_upload_id)
  UNION
  SELECT 'ACCOUNT', ml.created_by_account_id::TEXT
  FROM mailing_list ml
  JOIN page_rehearsal_export_mailing_list selected USING (mailing_list_id)
  UNION
  SELECT 'FILE_UPLOAD', selected.file_upload_id::TEXT
  FROM page_rehearsal_export_file_upload selected
  UNION
  SELECT 'CONTENT', association.content_id::TEXT
  FROM page_row_content association
  JOIN page_rehearsal_export_row selected USING (page_row_id)
  UNION
  SELECT 'GROUP_SESSION', association.group_session_id::TEXT
  FROM page_row_group_session association
  JOIN page_rehearsal_export_row selected USING (page_row_id)
  UNION
  SELECT 'TAG_GROUP', association.tag_group_id::TEXT
  FROM page_row_tag_group association
  JOIN page_rehearsal_export_row selected USING (page_row_id)
  UNION
  SELECT 'TAG', association.tag_id::TEXT
  FROM page_row_tag association
  JOIN page_rehearsal_export_row selected USING (page_row_id)
  UNION
  SELECT 'MAILING_LIST', selected.mailing_list_id::TEXT
  FROM page_rehearsal_export_mailing_list selected
) source_reference;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'cobalt'
      AND table_name = 'page_row'
      AND column_name IN ('name', 'padding_top_id', 'padding_bottom_id')
  ) OR EXISTS (
    SELECT 1 FROM _v.patches WHERE patch_name = '257-page-builder-v2'
  ) THEN
    RAISE EXCEPTION 'The export source must be a database before migration 257';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM page_rehearsal_export_page) THEN
    RAISE EXCEPTION 'No pages found for source institution %',
      (SELECT source_institution_id FROM page_rehearsal_export_settings);
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page child
    JOIN page_rehearsal_export_page selected ON selected.page_id = child.page_id
    LEFT JOIN page_rehearsal_export_page parent_scope
      ON parent_scope.page_id = child.parent_page_id
    WHERE child.parent_page_id IS NOT NULL
      AND parent_scope.page_id IS NULL
  ) THEN
    RAISE EXCEPTION 'A selected page has a parent outside the source institution';
  END IF;

  IF EXISTS (
    WITH selected_groups AS (
      SELECT DISTINCT p.page_group_id
      FROM page p
      JOIN page_rehearsal_export_page selected ON selected.page_id = p.page_id
    )
    SELECT 1
    FROM page p
    JOIN selected_groups sg ON sg.page_group_id = p.page_group_id
    GROUP BY p.page_group_id
    HAVING count(DISTINCT p.institution_id) <> 1
  ) THEN
    RAISE EXCEPTION 'A selected page group spans more than one institution';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page_section ps
    JOIN page_rehearsal_export_section selected
      ON selected.page_section_id = ps.page_section_id
    LEFT JOIN page_rehearsal_export_page p ON p.page_id = ps.page_id
    WHERE p.page_id IS NULL
  ) OR EXISTS (
    SELECT 1
    FROM page_row pr
    JOIN page_rehearsal_export_row selected
      ON selected.page_row_id = pr.page_row_id
    LEFT JOIN page_rehearsal_export_section ps
      ON ps.page_section_id = pr.page_section_id
    WHERE ps.page_section_id IS NULL
  ) THEN
    RAISE EXCEPTION 'The page hierarchy is not closed inside the selected export scope';
  END IF;
END
$$;

INSERT INTO page_rehearsal_export_manifest (table_name, row_count)
SELECT 'page_group'::TEXT AS table_name, count(*)::BIGINT AS row_count
FROM page_group pg
WHERE EXISTS (
  SELECT 1
  FROM page p
  JOIN page_rehearsal_export_page selected ON selected.page_id = p.page_id
  WHERE p.page_group_id = pg.page_group_id
)
UNION ALL
SELECT 'page', count(*)
FROM page p JOIN page_rehearsal_export_page selected USING (page_id)
UNION ALL
SELECT 'page_site_location', count(*)
FROM page_site_location x JOIN page_rehearsal_export_page selected USING (page_id)
UNION ALL
SELECT 'page_section', count(*)
FROM page_section x JOIN page_rehearsal_export_section selected USING (page_section_id)
UNION ALL
SELECT 'page_row', count(*)
FROM page_row x JOIN page_rehearsal_export_row selected USING (page_row_id)
UNION ALL
SELECT 'page_row_column', count(*)
FROM page_row_column x JOIN page_rehearsal_export_row selected USING (page_row_id)
UNION ALL
SELECT 'page_row_group_session', count(*)
FROM page_row_group_session x JOIN page_rehearsal_export_row selected USING (page_row_id)
UNION ALL
SELECT 'page_row_content', count(*)
FROM page_row_content x JOIN page_rehearsal_export_row selected USING (page_row_id)
UNION ALL
SELECT 'page_row_tag_group', count(*)
FROM page_row_tag_group x JOIN page_rehearsal_export_row selected USING (page_row_id)
UNION ALL
SELECT 'page_row_tag', count(*)
FROM page_row_tag x JOIN page_rehearsal_export_row selected USING (page_row_id)
UNION ALL
SELECT 'page_row_mailing_list', count(*)
FROM page_row_mailing_list x JOIN page_rehearsal_export_row selected USING (page_row_id)
UNION ALL
SELECT 'file_upload', count(*)
FROM page_rehearsal_export_file_upload
UNION ALL
SELECT 'mailing_list', count(*)
FROM page_rehearsal_export_mailing_list
UNION ALL
SELECT 'reference_map', count(*)
FROM page_rehearsal_export_reference_map;

INSERT INTO page_rehearsal_export_column_anomaly (
  page_row_id,
  column_count,
  distinct_order_count,
  minimum_order,
  maximum_order,
  over_four_columns,
  order_requires_normalization
)
SELECT
  pr.page_row_id,
  count(prc.page_row_column_id)::BIGINT AS column_count,
  count(DISTINCT prc.column_display_order)::BIGINT AS distinct_order_count,
  min(prc.column_display_order) AS minimum_order,
  max(prc.column_display_order) AS maximum_order,
  count(prc.page_row_column_id) > 4 AS over_four_columns,
  count(prc.page_row_column_id) > 0
    AND (
      count(prc.page_row_column_id) <> count(DISTINCT prc.column_display_order)
      OR min(prc.column_display_order) <> 0
      OR max(prc.column_display_order) <> count(prc.page_row_column_id) - 1
    ) AS order_requires_normalization
FROM page_row pr
JOIN page_rehearsal_export_row selected ON selected.page_row_id = pr.page_row_id
LEFT JOIN page_row_column prc ON prc.page_row_id = pr.page_row_id
GROUP BY pr.page_row_id
HAVING
  count(prc.page_row_column_id) > 4
  OR (
    count(prc.page_row_column_id) > 0
    AND (
      count(prc.page_row_column_id) <> count(DISTINCT prc.column_display_order)
      OR min(prc.column_display_order) <> 0
      OR max(prc.column_display_order) <> count(prc.page_row_column_id) - 1
    )
  );

INSERT INTO page_rehearsal_export_shape_metric (metric, value)
SELECT 'pages'::TEXT AS metric, count(*)::BIGINT AS value
FROM page_rehearsal_export_page
UNION ALL
SELECT 'page_groups', count(DISTINCT p.page_group_id)
FROM page p JOIN page_rehearsal_export_page selected USING (page_id)
UNION ALL
SELECT 'deleted_pages', count(*)
FROM page p JOIN page_rehearsal_export_page selected USING (page_id)
WHERE p.deleted_flag
UNION ALL
SELECT 'active_pages_without_active_sections', count(*)
FROM page p
JOIN page_rehearsal_export_page selected USING (page_id)
WHERE p.deleted_flag = FALSE
  AND NOT EXISTS (
    SELECT 1 FROM page_section ps
    WHERE ps.page_id = p.page_id AND ps.deleted_flag = FALSE
  )
UNION ALL
SELECT 'pages_with_multiple_active_sections', count(*)
FROM (
  SELECT p.page_id
  FROM page p
  JOIN page_rehearsal_export_page selected USING (page_id)
  JOIN page_section ps ON ps.page_id = p.page_id AND ps.deleted_flag = FALSE
  GROUP BY p.page_id
  HAVING count(*) > 1
) pages
UNION ALL
SELECT 'sections', count(*)
FROM page_rehearsal_export_section
UNION ALL
SELECT 'deleted_sections', count(*)
FROM page_section ps
JOIN page_rehearsal_export_section selected USING (page_section_id)
WHERE ps.deleted_flag
UNION ALL
SELECT 'rows', count(*)
FROM page_rehearsal_export_row
UNION ALL
SELECT 'deleted_rows', count(*)
FROM page_row pr
JOIN page_rehearsal_export_row selected USING (page_row_id)
WHERE pr.deleted_flag
UNION ALL
SELECT 'rows_in_deleted_sections', count(*)
FROM page_row pr
JOIN page_rehearsal_export_row selected USING (page_row_id)
JOIN page_section ps USING (page_section_id)
WHERE ps.deleted_flag
UNION ALL
SELECT 'active_rows_hidden_in_deleted_sections', count(*)
FROM page_row pr
JOIN page_rehearsal_export_row selected USING (page_row_id)
JOIN page_section ps USING (page_section_id)
JOIN page p ON p.page_id = ps.page_id
WHERE pr.deleted_flag = FALSE
  AND ps.deleted_flag = TRUE
  AND (
    p.deleted_flag = FALSE
    OR EXISTS (
      SELECT 1
      FROM page_section active_section
      WHERE active_section.page_id = p.page_id
        AND active_section.deleted_flag = FALSE
    )
  )
UNION ALL
SELECT 'columns', count(*)
FROM page_row_column prc
JOIN page_rehearsal_export_row selected USING (page_row_id)
UNION ALL
SELECT 'rows_over_four_columns', count(*)
FROM page_rehearsal_export_column_anomaly WHERE over_four_columns
UNION ALL
SELECT 'rows_with_order_anomalies', count(*)
FROM page_rehearsal_export_column_anomaly WHERE order_requires_normalization
UNION ALL
SELECT 'expected_new_text_rows', count(*)
FROM page_section ps
JOIN page_rehearsal_export_section selected USING (page_section_id)
WHERE ps.deleted_flag = FALSE
  AND COALESCE(NULLIF(BTRIM(ps.headline), ''), NULLIF(BTRIM(ps.description), '')) IS NOT NULL;

INSERT INTO page_rehearsal_export_sensitive_text_count (
  field_name,
  email_like,
  phone_like,
  link_like
)
WITH text_value(field_name, value) AS (
  SELECT 'page_group.analytics_campaign_key', pg.analytics_campaign_key
  FROM page_group pg
  WHERE EXISTS (
    SELECT 1
    FROM page p
    JOIN page_rehearsal_export_page selected USING (page_id)
    WHERE p.page_group_id = pg.page_group_id
  )
  UNION ALL
  SELECT 'page.name', p.name
  FROM page p JOIN page_rehearsal_export_page s USING (page_id)
  UNION ALL SELECT 'page.url_name', p.url_name
  FROM page p JOIN page_rehearsal_export_page s USING (page_id)
  UNION ALL SELECT 'page.headline', p.headline
  FROM page p JOIN page_rehearsal_export_page s USING (page_id)
  UNION ALL SELECT 'page.description', p.description
  FROM page p JOIN page_rehearsal_export_page s USING (page_id)
  UNION ALL SELECT 'page.image_alt_text', p.image_alt_text
  FROM page p JOIN page_rehearsal_export_page s USING (page_id)
  UNION ALL SELECT 'page_site_location.call_to_action', psl.call_to_action
  FROM page_site_location psl JOIN page_rehearsal_export_page s USING (page_id)
  UNION ALL SELECT 'page_site_location.short_description', psl.short_description
  FROM page_site_location psl JOIN page_rehearsal_export_page s USING (page_id)
  UNION ALL SELECT 'page_section.name', ps.name
  FROM page_section ps JOIN page_rehearsal_export_page s USING (page_id)
  UNION ALL SELECT 'page_section.headline', ps.headline
  FROM page_section ps JOIN page_rehearsal_export_page s USING (page_id)
  UNION ALL SELECT 'page_section.description', ps.description
  FROM page_section ps JOIN page_rehearsal_export_page s USING (page_id)
  UNION ALL SELECT 'page_row_column.headline', prc.headline
  FROM page_row_column prc JOIN page_rehearsal_export_row s USING (page_row_id)
  UNION ALL SELECT 'page_row_column.description', prc.description
  FROM page_row_column prc JOIN page_rehearsal_export_row s USING (page_row_id)
  UNION ALL SELECT 'page_row_column.image_alt_text', prc.image_alt_text
  FROM page_row_column prc JOIN page_rehearsal_export_row s USING (page_row_id)
  UNION ALL SELECT 'page_row_mailing_list.title', prml.title
  FROM page_row_mailing_list prml JOIN page_rehearsal_export_row s USING (page_row_id)
  UNION ALL SELECT 'page_row_mailing_list.description', prml.description
  FROM page_row_mailing_list prml JOIN page_rehearsal_export_row s USING (page_row_id)
  UNION ALL SELECT 'file_upload.url', fu.url
  FROM file_upload fu JOIN page_rehearsal_export_file_upload s USING (file_upload_id)
  UNION ALL SELECT 'file_upload.storage_key', fu.storage_key
  FROM file_upload fu JOIN page_rehearsal_export_file_upload s USING (file_upload_id)
)
SELECT
  field_name,
  count(*) FILTER (
    WHERE value ~* '[[:alnum:]._%+_-]+@[[:alnum:].-]+[.][[:alpha:]]{2,}'
  )::BIGINT AS email_like,
  count(*) FILTER (
    WHERE value ~* '[+]?[0-9][0-9() .-]{7,}[0-9]'
  )::BIGINT AS phone_like,
  count(*) FILTER (
    WHERE value ~* '(mailto:|tel:|https?://)'
  )::BIGINT AS link_like
FROM text_value
WHERE value IS NOT NULL
GROUP BY field_name
HAVING
  count(*) FILTER (
    WHERE value ~* '[[:alnum:]._%+_-]+@[[:alnum:].-]+[.][[:alpha:]]{2,}'
  ) > 0
  OR count(*) FILTER (
    WHERE value ~* '[+]?[0-9][0-9() .-]{7,}[0-9]'
  ) > 0
  OR count(*) FILTER (
    WHERE value ~* '(mailto:|tel:|https?://)'
  ) > 0;

\echo 'Writing faithful page-builder source CSVs to the current directory...'

\copy (SELECT pg.page_group_id, pg.analytics_campaign_key FROM page_group pg WHERE EXISTS (SELECT 1 FROM page p JOIN page_rehearsal_export_page selected ON selected.page_id = p.page_id WHERE p.page_group_id = pg.page_group_id) ORDER BY pg.page_group_id) TO '01-page-group.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT p.page_id, p.name, p.url_name, p.page_status_id, p.headline, p.description, p.image_file_upload_id, p.image_alt_text, p.published_date, p.deleted_flag, p.institution_id, p.parent_page_id, p.created_by_account_id, p.created, p.last_updated, p.page_group_id FROM page p JOIN page_rehearsal_export_page selected USING (page_id) ORDER BY p.created, p.page_id) TO '02-page.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT psl.page_site_location_id, psl.page_id, psl.site_location_id, psl.display_order, psl.publish_start_date, psl.publish_end_date, psl.call_to_action, psl.created_by_account_id, psl.created, psl.last_updated, psl.short_description, psl.icon_name FROM page_site_location psl JOIN page_rehearsal_export_page selected USING (page_id) ORDER BY psl.page_id, psl.display_order, psl.page_site_location_id) TO '03-page-site-location.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT ps.page_section_id, ps.page_id, ps.name, ps.headline, ps.description, ps.background_color_id, ps.deleted_flag, ps.display_order, ps.created_by_account_id, ps.created, ps.last_updated FROM page_section ps JOIN page_rehearsal_export_section selected USING (page_section_id) ORDER BY ps.page_id, ps.display_order, ps.page_section_id) TO '04-page-section.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT pr.page_row_id, pr.page_section_id, pr.row_type_id, pr.deleted_flag, pr.display_order, pr.created_by_account_id, pr.created, pr.last_updated FROM page_row pr JOIN page_rehearsal_export_row selected USING (page_row_id) ORDER BY pr.page_section_id, pr.display_order, pr.page_row_id) TO '05-page-row.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT prc.page_row_column_id, prc.page_row_id, prc.headline, prc.description, prc.image_file_upload_id, prc.image_alt_text, prc.column_display_order, prc.created, prc.last_updated FROM page_row_column prc JOIN page_rehearsal_export_row selected USING (page_row_id) ORDER BY prc.page_row_id, prc.column_display_order, prc.page_row_column_id) TO '06-page-row-column.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT x.page_row_group_session_id, x.page_row_id, x.group_session_id, x.group_session_display_order, x.created, x.last_updated FROM page_row_group_session x JOIN page_rehearsal_export_row selected USING (page_row_id) ORDER BY x.page_row_id, x.group_session_display_order, x.group_session_id, x.page_row_group_session_id) TO '07-page-row-group-session.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT x.page_row_content_id, x.page_row_id, x.content_id, x.content_display_order, x.created, x.last_updated FROM page_row_content x JOIN page_rehearsal_export_row selected USING (page_row_id) ORDER BY x.page_row_id, x.content_display_order, x.content_id, x.page_row_content_id) TO '08-page-row-content.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT x.page_row_tag_group_id, x.page_row_id, x.tag_group_id, x.created, x.last_updated FROM page_row_tag_group x JOIN page_rehearsal_export_row selected USING (page_row_id) ORDER BY x.page_row_id, x.tag_group_id, x.page_row_tag_group_id) TO '09-page-row-tag-group.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT x.page_row_tag_id, x.page_row_id, x.tag_id, x.created, x.last_updated FROM page_row_tag x JOIN page_rehearsal_export_row selected USING (page_row_id) ORDER BY x.page_row_id, x.tag_id, x.page_row_tag_id) TO '10-page-row-tag.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT x.page_row_mailing_list_id, x.page_row_id, x.mailing_list_id, x.title, x.description, x.created, x.last_updated FROM page_row_mailing_list x JOIN page_rehearsal_export_row selected USING (page_row_id) ORDER BY x.page_row_id, x.mailing_list_id, x.page_row_mailing_list_id) TO '11-page-row-mailing-list.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT fu.file_upload_id, fu.account_id, fu.url, fu.storage_key, fu.filename, fu.content_type, fu.created, fu.last_updated, fu.file_upload_type_id, fu.filesize, fu.remote_data_flag FROM file_upload fu JOIN page_rehearsal_export_file_upload selected USING (file_upload_id) ORDER BY fu.file_upload_id) TO '12-file-upload.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT ml.mailing_list_id, ml.institution_id, ml.created_by_account_id, ml.created, ml.last_updated FROM mailing_list ml JOIN page_rehearsal_export_mailing_list selected USING (mailing_list_id) ORDER BY ml.mailing_list_id) TO '13-mailing-list.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT reference_type, source_id, target_id FROM page_rehearsal_export_reference_map ORDER BY reference_type, source_id) TO '70-reference-map.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT table_name, row_count FROM page_rehearsal_export_manifest ORDER BY table_name) TO '80-manifest.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT metric, value FROM page_rehearsal_export_shape_metric ORDER BY metric) TO '81-shape-metrics.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT page_row_id, column_count, distinct_order_count, minimum_order, maximum_order, over_four_columns, order_requires_normalization FROM page_rehearsal_export_column_anomaly ORDER BY page_row_id) TO '82-column-order-anomalies.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT field_name, email_like, phone_like, link_like FROM page_rehearsal_export_sensitive_text_count ORDER BY field_name) TO '90-sensitive-text-counts.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT transaction_timestamp() AS exported_at, s.source_institution_id, 'pre-257'::TEXT AS expected_schema_state FROM page_rehearsal_export_settings s) TO '00-export-info.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\copy (SELECT transaction_timestamp() AS completed_at, 20::INTEGER AS expected_file_count) TO '99-export-complete.csv' WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)

\echo ''
\echo 'Exported row counts:'
TABLE page_rehearsal_export_manifest ORDER BY table_name;

\echo ''
\echo 'Sensitive-text triage counts:'
TABLE page_rehearsal_export_sensitive_text_count ORDER BY field_name;

\echo ''
\echo 'Export complete. Keep these files private and never commit them.'

ROLLBACK;
