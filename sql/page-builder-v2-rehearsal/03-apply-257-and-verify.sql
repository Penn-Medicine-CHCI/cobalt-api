\set ON_ERROR_STOP on

SET search_path TO cobalt, public;

DO $$
BEGIN
  IF to_regnamespace('page_builder_v2_rehearsal') IS NULL
    OR to_regclass('page_builder_v2_rehearsal.settings') IS NULL
    OR to_regclass('page_builder_v2_rehearsal.baseline_page') IS NULL
    OR to_regclass('page_builder_v2_rehearsal.baseline_file_upload') IS NULL
    OR to_regclass('page_builder_v2_rehearsal.import_verification_result') IS NULL
  THEN
    RAISE EXCEPTION 'Run 02-import-local-pre257.sql before applying migration 257';
  END IF;

  IF EXISTS (
    SELECT 1 FROM _v.patches WHERE patch_name = '257-page-builder-v2'
  ) THEN
    RAISE EXCEPTION 'Migration 257 is already applied; recreate and rerun the rehearsal';
  END IF;
END
$$;

\echo 'Applying the repository migration sql/updates/257-page-builder-v2.sql...'
\ir ../updates/257-page-builder-v2.sql

SET search_path TO cobalt, public;

CREATE OR REPLACE FUNCTION page_builder_v2_rehearsal.compare_backup_rows(
  baseline_table REGCLASS,
  backup_table REGCLASS,
  primary_key_column TEXT
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
  mismatch_count BIGINT;
BEGIN
  EXECUTE format(
    $query$
      WITH baseline_scope AS (
        SELECT to_jsonb(baseline_row) AS row_data
        FROM %s baseline_row
      ),
      backup_scope AS (
        SELECT to_jsonb(backup_row) - 'backed_up_at' AS row_data
        FROM %s backup_row
        JOIN %s baseline_row
          ON backup_row.%I = baseline_row.%I
      )
      SELECT count(*)
      FROM (
        (SELECT row_data FROM baseline_scope
         EXCEPT ALL
         SELECT row_data FROM backup_scope)
        UNION ALL
        (SELECT row_data FROM backup_scope
         EXCEPT ALL
         SELECT row_data FROM baseline_scope)
      ) differences
    $query$,
    baseline_table,
    backup_table,
    baseline_table,
    primary_key_column,
    primary_key_column
  ) INTO mismatch_count;

  RETURN mismatch_count;
END
$$;

CREATE OR REPLACE FUNCTION page_builder_v2_rehearsal.compare_current_rows(
  baseline_table REGCLASS,
  current_table REGCLASS,
  primary_key_column TEXT
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
  mismatch_count BIGINT;
BEGIN
  EXECUTE format(
    $query$
      WITH baseline_scope AS (
        SELECT to_jsonb(baseline_row) AS row_data
        FROM %s baseline_row
      ),
      current_scope AS (
        SELECT to_jsonb(current_row) AS row_data
        FROM %s current_row
        JOIN %s baseline_row
          ON current_row.%I = baseline_row.%I
      )
      SELECT count(*)
      FROM (
        (SELECT row_data FROM baseline_scope
         EXCEPT ALL
         SELECT row_data FROM current_scope)
        UNION ALL
        (SELECT row_data FROM current_scope
         EXCEPT ALL
         SELECT row_data FROM baseline_scope)
      ) differences
    $query$,
    baseline_table,
    current_table,
    baseline_table,
    primary_key_column,
    primary_key_column
  ) INTO mismatch_count;

  RETURN mismatch_count;
END
$$;

-- Exercise the trigger in a subtransaction and intentionally roll the probe
-- data back before returning. This covers both a fifth INSERT and moving a
-- column into a row that already has four columns.
CREATE OR REPLACE FUNCTION page_builder_v2_rehearsal.probe_column_limit_trigger()
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
  target_institution_id TEXT;
  target_account_id UUID;
  probe_page_group_id UUID := uuid_generate_v4();
  probe_page_id UUID := uuid_generate_v4();
  probe_page_section_id UUID := uuid_generate_v4();
  full_page_row_id UUID := uuid_generate_v4();
  source_page_row_id UUID := uuid_generate_v4();
  source_page_row_column_id UUID;
  column_index INTEGER;
  fifth_insert_rejected BOOLEAN := FALSE;
  reparent_rejected BOOLEAN := FALSE;
BEGIN
  SELECT settings.target_institution_id, settings.target_account_id
  INTO STRICT target_institution_id, target_account_id
  FROM page_builder_v2_rehearsal.settings;

  BEGIN
    INSERT INTO page_group (page_group_id)
    VALUES (probe_page_group_id);

    INSERT INTO page (
      page_id,
      name,
      url_name,
      page_status_id,
      institution_id,
      created_by_account_id,
      page_group_id
    )
    VALUES (
      probe_page_id,
      'Column Limit Probe',
      'column-limit-probe-' || probe_page_id,
      'DRAFT',
      target_institution_id,
      target_account_id,
      probe_page_group_id
    );

    INSERT INTO page_section (
      page_section_id,
      page_id,
      name,
      background_color_id,
      display_order,
      created_by_account_id
    )
    VALUES (
      probe_page_section_id,
      probe_page_id,
      'Content',
      'WHITE',
      0,
      target_account_id
    );

    INSERT INTO page_row (
      page_row_id,
      page_section_id,
      row_type_id,
      display_order,
      created_by_account_id
    )
    VALUES
      (
        full_page_row_id,
        probe_page_section_id,
        'ONE_COLUMN_TEXT',
        0,
        target_account_id
      ),
      (
        source_page_row_id,
        probe_page_section_id,
        'ONE_COLUMN_TEXT',
        1,
        target_account_id
      );

    FOR column_index IN 0..3 LOOP
      INSERT INTO page_row_column (page_row_id, column_display_order)
      VALUES (full_page_row_id, column_index);
    END LOOP;

    BEGIN
      INSERT INTO page_row_column (page_row_id, column_display_order)
      VALUES (full_page_row_id, 4);
    EXCEPTION
      WHEN check_violation THEN
        fifth_insert_rejected := TRUE;
      WHEN OTHERS THEN
        fifth_insert_rejected := FALSE;
    END;

    INSERT INTO page_row_column (page_row_id, column_display_order)
    VALUES (source_page_row_id, 0)
    RETURNING page_row_column_id INTO source_page_row_column_id;

    BEGIN
      UPDATE page_row_column
      SET page_row_id = full_page_row_id
      WHERE page_row_column_id = source_page_row_column_id;
    EXCEPTION
      WHEN check_violation THEN
        reparent_rejected := TRUE;
      WHEN OTHERS THEN
        reparent_rejected := FALSE;
    END;

    RAISE EXCEPTION USING
      ERRCODE = 'P0001',
      MESSAGE = 'page-builder-v2-column-probe-rollback';
  EXCEPTION
    WHEN raise_exception THEN
      IF SQLERRM <> 'page-builder-v2-column-probe-rollback' THEN
        RAISE;
      END IF;
  END;

  RETURN fifth_insert_rejected AND reparent_rejected;
END
$$;

-- Create active and deleted CTA rows, exercise URL/timestamp resolution in the
-- CTA view, and roll every probe object back before returning.
CREATE OR REPLACE FUNCTION page_builder_v2_rehearsal.probe_cta_view()
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
  target_institution_id TEXT;
  target_account_id UUID;
  probe_page_group_id UUID := uuid_generate_v4();
  probe_page_id UUID := uuid_generate_v4();
  probe_page_section_id UUID := uuid_generate_v4();
  active_page_row_id UUID := uuid_generate_v4();
  deleted_page_row_id UUID := uuid_generate_v4();
  active_cta_id UUID := uuid_generate_v4();
  deleted_cta_id UUID := uuid_generate_v4();
  probe_image_file_upload_id UUID := uuid_generate_v4();
  expected_image_url TEXT :=
    'https://example.invalid/page-builder-v2-cta-view-probe/'
      || probe_image_file_upload_id || '.png';
  active_row_matches BOOLEAN := FALSE;
  deleted_row_hidden BOOLEAN := FALSE;
  timestamp_trigger_worked BOOLEAN := FALSE;
BEGIN
  SELECT settings.target_institution_id, settings.target_account_id
  INTO STRICT target_institution_id, target_account_id
  FROM page_builder_v2_rehearsal.settings;

  BEGIN
    INSERT INTO file_upload (
      file_upload_id,
      account_id,
      url,
      storage_key,
      filename,
      content_type,
      file_upload_type_id,
      filesize,
      remote_data_flag
    )
    VALUES (
      probe_image_file_upload_id,
      target_account_id,
      expected_image_url,
      'page-builder-v2-rehearsal/cta-view-probe/'
        || probe_image_file_upload_id || '.png',
      probe_image_file_upload_id || '.png',
      'image/png',
      'PAGE_IMAGE',
      0,
      FALSE
    );

    INSERT INTO page_group (page_group_id)
    VALUES (probe_page_group_id);

    INSERT INTO page (
      page_id,
      name,
      url_name,
      page_status_id,
      institution_id,
      created_by_account_id,
      page_group_id
    )
    VALUES (
      probe_page_id,
      'CTA View Probe',
      'cta-view-probe-' || probe_page_id,
      'DRAFT',
      target_institution_id,
      target_account_id,
      probe_page_group_id
    );

    INSERT INTO page_section (
      page_section_id,
      page_id,
      name,
      background_color_id,
      display_order,
      created_by_account_id
    )
    VALUES (
      probe_page_section_id,
      probe_page_id,
      'Content',
      'WHITE',
      0,
      target_account_id
    );

    INSERT INTO page_row (
      page_row_id,
      page_section_id,
      row_type_id,
      deleted_flag,
      display_order,
      created_by_account_id
    )
    VALUES
      (
        active_page_row_id,
        probe_page_section_id,
        'CALL_TO_ACTION_BLOCK',
        FALSE,
        7,
        target_account_id
      ),
      (
        deleted_page_row_id,
        probe_page_section_id,
        'CALL_TO_ACTION_FULL_WIDTH',
        TRUE,
        8,
        target_account_id
      );

    INSERT INTO page_row_call_to_action (
      page_row_call_to_action_id,
      page_row_id,
      headline,
      description,
      button_text,
      button_url,
      image_file_upload_id
    )
    VALUES
      (
        active_cta_id,
        active_page_row_id,
        'Probe Headline',
        'Probe Description',
        'Probe Button',
        'https://example.invalid/probe',
        probe_image_file_upload_id
      ),
      (
        deleted_cta_id,
        deleted_page_row_id,
        'Deleted Probe Headline',
        'Deleted Probe Description',
        'Deleted Probe Button',
        '/deleted-probe',
        NULL
      );

    UPDATE page_row_call_to_action
    SET
      button_text = 'Probe Button Updated',
      last_updated = 'epoch'::TIMESTAMPTZ
    WHERE page_row_call_to_action_id = active_cta_id
    RETURNING last_updated <> 'epoch'::TIMESTAMPTZ
    INTO timestamp_trigger_worked;

    SELECT
      count(*) = 1
      AND COALESCE(
        bool_and(
          view_row.page_row_id = active_page_row_id
          AND view_row.page_section_id = probe_page_section_id
          AND view_row.display_order = 7
          AND view_row.row_type_id = 'CALL_TO_ACTION_BLOCK'
          AND view_row.headline = 'Probe Headline'
          AND view_row.description = 'Probe Description'
          AND view_row.button_text = 'Probe Button Updated'
          AND view_row.button_url = 'https://example.invalid/probe'
          AND view_row.image_file_upload_id = probe_image_file_upload_id
          AND view_row.image_url = expected_image_url
          AND view_row.created = source_row.created
          AND view_row.last_updated = source_row.last_updated
        ),
        FALSE
      )
    INTO active_row_matches
    FROM v_page_row_call_to_action view_row
    JOIN page_row source_row USING (page_row_id)
    WHERE view_row.page_row_call_to_action_id = active_cta_id;

    SELECT NOT EXISTS (
      SELECT 1
      FROM v_page_row_call_to_action
      WHERE page_row_call_to_action_id = deleted_cta_id
    ) INTO deleted_row_hidden;

    RAISE EXCEPTION USING
      ERRCODE = 'P0001',
      MESSAGE = 'page-builder-v2-cta-view-probe-rollback';
  EXCEPTION
    WHEN raise_exception THEN
      IF SQLERRM <> 'page-builder-v2-cta-view-probe-rollback' THEN
        RAISE;
      END IF;
  END;

  RETURN active_row_matches
    AND deleted_row_hidden
    AND timestamp_trigger_worked;
END
$$;

DROP TABLE IF EXISTS page_builder_v2_rehearsal.verification_result;
CREATE TABLE page_builder_v2_rehearsal.verification_result (
  display_order INTEGER NOT NULL,
  check_name TEXT PRIMARY KEY,
  passed BOOLEAN NOT NULL,
  details TEXT NOT NULL
);

INSERT INTO page_builder_v2_rehearsal.verification_result
SELECT
  0,
  'source_snapshot_to_local_import_verified',
  count(*) FILTER (WHERE passed = FALSE) = 0,
  format(
    '%s import check(s) passed; %s failed',
    count(*) FILTER (WHERE passed = TRUE),
    count(*) FILTER (WHERE passed = FALSE)
  )
FROM page_builder_v2_rehearsal.import_verification_result;

-- Reconstruct the migration's deterministic keeper and row ordering from the
-- imported pre-257 baseline. For active pages that had no active section, the
-- migration generates a random section UUID; in that one case the expected ID
-- must be discovered after migration. The canonical-section check below still
-- requires that exactly one correctly shaped section was created.
DROP TABLE IF EXISTS page_builder_v2_rehearsal.expected_v2_keeper_page;
CREATE TABLE page_builder_v2_rehearsal.expected_v2_keeper_page AS
WITH existing_keeper AS (
  SELECT page_id, page_section_id AS known_keeper_section_id
  FROM (
    SELECT
      section.page_id,
      section.page_section_id,
      row_number() OVER (
        PARTITION BY section.page_id
        ORDER BY section.display_order, section.page_section_id
      ) AS section_rank
    FROM page_builder_v2_rehearsal.baseline_page_section section
    WHERE section.deleted_flag = FALSE
  ) ranked
  WHERE section_rank = 1
)
SELECT
  page.page_id,
  COALESCE(
    existing.known_keeper_section_id,
    (
      SELECT current.page_section_id
      FROM page_section current
      WHERE current.page_id = page.page_id
      ORDER BY current.page_section_id
      LIMIT 1
    )
  ) AS keeper_section_id,
  existing.known_keeper_section_id IS NOT NULL AS keeper_existed_before_migration,
  COALESCE(section.created_by_account_id, page.created_by_account_id)
    AS expected_created_by_account_id
FROM page_builder_v2_rehearsal.baseline_page page
LEFT JOIN existing_keeper existing USING (page_id)
LEFT JOIN page_builder_v2_rehearsal.baseline_page_section section
  ON section.page_section_id = existing.known_keeper_section_id
WHERE page.deleted_flag = FALSE
   OR existing.known_keeper_section_id IS NOT NULL;

DROP TABLE IF EXISTS page_builder_v2_rehearsal.expected_v2_ordered_item;
CREATE TABLE page_builder_v2_rehearsal.expected_v2_ordered_item AS
WITH ordered_sections AS (
  SELECT section.*
  FROM page_builder_v2_rehearsal.baseline_page_section section
  WHERE section.deleted_flag = FALSE
),
items AS (
  SELECT
    section.page_id,
    section.page_section_id AS source_page_section_id,
    keeper.keeper_section_id,
    NULL::UUID AS existing_page_row_id,
    section.name AS expected_name,
    section.background_color_id AS expected_background_color_id,
    section.created_by_account_id AS expected_created_by_account_id,
    section.headline AS expected_headline,
    section.description AS expected_description,
    section.display_order AS section_order,
    0 AS item_order,
    0::SMALLINT AS row_order
  FROM ordered_sections section
  JOIN page_builder_v2_rehearsal.expected_v2_keeper_page keeper USING (page_id)
  WHERE COALESCE(
    NULLIF(BTRIM(section.headline), ''),
    NULLIF(BTRIM(section.description), '')
  ) IS NOT NULL

  UNION ALL

  SELECT
    section.page_id,
    section.page_section_id AS source_page_section_id,
    keeper.keeper_section_id,
    row.page_row_id AS existing_page_row_id,
    CASE
      WHEN row.row_type_id IN ('ONE_COLUMN_TEXT', 'TWO_COLUMN_TEXT') THEN 'Text'
      WHEN row.row_type_id = 'RESOURCES' THEN 'Resources'
      WHEN row.row_type_id = 'GROUP_SESSIONS' THEN 'Group Sessions'
      WHEN row.row_type_id = 'TAG_GROUP' THEN 'Tag Group'
      WHEN row.row_type_id = 'TAG' THEN 'Tag'
      WHEN row.row_type_id = 'MAILING_LIST' THEN 'Subscribe'
      ELSE 'Text & Image'
    END AS expected_name,
    section.background_color_id AS expected_background_color_id,
    row.created_by_account_id AS expected_created_by_account_id,
    NULL::TEXT AS expected_headline,
    NULL::TEXT AS expected_description,
    section.display_order AS section_order,
    1 AS item_order,
    row.display_order AS row_order
  FROM ordered_sections section
  JOIN page_builder_v2_rehearsal.expected_v2_keeper_page keeper USING (page_id)
  JOIN page_builder_v2_rehearsal.baseline_page_row row
    ON row.page_section_id = section.page_section_id
   AND row.deleted_flag = FALSE
)
SELECT
  items.*,
  (
    row_number() OVER (
      PARTITION BY items.page_id
      ORDER BY
        items.section_order,
        items.item_order,
        items.row_order,
        items.source_page_section_id,
        items.existing_page_row_id
    ) - 1
  )::INTEGER AS expected_display_order
FROM items;

INSERT INTO page_builder_v2_rehearsal.verification_result
SELECT 10, 'backup_page_fixture_scope_exact', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_backup_rows(
    'page_builder_v2_rehearsal.baseline_page'::REGCLASS,
    'page_builder_v2_257_backup_page'::REGCLASS,
    'page_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 11, 'backup_page_site_location_fixture_scope_exact', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_backup_rows(
    'page_builder_v2_rehearsal.baseline_page_site_location'::REGCLASS,
    'page_builder_v2_257_backup_page_site_location'::REGCLASS,
    'page_site_location_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 12, 'backup_page_section_fixture_scope_exact', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_backup_rows(
    'page_builder_v2_rehearsal.baseline_page_section'::REGCLASS,
    'page_builder_v2_257_backup_page_section'::REGCLASS,
    'page_section_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 13, 'backup_page_row_fixture_scope_exact', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_backup_rows(
    'page_builder_v2_rehearsal.baseline_page_row'::REGCLASS,
    'page_builder_v2_257_backup_page_row'::REGCLASS,
    'page_row_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 14, 'backup_page_row_column_fixture_scope_exact', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_backup_rows(
    'page_builder_v2_rehearsal.baseline_page_row_column'::REGCLASS,
    'page_builder_v2_257_backup_page_row_column'::REGCLASS,
    'page_row_column_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 15, 'backup_page_row_group_session_fixture_scope_exact', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_backup_rows(
    'page_builder_v2_rehearsal.baseline_page_row_group_session'::REGCLASS,
    'page_builder_v2_257_backup_page_row_group_session'::REGCLASS,
    'page_row_group_session_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 16, 'backup_page_row_content_fixture_scope_exact', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_backup_rows(
    'page_builder_v2_rehearsal.baseline_page_row_content'::REGCLASS,
    'page_builder_v2_257_backup_page_row_content'::REGCLASS,
    'page_row_content_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 17, 'backup_page_row_tag_group_fixture_scope_exact', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_backup_rows(
    'page_builder_v2_rehearsal.baseline_page_row_tag_group'::REGCLASS,
    'page_builder_v2_257_backup_page_row_tag_group'::REGCLASS,
    'page_row_tag_group_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 18, 'backup_page_row_tag_fixture_scope_exact', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_backup_rows(
    'page_builder_v2_rehearsal.baseline_page_row_tag'::REGCLASS,
    'page_builder_v2_257_backup_page_row_tag'::REGCLASS,
    'page_row_tag_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 19, 'backup_page_row_mailing_list_fixture_scope_exact', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_backup_rows(
    'page_builder_v2_rehearsal.baseline_page_row_mailing_list'::REGCLASS,
    'page_builder_v2_257_backup_page_row_mailing_list'::REGCLASS,
    'page_row_mailing_list_id'
  ) AS mismatches
) checked;

INSERT INTO page_builder_v2_rehearsal.verification_result
SELECT 20, 'page_group_unchanged', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_current_rows(
    'page_builder_v2_rehearsal.baseline_page_group'::REGCLASS,
    'page_group'::REGCLASS,
    'page_group_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 21, 'page_unchanged', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_current_rows(
    'page_builder_v2_rehearsal.baseline_page'::REGCLASS,
    'page'::REGCLASS,
    'page_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 22, 'page_site_location_unchanged', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_current_rows(
    'page_builder_v2_rehearsal.baseline_page_site_location'::REGCLASS,
    'page_site_location'::REGCLASS,
    'page_site_location_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 23, 'page_row_group_session_unchanged', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_current_rows(
    'page_builder_v2_rehearsal.baseline_page_row_group_session'::REGCLASS,
    'page_row_group_session'::REGCLASS,
    'page_row_group_session_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 24, 'page_row_content_unchanged', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_current_rows(
    'page_builder_v2_rehearsal.baseline_page_row_content'::REGCLASS,
    'page_row_content'::REGCLASS,
    'page_row_content_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 25, 'page_row_tag_group_unchanged', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_current_rows(
    'page_builder_v2_rehearsal.baseline_page_row_tag_group'::REGCLASS,
    'page_row_tag_group'::REGCLASS,
    'page_row_tag_group_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 26, 'page_row_tag_unchanged', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_current_rows(
    'page_builder_v2_rehearsal.baseline_page_row_tag'::REGCLASS,
    'page_row_tag'::REGCLASS,
    'page_row_tag_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 27, 'page_row_mailing_list_unchanged', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_current_rows(
    'page_builder_v2_rehearsal.baseline_page_row_mailing_list'::REGCLASS,
    'page_row_mailing_list'::REGCLASS,
    'page_row_mailing_list_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 27, 'referenced_file_uploads_unchanged', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_current_rows(
    'page_builder_v2_rehearsal.baseline_file_upload'::REGCLASS,
    'file_upload'::REGCLASS,
    'file_upload_id'
  ) AS mismatches
) checked
UNION ALL
SELECT 27, 'referenced_mailing_lists_unchanged', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_current_rows(
    'page_builder_v2_rehearsal.baseline_mailing_list'::REGCLASS,
    'mailing_list'::REGCLASS,
    'mailing_list_id'
  ) AS mismatches
) checked;

