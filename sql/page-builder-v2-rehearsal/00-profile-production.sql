\set ON_ERROR_STOP on

\if :{?source_institution_id}
\else
  \echo 'ERROR: Pass -v source_institution_id=...'
  SELECT 1 / 0;
\endif

-- This profile is safe to run on a physical read replica. A writable primary
-- is also supported only through an explicit opt-in on a connection whose
-- default transaction mode was forced read-only before this file was read.
-- Every database access operation below is a SELECT; scope sets are
-- reconstructed with CTEs instead of being materialized in session work
-- tables. Transaction control keeps all profile sections on one
-- repeatable-read snapshot.

\if :{?confirm_read_only_primary}
\else
  \set confirm_read_only_primary NO
\endif

SELECT
  pg_is_in_recovery() AS source_is_read_replica,
  current_setting('default_transaction_read_only')::BOOLEAN
    AS source_defaults_to_read_only,
  (
    pg_is_in_recovery()
    OR (
      :'confirm_read_only_primary' = 'YES'
      AND current_setting('default_transaction_read_only')::BOOLEAN
    )
  ) AS source_connection_is_safe
\gset

\if :source_connection_is_safe
\else
  \echo 'ERROR: Source must be a physical read replica, or pass confirm_read_only_primary=YES on a connection with default_transaction_read_only=on'
  SELECT 1 / 0;
\endif

BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;

SELECT current_setting('transaction_read_only')::BOOLEAN
  AS source_transaction_is_read_only
\gset

\if :source_transaction_is_read_only
\else
  \echo 'ERROR: Source transaction is not read-only'
  SELECT 1 / 0;
\endif

SELECT NOT (
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'cobalt'
      AND table_name = 'page_row'
      AND column_name IN ('name', 'padding_top_id', 'padding_bottom_id')
  ) OR EXISTS (
    SELECT 1
    FROM _v.patches
    WHERE patch_name = '257-page-builder-v2'
  )
) AS source_is_before_migration_257
\gset

\if :source_is_before_migration_257
\else
  \echo 'ERROR: This profile must run against a database before migration 257'
  SELECT 1 / 0;
\endif

SELECT EXISTS (
  SELECT 1
  FROM cobalt.page
  WHERE institution_id = :'source_institution_id'
) AS source_has_pages
\gset

\if :source_has_pages
\else
  \echo 'ERROR: No pages found for source institution' :'source_institution_id'
  SELECT 1 / 0;
\endif

WITH selected_page AS (
  SELECT p.page_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
)
SELECT NOT EXISTS (
  SELECT 1
  FROM cobalt.page child
  JOIN selected_page selected ON selected.page_id = child.page_id
  LEFT JOIN selected_page parent_scope
    ON parent_scope.page_id = child.parent_page_id
  WHERE child.parent_page_id IS NOT NULL
    AND parent_scope.page_id IS NULL
) AS selected_page_parents_are_in_scope
\gset

\if :selected_page_parents_are_in_scope
\else
  \echo 'ERROR: A selected page has a parent outside the source institution'
  SELECT 1 / 0;
\endif

WITH selected_page AS (
  SELECT p.page_id, p.page_group_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
),
selected_group AS (
  SELECT DISTINCT p.page_group_id
  FROM selected_page p
)
SELECT NOT EXISTS (
  SELECT 1
  FROM cobalt.page p
  JOIN selected_group selected
    ON selected.page_group_id = p.page_group_id
  GROUP BY p.page_group_id
  HAVING count(DISTINCT p.institution_id) <> 1
) AS selected_page_groups_are_in_scope
\gset

\if :selected_page_groups_are_in_scope
\else
  \echo 'ERROR: A selected page group spans more than one institution'
  SELECT 1 / 0;
\endif

\echo ''
\echo 'Scope summary'
WITH settings AS (
  SELECT :'source_institution_id'::TEXT AS source_institution_id
)
SELECT
  s.source_institution_id,
  count(DISTINCT p.page_id) AS pages,
  count(DISTINCT p.page_group_id) AS page_groups,
  count(DISTINCT ps.page_section_id) AS sections,
  count(DISTINCT pr.page_row_id) AS rows,
  count(DISTINCT prc.page_row_column_id) AS columns
