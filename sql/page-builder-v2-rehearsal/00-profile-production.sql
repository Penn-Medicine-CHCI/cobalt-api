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

-- PostgreSQL permits writes to pre-existing temporary tables in a read-only
-- transaction, but it does not permit CREATE TEMP TABLE AS after the read-only
-- transaction starts. Create empty session-local work tables first, then acquire
-- one repeatable-read snapshot for every production query below.
CREATE TEMPORARY TABLE page_rehearsal_profile_settings (
  source_institution_id TEXT NOT NULL
) ON COMMIT PRESERVE ROWS;

CREATE TEMPORARY TABLE page_rehearsal_profile_page (
  page_id UUID NOT NULL
) ON COMMIT PRESERVE ROWS;
CREATE UNIQUE INDEX ON page_rehearsal_profile_page(page_id);

CREATE TEMPORARY TABLE page_rehearsal_profile_section (
  page_section_id UUID NOT NULL,
  page_id UUID NOT NULL
) ON COMMIT PRESERVE ROWS;
CREATE UNIQUE INDEX ON page_rehearsal_profile_section(page_section_id);

CREATE TEMPORARY TABLE page_rehearsal_profile_row (
  page_row_id UUID NOT NULL,
  page_id UUID NOT NULL
) ON COMMIT PRESERVE ROWS;
CREATE UNIQUE INDEX ON page_rehearsal_profile_row(page_row_id);

BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;

INSERT INTO page_rehearsal_profile_settings (source_institution_id)
VALUES (:'source_institution_id');

INSERT INTO page_rehearsal_profile_page (page_id)
SELECT p.page_id
FROM page p
JOIN page_rehearsal_profile_settings s
  ON s.source_institution_id = p.institution_id;

INSERT INTO page_rehearsal_profile_section (page_section_id, page_id)
SELECT ps.page_section_id, ps.page_id
FROM page_section ps
JOIN page_rehearsal_profile_page p ON p.page_id = ps.page_id;

INSERT INTO page_rehearsal_profile_row (page_row_id, page_id)
SELECT pr.page_row_id, ps.page_id
FROM page_row pr
JOIN page_rehearsal_profile_section ps
  ON ps.page_section_id = pr.page_section_id;

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
    RAISE EXCEPTION 'This profile must run against a database before migration 257';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM page_rehearsal_profile_page) THEN
    RAISE EXCEPTION 'No pages found for source institution %',
      (SELECT source_institution_id FROM page_rehearsal_profile_settings);
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page child
    JOIN page_rehearsal_profile_page selected ON selected.page_id = child.page_id
    LEFT JOIN page_rehearsal_profile_page parent_scope
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
      JOIN page_rehearsal_profile_page selected ON selected.page_id = p.page_id
    )
    SELECT 1
    FROM page p
    JOIN selected_groups sg ON sg.page_group_id = p.page_group_id
    GROUP BY p.page_group_id
    HAVING count(DISTINCT p.institution_id) <> 1
  ) THEN
    RAISE EXCEPTION 'A selected page group spans more than one institution';
  END IF;
END
$$;

\echo ''
\echo 'Scope summary'
SELECT
  s.source_institution_id,
  count(DISTINCT p.page_id) AS pages,
  count(DISTINCT p.page_group_id) AS page_groups,
  count(DISTINCT ps.page_section_id) AS sections,
  count(DISTINCT pr.page_row_id) AS rows,
  count(DISTINCT prc.page_row_column_id) AS columns
FROM page_rehearsal_profile_settings s
LEFT JOIN page p ON p.institution_id = s.source_institution_id
LEFT JOIN page_section ps ON ps.page_id = p.page_id
LEFT JOIN page_row pr ON pr.page_section_id = ps.page_section_id
LEFT JOIN page_row_column prc ON prc.page_row_id = pr.page_row_id
GROUP BY s.source_institution_id;

\echo ''
\echo 'Page status and deletion shape'
SELECT page_status_id, deleted_flag, count(*) AS page_count
FROM page p
JOIN page_rehearsal_profile_page selected ON selected.page_id = p.page_id
GROUP BY page_status_id, deleted_flag
ORDER BY page_status_id, deleted_flag;

\echo ''
\echo 'Active-section count per page'
SELECT active_section_count, count(*) AS page_count
FROM (
  SELECT
    p.page_id,
    count(ps.page_section_id) FILTER (WHERE ps.deleted_flag = FALSE) AS active_section_count
  FROM page p
  JOIN page_rehearsal_profile_page selected ON selected.page_id = p.page_id
  LEFT JOIN page_section ps ON ps.page_id = p.page_id
  GROUP BY p.page_id
) counts
GROUP BY active_section_count
ORDER BY active_section_count;