INSERT INTO page_builder_v2_rehearsal.verification_result
SELECT 28, 'existing_analytics_event_types_unchanged', mismatches = 0,
       format('%s differing row(s)', mismatches)
FROM (
  SELECT page_builder_v2_rehearsal.compare_current_rows(
    'page_builder_v2_rehearsal.baseline_analytics_native_event_type'::REGCLASS,
    'analytics_native_event_type'::REGCLASS,
    'analytics_native_event_type_id'
  ) AS mismatches
) checked;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH mismatches AS (
  SELECT source.page_id
  FROM page_builder_v2_rehearsal.source_page source
  JOIN page_builder_v2_rehearsal.page_url_map url_mapping USING (page_id)
  LEFT JOIN page current USING (page_id)
  LEFT JOIN page_builder_v2_rehearsal.reference_map image_mapping
    ON image_mapping.reference_type = 'FILE_UPLOAD'
   AND image_mapping.source_id = source.image_file_upload_id::TEXT
  LEFT JOIN page_builder_v2_rehearsal.source_file_upload source_file
    ON source_file.file_upload_id = source.image_file_upload_id
  LEFT JOIN file_upload current_file
    ON current_file.file_upload_id = image_mapping.target_id::UUID
  LEFT JOIN v_page current_view USING (page_id)
  WHERE current.page_id IS NULL
     OR current.url_name IS DISTINCT FROM url_mapping.target_url_name
     OR current.image_file_upload_id
          IS DISTINCT FROM image_mapping.target_id::UUID
     OR current_file.url IS DISTINCT FROM source_file.url
     OR (
       source.deleted_flag = FALSE
       AND (
         current_view.page_id IS NULL
         OR current_view.url_name IS DISTINCT FROM url_mapping.target_url_name
         OR current_view.image_url IS DISTINCT FROM source_file.url
       )
     )
     OR (source.deleted_flag = TRUE AND current_view.page_id IS NOT NULL)
)
SELECT 29, 'source_page_slugs_images_and_visibility_still_match', count(*) = 0,
       format('%s page(s) differ from the declared slug/image/visibility mapping', count(*))
