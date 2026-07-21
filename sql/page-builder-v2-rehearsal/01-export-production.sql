\set ON_ERROR_STOP on

\if :{?source_institution_id}
\else
  \warn 'Pass -v source_institution_id=...'
  SELECT 1 / 0;
\endif

-- A physical hot standby rejects all DDL and DML, including operations on
-- temporary tables. Keep every server-side operation in this script limited to
-- transaction control, SELECT, and COPY TO STDOUT. The pg_is_in_recovery
-- guard prevents accidentally pointing this production-data exporter at the
-- writable primary.
SELECT pg_is_in_recovery() AS page_rehearsal_is_replica
\gset
\if :page_rehearsal_is_replica
\else
  \warn 'The export source must be a physical read replica (pg_is_in_recovery() must be true)'
  SELECT 1 / 0;
\endif

BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;

SELECT NOT (
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'cobalt'
      AND table_name = 'page_row'
      AND column_name IN ('name', 'padding_top_id', 'padding_bottom_id')
  ) OR EXISTS (
    SELECT 1 FROM _v.patches WHERE patch_name = '257-page-builder-v2'
  )
) AS page_rehearsal_source_is_pre257
\gset
\if :page_rehearsal_source_is_pre257
\else
  \warn 'The export source must be a database before migration 257'
  SELECT 1 / 0;
\endif

SELECT EXISTS (
  SELECT 1 FROM cobalt.page WHERE institution_id = :'source_institution_id'
) AS page_rehearsal_has_pages
\gset
\if :page_rehearsal_has_pages
\else
  \warn 'No pages found for the requested source institution'
  SELECT 1 / 0;
\endif

SELECT NOT EXISTS (
  SELECT 1
  FROM cobalt.page child
  LEFT JOIN cobalt.page parent_scope
    ON parent_scope.page_id = child.parent_page_id
   AND parent_scope.institution_id = :'source_institution_id'
  WHERE child.institution_id = :'source_institution_id'
    AND child.parent_page_id IS NOT NULL
    AND parent_scope.page_id IS NULL
) AS page_rehearsal_parents_are_scoped
\gset
\if :page_rehearsal_parents_are_scoped
\else
  \warn 'A selected page has a parent outside the source institution'
  SELECT 1 / 0;
\endif

SELECT NOT EXISTS (
  SELECT 1
  FROM cobalt.page p
  WHERE p.page_group_id IN (
    SELECT selected.page_group_id
    FROM cobalt.page selected
    WHERE selected.institution_id = :'source_institution_id'
  )
  GROUP BY p.page_group_id
  HAVING count(DISTINCT p.institution_id) <> 1
) AS page_rehearsal_groups_are_scoped
\gset
\if :page_rehearsal_groups_are_scoped
\else
  \warn 'A selected page group spans more than one institution'
  SELECT 1 / 0;
\endif

\echo 'Writing faithful page-builder source CSVs to the current directory...'