FROM settings s
LEFT JOIN cobalt.page p ON p.institution_id = s.source_institution_id
LEFT JOIN cobalt.page_section ps ON ps.page_id = p.page_id
LEFT JOIN cobalt.page_row pr ON pr.page_section_id = ps.page_section_id
LEFT JOIN cobalt.page_row_column prc ON prc.page_row_id = pr.page_row_id
GROUP BY s.source_institution_id;

\echo ''
\echo 'Page status and deletion shape'
WITH selected_page AS (
  SELECT p.page_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
)
SELECT p.page_status_id, p.deleted_flag, count(*) AS page_count
FROM cobalt.page p
JOIN selected_page selected ON selected.page_id = p.page_id
GROUP BY p.page_status_id, p.deleted_flag
ORDER BY p.page_status_id, p.deleted_flag;

\echo ''
\echo 'Active-section count per page'
WITH selected_page AS (
  SELECT p.page_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
)
SELECT active_section_count, count(*) AS page_count
FROM (
  SELECT
    p.page_id,
    count(ps.page_section_id) FILTER (WHERE ps.deleted_flag = FALSE)
      AS active_section_count
  FROM cobalt.page p
  JOIN selected_page selected ON selected.page_id = p.page_id
  LEFT JOIN cobalt.page_section ps ON ps.page_id = p.page_id
  GROUP BY p.page_id
) counts
GROUP BY active_section_count
ORDER BY active_section_count;

\echo ''
\echo 'Row-type distribution, including deleted rows'
WITH selected_page AS (
  SELECT p.page_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
),
selected_section AS (
  SELECT ps.page_section_id
  FROM cobalt.page_section ps
  JOIN selected_page p ON p.page_id = ps.page_id
),
selected_row AS (
  SELECT pr.page_row_id
  FROM cobalt.page_row pr
  JOIN selected_section ps ON ps.page_section_id = pr.page_section_id
)
SELECT pr.row_type_id, pr.deleted_flag, count(*) AS row_count
FROM cobalt.page_row pr
JOIN selected_row selected ON selected.page_row_id = pr.page_row_id
GROUP BY pr.row_type_id, pr.deleted_flag
ORDER BY pr.row_type_id, pr.deleted_flag;

\echo ''
\echo 'Rows attached to deleted sections'
WITH selected_page AS (
  SELECT p.page_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
),
selected_section AS (
  SELECT ps.page_section_id
  FROM cobalt.page_section ps
  JOIN selected_page p ON p.page_id = ps.page_id
),
selected_row AS (
  SELECT pr.page_row_id
  FROM cobalt.page_row pr
  JOIN selected_section ps ON ps.page_section_id = pr.page_section_id
)
SELECT
  count(*) AS rows_in_deleted_sections,
  count(*) FILTER (
    WHERE row.deleted_flag = FALSE
      AND (
        page.deleted_flag = FALSE
        OR EXISTS (
          SELECT 1
          FROM cobalt.page_section active_section
          WHERE active_section.page_id = page.page_id
            AND active_section.deleted_flag = FALSE
        )
      )
  ) AS active_rows_hidden_in_deleted_sections
FROM cobalt.page_row row
JOIN selected_row selected USING (page_row_id)
JOIN cobalt.page_section section USING (page_section_id)
JOIN cobalt.page ON page.page_id = section.page_id
WHERE section.deleted_flag = TRUE;

\echo ''
\echo 'Active rows hidden in deleted sections (zero rows is expected)'
WITH selected_page AS (
  SELECT p.page_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
),
selected_section AS (
  SELECT ps.page_section_id
  FROM cobalt.page_section ps
  JOIN selected_page p ON p.page_id = ps.page_id
),
selected_row AS (
  SELECT pr.page_row_id
  FROM cobalt.page_row pr
  JOIN selected_section ps ON ps.page_section_id = pr.page_section_id
)
SELECT row.page_row_id, row.page_section_id, section.page_id
FROM cobalt.page_row row
JOIN selected_row selected USING (page_row_id)
JOIN cobalt.page_section section USING (page_section_id)
JOIN cobalt.page ON page.page_id = section.page_id
WHERE row.deleted_flag = FALSE
  AND section.deleted_flag = TRUE
  AND (
    page.deleted_flag = FALSE
    OR EXISTS (
      SELECT 1
      FROM cobalt.page_section active_section
      WHERE active_section.page_id = page.page_id
        AND active_section.deleted_flag = FALSE
    )
  )