\echo ''
\echo 'Row-type distribution, including deleted rows'
SELECT pr.row_type_id, pr.deleted_flag, count(*) AS row_count
FROM page_row pr
JOIN page_rehearsal_profile_row selected ON selected.page_row_id = pr.page_row_id
GROUP BY pr.row_type_id, pr.deleted_flag
ORDER BY pr.row_type_id, pr.deleted_flag;

\echo ''
\echo 'Rows attached to deleted sections'
SELECT
  count(*) AS rows_in_deleted_sections,
  count(*) FILTER (
    WHERE row.deleted_flag = FALSE
      AND (
        page.deleted_flag = FALSE
        OR EXISTS (
          SELECT 1
          FROM page_section active_section
          WHERE active_section.page_id = page.page_id
            AND active_section.deleted_flag = FALSE
        )
      )
  )
    AS active_rows_hidden_in_deleted_sections
FROM page_row row
JOIN page_rehearsal_profile_row selected USING (page_row_id)
JOIN page_section section USING (page_section_id)
JOIN page ON page.page_id = section.page_id
WHERE section.deleted_flag = TRUE;

\echo ''
\echo 'Active rows hidden in deleted sections (zero rows is expected)'
SELECT row.page_row_id, row.page_section_id, section.page_id
FROM page_row row
JOIN page_rehearsal_profile_row selected USING (page_row_id)
JOIN page_section section USING (page_section_id)
JOIN page ON page.page_id = section.page_id
WHERE row.deleted_flag = FALSE
  AND section.deleted_flag = TRUE
  AND (
    page.deleted_flag = FALSE
    OR EXISTS (
      SELECT 1
      FROM page_section active_section
      WHERE active_section.page_id = page.page_id
        AND active_section.deleted_flag = FALSE
    )
  )
ORDER BY section.page_id, row.page_section_id, row.page_row_id;

\echo ''
\echo 'Column cardinality and order anomalies'
WITH column_shape AS (
  SELECT
    pr.page_row_id,
    count(prc.page_row_column_id) AS column_count,
    count(DISTINCT prc.column_display_order) AS distinct_order_count,
    min(prc.column_display_order) AS minimum_order,
    max(prc.column_display_order) AS maximum_order
  FROM page_row pr
  JOIN page_rehearsal_profile_row selected ON selected.page_row_id = pr.page_row_id
  LEFT JOIN page_row_column prc ON prc.page_row_id = pr.page_row_id
  GROUP BY pr.page_row_id
)
SELECT
  count(*) FILTER (WHERE column_count > 4) AS rows_over_four_columns,
  count(*) FILTER (
    WHERE column_count > 0
      AND (
        column_count <> distinct_order_count
        OR minimum_order <> 0
        OR maximum_order <> column_count - 1
      )
  ) AS rows_with_duplicate_gapped_or_nonzero_order,
  max(column_count) AS maximum_columns_on_one_row
FROM column_shape;

\echo ''
\echo 'Rows with more than four columns (zero rows is expected)'
SELECT pr.page_row_id, count(*) AS column_count
FROM page_row pr
JOIN page_rehearsal_profile_row selected ON selected.page_row_id = pr.page_row_id
JOIN page_row_column prc ON prc.page_row_id = pr.page_row_id
GROUP BY pr.page_row_id
HAVING count(*) > 4
ORDER BY pr.page_row_id;

\echo ''
\echo 'Expected section-derived text rows created by migration 257'
SELECT count(*) AS expected_new_text_rows
FROM page_section ps
JOIN page_rehearsal_profile_section selected
  ON selected.page_section_id = ps.page_section_id
WHERE ps.deleted_flag = FALSE
  AND COALESCE(
    NULLIF(BTRIM(ps.headline), ''),
    NULLIF(BTRIM(ps.description), '')
  ) IS NOT NULL;

\echo ''
\echo 'External association cardinality and required local slots'
SELECT 'CONTENT' AS reference_type, COALESCE(sum(per_row), 0) AS total_links,
       COALESCE(max(per_row), 0) AS maximum_links_on_one_row