FROM mismatches;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH mismatches AS (
  SELECT source.page_row_column_id
  FROM page_builder_v2_rehearsal.source_page_row_column source
  LEFT JOIN page_row_column current USING (page_row_column_id)
  LEFT JOIN page_builder_v2_rehearsal.reference_map image_mapping
    ON image_mapping.reference_type = 'FILE_UPLOAD'
   AND image_mapping.source_id = source.image_file_upload_id::TEXT
  LEFT JOIN page_builder_v2_rehearsal.source_file_upload source_file
    ON source_file.file_upload_id = source.image_file_upload_id
  LEFT JOIN v_page_row_column current_view USING (page_row_column_id)
  WHERE current.page_row_column_id IS NULL
     OR current.image_file_upload_id
          IS DISTINCT FROM image_mapping.target_id::UUID
     OR current_view.image_url IS DISTINCT FROM source_file.url
)
SELECT 29, 'source_column_image_urls_still_resolve', count(*) = 0,
       format('%s column(s) differ from the declared image mapping', count(*))
FROM mismatches;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH mismatches AS (
  SELECT count(*)::BIGINT AS mismatch_count
  FROM page_builder_v2_rehearsal.baseline_page_row baseline
  LEFT JOIN page_row current USING (page_row_id)
  WHERE current.page_row_id IS NULL
     OR current.row_type_id IS DISTINCT FROM baseline.row_type_id
     OR current.deleted_flag IS DISTINCT FROM baseline.deleted_flag
     OR current.created_by_account_id IS DISTINCT FROM baseline.created_by_account_id
     OR current.created IS DISTINCT FROM baseline.created
)
SELECT 30, 'existing_page_rows_retained', mismatch_count = 0,
       format('%s missing or payload-mismatched row(s)', mismatch_count)