ORDER BY section.page_id, row.page_section_id, row.page_row_id;

\echo ''
\echo 'Column cardinality and order anomalies'
WITH selected_page AS (
  SELECT p.page_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
),
selected_section AS (
  SELECT ps.page_section_id
  FROM cobalt.page_section ps
  JOIN selected_page p ON p.page_id = ps.page_id
),
selected_row AS (
  SELECT pr.page_row_id
  FROM cobalt.page_row pr
  JOIN selected_section ps ON ps.page_section_id = pr.page_section_id
),
column_shape AS (
  SELECT
    pr.page_row_id,
    count(prc.page_row_column_id) AS column_count,
    count(DISTINCT prc.column_display_order) AS distinct_order_count,
    min(prc.column_display_order) AS minimum_order,
    max(prc.column_display_order) AS maximum_order
  FROM cobalt.page_row pr
  JOIN selected_row selected ON selected.page_row_id = pr.page_row_id
  LEFT JOIN cobalt.page_row_column prc ON prc.page_row_id = pr.page_row_id
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
WITH selected_page AS (
  SELECT p.page_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
),
selected_section AS (
  SELECT ps.page_section_id
  FROM cobalt.page_section ps
  JOIN selected_page p ON p.page_id = ps.page_id
),
selected_row AS (
  SELECT pr.page_row_id
  FROM cobalt.page_row pr
  JOIN selected_section ps ON ps.page_section_id = pr.page_section_id
)
SELECT pr.page_row_id, count(*) AS column_count
FROM cobalt.page_row pr
JOIN selected_row selected ON selected.page_row_id = pr.page_row_id
JOIN cobalt.page_row_column prc ON prc.page_row_id = pr.page_row_id
GROUP BY pr.page_row_id
HAVING count(*) > 4
ORDER BY pr.page_row_id;

\echo ''
\echo 'Expected section-derived text rows created by migration 257'
WITH selected_page AS (
  SELECT p.page_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
),
selected_section AS (
  SELECT ps.page_section_id
  FROM cobalt.page_section ps
  JOIN selected_page p ON p.page_id = ps.page_id
)
SELECT count(*) AS expected_new_text_rows
FROM cobalt.page_section ps
JOIN selected_section selected
  ON selected.page_section_id = ps.page_section_id
WHERE ps.deleted_flag = FALSE
  AND COALESCE(
    NULLIF(BTRIM(ps.headline), ''),
    NULLIF(BTRIM(ps.description), '')
  ) IS NOT NULL;

\echo ''
\echo 'External association cardinality and required local slots'
WITH selected_page AS (
  SELECT p.page_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
),
selected_section AS (
  SELECT ps.page_section_id
  FROM cobalt.page_section ps
  JOIN selected_page p ON p.page_id = ps.page_id
),
selected_row AS (
  SELECT pr.page_row_id
  FROM cobalt.page_row pr
  JOIN selected_section ps ON ps.page_section_id = pr.page_section_id
)
SELECT 'CONTENT' AS reference_type, COALESCE(sum(per_row), 0) AS total_links,
       COALESCE(max(per_row), 0) AS maximum_links_on_one_row
FROM (
  SELECT x.page_row_id, count(*) AS per_row
  FROM cobalt.page_row_content x
  JOIN selected_row selected ON selected.page_row_id = x.page_row_id
  GROUP BY x.page_row_id
) counts
UNION ALL
SELECT 'GROUP_SESSION', COALESCE(sum(per_row), 0), COALESCE(max(per_row), 0)
FROM (
  SELECT x.page_row_id, count(*) AS per_row
  FROM cobalt.page_row_group_session x
  JOIN selected_row selected ON selected.page_row_id = x.page_row_id
  GROUP BY x.page_row_id
) counts
UNION ALL
SELECT 'TAG_GROUP', COALESCE(sum(per_row), 0), COALESCE(max(per_row), 0)
FROM (
  SELECT x.page_row_id, count(*) AS per_row
  FROM cobalt.page_row_tag_group x
  JOIN selected_row selected ON selected.page_row_id = x.page_row_id
  GROUP BY x.page_row_id
) counts
UNION ALL
SELECT 'TAG', COALESCE(sum(per_row), 0), COALESCE(max(per_row), 0)
FROM (
  SELECT x.page_row_id, count(*) AS per_row
  FROM cobalt.page_row_tag x
  JOIN selected_row selected ON selected.page_row_id = x.page_row_id
  GROUP BY x.page_row_id
) counts
UNION ALL
SELECT 'MAILING_LIST', COALESCE(sum(per_row), 0), COALESCE(max(per_row), 0)
FROM (
  SELECT x.page_row_id, count(*) AS per_row
  FROM cobalt.page_row_mailing_list x
  JOIN selected_row selected ON selected.page_row_id = x.page_row_id
  GROUP BY x.page_row_id
) counts
ORDER BY reference_type;

\echo ''
\echo 'Sensitive-text triage counts (matching text is intentionally not printed)'
WITH selected_page AS (
  SELECT p.page_id, p.page_group_id
  FROM cobalt.page p
  WHERE p.institution_id = :'source_institution_id'
),
selected_section AS (
  SELECT ps.page_section_id
  FROM cobalt.page_section ps
  JOIN selected_page p ON p.page_id = ps.page_id
),
selected_row AS (
  SELECT pr.page_row_id
  FROM cobalt.page_row pr
  JOIN selected_section ps ON ps.page_section_id = pr.page_section_id
),
text_value(field_name, value) AS (
  SELECT 'page_group.analytics_campaign_key', pg.analytics_campaign_key
  FROM cobalt.page_group pg
  WHERE EXISTS (
    SELECT 1
    FROM selected_page p
    WHERE p.page_group_id = pg.page_group_id
  )
  UNION ALL
  SELECT 'page.name', p.name
  FROM cobalt.page p JOIN selected_page s USING (page_id)
  UNION ALL
  SELECT 'page.url_name', p.url_name
  FROM cobalt.page p JOIN selected_page s USING (page_id)
  UNION ALL
  SELECT 'page.headline', p.headline
  FROM cobalt.page p JOIN selected_page s USING (page_id)
  UNION ALL
  SELECT 'page.description', p.description
  FROM cobalt.page p JOIN selected_page s USING (page_id)
  UNION ALL
  SELECT 'page.image_alt_text', p.image_alt_text
  FROM cobalt.page p JOIN selected_page s USING (page_id)
  UNION ALL
  SELECT 'page_site_location.call_to_action', psl.call_to_action
  FROM cobalt.page_site_location psl JOIN selected_page s USING (page_id)
  UNION ALL
  SELECT 'page_site_location.short_description', psl.short_description
  FROM cobalt.page_site_location psl JOIN selected_page s USING (page_id)
  UNION ALL
  SELECT 'page_section.name', ps.name
  FROM cobalt.page_section ps JOIN selected_page s USING (page_id)
  UNION ALL
  SELECT 'page_section.headline', ps.headline
  FROM cobalt.page_section ps JOIN selected_page s USING (page_id)
  UNION ALL
  SELECT 'page_section.description', ps.description
  FROM cobalt.page_section ps JOIN selected_page s USING (page_id)
  UNION ALL
  SELECT 'page_row_column.headline', prc.headline
  FROM cobalt.page_row_column prc
  JOIN selected_row s USING (page_row_id)
  UNION ALL
  SELECT 'page_row_column.description', prc.description
  FROM cobalt.page_row_column prc
  JOIN selected_row s USING (page_row_id)
  UNION ALL
  SELECT 'page_row_column.image_alt_text', prc.image_alt_text
  FROM cobalt.page_row_column prc
  JOIN selected_row s USING (page_row_id)
  UNION ALL
  SELECT 'page_row_mailing_list.title', prml.title
  FROM cobalt.page_row_mailing_list prml
  JOIN selected_row s USING (page_row_id)
  UNION ALL
  SELECT 'page_row_mailing_list.description', prml.description
  FROM cobalt.page_row_mailing_list prml
  JOIN selected_row s USING (page_row_id)
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