FROM (
  SELECT x.page_row_id, count(*) AS per_row
  FROM page_row_content x
  JOIN page_rehearsal_profile_row selected ON selected.page_row_id = x.page_row_id
  GROUP BY x.page_row_id
) counts
UNION ALL
SELECT 'GROUP_SESSION', COALESCE(sum(per_row), 0), COALESCE(max(per_row), 0)
FROM (
  SELECT x.page_row_id, count(*) AS per_row
  FROM page_row_group_session x
  JOIN page_rehearsal_profile_row selected ON selected.page_row_id = x.page_row_id
  GROUP BY x.page_row_id
) counts
UNION ALL
SELECT 'TAG_GROUP', COALESCE(sum(per_row), 0), COALESCE(max(per_row), 0)
FROM (
  SELECT x.page_row_id, count(*) AS per_row
  FROM page_row_tag_group x
  JOIN page_rehearsal_profile_row selected ON selected.page_row_id = x.page_row_id
  GROUP BY x.page_row_id
) counts
UNION ALL
SELECT 'TAG', COALESCE(sum(per_row), 0), COALESCE(max(per_row), 0)
FROM (
  SELECT x.page_row_id, count(*) AS per_row
  FROM page_row_tag x
  JOIN page_rehearsal_profile_row selected ON selected.page_row_id = x.page_row_id
  GROUP BY x.page_row_id
) counts
UNION ALL
SELECT 'MAILING_LIST', COALESCE(sum(per_row), 0), COALESCE(max(per_row), 0)
FROM (
  SELECT x.page_row_id, count(*) AS per_row
  FROM page_row_mailing_list x
  JOIN page_rehearsal_profile_row selected ON selected.page_row_id = x.page_row_id
  GROUP BY x.page_row_id
) counts
ORDER BY reference_type;

\echo ''
\echo 'Sensitive-text triage counts (matching text is intentionally not printed)'
WITH text_value(field_name, value) AS (
  SELECT 'page_group.analytics_campaign_key', pg.analytics_campaign_key
  FROM page_group pg
  WHERE EXISTS (
    SELECT 1
    FROM page p
    JOIN page_rehearsal_profile_page selected USING (page_id)
    WHERE p.page_group_id = pg.page_group_id
  )
  UNION ALL
  SELECT 'page.name', p.name
  FROM page p JOIN page_rehearsal_profile_page s USING (page_id)
  UNION ALL
  SELECT 'page.url_name', p.url_name
  FROM page p JOIN page_rehearsal_profile_page s USING (page_id)
  UNION ALL
  SELECT 'page.headline', p.headline
  FROM page p JOIN page_rehearsal_profile_page s USING (page_id)
  UNION ALL
  SELECT 'page.description', p.description
  FROM page p JOIN page_rehearsal_profile_page s USING (page_id)
  UNION ALL
  SELECT 'page.image_alt_text', p.image_alt_text
  FROM page p JOIN page_rehearsal_profile_page s USING (page_id)
  UNION ALL
  SELECT 'page_site_location.call_to_action', psl.call_to_action
  FROM page_site_location psl JOIN page_rehearsal_profile_page s USING (page_id)
  UNION ALL
  SELECT 'page_site_location.short_description', psl.short_description
  FROM page_site_location psl JOIN page_rehearsal_profile_page s USING (page_id)
  UNION ALL
  SELECT 'page_section.name', ps.name
  FROM page_section ps JOIN page_rehearsal_profile_page s USING (page_id)
  UNION ALL
  SELECT 'page_section.headline', ps.headline
  FROM page_section ps JOIN page_rehearsal_profile_page s USING (page_id)
  UNION ALL
  SELECT 'page_section.description', ps.description
  FROM page_section ps JOIN page_rehearsal_profile_page s USING (page_id)
  UNION ALL
  SELECT 'page_row_column.headline', prc.headline
  FROM page_row_column prc
  JOIN page_rehearsal_profile_row s USING (page_row_id)
  UNION ALL
  SELECT 'page_row_column.description', prc.description
  FROM page_row_column prc
  JOIN page_rehearsal_profile_row s USING (page_row_id)
  UNION ALL
  SELECT 'page_row_column.image_alt_text', prc.image_alt_text
  FROM page_row_column prc
  JOIN page_rehearsal_profile_row s USING (page_row_id)
  UNION ALL
  SELECT 'page_row_mailing_list.title', prml.title
  FROM page_row_mailing_list prml
  JOIN page_rehearsal_profile_row s USING (page_row_id)
  UNION ALL
  SELECT 'page_row_mailing_list.description', prml.description
  FROM page_row_mailing_list prml
  JOIN page_rehearsal_profile_row s USING (page_row_id)
)
SELECT
  field_name,
  count(*) FILTER (
    WHERE value ~* '[[:alnum:]._%+_-]+@[[:alnum:].-]+[.][[:alpha:]]{2,}'
  ) AS email_like,
  count(*) FILTER (
    WHERE value ~* '[+]?[0-9][0-9() .-]{7,}[0-9]'
  ) AS phone_like,
  count(*) FILTER (
    WHERE value ~* '(mailto:|tel:|https?://)'
  ) AS link_like
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
ORDER BY field_name;

ROLLBACK;