FROM mismatches;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH expected AS (
  SELECT
    baseline.page_row_id,
    CASE
      WHEN baseline.deleted_flag = FALSE AND section.deleted_flag = FALSE
        THEN ordered.keeper_section_id
      ELSE COALESCE(keeper.keeper_section_id, baseline.page_section_id)
    END AS expected_page_section_id,
    CASE
      WHEN baseline.deleted_flag = FALSE AND section.deleted_flag = FALSE
        THEN ordered.expected_name
      ELSE NULL::TEXT
    END AS expected_name,
    CASE
      WHEN baseline.deleted_flag = FALSE AND section.deleted_flag = FALSE
        THEN ordered.expected_background_color_id
      ELSE 'WHITE'
    END AS expected_background_color_id,
    CASE
      WHEN baseline.deleted_flag = FALSE AND section.deleted_flag = FALSE
        THEN ordered.expected_display_order
      ELSE baseline.display_order
    END AS expected_display_order
  FROM page_builder_v2_rehearsal.baseline_page_row baseline
  JOIN page_builder_v2_rehearsal.baseline_page_section section
    ON section.page_section_id = baseline.page_section_id
  LEFT JOIN page_builder_v2_rehearsal.expected_v2_keeper_page keeper
    ON keeper.page_id = section.page_id
  LEFT JOIN page_builder_v2_rehearsal.expected_v2_ordered_item ordered
    ON ordered.existing_page_row_id = baseline.page_row_id
),
mismatches AS (
  SELECT count(*)::BIGINT AS mismatch_count
  FROM expected
  LEFT JOIN page_row current USING (page_row_id)
  WHERE current.page_row_id IS NULL
     OR current.page_section_id IS DISTINCT FROM expected.expected_page_section_id
     OR current.page_row_anchor_id IS DISTINCT FROM current.page_row_id
     OR current.name IS DISTINCT FROM expected.expected_name
     OR current.background_color_id IS DISTINCT FROM expected.expected_background_color_id
     OR current.display_order IS DISTINCT FROM expected.expected_display_order
     OR current.padding_id IS DISTINCT FROM 'MEDIUM'
     OR current.padding_top_id IS DISTINCT FROM 'MEDIUM'
     OR current.padding_bottom_id IS DISTINCT FROM 'MEDIUM'
)
SELECT 31, 'existing_rows_match_expected_v2_state', mismatch_count = 0,
       format('%s row(s) differ in keeper, anchor, name, background, order, or padding', mismatch_count)
FROM mismatches;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH duplicate_anchors AS (
  SELECT page_section_id, page_row_anchor_id, count(*) AS row_count
  FROM page_row
  GROUP BY page_section_id, page_row_anchor_id
  HAVING count(*) > 1
),
invalid AS (
  SELECT
    (SELECT count(*) FROM page_row WHERE page_row_anchor_id IS NULL)
      + COALESCE((SELECT sum(row_count - 1) FROM duplicate_anchors), 0)
      AS invalid_count
)
SELECT 31, 'page_row_anchors_populated_and_unique_within_section',
       invalid_count = 0,
       format('%s null or duplicate row anchor(s)', invalid_count)
FROM invalid;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH mismatches AS (
  SELECT count(*)::BIGINT AS mismatch_count
  FROM page_builder_v2_rehearsal.baseline_page_row_column baseline
  LEFT JOIN page_row_column current USING (page_row_column_id)
  WHERE current.page_row_column_id IS NULL
     OR current.page_row_id IS DISTINCT FROM baseline.page_row_id
     OR current.headline IS DISTINCT FROM baseline.headline
     OR current.description IS DISTINCT FROM baseline.description
     OR current.image_file_upload_id IS DISTINCT FROM baseline.image_file_upload_id
     OR current.image_alt_text IS DISTINCT FROM baseline.image_alt_text
     OR current.created IS DISTINCT FROM baseline.created
)
SELECT 32, 'existing_column_payload_retained', mismatch_count = 0,
       format('%s missing or payload-mismatched column(s)', mismatch_count)
FROM mismatches;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH expected AS (
  SELECT
    baseline.page_row_column_id,
    row_number() OVER (
      PARTITION BY baseline.page_row_id
      ORDER BY baseline.column_display_order, baseline.page_row_column_id
    ) - 1 AS expected_display_order
  FROM page_builder_v2_rehearsal.baseline_page_row_column baseline
),
mismatches AS (
  SELECT count(*)::BIGINT AS mismatch_count
  FROM expected
  LEFT JOIN page_row_column current USING (page_row_column_id)
  WHERE current.page_row_column_id IS NULL
     OR current.column_display_order <> expected.expected_display_order
)
SELECT 33, 'existing_column_order_normalized_deterministically',
       mismatch_count = 0,
       format('%s column(s) have unexpected normalized order', mismatch_count)
FROM mismatches;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH mismatches AS (
  SELECT count(*)::BIGINT AS mismatch_count
  FROM page_builder_v2_rehearsal.baseline_page_row_column baseline
  JOIN page_builder_v2_rehearsal.baseline_page_row baseline_row USING (page_row_id)
  LEFT JOIN page_row_column current USING (page_row_column_id)
  WHERE current.page_row_column_id IS NULL
     OR current.content_order_id IS DISTINCT FROM 'IMAGE_THEN_TEXT'
     OR current.use_placeholder_image IS DISTINCT FROM (
       baseline.image_file_upload_id IS NULL
       AND baseline_row.row_type_id = 'CUSTOM_ROW'
     )
)
SELECT 33, 'existing_columns_match_expected_v2_state', mismatch_count = 0,
       format('%s column(s) differ in content order or placeholder behavior', mismatch_count)
FROM mismatches;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH invalid_pages AS (
  SELECT keeper.page_id
  FROM page_builder_v2_rehearsal.expected_v2_keeper_page keeper
  LEFT JOIN page_section current ON current.page_id = keeper.page_id
  GROUP BY
    keeper.page_id,
    keeper.keeper_section_id,
    keeper.expected_created_by_account_id
  HAVING count(current.page_section_id) <> 1
     OR count(*) FILTER (
       WHERE current.page_section_id IS NOT DISTINCT FROM keeper.keeper_section_id
         AND current.name = 'Content'
         AND current.headline IS NULL
         AND current.description IS NULL
         AND current.background_color_id = 'WHITE'
         AND current.deleted_flag = FALSE
         AND current.display_order = 0
         AND current.created_by_account_id = keeper.expected_created_by_account_id
     ) <> 1
)
SELECT 34, 'keeper_section_is_unique_and_canonical', count(*) = 0,
       format('%s page(s) have an incorrect keeper or residual section', count(*))
FROM invalid_pages;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH pages_without_keeper AS (
  SELECT page.page_id
  FROM page_builder_v2_rehearsal.baseline_page page
  LEFT JOIN page_builder_v2_rehearsal.expected_v2_keeper_page keeper USING (page_id)
  WHERE keeper.page_id IS NULL
),
baseline AS (
  SELECT to_jsonb(section) AS row_data
  FROM page_builder_v2_rehearsal.baseline_page_section section
  JOIN pages_without_keeper page USING (page_id)
),
current AS (
  SELECT to_jsonb(section) AS row_data
  FROM page_section section
  JOIN pages_without_keeper page USING (page_id)
),
differences AS (
  (SELECT row_data FROM baseline EXCEPT ALL SELECT row_data FROM current)
  UNION ALL
  (SELECT row_data FROM current EXCEPT ALL SELECT row_data FROM baseline)
)
SELECT 35, 'deleted_pages_without_keeper_remain_unchanged', count(*) = 0,
       format('%s differing section row(s)', count(*))
FROM differences;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH unsafe_rows AS (
  SELECT row.page_row_id
  FROM page_builder_v2_rehearsal.baseline_page_row row
  JOIN page_builder_v2_rehearsal.baseline_page_section section USING (page_section_id)
  JOIN page_builder_v2_rehearsal.expected_v2_keeper_page keeper
    ON keeper.page_id = section.page_id
  WHERE row.deleted_flag = FALSE
    AND section.deleted_flag = TRUE
)
SELECT 35, 'no_active_rows_hidden_in_deleted_sections', count(*) = 0,
       format('%s active row(s) would become visible when their deleted section is removed', count(*))
FROM unsafe_rows;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH orphan_rows AS (
  SELECT pr.page_row_id
  FROM page_row pr
  LEFT JOIN page_section ps ON ps.page_section_id = pr.page_section_id
  WHERE ps.page_section_id IS NULL
)
SELECT 36, 'no_orphan_page_rows', count(*) = 0,
       format('%s orphan row(s)', count(*))
FROM orphan_rows;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH expected_count AS (
  SELECT
    (SELECT count(*) FROM page_builder_v2_rehearsal.baseline_page_row)
      + (SELECT count(*) FROM page_builder_v2_rehearsal.expected_section_text_row)
      AS row_count
),
actual_count AS (
  SELECT count(*) AS row_count
  FROM page_row pr
  JOIN page_section ps USING (page_section_id)
  JOIN page_builder_v2_rehearsal.source_page imported USING (page_id)
)
SELECT 37, 'post_row_count_matches_expected',
       actual.row_count = expected.row_count,
       format('expected %s row(s), found %s', expected.row_count, actual.row_count)