COPY (
  SELECT pg.page_group_id, pg.analytics_campaign_key
  FROM cobalt.page_group pg
  WHERE EXISTS (
    SELECT 1
    FROM cobalt.page p
    WHERE p.institution_id = :'source_institution_id'
      AND p.page_group_id = pg.page_group_id
  )
  ORDER BY pg.page_group_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 01-page-group.csv

COPY (
  SELECT
    p.page_id,
    p.name,
    p.url_name,
    p.page_status_id,
    p.headline,
    p.description,
    p.image_file_upload_id,
    p.image_alt_text,
    p.published_date,
    p.deleted_flag,
    p.institution_id,
    p.parent_page_id,
    p.created_by_account_id,
    p.created,
    p.last_updated,
    p.page_group_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
  ORDER BY p.created, p.page_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 02-page.csv

COPY (
  SELECT
    psl.page_site_location_id,
    psl.page_id,
    psl.site_location_id,
    psl.display_order,
    psl.publish_start_date,
    psl.publish_end_date,
    psl.call_to_action,
    psl.created_by_account_id,
    psl.created,
    psl.last_updated,
    psl.short_description,
    psl.icon_name
  FROM cobalt.page_site_location psl
  JOIN cobalt.page p USING (page_id)
  WHERE p.institution_id = :'source_institution_id'
  ORDER BY psl.page_id, psl.display_order, psl.page_site_location_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 03-page-site-location.csv

COPY (
  SELECT
    ps.page_section_id,
    ps.page_id,
    ps.name,
    ps.headline,
    ps.description,
    ps.background_color_id,
    ps.deleted_flag,
    ps.display_order,
    ps.created_by_account_id,
    ps.created,
    ps.last_updated
  FROM cobalt.page_section ps
  JOIN cobalt.page p USING (page_id)
  WHERE p.institution_id = :'source_institution_id'
  ORDER BY ps.page_id, ps.display_order, ps.page_section_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 04-page-section.csv

COPY (
  SELECT
    pr.page_row_id,
    pr.page_section_id,
    pr.row_type_id,
    pr.deleted_flag,
    pr.display_order,
    pr.created_by_account_id,
    pr.created,
    pr.last_updated
  FROM cobalt.page_row pr
  JOIN cobalt.page_section ps USING (page_section_id)
  JOIN cobalt.page p USING (page_id)
  WHERE p.institution_id = :'source_institution_id'
  ORDER BY pr.page_section_id, pr.display_order, pr.page_row_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 05-page-row.csv

COPY (
  SELECT
    prc.page_row_column_id,
    prc.page_row_id,
    prc.headline,
    prc.description,
    prc.image_file_upload_id,
    prc.image_alt_text,
    prc.column_display_order,
    prc.created,
    prc.last_updated
  FROM cobalt.page_row_column prc
  JOIN cobalt.page_row pr USING (page_row_id)
  JOIN cobalt.page_section ps USING (page_section_id)
  JOIN cobalt.page p USING (page_id)
  WHERE p.institution_id = :'source_institution_id'
  ORDER BY prc.page_row_id, prc.column_display_order, prc.page_row_column_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 06-page-row-column.csv

COPY (
  SELECT
    x.page_row_group_session_id,
    x.page_row_id,
    x.group_session_id,
    x.group_session_display_order,
    x.created,
    x.last_updated
  FROM cobalt.page_row_group_session x
  JOIN cobalt.page_row pr USING (page_row_id)
  JOIN cobalt.page_section ps USING (page_section_id)
  JOIN cobalt.page p USING (page_id)
  WHERE p.institution_id = :'source_institution_id'
  ORDER BY
    x.page_row_id,
    x.group_session_display_order,
    x.group_session_id,
    x.page_row_group_session_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 07-page-row-group-session.csv

COPY (
  SELECT
    x.page_row_content_id,
    x.page_row_id,
    x.content_id,
    x.content_display_order,
    x.created,
    x.last_updated
  FROM cobalt.page_row_content x
  JOIN cobalt.page_row pr USING (page_row_id)
  JOIN cobalt.page_section ps USING (page_section_id)
  JOIN cobalt.page p USING (page_id)
  WHERE p.institution_id = :'source_institution_id'
  ORDER BY
    x.page_row_id,
    x.content_display_order,
    x.content_id,
    x.page_row_content_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 08-page-row-content.csv

COPY (
  SELECT
    x.page_row_tag_group_id,
    x.page_row_id,
    x.tag_group_id,
    x.created,
    x.last_updated
  FROM cobalt.page_row_tag_group x
  JOIN cobalt.page_row pr USING (page_row_id)
  JOIN cobalt.page_section ps USING (page_section_id)
  JOIN cobalt.page p USING (page_id)
  WHERE p.institution_id = :'source_institution_id'
  ORDER BY x.page_row_id, x.tag_group_id, x.page_row_tag_group_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 09-page-row-tag-group.csv

COPY (
  SELECT
    x.page_row_tag_id,
    x.page_row_id,
    x.tag_id,
    x.created,
    x.last_updated
  FROM cobalt.page_row_tag x
  JOIN cobalt.page_row pr USING (page_row_id)
  JOIN cobalt.page_section ps USING (page_section_id)
  JOIN cobalt.page p USING (page_id)
  WHERE p.institution_id = :'source_institution_id'
  ORDER BY x.page_row_id, x.tag_id, x.page_row_tag_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 10-page-row-tag.csv

COPY (
  SELECT
    x.page_row_mailing_list_id,
    x.page_row_id,
    x.mailing_list_id,
    x.title,
    x.description,
    x.created,
    x.last_updated
  FROM cobalt.page_row_mailing_list x
  JOIN cobalt.page_row pr USING (page_row_id)
  JOIN cobalt.page_section ps USING (page_section_id)
  JOIN cobalt.page p USING (page_id)
  WHERE p.institution_id = :'source_institution_id'
  ORDER BY x.page_row_id, x.mailing_list_id, x.page_row_mailing_list_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 11-page-row-mailing-list.csv

COPY (
  WITH selected_file_upload AS (
    SELECT p.image_file_upload_id AS file_upload_id
    FROM cobalt.page p
    WHERE p.institution_id = :'source_institution_id'
      AND p.image_file_upload_id IS NOT NULL
    UNION
    SELECT prc.image_file_upload_id
    FROM cobalt.page_row_column prc
    JOIN cobalt.page_row pr USING (page_row_id)
    JOIN cobalt.page_section ps USING (page_section_id)
    JOIN cobalt.page p USING (page_id)
    WHERE p.institution_id = :'source_institution_id'
      AND prc.image_file_upload_id IS NOT NULL
  )
  SELECT
    fu.file_upload_id,
    fu.account_id,
    fu.url,
    fu.storage_key,
    fu.filename,
    fu.content_type,
    fu.created,
    fu.last_updated,
    fu.file_upload_type_id,
    fu.filesize,
    fu.remote_data_flag
  FROM cobalt.file_upload fu
  JOIN selected_file_upload selected USING (file_upload_id)
  ORDER BY fu.file_upload_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 12-file-upload.csv

COPY (
  SELECT
    ml.mailing_list_id,
    ml.institution_id,
    ml.created_by_account_id,
    ml.created,
    ml.last_updated
  FROM cobalt.mailing_list ml
  WHERE EXISTS (
    SELECT 1
    FROM cobalt.page_row_mailing_list association
    JOIN cobalt.page_row pr USING (page_row_id)
    JOIN cobalt.page_section ps USING (page_section_id)
    JOIN cobalt.page p USING (page_id)
    WHERE p.institution_id = :'source_institution_id'
      AND association.mailing_list_id = ml.mailing_list_id
  )
  ORDER BY ml.mailing_list_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 13-mailing-list.csv
COPY (
  WITH
  selected_page AS (
    SELECT p.page_id
    FROM cobalt.page p
    WHERE p.institution_id = :'source_institution_id'
  ),
  selected_section AS (
    SELECT ps.page_section_id, ps.page_id
    FROM cobalt.page_section ps
    JOIN selected_page p USING (page_id)
  ),
  selected_row AS (
    SELECT pr.page_row_id
    FROM cobalt.page_row pr
    JOIN selected_section ps USING (page_section_id)
  ),
  selected_file_upload AS (
    SELECT p.image_file_upload_id AS file_upload_id
    FROM cobalt.page p
    JOIN selected_page selected USING (page_id)
    WHERE p.image_file_upload_id IS NOT NULL
    UNION
    SELECT prc.image_file_upload_id
    FROM cobalt.page_row_column prc
    JOIN selected_row selected USING (page_row_id)
    WHERE prc.image_file_upload_id IS NOT NULL
  ),
  selected_mailing_list AS (
    SELECT DISTINCT association.mailing_list_id
    FROM cobalt.page_row_mailing_list association
    JOIN selected_row selected USING (page_row_id)
  ),
  source_reference AS (
    SELECT 'INSTITUTION'::TEXT AS reference_type, p.institution_id::TEXT AS source_id
    FROM cobalt.page p
    JOIN selected_page selected USING (page_id)
    UNION
    SELECT 'INSTITUTION', ml.institution_id::TEXT
    FROM cobalt.mailing_list ml
    JOIN selected_mailing_list selected USING (mailing_list_id)
    UNION
    SELECT 'ACCOUNT', p.created_by_account_id::TEXT
    FROM cobalt.page p
    JOIN selected_page selected USING (page_id)
    UNION
    SELECT 'ACCOUNT', psl.created_by_account_id::TEXT
    FROM cobalt.page_site_location psl
    JOIN selected_page selected USING (page_id)
    UNION
    SELECT 'ACCOUNT', ps.created_by_account_id::TEXT
    FROM cobalt.page_section ps
    JOIN selected_section selected USING (page_section_id)
    UNION
    SELECT 'ACCOUNT', pr.created_by_account_id::TEXT
    FROM cobalt.page_row pr
    JOIN selected_row selected USING (page_row_id)
    UNION
    SELECT 'ACCOUNT', fu.account_id::TEXT
    FROM cobalt.file_upload fu
    JOIN selected_file_upload selected USING (file_upload_id)
    UNION
    SELECT 'ACCOUNT', ml.created_by_account_id::TEXT
    FROM cobalt.mailing_list ml
    JOIN selected_mailing_list selected USING (mailing_list_id)
    UNION
    SELECT 'FILE_UPLOAD', selected.file_upload_id::TEXT
    FROM selected_file_upload selected
    UNION
    SELECT 'CONTENT', association.content_id::TEXT
    FROM cobalt.page_row_content association
    JOIN selected_row selected USING (page_row_id)
    UNION
    SELECT 'GROUP_SESSION', association.group_session_id::TEXT
    FROM cobalt.page_row_group_session association
    JOIN selected_row selected USING (page_row_id)
    UNION
    SELECT 'TAG_GROUP', association.tag_group_id::TEXT
    FROM cobalt.page_row_tag_group association
    JOIN selected_row selected USING (page_row_id)
    UNION
    SELECT 'TAG', association.tag_id::TEXT
    FROM cobalt.page_row_tag association
    JOIN selected_row selected USING (page_row_id)
    UNION
    SELECT 'MAILING_LIST', selected.mailing_list_id::TEXT
    FROM selected_mailing_list selected
  )
  SELECT reference_type, source_id, source_id AS target_id
  FROM source_reference
  ORDER BY reference_type, source_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 70-reference-map.csv

COPY (
  WITH
  selected_page AS (
    SELECT p.page_id
    FROM cobalt.page p
    WHERE p.institution_id = :'source_institution_id'
  ),
  selected_section AS (
    SELECT ps.page_section_id, ps.page_id
    FROM cobalt.page_section ps
    JOIN selected_page p USING (page_id)
  ),
  selected_row AS (
    SELECT pr.page_row_id
    FROM cobalt.page_row pr
    JOIN selected_section ps USING (page_section_id)
  ),
  selected_file_upload AS (
    SELECT p.image_file_upload_id AS file_upload_id
    FROM cobalt.page p
    JOIN selected_page selected USING (page_id)
    WHERE p.image_file_upload_id IS NOT NULL
    UNION
    SELECT prc.image_file_upload_id
    FROM cobalt.page_row_column prc
    JOIN selected_row selected USING (page_row_id)
    WHERE prc.image_file_upload_id IS NOT NULL
  ),
  selected_mailing_list AS (
    SELECT DISTINCT association.mailing_list_id
    FROM cobalt.page_row_mailing_list association
    JOIN selected_row selected USING (page_row_id)
  ),
  source_reference AS (
    SELECT 'INSTITUTION'::TEXT AS reference_type, p.institution_id::TEXT AS source_id
    FROM cobalt.page p
    JOIN selected_page selected USING (page_id)
    UNION
    SELECT 'INSTITUTION', ml.institution_id::TEXT
    FROM cobalt.mailing_list ml
    JOIN selected_mailing_list selected USING (mailing_list_id)
    UNION
    SELECT 'ACCOUNT', p.created_by_account_id::TEXT
    FROM cobalt.page p
    JOIN selected_page selected USING (page_id)
    UNION
    SELECT 'ACCOUNT', psl.created_by_account_id::TEXT
    FROM cobalt.page_site_location psl
    JOIN selected_page selected USING (page_id)
    UNION
    SELECT 'ACCOUNT', ps.created_by_account_id::TEXT
    FROM cobalt.page_section ps
    JOIN selected_section selected USING (page_section_id)
    UNION
    SELECT 'ACCOUNT', pr.created_by_account_id::TEXT
    FROM cobalt.page_row pr
    JOIN selected_row selected USING (page_row_id)
    UNION
    SELECT 'ACCOUNT', fu.account_id::TEXT
    FROM cobalt.file_upload fu
    JOIN selected_file_upload selected USING (file_upload_id)
    UNION
    SELECT 'ACCOUNT', ml.created_by_account_id::TEXT
    FROM cobalt.mailing_list ml
    JOIN selected_mailing_list selected USING (mailing_list_id)
    UNION
    SELECT 'FILE_UPLOAD', selected.file_upload_id::TEXT
    FROM selected_file_upload selected
    UNION
    SELECT 'CONTENT', association.content_id::TEXT
    FROM cobalt.page_row_content association
    JOIN selected_row selected USING (page_row_id)
    UNION
    SELECT 'GROUP_SESSION', association.group_session_id::TEXT
    FROM cobalt.page_row_group_session association
    JOIN selected_row selected USING (page_row_id)
    UNION
    SELECT 'TAG_GROUP', association.tag_group_id::TEXT
    FROM cobalt.page_row_tag_group association
    JOIN selected_row selected USING (page_row_id)
    UNION
    SELECT 'TAG', association.tag_id::TEXT
    FROM cobalt.page_row_tag association
    JOIN selected_row selected USING (page_row_id)
    UNION
    SELECT 'MAILING_LIST', selected.mailing_list_id::TEXT
    FROM selected_mailing_list selected
  ),
  manifest AS (
    SELECT 'page_group'::TEXT AS table_name, count(*)::BIGINT AS row_count
    FROM cobalt.page_group pg
    WHERE EXISTS (
      SELECT 1
      FROM cobalt.page p
      JOIN selected_page selected USING (page_id)
      WHERE p.page_group_id = pg.page_group_id
    )
    UNION ALL
    SELECT 'page', count(*) FROM selected_page
    UNION ALL
    SELECT 'page_site_location', count(*)
    FROM cobalt.page_site_location x JOIN selected_page selected USING (page_id)
    UNION ALL
    SELECT 'page_section', count(*) FROM selected_section
    UNION ALL
    SELECT 'page_row', count(*) FROM selected_row
    UNION ALL
    SELECT 'page_row_column', count(*)
    FROM cobalt.page_row_column x JOIN selected_row selected USING (page_row_id)
    UNION ALL
    SELECT 'page_row_group_session', count(*)
    FROM cobalt.page_row_group_session x JOIN selected_row selected USING (page_row_id)
    UNION ALL
    SELECT 'page_row_content', count(*)
    FROM cobalt.page_row_content x JOIN selected_row selected USING (page_row_id)
    UNION ALL
    SELECT 'page_row_tag_group', count(*)
    FROM cobalt.page_row_tag_group x JOIN selected_row selected USING (page_row_id)
    UNION ALL
    SELECT 'page_row_tag', count(*)
    FROM cobalt.page_row_tag x JOIN selected_row selected USING (page_row_id)
    UNION ALL
    SELECT 'page_row_mailing_list', count(*)
    FROM cobalt.page_row_mailing_list x JOIN selected_row selected USING (page_row_id)
    UNION ALL
    SELECT 'file_upload', count(*) FROM selected_file_upload
    UNION ALL
    SELECT 'mailing_list', count(*) FROM selected_mailing_list
    UNION ALL
    SELECT 'reference_map', count(*) FROM source_reference
  )
  SELECT table_name, row_count
  FROM manifest
  ORDER BY table_name
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 80-manifest.csv
COPY (
  WITH
  selected_page AS (
    SELECT p.page_id
    FROM cobalt.page p
    WHERE p.institution_id = :'source_institution_id'
  ),
  selected_section AS (
    SELECT ps.page_section_id, ps.page_id
    FROM cobalt.page_section ps
    JOIN selected_page p USING (page_id)
  ),
  selected_row AS (
    SELECT pr.page_row_id
    FROM cobalt.page_row pr
    JOIN selected_section ps USING (page_section_id)
  ),
  column_anomaly AS (
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
    FROM cobalt.page_row pr
    JOIN selected_row selected USING (page_row_id)
    LEFT JOIN cobalt.page_row_column prc USING (page_row_id)
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
      )
  ),
  shape_metric AS (
    SELECT 'pages'::TEXT AS metric, count(*)::BIGINT AS value
    FROM selected_page
    UNION ALL
    SELECT 'page_groups', count(DISTINCT p.page_group_id)
    FROM cobalt.page p JOIN selected_page selected USING (page_id)
    UNION ALL
    SELECT 'deleted_pages', count(*)
    FROM cobalt.page p JOIN selected_page selected USING (page_id)
    WHERE p.deleted_flag
    UNION ALL
    SELECT 'active_pages_without_active_sections', count(*)
    FROM cobalt.page p
    JOIN selected_page selected USING (page_id)
    WHERE p.deleted_flag = FALSE
      AND NOT EXISTS (
        SELECT 1
        FROM cobalt.page_section ps
        WHERE ps.page_id = p.page_id
          AND ps.deleted_flag = FALSE
      )
    UNION ALL
    SELECT 'pages_with_multiple_active_sections', count(*)
    FROM (
      SELECT p.page_id
      FROM cobalt.page p
      JOIN selected_page selected USING (page_id)
      JOIN cobalt.page_section ps
        ON ps.page_id = p.page_id
       AND ps.deleted_flag = FALSE
      GROUP BY p.page_id
      HAVING count(*) > 1
    ) pages
    UNION ALL
    SELECT 'sections', count(*) FROM selected_section
    UNION ALL
    SELECT 'deleted_sections', count(*)
    FROM cobalt.page_section ps
    JOIN selected_section selected USING (page_section_id)
    WHERE ps.deleted_flag
    UNION ALL
    SELECT 'rows', count(*) FROM selected_row
    UNION ALL
    SELECT 'deleted_rows', count(*)
    FROM cobalt.page_row pr
    JOIN selected_row selected USING (page_row_id)
    WHERE pr.deleted_flag
    UNION ALL
    SELECT 'rows_in_deleted_sections', count(*)
    FROM cobalt.page_row pr
    JOIN selected_row selected USING (page_row_id)
    JOIN cobalt.page_section ps USING (page_section_id)
    WHERE ps.deleted_flag
    UNION ALL
    SELECT 'active_rows_hidden_in_deleted_sections', count(*)
    FROM cobalt.page_row pr
    JOIN selected_row selected USING (page_row_id)
    JOIN cobalt.page_section ps USING (page_section_id)
    JOIN cobalt.page p USING (page_id)
    WHERE pr.deleted_flag = FALSE
      AND ps.deleted_flag = TRUE
      AND (
        p.deleted_flag = FALSE
        OR EXISTS (
          SELECT 1
          FROM cobalt.page_section active_section
          WHERE active_section.page_id = p.page_id
            AND active_section.deleted_flag = FALSE
        )
      )
    UNION ALL
    SELECT 'columns', count(*)
    FROM cobalt.page_row_column prc
    JOIN selected_row selected USING (page_row_id)
    UNION ALL
    SELECT 'rows_over_four_columns', count(*)
    FROM column_anomaly WHERE over_four_columns
    UNION ALL
    SELECT 'rows_with_order_anomalies', count(*)
    FROM column_anomaly WHERE order_requires_normalization
    UNION ALL
    SELECT 'expected_new_text_rows', count(*)
    FROM cobalt.page_section ps
    JOIN selected_section selected USING (page_section_id)
    WHERE ps.deleted_flag = FALSE
      AND COALESCE(
        NULLIF(BTRIM(ps.headline), ''),
        NULLIF(BTRIM(ps.description), '')
      ) IS NOT NULL
  )
  SELECT metric, value
  FROM shape_metric
  ORDER BY metric
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 81-shape-metrics.csv

COPY (
  WITH
  selected_page AS (
    SELECT p.page_id
    FROM cobalt.page p
    WHERE p.institution_id = :'source_institution_id'
  ),
  selected_section AS (
    SELECT ps.page_section_id, ps.page_id
    FROM cobalt.page_section ps
    JOIN selected_page p USING (page_id)
  ),
  selected_row AS (
    SELECT pr.page_row_id
    FROM cobalt.page_row pr
    JOIN selected_section ps USING (page_section_id)
  ),
  column_anomaly AS (
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
    FROM cobalt.page_row pr
    JOIN selected_row selected USING (page_row_id)
    LEFT JOIN cobalt.page_row_column prc USING (page_row_id)
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
      )
  )
  SELECT
    page_row_id,
    column_count,
    distinct_order_count,
    minimum_order,
    maximum_order,
    over_four_columns,
    order_requires_normalization
  FROM column_anomaly
  ORDER BY page_row_id
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 82-column-order-anomalies.csv
COPY (
  WITH
  selected_page AS (
    SELECT p.page_id
    FROM cobalt.page p
    WHERE p.institution_id = :'source_institution_id'
  ),
  selected_section AS (
    SELECT ps.page_section_id, ps.page_id
    FROM cobalt.page_section ps
    JOIN selected_page p USING (page_id)
  ),
  selected_row AS (
    SELECT pr.page_row_id
    FROM cobalt.page_row pr
    JOIN selected_section ps USING (page_section_id)
  ),
  selected_file_upload AS (
    SELECT p.image_file_upload_id AS file_upload_id
    FROM cobalt.page p
    JOIN selected_page selected USING (page_id)
    WHERE p.image_file_upload_id IS NOT NULL
    UNION
    SELECT prc.image_file_upload_id
    FROM cobalt.page_row_column prc
    JOIN selected_row selected USING (page_row_id)
    WHERE prc.image_file_upload_id IS NOT NULL
  ),
  text_value(field_name, value) AS (
    SELECT 'page_group.analytics_campaign_key', pg.analytics_campaign_key
    FROM cobalt.page_group pg
    WHERE EXISTS (
      SELECT 1
      FROM cobalt.page p
      JOIN selected_page selected USING (page_id)
      WHERE p.page_group_id = pg.page_group_id
    )
    UNION ALL
    SELECT 'page.name', p.name
    FROM cobalt.page p JOIN selected_page selected USING (page_id)
    UNION ALL
    SELECT 'page.url_name', p.url_name
    FROM cobalt.page p JOIN selected_page selected USING (page_id)
    UNION ALL
    SELECT 'page.headline', p.headline
    FROM cobalt.page p JOIN selected_page selected USING (page_id)
    UNION ALL
    SELECT 'page.description', p.description
    FROM cobalt.page p JOIN selected_page selected USING (page_id)
    UNION ALL
    SELECT 'page.image_alt_text', p.image_alt_text
    FROM cobalt.page p JOIN selected_page selected USING (page_id)
    UNION ALL
    SELECT 'page_site_location.call_to_action', psl.call_to_action
    FROM cobalt.page_site_location psl JOIN selected_page selected USING (page_id)
    UNION ALL
    SELECT 'page_site_location.short_description', psl.short_description
    FROM cobalt.page_site_location psl JOIN selected_page selected USING (page_id)
    UNION ALL
    SELECT 'page_section.name', ps.name
    FROM cobalt.page_section ps JOIN selected_page selected USING (page_id)
    UNION ALL
    SELECT 'page_section.headline', ps.headline
    FROM cobalt.page_section ps JOIN selected_page selected USING (page_id)
    UNION ALL
    SELECT 'page_section.description', ps.description
    FROM cobalt.page_section ps JOIN selected_page selected USING (page_id)
    UNION ALL
    SELECT 'page_row_column.headline', prc.headline
    FROM cobalt.page_row_column prc JOIN selected_row selected USING (page_row_id)
    UNION ALL
    SELECT 'page_row_column.description', prc.description
    FROM cobalt.page_row_column prc JOIN selected_row selected USING (page_row_id)
    UNION ALL
    SELECT 'page_row_column.image_alt_text', prc.image_alt_text
    FROM cobalt.page_row_column prc JOIN selected_row selected USING (page_row_id)
    UNION ALL
    SELECT 'page_row_mailing_list.title', prml.title
    FROM cobalt.page_row_mailing_list prml JOIN selected_row selected USING (page_row_id)
    UNION ALL
    SELECT 'page_row_mailing_list.description', prml.description
    FROM cobalt.page_row_mailing_list prml JOIN selected_row selected USING (page_row_id)
    UNION ALL
    SELECT 'file_upload.url', fu.url
    FROM cobalt.file_upload fu JOIN selected_file_upload selected USING (file_upload_id)
    UNION ALL
    SELECT 'file_upload.storage_key', fu.storage_key
    FROM cobalt.file_upload fu JOIN selected_file_upload selected USING (file_upload_id)
  ),
  sensitive_text_count AS (
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
      ) > 0
  )
  SELECT field_name, email_like, phone_like, link_like
  FROM sensitive_text_count
  ORDER BY field_name
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 90-sensitive-text-counts.csv

COPY (
  SELECT
    transaction_timestamp() AS exported_at,
    :'source_institution_id'::TEXT AS source_institution_id,
    'pre-257'::TEXT AS expected_schema_state
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 00-export-info.csv

\echo ''
\echo 'Exported row counts are recorded in 80-manifest.csv.'
\echo 'Sensitive-text triage counts are recorded in 90-sensitive-text-counts.csv.'

-- Write the completion marker only after every data and diagnostic file has
-- been copied successfully.
COPY (
  SELECT transaction_timestamp() AS completed_at, 20::INTEGER AS expected_file_count
) TO STDOUT WITH (FORMAT csv, HEADER true, NULL '\N', FORCE_QUOTE *)
\g 99-export-complete.csv

\echo ''
\echo 'Export complete. Keep these files private and never commit them.'

ROLLBACK;