FROM expected_count expected, actual_count actual;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH expected_count AS (
  SELECT
    (SELECT count(*) FROM page_builder_v2_rehearsal.baseline_page_row_column)
      + (SELECT count(*) FROM page_builder_v2_rehearsal.expected_section_text_row)
      AS column_count
),
actual_count AS (
  SELECT count(*) AS column_count
  FROM page_row_column prc
  JOIN page_row pr USING (page_row_id)
  JOIN page_section ps USING (page_section_id)
  JOIN page_builder_v2_rehearsal.source_page imported USING (page_id)
)
SELECT 38, 'post_column_count_matches_expected',
       actual.column_count = expected.column_count,
       format('expected %s column(s), found %s', expected.column_count, actual.column_count)
FROM expected_count expected, actual_count actual;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH expected AS (
  SELECT
    item.page_id,
    item.keeper_section_id AS page_section_id,
    item.expected_name AS name,
    item.expected_background_color_id AS background_color_id,
    item.expected_created_by_account_id AS created_by_account_id,
    'ONE_COLUMN_TEXT'::TEXT AS row_type_id,
    FALSE AS deleted_flag,
    item.expected_display_order AS display_order,
    'MEDIUM'::TEXT AS padding_id,
    'MEDIUM'::TEXT AS padding_top_id,
    'MEDIUM'::TEXT AS padding_bottom_id,
    jsonb_build_array(
      jsonb_build_object(
        'headline', item.expected_headline,
        'description', item.expected_description,
        'image_file_upload_id', NULL,
        'image_alt_text', NULL,
        'column_display_order', 0,
        'content_order_id', 'IMAGE_THEN_TEXT',
        'use_placeholder_image', FALSE
      )
    ) AS columns
  FROM page_builder_v2_rehearsal.expected_v2_ordered_item item
  WHERE item.existing_page_row_id IS NULL
),
actual AS (
  SELECT
    ps.page_id,
    pr.page_section_id,
    pr.name,
    pr.background_color_id,
    pr.created_by_account_id,
    pr.row_type_id,
    pr.deleted_flag,
    pr.display_order,
    pr.padding_id,
    pr.padding_top_id,
    pr.padding_bottom_id,
    COALESCE(
      jsonb_agg(
        jsonb_build_object(
          'headline', prc.headline,
          'description', prc.description,
          'image_file_upload_id', prc.image_file_upload_id,
          'image_alt_text', prc.image_alt_text,
          'column_display_order', prc.column_display_order,
          'content_order_id', prc.content_order_id,
          'use_placeholder_image', prc.use_placeholder_image
        )
        ORDER BY prc.column_display_order, prc.page_row_column_id
      ) FILTER (WHERE prc.page_row_column_id IS NOT NULL),
      '[]'::JSONB
    ) AS columns
  FROM page_row pr
  JOIN page_section ps USING (page_section_id)
  JOIN page_builder_v2_rehearsal.source_page imported USING (page_id)
  LEFT JOIN page_row_column prc USING (page_row_id)
  LEFT JOIN page_builder_v2_rehearsal.baseline_page_row baseline USING (page_row_id)
  WHERE baseline.page_row_id IS NULL
  GROUP BY
    ps.page_id,
    pr.page_row_id,
    pr.page_section_id,
    pr.name,
    pr.background_color_id,
    pr.created_by_account_id,
    pr.row_type_id,
    pr.deleted_flag,
    pr.display_order,
    pr.padding_id,
    pr.padding_top_id,
    pr.padding_bottom_id
),
differences AS (
  (SELECT * FROM expected EXCEPT ALL SELECT * FROM actual)
  UNION ALL
  (SELECT * FROM actual EXCEPT ALL SELECT * FROM expected)
)
SELECT 39, 'section_text_rows_match_expected_structure', count(*) = 0,
       format('%s differing expected/actual text-row structure(s)', count(*))
FROM differences;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH scoped_columns AS (
  SELECT
    prc.page_row_id,
    prc.page_row_column_id,
    prc.column_display_order,
    row_number() OVER (
      PARTITION BY prc.page_row_id
      ORDER BY prc.column_display_order, prc.page_row_column_id
    ) - 1 AS expected_display_order
  FROM page_row_column prc
  JOIN page_row pr USING (page_row_id)
  JOIN page_section ps USING (page_section_id)
  JOIN page_builder_v2_rehearsal.source_page imported USING (page_id)
),
invalid AS (
  SELECT * FROM scoped_columns
  WHERE column_display_order <> expected_display_order
)
SELECT 40, 'column_order_unique_contiguous_zero_based', count(*) = 0,
       format('%s column(s) violate normalized order', count(*))
FROM invalid;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH invalid AS (
  SELECT prc.page_row_id
  FROM page_row_column prc
  JOIN page_row pr USING (page_row_id)
  JOIN page_section ps USING (page_section_id)
  JOIN page_builder_v2_rehearsal.source_page imported USING (page_id)
  GROUP BY prc.page_row_id
  HAVING count(*) > 4
)
SELECT 41, 'no_rows_over_four_columns', count(*) = 0,
       format('%s row(s) still contain more than four columns', count(*))
FROM invalid;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH scoped_rows AS (
  SELECT
    ps.page_id,
    pr.page_row_id,
    pr.display_order,
    row_number() OVER (
      PARTITION BY ps.page_id
      ORDER BY pr.display_order, pr.page_row_id
    ) - 1 AS expected_display_order
  FROM page_row pr
  JOIN page_section ps USING (page_section_id)
  JOIN page_builder_v2_rehearsal.source_page imported USING (page_id)
  WHERE pr.deleted_flag = FALSE
    AND ps.deleted_flag = FALSE
),
invalid AS (
  SELECT * FROM scoped_rows
  WHERE display_order <> expected_display_order
)
SELECT 42, 'active_row_order_contiguous_per_page', count(*) = 0,
       format('%s active row(s) have noncontiguous order', count(*))
FROM invalid;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH required_row_types(row_type_id, description) AS (
  VALUES
    ('ONE_COLUMN_TEXT', 'One Column Text'),
    ('TWO_COLUMN_TEXT', 'Two Column Text'),
    ('ONE_COLUMN_IMAGE_RIGHT', 'One Column Image Right'),
    ('CUSTOM_ROW', 'Custom Row'),
    ('CALL_TO_ACTION_BLOCK', 'Call-to-Action (Block)'),
    ('CALL_TO_ACTION_FULL_WIDTH', 'Call-to-Action (Full Width)')
),
missing_or_changed AS (
  SELECT required.row_type_id
  FROM required_row_types required
  LEFT JOIN row_type actual USING (row_type_id)
  WHERE actual.row_type_id IS NULL
     OR actual.description IS DISTINCT FROM required.description
)
SELECT 50, 'v2_row_types_registered_exactly', count(*) = 0,
       format('%s V2 row type(s) missing or changed', count(*))
FROM missing_or_changed;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH required_objects(relation_name, relation_kind) AS (
  VALUES
    ('page_row_call_to_action', 'r'::"char"),
    ('page_row_padding', 'r'::"char"),
    ('page_row_column_content_order', 'r'::"char"),
    ('v_page_row', 'v'::"char"),
    ('v_page_row_column', 'v'::"char"),
    ('v_page_row_call_to_action', 'v'::"char")
),
missing_or_wrong_kind AS (
  SELECT required.relation_name
  FROM required_objects required
  LEFT JOIN pg_namespace namespace ON namespace.nspname = 'cobalt'
  LEFT JOIN pg_class relation
    ON relation.relnamespace = namespace.oid
   AND relation.relname = required.relation_name
   AND relation.relkind = required.relation_kind
  WHERE relation.oid IS NULL
)
SELECT 51, 'v2_schema_objects_have_expected_kinds', count(*) = 0,
       format('%s table/view object(s) missing or the wrong relation kind', count(*))
FROM missing_or_wrong_kind;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH expected_padding(page_row_padding_id, description) AS (
  VALUES
    ('NONE', 'None'),
    ('SMALL', 'Small'),
    ('MEDIUM', 'Medium'),
    ('LARGE', 'Large')
),
expected_content_order(page_row_column_content_order_id, description) AS (
  VALUES
    ('IMAGE_THEN_TEXT', 'Image Then Text'),
    ('TEXT_THEN_IMAGE', 'Text Then Image')
),
differences AS (
  (SELECT * FROM expected_padding
   EXCEPT ALL
   SELECT page_row_padding_id, description FROM page_row_padding)
  UNION ALL
  (SELECT page_row_padding_id, description FROM page_row_padding
   EXCEPT ALL
   SELECT * FROM expected_padding)
  UNION ALL
  (SELECT * FROM expected_content_order
   EXCEPT ALL
   SELECT page_row_column_content_order_id, description
   FROM page_row_column_content_order)
  UNION ALL
  (SELECT page_row_column_content_order_id, description
   FROM page_row_column_content_order
   EXCEPT ALL
   SELECT * FROM expected_content_order)
)
SELECT 52, 'v2_lookup_values_exact', count(*) = 0,
       format('%s differing lookup row(s)', count(*))
FROM differences;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH required_primary_keys(table_name, columns) AS (
  VALUES
    ('page_row_padding', ARRAY['page_row_padding_id']::TEXT[]),
    (
      'page_row_column_content_order',
      ARRAY['page_row_column_content_order_id']::TEXT[]
    ),
    ('page_row_call_to_action', ARRAY['page_row_call_to_action_id']::TEXT[])
),
missing AS (
  SELECT required.table_name
  FROM required_primary_keys required
  WHERE NOT EXISTS (
    SELECT 1
    FROM pg_constraint constraint_record
    JOIN pg_index index_record
      ON index_record.indexrelid = constraint_record.conindid
    WHERE constraint_record.conrelid = to_regclass(
        format('cobalt.%I', required.table_name)
      )
      AND constraint_record.contype = 'p'
      AND constraint_record.convalidated
      AND (
        SELECT array_agg(column_record.attname::TEXT ORDER BY key_record.ordinality)
        FROM unnest(constraint_record.conkey) WITH ORDINALITY
          AS key_record(attnum, ordinality)
        JOIN pg_attribute column_record
          ON column_record.attrelid = constraint_record.conrelid
         AND column_record.attnum = key_record.attnum
      ) = required.columns
      AND index_record.indisprimary
      AND index_record.indisunique
      AND index_record.indisvalid
      AND index_record.indisready
  )
)
SELECT 53, 'v2_primary_keys_present', count(*) = 0,
       format('%s primary key(s) missing or changed', count(*))
FROM missing;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH required_columns(
  table_name,
  column_name,
  type_oid,
  not_null,
  default_expression
) AS (
  VALUES
    ('page_row', 'name', to_regtype('text')::OID, FALSE, NULL::TEXT),
    ('page_row', 'background_color_id', to_regtype('text')::OID, TRUE, '''WHITE''::text'),
    ('page_row', 'padding_id', to_regtype('text')::OID, TRUE, '''MEDIUM''::text'),
    ('page_row', 'padding_top_id', to_regtype('text')::OID, TRUE, '''MEDIUM''::text'),
    ('page_row', 'padding_bottom_id', to_regtype('text')::OID, TRUE, '''MEDIUM''::text'),
    ('page_row', 'page_row_anchor_id', to_regtype('uuid')::OID, TRUE, 'uuid_generate_v4()'),
    ('page_row_column', 'content_order_id', to_regtype('text')::OID, TRUE, '''IMAGE_THEN_TEXT''::text'),
    ('page_row_column', 'use_placeholder_image', to_regtype('boolean')::OID, TRUE, 'false'),
    ('page_row_call_to_action', 'page_row_call_to_action_id', to_regtype('uuid')::OID, TRUE, 'uuid_generate_v4()'),
    ('page_row_call_to_action', 'page_row_id', to_regtype('uuid')::OID, TRUE, NULL::TEXT),
    ('page_row_call_to_action', 'headline', to_regtype('text')::OID, TRUE, NULL::TEXT),
    ('page_row_call_to_action', 'description', to_regtype('text')::OID, TRUE, NULL::TEXT),
    ('page_row_call_to_action', 'button_text', to_regtype('text')::OID, TRUE, NULL::TEXT),
    ('page_row_call_to_action', 'button_url', to_regtype('text')::OID, TRUE, NULL::TEXT),
    ('page_row_call_to_action', 'image_file_upload_id', to_regtype('uuid')::OID, FALSE, NULL::TEXT),
    ('page_row_call_to_action', 'created', to_regtype('timestamptz')::OID, TRUE, 'now()'),
    ('page_row_call_to_action', 'last_updated', to_regtype('timestamptz')::OID, TRUE, 'now()')
),
actual AS (
  SELECT
    required.*,
    column_record.attname AS actual_column_name,
    column_record.atttypid AS actual_type_oid,
    column_record.attnotnull AS actual_not_null,
    pg_get_expr(default_record.adbin, default_record.adrelid)
      AS actual_default_expression
  FROM required_columns required
  LEFT JOIN pg_class table_record
    ON table_record.oid = to_regclass(format('cobalt.%I', required.table_name))
  LEFT JOIN pg_attribute column_record
    ON column_record.attrelid = table_record.oid
   AND column_record.attname = required.column_name
   AND column_record.attnum > 0
   AND NOT column_record.attisdropped
  LEFT JOIN pg_attrdef default_record
    ON default_record.adrelid = column_record.attrelid
   AND default_record.adnum = column_record.attnum
),
missing_or_changed AS (
  SELECT table_name, column_name
  FROM actual
  WHERE actual_column_name IS NULL
     OR actual_type_oid IS DISTINCT FROM type_oid
     OR actual_not_null IS DISTINCT FROM not_null
     OR actual_default_expression IS DISTINCT FROM default_expression
)
SELECT 55, 'v2_columns_defaults_and_nullability_exact', count(*) = 0,
       format('%s column definition(s) missing or changed', count(*))
FROM missing_or_changed;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH required_foreign_keys(
  table_name,
  column_name,
  referenced_table_name,
  referenced_column_name
) AS (
  VALUES
    ('page_row', 'background_color_id', 'background_color', 'background_color_id'),
    ('page_row', 'padding_id', 'page_row_padding', 'page_row_padding_id'),
    ('page_row', 'padding_top_id', 'page_row_padding', 'page_row_padding_id'),
    ('page_row', 'padding_bottom_id', 'page_row_padding', 'page_row_padding_id'),
    ('page_row_column', 'content_order_id', 'page_row_column_content_order', 'page_row_column_content_order_id'),
    ('page_row_call_to_action', 'page_row_id', 'page_row', 'page_row_id'),
    ('page_row_call_to_action', 'image_file_upload_id', 'file_upload', 'file_upload_id')
),
missing AS (
  SELECT required.*
  FROM required_foreign_keys required
  WHERE NOT EXISTS (
    SELECT 1
    FROM pg_constraint constraint_record
    WHERE constraint_record.contype = 'f'
      AND constraint_record.conrelid = to_regclass(
        format('cobalt.%I', required.table_name)
      )
      AND constraint_record.confrelid = to_regclass(
        format('cobalt.%I', required.referenced_table_name)
      )
      AND (
        SELECT array_agg(column_record.attname::TEXT ORDER BY key_record.ordinality)
        FROM unnest(constraint_record.conkey) WITH ORDINALITY
          AS key_record(attnum, ordinality)
        JOIN pg_attribute column_record
          ON column_record.attrelid = constraint_record.conrelid
         AND column_record.attnum = key_record.attnum
      ) = ARRAY[required.column_name]::TEXT[]
      AND (
        SELECT array_agg(column_record.attname::TEXT ORDER BY key_record.ordinality)
        FROM unnest(constraint_record.confkey) WITH ORDINALITY
          AS key_record(attnum, ordinality)
        JOIN pg_attribute column_record
          ON column_record.attrelid = constraint_record.confrelid
         AND column_record.attnum = key_record.attnum
      ) = ARRAY[required.referenced_column_name]::TEXT[]
      AND constraint_record.convalidated
      AND NOT constraint_record.condeferrable
      AND NOT constraint_record.condeferred
      AND constraint_record.confmatchtype = 's'
      AND constraint_record.confupdtype = 'a'
      AND constraint_record.confdeltype = 'a'
  )
)
SELECT 56, 'v2_foreign_keys_present', count(*) = 0,
       format('%s required foreign key(s) missing', count(*))
FROM missing;

INSERT INTO page_builder_v2_rehearsal.verification_result
SELECT 57, 'column_invariants_are_enforced',
       EXISTS (
         SELECT 1
         FROM pg_constraint constraint_record
         JOIN pg_index index_record
           ON index_record.indexrelid = constraint_record.conindid
         WHERE constraint_record.conrelid = 'cobalt.page_row_column'::REGCLASS
           AND constraint_record.conname =
             'page_row_column_page_row_id_column_display_order_key'
           AND constraint_record.contype = 'u'
           AND constraint_record.condeferrable
           AND NOT constraint_record.condeferred
           AND constraint_record.convalidated
           AND (
             SELECT array_agg(column_record.attname::TEXT ORDER BY key_record.ordinality)
             FROM unnest(constraint_record.conkey) WITH ORDINALITY
               AS key_record(attnum, ordinality)
             JOIN pg_attribute column_record
               ON column_record.attrelid = constraint_record.conrelid
              AND column_record.attnum = key_record.attnum
           ) = ARRAY['page_row_id', 'column_display_order']::TEXT[]
           AND index_record.indisunique
           AND index_record.indisvalid
           AND index_record.indisready
           AND NOT index_record.indimmediate
           AND index_record.indpred IS NULL
           AND index_record.indexprs IS NULL
       )
       AND EXISTS (
         SELECT 1
         FROM pg_trigger trigger_record
         WHERE trigger_record.tgrelid = 'cobalt.page_row_column'::REGCLASS
           AND trigger_record.tgname = 'page_row_column_enforce_limit'
           AND NOT trigger_record.tgisinternal
           AND trigger_record.tgfoid = to_regprocedure(
             'cobalt.page_row_column_enforce_limit()'
           )
           AND trigger_record.tgtype = 23
           AND trigger_record.tgenabled = 'O'
           AND trigger_record.tgqual IS NULL
           AND COALESCE(
             (
               SELECT array_agg(column_record.attname::TEXT ORDER BY key_record.ordinality)
               FROM unnest(trigger_record.tgattr::SMALLINT[]) WITH ORDINALITY
                 AS key_record(attnum, ordinality)
               JOIN pg_attribute column_record
                 ON column_record.attrelid = trigger_record.tgrelid
                AND column_record.attnum = key_record.attnum
             ),
             ARRAY[]::TEXT[]
           ) = ARRAY['page_row_id']::TEXT[]
       ),
       'deferrable column-order uniqueness and the four-column trigger must be active';

INSERT INTO page_builder_v2_rehearsal.verification_result
SELECT 57, 'page_row_anchor_uniqueness_is_enforced',
       EXISTS (
         SELECT 1
         FROM pg_constraint constraint_record
         JOIN pg_index index_record
           ON index_record.indexrelid = constraint_record.conindid
         WHERE constraint_record.conrelid = 'cobalt.page_row'::REGCLASS
           AND constraint_record.conname =
             'page_row_page_section_id_page_row_anchor_id_key'
           AND constraint_record.contype = 'u'
           AND NOT constraint_record.condeferrable
           AND NOT constraint_record.condeferred
           AND constraint_record.convalidated
           AND (
             SELECT array_agg(column_record.attname::TEXT ORDER BY key_record.ordinality)
             FROM unnest(constraint_record.conkey) WITH ORDINALITY
               AS key_record(attnum, ordinality)
             JOIN pg_attribute column_record
               ON column_record.attrelid = constraint_record.conrelid
              AND column_record.attnum = key_record.attnum
           ) = ARRAY['page_section_id', 'page_row_anchor_id']::TEXT[]
           AND index_record.indisunique
           AND index_record.indisvalid
           AND index_record.indisready
           AND index_record.indimmediate
           AND index_record.indpred IS NULL
           AND index_record.indexprs IS NULL
       ),
       'row anchors must be unique within each concrete page section';

INSERT INTO page_builder_v2_rehearsal.verification_result
SELECT
  57,
  'column_limit_trigger_rejects_fifth_and_reparent',
  passed,
  'A fifth column and a move into a four-column row must both raise check_violation'
FROM (
  SELECT page_builder_v2_rehearsal.probe_column_limit_trigger() AS passed
) probe;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH
cta_primary_key AS (
  SELECT 1
  FROM pg_constraint constraint_record
  WHERE constraint_record.conrelid = 'cobalt.page_row_call_to_action'::REGCLASS
    AND constraint_record.contype = 'p'
    AND constraint_record.convalidated
    AND (
      SELECT array_agg(column_record.attname::TEXT ORDER BY key_record.ordinality)
      FROM unnest(constraint_record.conkey) WITH ORDINALITY
        AS key_record(attnum, ordinality)
      JOIN pg_attribute column_record
        ON column_record.attrelid = constraint_record.conrelid
       AND column_record.attnum = key_record.attnum
    ) = ARRAY['page_row_call_to_action_id']::TEXT[]
)
SELECT 58, 'cta_constraints_and_timestamp_trigger_present',
       EXISTS (
         SELECT 1
         FROM pg_index index_record
         JOIN pg_class index_relation
           ON index_relation.oid = index_record.indexrelid
         JOIN pg_am access_method ON access_method.oid = index_relation.relam
         WHERE index_relation.oid =
             to_regclass('cobalt.idx_page_row_call_to_action_row')
           AND index_relation.relkind = 'i'
           AND index_relation.relpersistence = 'p'
           AND index_record.indrelid =
             'cobalt.page_row_call_to_action'::REGCLASS
           AND index_record.indisunique
           AND NOT index_record.indisprimary
           AND index_record.indisvalid
           AND index_record.indisready
           AND index_record.indimmediate
           AND index_record.indnkeyatts = 1
           AND index_record.indnatts = 1
           AND index_record.indpred IS NULL
           AND index_record.indexprs IS NULL
           AND access_method.amname = 'btree'
           AND (
             SELECT array_agg(column_record.attname::TEXT ORDER BY key_record.ordinality)
             FROM unnest(index_record.indkey::SMALLINT[]) WITH ORDINALITY
               AS key_record(attnum, ordinality)
             JOIN pg_attribute column_record
               ON column_record.attrelid = index_record.indrelid
              AND column_record.attnum = key_record.attnum
           ) = ARRAY['page_row_id']::TEXT[]
       )
       AND EXISTS (
         SELECT 1
         FROM pg_trigger trigger_record
         WHERE trigger_record.tgrelid =
             'cobalt.page_row_call_to_action'::REGCLASS
           AND trigger_record.tgname = 'set_last_updated'
           AND NOT trigger_record.tgisinternal
           AND trigger_record.tgfoid = to_regprocedure('cobalt.set_last_updated()')
           AND trigger_record.tgtype = 23
           AND trigger_record.tgenabled = 'O'
           AND trigger_record.tgqual IS NULL
           AND COALESCE(
             (
               SELECT array_agg(column_record.attname::TEXT ORDER BY key_record.ordinality)
               FROM unnest(trigger_record.tgattr::SMALLINT[]) WITH ORDINALITY
                 AS key_record(attnum, ordinality)
               JOIN pg_attribute column_record
                 ON column_record.attrelid = trigger_record.tgrelid
                AND column_record.attnum = key_record.attnum
             ),
             ARRAY[]::TEXT[]
           ) = ARRAY[]::TEXT[]
       )
       AND EXISTS (SELECT 1 FROM cta_primary_key),
       'CTA row uniqueness, primary key, and last-updated trigger must be active';

INSERT INTO page_builder_v2_rehearsal.verification_result
SELECT
  58,
  'cta_view_and_timestamp_trigger_behavior_work',
  passed,
  'CTA trigger must refresh timestamps; view must resolve payload and hide deleted rows'
FROM (
  SELECT page_builder_v2_rehearsal.probe_cta_view() AS passed
) probe;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH expected(view_name, columns) AS (
  VALUES
    (
      'v_page_row',
      ARRAY[
        'page_row_id', 'page_section_id', 'row_type_id', 'deleted_flag',
        'display_order', 'created_by_account_id', 'created', 'last_updated',
        'institution_id', 'name', 'background_color_id', 'padding_top_id',
        'padding_bottom_id', 'page_row_anchor_id'
      ]::TEXT[]
    ),
    (
      'v_page_row_column',
      ARRAY[
        'page_row_column_id', 'page_row_id', 'headline', 'description',
        'image_file_upload_id', 'image_alt_text', 'column_display_order',
        'created', 'last_updated', 'content_order_id',
        'use_placeholder_image', 'image_url'
      ]::TEXT[]
    ),
    (
      'v_page_row_call_to_action',
      ARRAY[
        'page_row_call_to_action_id', 'page_row_id', 'page_section_id',
        'display_order', 'row_type_id', 'headline', 'description',
        'button_text', 'button_url', 'image_file_upload_id', 'image_url',
        'created', 'last_updated'
      ]::TEXT[]
    )
),
actual AS (
  SELECT
    columns.table_name AS view_name,
    array_agg(columns.column_name::TEXT ORDER BY columns.ordinal_position) AS columns
  FROM information_schema.columns columns
  WHERE columns.table_schema = 'cobalt'
    AND columns.table_name IN (
      'v_page_row',
      'v_page_row_column',
      'v_page_row_call_to_action'
    )
  GROUP BY columns.table_name
),
missing_or_changed AS (
  SELECT expected.view_name
  FROM expected
  LEFT JOIN actual USING (view_name)
  WHERE actual.columns IS DISTINCT FROM expected.columns
)
SELECT 59, 'v2_view_column_contracts_exact', count(*) = 0,
       format('%s view column contract(s) missing or changed', count(*))
FROM missing_or_changed;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH expected_mapping(
  view_name,
  ordinal_position,
  view_column_name,
  source_table_name,
  source_column_name
) AS (
  VALUES
    ('v_page_row', 1, 'page_row_id', 'page_row', 'page_row_id'),
    ('v_page_row', 2, 'page_section_id', 'page_row', 'page_section_id'),
    ('v_page_row', 3, 'row_type_id', 'page_row', 'row_type_id'),
    ('v_page_row', 4, 'deleted_flag', 'page_row', 'deleted_flag'),
    ('v_page_row', 5, 'display_order', 'page_row', 'display_order'),
    ('v_page_row', 6, 'created_by_account_id',
      'page_row', 'created_by_account_id'),
    ('v_page_row', 7, 'created', 'page_row', 'created'),
    ('v_page_row', 8, 'last_updated', 'page_row', 'last_updated'),
    ('v_page_row', 9, 'institution_id', 'page', 'institution_id'),
    ('v_page_row', 10, 'name', 'page_row', 'name'),
    ('v_page_row', 11, 'background_color_id',
      'page_row', 'background_color_id'),
    ('v_page_row', 12, 'padding_top_id', 'page_row', 'padding_top_id'),
    ('v_page_row', 13, 'padding_bottom_id',
      'page_row', 'padding_bottom_id'),
    ('v_page_row', 14, 'page_row_anchor_id',
      'page_row', 'page_row_anchor_id'),
    ('v_page_row_column', 1, 'page_row_column_id',
      'page_row_column', 'page_row_column_id'),
    ('v_page_row_column', 2, 'page_row_id',
      'page_row_column', 'page_row_id'),
    ('v_page_row_column', 3, 'headline',
      'page_row_column', 'headline'),
    ('v_page_row_column', 4, 'description',
      'page_row_column', 'description'),
    ('v_page_row_column', 5, 'image_file_upload_id',
      'page_row_column', 'image_file_upload_id'),
    ('v_page_row_column', 6, 'image_alt_text',
      'page_row_column', 'image_alt_text'),
    ('v_page_row_column', 7, 'column_display_order',
      'page_row_column', 'column_display_order'),
    ('v_page_row_column', 8, 'created',
      'page_row_column', 'created'),
    ('v_page_row_column', 9, 'last_updated',
      'page_row_column', 'last_updated'),
    ('v_page_row_column', 10, 'content_order_id',
      'page_row_column', 'content_order_id'),
    ('v_page_row_column', 11, 'use_placeholder_image',
      'page_row_column', 'use_placeholder_image'),
    ('v_page_row_column', 12, 'image_url', 'file_upload', 'url')
),
expected AS (
  SELECT
    mapping.view_name,
    mapping.ordinal_position,
    mapping.view_column_name,
    source_attribute.atttypid,
    source_attribute.atttypmod,
    source_attribute.attcollation
  FROM expected_mapping mapping
  LEFT JOIN pg_attribute source_attribute
    ON source_attribute.attrelid = to_regclass(
         format('cobalt.%I', mapping.source_table_name)
       )
   AND source_attribute.attname::TEXT = mapping.source_column_name
   AND source_attribute.attnum > 0
   AND NOT source_attribute.attisdropped
),
actual AS (
  SELECT
    view_relation.relname::TEXT AS view_name,
    view_attribute.attnum::INTEGER AS ordinal_position,
    view_attribute.attname::TEXT AS view_column_name,
    view_attribute.atttypid,
    view_attribute.atttypmod,
    view_attribute.attcollation
  FROM pg_class view_relation
  JOIN pg_namespace view_namespace
    ON view_namespace.oid = view_relation.relnamespace
  JOIN pg_attribute view_attribute
    ON view_attribute.attrelid = view_relation.oid
   AND view_attribute.attnum > 0
   AND NOT view_attribute.attisdropped
  WHERE view_namespace.nspname = 'cobalt'
    AND view_relation.relkind = 'v'
    AND view_relation.relname IN ('v_page_row', 'v_page_row_column')
),
differences AS (
  (SELECT * FROM expected EXCEPT ALL SELECT * FROM actual)
  UNION ALL
  (SELECT * FROM actual EXCEPT ALL SELECT * FROM expected)
)
SELECT 59, 'v2_row_view_column_types_exact', count(*) = 0,
       format('%s differing view catalog row(s)', count(*))
FROM differences;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH expected AS (
  SELECT to_jsonb(expected_row) AS row_data
  FROM (
    SELECT
      row.page_row_id,
      row.page_section_id,
      row.row_type_id,
      row.deleted_flag,
      row.display_order,
      row.created_by_account_id,
      row.created,
      row.last_updated,
      page.institution_id,
      row.name,
      row.background_color_id,
      row.padding_top_id,
      row.padding_bottom_id,
      row.page_row_anchor_id
    FROM page_row row
    JOIN page_section section USING (page_section_id)
    JOIN page USING (page_id)
    WHERE row.deleted_flag = FALSE
  ) expected_row
),
actual AS (
  SELECT to_jsonb(view_row) AS row_data
  FROM v_page_row view_row
),
differences AS (
  (SELECT row_data FROM expected EXCEPT ALL SELECT row_data FROM actual)
  UNION ALL
  (SELECT row_data FROM actual EXCEPT ALL SELECT row_data FROM expected)
)
SELECT 59, 'v_page_row_matches_base_tables', count(*) = 0,
       format('%s differing expected/actual view row(s)', count(*))
FROM differences;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH expected AS (
  SELECT to_jsonb(expected_row) AS row_data
  FROM (
    SELECT column_record.*, file_upload.url AS image_url
    FROM page_row_column column_record
    LEFT JOIN file_upload
      ON file_upload.file_upload_id = column_record.image_file_upload_id
  ) expected_row
),
actual AS (
  SELECT to_jsonb(view_row) AS row_data
  FROM v_page_row_column view_row
),
differences AS (
  (SELECT row_data FROM expected EXCEPT ALL SELECT row_data FROM actual)
  UNION ALL
  (SELECT row_data FROM actual EXCEPT ALL SELECT row_data FROM expected)
)
SELECT 59, 'v_page_row_column_matches_base_tables', count(*) = 0,
       format('%s differing expected/actual view row(s)', count(*))
FROM differences;

INSERT INTO page_builder_v2_rehearsal.verification_result
SELECT
  59,
  'active_imported_tag_rows_have_exactly_one_association',
  count(*) = 0,
  format('%s active imported TAG row(s) have an association count other than one', count(*))
FROM (
  SELECT current_row.page_row_id
  FROM page_row current_row
  JOIN page_builder_v2_rehearsal.baseline_page_row imported_row
    USING (page_row_id)
  LEFT JOIN page_row_tag association USING (page_row_id)
  WHERE current_row.row_type_id = 'TAG'
    AND current_row.deleted_flag = FALSE
  GROUP BY current_row.page_row_id
  HAVING count(association.page_row_tag_id) <> 1
) anomalies;

INSERT INTO page_builder_v2_rehearsal.verification_result
SELECT
  59,
  'active_imported_tag_group_rows_have_exactly_one_association',
  count(*) = 0,
  format('%s active imported TAG_GROUP row(s) have an association count other than one', count(*))
FROM (
  SELECT current_row.page_row_id
  FROM page_row current_row
  JOIN page_builder_v2_rehearsal.baseline_page_row imported_row
    USING (page_row_id)
  LEFT JOIN page_row_tag_group association USING (page_row_id)
  WHERE current_row.row_type_id = 'TAG_GROUP'
    AND current_row.deleted_flag = FALSE
  GROUP BY current_row.page_row_id
  HAVING count(association.page_row_tag_group_id) <> 1
) anomalies;

INSERT INTO page_builder_v2_rehearsal.verification_result
WITH expected AS (
  SELECT analytics_native_event_type_id, description
  FROM page_builder_v2_rehearsal.baseline_analytics_native_event_type
  UNION ALL
  SELECT
    'CLICKTHROUGH_PAGE_CALL_TO_ACTION'::TEXT,
    'Clickthrough (Page Call to Action)'::VARCHAR
),
actual AS (
  SELECT analytics_native_event_type_id, description
  FROM analytics_native_event_type
),
differences AS (
  (SELECT * FROM expected EXCEPT ALL SELECT * FROM actual)
  UNION ALL
  (SELECT * FROM actual EXCEPT ALL SELECT * FROM expected)
)
SELECT 60, 'analytics_event_types_preserved_and_extended_exactly', count(*) = 0,
       format('%s missing, changed, or unexpected analytics event row(s)', count(*))
FROM differences;

INSERT INTO page_builder_v2_rehearsal.verification_result
SELECT 61, 'migration_patch_registered', EXISTS (
  SELECT 1 FROM _v.patches WHERE patch_name = '257-page-builder-v2'
), '257-page-builder-v2 must be recorded in _v.patches';

\echo ''
\echo 'Page Builder V2 rehearsal verification results:'
SELECT
  CASE WHEN passed THEN 'PASS' ELSE 'FAIL' END AS result,
  check_name,
  details
FROM page_builder_v2_rehearsal.verification_result
ORDER BY display_order, check_name;

SELECT EXISTS (
  SELECT 1
  FROM page_builder_v2_rehearsal.verification_result
  WHERE NOT passed
) AS page_rehearsal_has_failures \gset

\if :page_rehearsal_has_failures
  DO $$
  BEGIN
    RAISE EXCEPTION 'Page Builder V2 rehearsal verification failed. Review page_builder_v2_rehearsal.verification_result.';
  END
  $$;
\else
  \echo ''
  \echo 'All Page Builder V2 rehearsal checks passed.'
\endif
