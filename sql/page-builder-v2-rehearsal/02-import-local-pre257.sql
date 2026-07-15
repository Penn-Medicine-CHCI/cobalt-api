\set ON_ERROR_STOP on

\if :{?confirm_local_import}
\else
  DO $$
  BEGIN
    RAISE EXCEPTION 'Pass -v confirm_local_import=YES after verifying this is a disposable local database';
  END
  $$;
\endif

SELECT upper(:'confirm_local_import') = 'YES' AS page_rehearsal_confirmed \gset
\if :page_rehearsal_confirmed
\else
  DO $$
  BEGIN
    RAISE EXCEPTION 'confirm_local_import must equal YES';
  END
  $$;
\endif

\if :{?target_institution_id}
\else
  DO $$
  BEGIN
    RAISE EXCEPTION 'Pass -v target_institution_id=...';
  END
  $$;
\endif

\if :{?target_account_id}
\else
  DO $$
  BEGIN
    RAISE EXCEPTION 'Pass -v target_account_id=...';
  END
  $$;
\endif

\if :{?include_external_associations}
\else
  \set include_external_associations 'true'
\endif

\if :{?url_prefix}
\else
  \set url_prefix ''
\endif

SET search_path TO cobalt, public;
BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM _v.patches
    WHERE patch_name = '256-autism-clinic-penn-updates'
  ) THEN
    RAISE EXCEPTION 'Patch 256 must be installed before importing the rehearsal fixture';
  END IF;

  IF EXISTS (
    SELECT 1 FROM _v.patches WHERE patch_name = '257-page-builder-v2'
  ) OR EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'cobalt'
      AND table_name = 'page_row'
      AND column_name IN ('name', 'padding_top_id', 'padding_bottom_id')
  ) THEN
    RAISE EXCEPTION 'This database already has migration 257; recreate it through patch 256';
  END IF;
END
$$;

DROP SCHEMA IF EXISTS page_builder_v2_rehearsal CASCADE;
CREATE SCHEMA page_builder_v2_rehearsal;

CREATE TABLE page_builder_v2_rehearsal.settings (
  target_institution_id TEXT NOT NULL,
  target_account_id UUID NOT NULL,
  include_external_associations BOOLEAN NOT NULL,
  url_prefix TEXT NOT NULL
);

INSERT INTO page_builder_v2_rehearsal.settings (
  target_institution_id,
  target_account_id,
  include_external_associations,
  url_prefix
)
VALUES (
  :'target_institution_id',
  :'target_account_id'::UUID,
  :'include_external_associations'::BOOLEAN,
  :'url_prefix'
);

CREATE TABLE page_builder_v2_rehearsal.created_local_object (
  object_type TEXT NOT NULL,
  object_id TEXT NOT NULL,
  PRIMARY KEY (object_type, object_id)
);

DO $$
DECLARE
  configured page_builder_v2_rehearsal.settings%ROWTYPE;
BEGIN
  SELECT * INTO STRICT configured FROM page_builder_v2_rehearsal.settings;

  IF NOT EXISTS (
    SELECT 1 FROM institution
    WHERE institution_id = configured.target_institution_id
  ) THEN
    RAISE EXCEPTION 'Target institution % does not exist',
      configured.target_institution_id;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM account
    WHERE account_id = configured.target_account_id
      AND institution_id = configured.target_institution_id
  ) THEN
    RAISE EXCEPTION 'Target account % does not exist in institution %',
      configured.target_account_id,
      configured.target_institution_id;
  END IF;

END
$$;

CREATE TABLE page_builder_v2_rehearsal.export_info (
  exported_at TIMESTAMPTZ NOT NULL,
  source_institution_id TEXT NOT NULL,
  expected_schema_state TEXT NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.export_complete (
  completed_at TIMESTAMPTZ NOT NULL,
  expected_file_count INTEGER NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_page_group (
  page_group_id UUID NOT NULL,
  analytics_campaign_key TEXT
);

CREATE TABLE page_builder_v2_rehearsal.source_page (
  page_id UUID NOT NULL,
  name TEXT NOT NULL,
  url_name TEXT NOT NULL,
  page_status_id TEXT NOT NULL,
  headline TEXT,
  description TEXT,
  image_file_upload_id UUID,
  image_alt_text TEXT,
  published_date TIMESTAMPTZ,
  deleted_flag BOOLEAN NOT NULL,
  institution_id TEXT NOT NULL,
  parent_page_id UUID,
  created_by_account_id UUID NOT NULL,
  created TIMESTAMPTZ NOT NULL,
  last_updated TIMESTAMPTZ NOT NULL,
  page_group_id UUID NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_page_site_location (
  page_site_location_id UUID NOT NULL,
  page_id UUID NOT NULL,
  site_location_id TEXT,
  display_order SMALLINT NOT NULL,
  publish_start_date TIMESTAMPTZ,
  publish_end_date TIMESTAMPTZ,
  call_to_action TEXT NOT NULL,
  created_by_account_id UUID NOT NULL,
  created TIMESTAMPTZ NOT NULL,
  last_updated TIMESTAMPTZ NOT NULL,
  short_description TEXT,
  icon_name TEXT
);

CREATE TABLE page_builder_v2_rehearsal.source_page_section (
  page_section_id UUID NOT NULL,
  page_id UUID NOT NULL,
  name TEXT NOT NULL,
  headline TEXT,
  description TEXT,
  background_color_id TEXT NOT NULL,
  deleted_flag BOOLEAN NOT NULL,
  display_order SMALLINT NOT NULL,
  created_by_account_id UUID NOT NULL,
  created TIMESTAMPTZ NOT NULL,
  last_updated TIMESTAMPTZ NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_page_row (
  page_row_id UUID NOT NULL,
  page_section_id UUID NOT NULL,
  row_type_id TEXT NOT NULL,
  deleted_flag BOOLEAN NOT NULL,
  display_order SMALLINT NOT NULL,
  created_by_account_id UUID NOT NULL,
  created TIMESTAMPTZ NOT NULL,
  last_updated TIMESTAMPTZ NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_page_row_column (
  page_row_column_id UUID NOT NULL,
  page_row_id UUID NOT NULL,
  headline TEXT,
  description TEXT,
  image_file_upload_id UUID,
  image_alt_text TEXT,
  column_display_order SMALLINT NOT NULL,
  created TIMESTAMPTZ NOT NULL,
  last_updated TIMESTAMPTZ NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_page_row_group_session (
  page_row_group_session_id UUID NOT NULL,
  page_row_id UUID NOT NULL,
  group_session_id UUID NOT NULL,
  group_session_display_order SMALLINT NOT NULL,
  created TIMESTAMPTZ NOT NULL,
  last_updated TIMESTAMPTZ NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_page_row_content (
  page_row_content_id UUID NOT NULL,
  page_row_id UUID NOT NULL,
  content_id UUID NOT NULL,
  content_display_order SMALLINT NOT NULL,
  created TIMESTAMPTZ NOT NULL,
  last_updated TIMESTAMPTZ NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_page_row_tag_group (
  page_row_tag_group_id UUID NOT NULL,
  page_row_id UUID NOT NULL,
  tag_group_id TEXT NOT NULL,
  created TIMESTAMPTZ NOT NULL,
  last_updated TIMESTAMPTZ NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_page_row_tag (
  page_row_tag_id UUID NOT NULL,
  page_row_id UUID NOT NULL,
  tag_id TEXT NOT NULL,
  created TIMESTAMPTZ NOT NULL,
  last_updated TIMESTAMPTZ NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_page_row_mailing_list (
  page_row_mailing_list_id UUID NOT NULL,
  page_row_id UUID NOT NULL,
  mailing_list_id UUID NOT NULL,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  created TIMESTAMPTZ NOT NULL,
  last_updated TIMESTAMPTZ NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_file_upload (
  file_upload_id UUID NOT NULL,
  account_id UUID NOT NULL,
  url TEXT NOT NULL,
  storage_key TEXT NOT NULL,
  filename TEXT NOT NULL,
  content_type TEXT NOT NULL,
  created TIMESTAMPTZ NOT NULL,
  last_updated TIMESTAMPTZ NOT NULL,
  file_upload_type_id TEXT NOT NULL,
  filesize NUMERIC,
  remote_data_flag BOOLEAN NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_mailing_list (
  mailing_list_id UUID NOT NULL,
  institution_id TEXT NOT NULL,
  created_by_account_id UUID NOT NULL,
  created TIMESTAMPTZ NOT NULL,
  last_updated TIMESTAMPTZ NOT NULL
);

-- This is the editable mapping template from the source bundle. It remains
-- immutable after load. Resolved local mappings are stored separately below.
CREATE TABLE page_builder_v2_rehearsal.source_reference_map (
  reference_type TEXT NOT NULL,
  source_id TEXT NOT NULL,
  target_id TEXT NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_manifest (
  table_name TEXT NOT NULL,
  row_count BIGINT NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_shape_metric (
  metric TEXT NOT NULL,
  value BIGINT NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_column_anomaly (
  page_row_id UUID NOT NULL,
  column_count BIGINT NOT NULL,
  distinct_order_count BIGINT NOT NULL,
  minimum_order SMALLINT,
  maximum_order SMALLINT,
  over_four_columns BOOLEAN NOT NULL,
  order_requires_normalization BOOLEAN NOT NULL
);

CREATE TABLE page_builder_v2_rehearsal.source_sensitive_text_count (
  field_name TEXT NOT NULL,
  email_like BIGINT NOT NULL,
  phone_like BIGINT NOT NULL,
  link_like BIGINT NOT NULL
);

\echo 'Loading CSV bundle from the current directory...'

\copy page_builder_v2_rehearsal.export_info FROM '00-export-info.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_page_group FROM '01-page-group.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_page FROM '02-page.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_page_site_location FROM '03-page-site-location.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_page_section FROM '04-page-section.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_page_row FROM '05-page-row.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_page_row_column FROM '06-page-row-column.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_page_row_group_session FROM '07-page-row-group-session.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_page_row_content FROM '08-page-row-content.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_page_row_tag_group FROM '09-page-row-tag-group.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_page_row_tag FROM '10-page-row-tag.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_page_row_mailing_list FROM '11-page-row-mailing-list.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_file_upload FROM '12-file-upload.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_mailing_list FROM '13-mailing-list.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_reference_map FROM '70-reference-map.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_manifest FROM '80-manifest.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_shape_metric FROM '81-shape-metrics.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_column_anomaly FROM '82-column-order-anomalies.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.source_sensitive_text_count FROM '90-sensitive-text-counts.csv' WITH (FORMAT csv, HEADER true, NULL '\N')
\copy page_builder_v2_rehearsal.export_complete FROM '99-export-complete.csv' WITH (FORMAT csv, HEADER true, NULL '\N')

ALTER TABLE page_builder_v2_rehearsal.source_page_group
  ADD PRIMARY KEY (page_group_id);
ALTER TABLE page_builder_v2_rehearsal.source_page
  ADD PRIMARY KEY (page_id);
ALTER TABLE page_builder_v2_rehearsal.source_page_site_location
  ADD PRIMARY KEY (page_site_location_id);
ALTER TABLE page_builder_v2_rehearsal.source_page_section
  ADD PRIMARY KEY (page_section_id);
ALTER TABLE page_builder_v2_rehearsal.source_page_row
  ADD PRIMARY KEY (page_row_id);
ALTER TABLE page_builder_v2_rehearsal.source_page_row_column
  ADD PRIMARY KEY (page_row_column_id);
ALTER TABLE page_builder_v2_rehearsal.source_page_row_group_session
  ADD PRIMARY KEY (page_row_group_session_id),
  ADD UNIQUE (page_row_id, group_session_id);
ALTER TABLE page_builder_v2_rehearsal.source_page_row_content
  ADD PRIMARY KEY (page_row_content_id),
  ADD UNIQUE (page_row_id, content_id);
ALTER TABLE page_builder_v2_rehearsal.source_page_row_tag_group
  ADD PRIMARY KEY (page_row_tag_group_id),
  ADD UNIQUE (page_row_id, tag_group_id);
ALTER TABLE page_builder_v2_rehearsal.source_page_row_tag
  ADD PRIMARY KEY (page_row_tag_id),
  ADD UNIQUE (page_row_id, tag_id);
ALTER TABLE page_builder_v2_rehearsal.source_page_row_mailing_list
  ADD PRIMARY KEY (page_row_mailing_list_id),
  ADD UNIQUE (page_row_id, mailing_list_id);
ALTER TABLE page_builder_v2_rehearsal.source_file_upload
  ADD PRIMARY KEY (file_upload_id),
  ADD UNIQUE (url),
  ADD UNIQUE (storage_key);
ALTER TABLE page_builder_v2_rehearsal.source_mailing_list
  ADD PRIMARY KEY (mailing_list_id);
ALTER TABLE page_builder_v2_rehearsal.source_reference_map
  ADD PRIMARY KEY (reference_type, source_id);
ALTER TABLE page_builder_v2_rehearsal.source_manifest
  ADD PRIMARY KEY (table_name);
ALTER TABLE page_builder_v2_rehearsal.source_shape_metric
  ADD PRIMARY KEY (metric);
ALTER TABLE page_builder_v2_rehearsal.source_column_anomaly
  ADD PRIMARY KEY (page_row_id);
ALTER TABLE page_builder_v2_rehearsal.source_sensitive_text_count
  ADD PRIMARY KEY (field_name);

CREATE TABLE page_builder_v2_rehearsal.actual_source_manifest AS
SELECT 'page_group'::TEXT AS table_name, count(*)::BIGINT AS row_count
FROM page_builder_v2_rehearsal.source_page_group
UNION ALL SELECT 'page', count(*) FROM page_builder_v2_rehearsal.source_page
UNION ALL SELECT 'page_site_location', count(*) FROM page_builder_v2_rehearsal.source_page_site_location
UNION ALL SELECT 'page_section', count(*) FROM page_builder_v2_rehearsal.source_page_section
UNION ALL SELECT 'page_row', count(*) FROM page_builder_v2_rehearsal.source_page_row
UNION ALL SELECT 'page_row_column', count(*) FROM page_builder_v2_rehearsal.source_page_row_column
UNION ALL SELECT 'page_row_group_session', count(*) FROM page_builder_v2_rehearsal.source_page_row_group_session
UNION ALL SELECT 'page_row_content', count(*) FROM page_builder_v2_rehearsal.source_page_row_content
UNION ALL SELECT 'page_row_tag_group', count(*) FROM page_builder_v2_rehearsal.source_page_row_tag_group
UNION ALL SELECT 'page_row_tag', count(*) FROM page_builder_v2_rehearsal.source_page_row_tag
UNION ALL SELECT 'page_row_mailing_list', count(*) FROM page_builder_v2_rehearsal.source_page_row_mailing_list
UNION ALL SELECT 'file_upload', count(*) FROM page_builder_v2_rehearsal.source_file_upload
UNION ALL SELECT 'mailing_list', count(*) FROM page_builder_v2_rehearsal.source_mailing_list
UNION ALL SELECT 'reference_map', count(*) FROM page_builder_v2_rehearsal.source_reference_map;

DO $$
BEGIN
  IF (SELECT count(*) FROM page_builder_v2_rehearsal.export_info) <> 1
    OR EXISTS (
      SELECT 1
      FROM page_builder_v2_rehearsal.export_info
      WHERE expected_schema_state <> 'pre-257'
    ) THEN
    RAISE EXCEPTION 'The export metadata is missing or does not describe a pre-257 source';
  END IF;

  IF (SELECT count(*) FROM page_builder_v2_rehearsal.export_complete) <> 1
    OR EXISTS (
      SELECT 1
      FROM page_builder_v2_rehearsal.export_complete
      WHERE expected_file_count <> 20
    ) THEN
    RAISE EXCEPTION '99-export-complete.csv is missing or invalid; the bundle may be incomplete';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_manifest expected
    FULL JOIN page_builder_v2_rehearsal.actual_source_manifest actual
      USING (table_name)
    WHERE expected.row_count IS DISTINCT FROM actual.row_count
  ) THEN
    RAISE EXCEPTION 'CSV row counts do not match 80-manifest.csv';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_page p
    LEFT JOIN page_builder_v2_rehearsal.source_page_group pg
      ON pg.page_group_id = p.page_group_id
    WHERE pg.page_group_id IS NULL
  ) THEN
    RAISE EXCEPTION 'A staged page references a page group outside the bundle';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_page child
    LEFT JOIN page_builder_v2_rehearsal.source_page parent
      ON parent.page_id = child.parent_page_id
    WHERE child.parent_page_id IS NOT NULL
      AND parent.page_id IS NULL
  ) THEN
    RAISE EXCEPTION 'A staged page references a parent outside the bundle';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_page_site_location x
    LEFT JOIN page_builder_v2_rehearsal.source_page p USING (page_id)
    WHERE p.page_id IS NULL
  ) OR EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_page_section x
    LEFT JOIN page_builder_v2_rehearsal.source_page p USING (page_id)
    WHERE p.page_id IS NULL
  ) OR EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_page_row x
    LEFT JOIN page_builder_v2_rehearsal.source_page_section ps USING (page_section_id)
    WHERE ps.page_section_id IS NULL
  ) OR EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_page_row_column x
    LEFT JOIN page_builder_v2_rehearsal.source_page_row pr USING (page_row_id)
    WHERE pr.page_row_id IS NULL
  ) THEN
    RAISE EXCEPTION 'The staged core page hierarchy is not closed';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT page_row_id FROM page_builder_v2_rehearsal.source_page_row_group_session
      UNION ALL SELECT page_row_id FROM page_builder_v2_rehearsal.source_page_row_content
      UNION ALL SELECT page_row_id FROM page_builder_v2_rehearsal.source_page_row_tag_group
      UNION ALL SELECT page_row_id FROM page_builder_v2_rehearsal.source_page_row_tag
      UNION ALL SELECT page_row_id FROM page_builder_v2_rehearsal.source_page_row_mailing_list
    ) child
    LEFT JOIN page_builder_v2_rehearsal.source_page_row pr USING (page_row_id)
    WHERE pr.page_row_id IS NULL
  ) THEN
    RAISE EXCEPTION 'A staged external association references a row outside the bundle';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM (
      SELECT image_file_upload_id AS file_upload_id
      FROM page_builder_v2_rehearsal.source_page
      WHERE image_file_upload_id IS NOT NULL
      UNION
      SELECT image_file_upload_id
      FROM page_builder_v2_rehearsal.source_page_row_column
      WHERE image_file_upload_id IS NOT NULL
    ) reference
    LEFT JOIN page_builder_v2_rehearsal.source_file_upload source
      USING (file_upload_id)
    WHERE source.file_upload_id IS NULL
  ) OR EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_file_upload source
    WHERE NOT EXISTS (
      SELECT 1
      FROM page_builder_v2_rehearsal.source_page page
      WHERE page.image_file_upload_id = source.file_upload_id
    ) AND NOT EXISTS (
      SELECT 1
      FROM page_builder_v2_rehearsal.source_page_row_column column_record
      WHERE column_record.image_file_upload_id = source.file_upload_id
    )
  ) THEN
    RAISE EXCEPTION 'The staged image references and file-upload snapshot do not match exactly';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_page_row_mailing_list association
    LEFT JOIN page_builder_v2_rehearsal.source_mailing_list source
      USING (mailing_list_id)
    WHERE source.mailing_list_id IS NULL
  ) OR EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_mailing_list source
    WHERE NOT EXISTS (
      SELECT 1
      FROM page_builder_v2_rehearsal.source_page_row_mailing_list association
      WHERE association.mailing_list_id = source.mailing_list_id
    )
  ) THEN
    RAISE EXCEPTION 'The staged mailing-list references and list snapshot do not match exactly';
  END IF;

  IF EXISTS (
    WITH expected(reference_type, source_id) AS (
      SELECT 'INSTITUTION'::TEXT, institution_id::TEXT
      FROM page_builder_v2_rehearsal.source_page
      UNION SELECT 'INSTITUTION', institution_id::TEXT
      FROM page_builder_v2_rehearsal.source_mailing_list
      UNION SELECT 'ACCOUNT', created_by_account_id::TEXT
      FROM page_builder_v2_rehearsal.source_page
      UNION SELECT 'ACCOUNT', created_by_account_id::TEXT
      FROM page_builder_v2_rehearsal.source_page_site_location
      UNION SELECT 'ACCOUNT', created_by_account_id::TEXT
      FROM page_builder_v2_rehearsal.source_page_section
      UNION SELECT 'ACCOUNT', created_by_account_id::TEXT
      FROM page_builder_v2_rehearsal.source_page_row
      UNION SELECT 'ACCOUNT', account_id::TEXT
      FROM page_builder_v2_rehearsal.source_file_upload
      UNION SELECT 'ACCOUNT', created_by_account_id::TEXT
      FROM page_builder_v2_rehearsal.source_mailing_list
      UNION SELECT 'FILE_UPLOAD', file_upload_id::TEXT
      FROM page_builder_v2_rehearsal.source_file_upload
      UNION SELECT 'CONTENT', content_id::TEXT
      FROM page_builder_v2_rehearsal.source_page_row_content
      UNION SELECT 'GROUP_SESSION', group_session_id::TEXT
      FROM page_builder_v2_rehearsal.source_page_row_group_session
      UNION SELECT 'TAG_GROUP', tag_group_id::TEXT
      FROM page_builder_v2_rehearsal.source_page_row_tag_group
      UNION SELECT 'TAG', tag_id::TEXT
      FROM page_builder_v2_rehearsal.source_page_row_tag
      UNION SELECT 'MAILING_LIST', mailing_list_id::TEXT
      FROM page_builder_v2_rehearsal.source_mailing_list
    ),
    actual AS (
      SELECT reference_type, source_id
      FROM page_builder_v2_rehearsal.source_reference_map
    ),
    differences AS (
      (SELECT * FROM expected EXCEPT ALL SELECT * FROM actual)
      UNION ALL
      (SELECT * FROM actual EXCEPT ALL SELECT * FROM expected)
    )
    SELECT 1 FROM differences
  ) THEN
    RAISE EXCEPTION '70-reference-map.csv does not exactly cover the staged source references';
  END IF;
END
$$;

DO $$
DECLARE
  configured page_builder_v2_rehearsal.settings%ROWTYPE;
BEGIN
  SELECT * INTO STRICT configured FROM page_builder_v2_rehearsal.settings;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_page source
    LEFT JOIN page_status target USING (page_status_id)
    WHERE target.page_status_id IS NULL
  ) THEN
    RAISE EXCEPTION 'The local database is missing a page_status used by the export';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_page_site_location source
    LEFT JOIN site_location target USING (site_location_id)
    WHERE source.site_location_id IS NOT NULL
      AND target.site_location_id IS NULL
  ) THEN
    RAISE EXCEPTION 'The local database is missing a site_location used by the export';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_page_section source
    LEFT JOIN background_color target USING (background_color_id)
    WHERE target.background_color_id IS NULL
  ) THEN
    RAISE EXCEPTION 'The local database is missing a background_color used by the export';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_page_row source
    LEFT JOIN row_type target USING (row_type_id)
    WHERE target.row_type_id IS NULL
  ) THEN
    RAISE EXCEPTION 'The local database is missing a row_type used by the export';
  END IF;

  IF EXISTS (
    SELECT 1 FROM page_group target
    JOIN page_builder_v2_rehearsal.source_page_group source USING (page_group_id)
  ) OR EXISTS (
    SELECT 1 FROM page target
    JOIN page_builder_v2_rehearsal.source_page source USING (page_id)
  ) OR EXISTS (
    SELECT 1 FROM page_site_location target
    JOIN page_builder_v2_rehearsal.source_page_site_location source
      USING (page_site_location_id)
  ) OR EXISTS (
    SELECT 1 FROM page_section target
    JOIN page_builder_v2_rehearsal.source_page_section source USING (page_section_id)
  ) OR EXISTS (
    SELECT 1 FROM page_row target
    JOIN page_builder_v2_rehearsal.source_page_row source USING (page_row_id)
  ) OR EXISTS (
    SELECT 1 FROM page_row_column target
    JOIN page_builder_v2_rehearsal.source_page_row_column source USING (page_row_column_id)
  ) THEN
    RAISE EXCEPTION 'A core page-owned UUID from the export already exists locally; use a fresh disposable database';
  END IF;

  IF configured.include_external_associations AND (
    EXISTS (
      SELECT 1 FROM page_row_group_session target
      JOIN page_builder_v2_rehearsal.source_page_row_group_session source
        USING (page_row_group_session_id)
    ) OR EXISTS (
      SELECT 1 FROM page_row_content target
      JOIN page_builder_v2_rehearsal.source_page_row_content source
        USING (page_row_content_id)
    ) OR EXISTS (
      SELECT 1 FROM page_row_tag_group target
      JOIN page_builder_v2_rehearsal.source_page_row_tag_group source
        USING (page_row_tag_group_id)
    ) OR EXISTS (
      SELECT 1 FROM page_row_tag target
      JOIN page_builder_v2_rehearsal.source_page_row_tag source
        USING (page_row_tag_id)
    ) OR EXISTS (
      SELECT 1 FROM page_row_mailing_list target
      JOIN page_builder_v2_rehearsal.source_page_row_mailing_list source
        USING (page_row_mailing_list_id)
    )
  ) THEN
    RAISE EXCEPTION 'An external-association UUID from the export already exists locally; use a fresh disposable database';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page existing
    JOIN page_builder_v2_rehearsal.source_page source
      ON existing.institution_id = configured.target_institution_id
     AND existing.url_name = configured.url_prefix || source.url_name
    WHERE existing.deleted_flag = FALSE
      AND existing.page_status_id = 'LIVE'
      AND source.deleted_flag = FALSE
      AND source.page_status_id = 'LIVE'
  ) THEN
    RAISE EXCEPTION 'An imported LIVE page URL would collide in the target institution';
  END IF;
END
$$;

DO $$
DECLARE
  over_four_count BIGINT;
BEGIN
  SELECT count(*) INTO over_four_count
  FROM page_builder_v2_rehearsal.source_column_anomaly
  WHERE over_four_columns;

  IF over_four_count > 0 THEN
    RAISE WARNING '% source rows contain more than four columns. Import will continue so post-migration verification can expose the issue.',
      over_four_count;
  END IF;
END
$$;

CREATE TABLE page_builder_v2_rehearsal.reference_map (
  reference_type TEXT NOT NULL,
  source_id TEXT NOT NULL,
  target_id TEXT NOT NULL,
  mapping_strategy TEXT NOT NULL,
  mapping_reason TEXT NOT NULL,
  PRIMARY KEY (reference_type, source_id)
);

INSERT INTO page_builder_v2_rehearsal.reference_map (
  reference_type,
  source_id,
  target_id,
  mapping_strategy,
  mapping_reason
)
SELECT
  source.reference_type,
  source.source_id,
  CASE source.reference_type
    WHEN 'INSTITUTION' THEN configured.target_institution_id
    WHEN 'ACCOUNT' THEN configured.target_account_id::TEXT
    ELSE source.target_id
  END AS target_id,
  CASE
    WHEN source.reference_type IN ('INSTITUTION', 'ACCOUNT')
      THEN 'CONFIGURED_TARGET'
    WHEN source.source_id = source.target_id
      THEN 'SOURCE_ID_REUSED'
    ELSE 'EXPLICIT_OVERRIDE'
  END AS mapping_strategy,
  CASE
    WHEN source.reference_type = 'INSTITUTION'
      THEN 'Mapped by target_institution_id'
    WHEN source.reference_type = 'ACCOUNT'
      THEN 'Mapped by target_account_id'
    WHEN source.source_id = source.target_id
      THEN 'Identity mapping from the source bundle'
    ELSE 'Target ID edited in 70-reference-map.csv'
  END AS mapping_reason
FROM page_builder_v2_rehearsal.source_reference_map source
CROSS JOIN page_builder_v2_rehearsal.settings configured
ORDER BY source.reference_type, source.source_id;

-- Distinct source objects must not silently collapse onto one local object.
-- Infrastructure account/institution mappings intentionally may be many-to-one.
CREATE UNIQUE INDEX reference_map_distinct_target_idx
ON page_builder_v2_rehearsal.reference_map (reference_type, target_id)
WHERE reference_type NOT IN ('ACCOUNT', 'INSTITUTION');

CREATE TABLE page_builder_v2_rehearsal.page_url_map AS
SELECT
  source.page_id,
  source.url_name AS source_url_name,
  configured.url_prefix || source.url_name AS target_url_name,
  CASE WHEN configured.url_prefix = ''
    THEN 'SOURCE_VALUE_REUSED'
    ELSE 'CONFIGURED_PREFIX'
  END AS mapping_strategy,
  CASE WHEN configured.url_prefix = ''
    THEN 'Source slug preserved exactly'
    ELSE 'Explicit url_prefix supplied to the importer'
  END AS mapping_reason
FROM page_builder_v2_rehearsal.source_page source
CROSS JOIN page_builder_v2_rehearsal.settings configured;

ALTER TABLE page_builder_v2_rehearsal.page_url_map
  ADD PRIMARY KEY (page_id);

DO $$
DECLARE
  configured page_builder_v2_rehearsal.settings%ROWTYPE;
BEGIN
  SELECT * INTO STRICT configured FROM page_builder_v2_rehearsal.settings;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_file_upload source
    JOIN page_builder_v2_rehearsal.reference_map file_mapping
      ON file_mapping.reference_type = 'FILE_UPLOAD'
     AND file_mapping.source_id = source.file_upload_id::TEXT
    JOIN file_upload target
      ON target.file_upload_id = file_mapping.target_id::UUID
    JOIN page_builder_v2_rehearsal.reference_map account_mapping
      ON account_mapping.reference_type = 'ACCOUNT'
     AND account_mapping.source_id = source.account_id::TEXT
    WHERE target.account_id IS DISTINCT FROM account_mapping.target_id::UUID
       OR target.url IS DISTINCT FROM source.url
       OR target.storage_key IS DISTINCT FROM source.storage_key
       OR target.filename IS DISTINCT FROM source.filename
       OR target.content_type IS DISTINCT FROM source.content_type
       OR target.created IS DISTINCT FROM source.created
       OR target.last_updated IS DISTINCT FROM source.last_updated
       OR target.file_upload_type_id IS DISTINCT FROM source.file_upload_type_id
       OR target.filesize IS DISTINCT FROM source.filesize
       OR target.remote_data_flag IS DISTINCT FROM source.remote_data_flag
  ) THEN
    RAISE EXCEPTION 'A mapped local file_upload conflicts with the faithful source snapshot';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_file_upload source
    JOIN page_builder_v2_rehearsal.reference_map mapping
      ON mapping.reference_type = 'FILE_UPLOAD'
     AND mapping.source_id = source.file_upload_id::TEXT
    JOIN file_upload collision
      ON collision.url = source.url OR collision.storage_key = source.storage_key
    WHERE collision.file_upload_id <> mapping.target_id::UUID
  ) THEN
    RAISE EXCEPTION 'A source image URL or storage key belongs to a different local file_upload; edit 70-reference-map.csv or use a clean database';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_file_upload source
    LEFT JOIN file_upload_type target USING (file_upload_type_id)
    WHERE target.file_upload_type_id IS NULL
  ) THEN
    RAISE EXCEPTION 'The local database is missing a file_upload_type used by a source image';
  END IF;

  IF configured.include_external_associations AND EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_mailing_list source
    JOIN page_builder_v2_rehearsal.reference_map list_mapping
      ON list_mapping.reference_type = 'MAILING_LIST'
     AND list_mapping.source_id = source.mailing_list_id::TEXT
    JOIN mailing_list target
      ON target.mailing_list_id = list_mapping.target_id::UUID
    JOIN page_builder_v2_rehearsal.reference_map institution_mapping
      ON institution_mapping.reference_type = 'INSTITUTION'
     AND institution_mapping.source_id = source.institution_id
    JOIN page_builder_v2_rehearsal.reference_map account_mapping
      ON account_mapping.reference_type = 'ACCOUNT'
     AND account_mapping.source_id = source.created_by_account_id::TEXT
    WHERE target.institution_id IS DISTINCT FROM institution_mapping.target_id
       OR target.created_by_account_id IS DISTINCT FROM account_mapping.target_id::UUID
       OR target.created IS DISTINCT FROM source.created
       OR target.last_updated IS DISTINCT FROM source.last_updated
  ) THEN
    RAISE EXCEPTION 'A mapped local mailing_list conflicts with the faithful source snapshot';
  END IF;

  IF configured.include_external_associations AND EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.reference_map mapping
    LEFT JOIN v_admin_content target
      ON target.content_id = mapping.target_id::UUID
     AND target.content_status_id = 'LIVE'
    WHERE mapping.reference_type = 'CONTENT'
      AND target.content_id IS NULL
  ) THEN
    RAISE EXCEPTION 'A CONTENT source ID has no mapped LIVE local record; edit 70-reference-map.csv';
  END IF;

  IF configured.include_external_associations AND EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.reference_map mapping
    LEFT JOIN group_session target
      ON target.group_session_id = mapping.target_id::UUID
     AND target.institution_id = configured.target_institution_id
     AND target.group_session_status_id = 'ADDED'
    WHERE mapping.reference_type = 'GROUP_SESSION'
      AND target.group_session_id IS NULL
  ) THEN
    RAISE EXCEPTION 'A GROUP_SESSION source ID has no mapped ADDED target-institution record; edit 70-reference-map.csv';
  END IF;

  IF configured.include_external_associations AND EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.reference_map mapping
    LEFT JOIN tag_group target ON target.tag_group_id = mapping.target_id
    WHERE mapping.reference_type = 'TAG_GROUP'
      AND target.tag_group_id IS NULL
  ) THEN
    RAISE EXCEPTION 'A TAG_GROUP source ID has no mapped local record; edit 70-reference-map.csv';
  END IF;

  IF configured.include_external_associations AND EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.reference_map mapping
    LEFT JOIN tag target ON target.tag_id = mapping.target_id
    WHERE mapping.reference_type = 'TAG'
      AND target.tag_id IS NULL
  ) THEN
    RAISE EXCEPTION 'A TAG source ID has no mapped local record; edit 70-reference-map.csv';
  END IF;
END
$$;

-- Snapshot exact user-trigger modes, then disable user triggers inside this
-- transaction so source timestamps are not replaced with now(). Foreign-key
-- constraint triggers remain active. The saved modes are restored after load.
CREATE TABLE page_builder_v2_rehearsal.original_user_trigger_state AS
SELECT
  relation.relname::TEXT AS table_name,
  trigger_record.tgname::TEXT AS trigger_name,
  trigger_record.tgenabled AS trigger_mode
FROM pg_trigger trigger_record
JOIN pg_class relation ON relation.oid = trigger_record.tgrelid
JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
WHERE namespace.nspname = 'cobalt'
  AND relation.relname IN (
    'file_upload',
    'mailing_list',
    'page',
    'page_site_location',
    'page_section',
    'page_row',
    'page_row_column',
    'page_row_group_session',
    'page_row_content',
    'page_row_tag_group',
    'page_row_tag',
    'page_row_mailing_list'
  )
  AND trigger_record.tgisinternal = FALSE;

ALTER TABLE file_upload DISABLE TRIGGER USER;
ALTER TABLE mailing_list DISABLE TRIGGER USER;
ALTER TABLE page DISABLE TRIGGER USER;
ALTER TABLE page_site_location DISABLE TRIGGER USER;
ALTER TABLE page_section DISABLE TRIGGER USER;
ALTER TABLE page_row DISABLE TRIGGER USER;
ALTER TABLE page_row_column DISABLE TRIGGER USER;
ALTER TABLE page_row_group_session DISABLE TRIGGER USER;
ALTER TABLE page_row_content DISABLE TRIGGER USER;
ALTER TABLE page_row_tag_group DISABLE TRIGGER USER;
ALTER TABLE page_row_tag DISABLE TRIGGER USER;
ALTER TABLE page_row_mailing_list DISABLE TRIGGER USER;

INSERT INTO page_builder_v2_rehearsal.created_local_object (object_type, object_id)
SELECT 'FILE_UPLOAD', mapping.target_id
FROM page_builder_v2_rehearsal.reference_map mapping
LEFT JOIN file_upload target ON target.file_upload_id = mapping.target_id::UUID
WHERE mapping.reference_type = 'FILE_UPLOAD'
  AND target.file_upload_id IS NULL;

INSERT INTO file_upload (
  file_upload_id,
  account_id,
  url,
  storage_key,
  filename,
  content_type,
  created,
  last_updated,
  file_upload_type_id,
  filesize,
  remote_data_flag
)
SELECT
  file_mapping.target_id::UUID,
  account_mapping.target_id::UUID,
  source.url,
  source.storage_key,
  source.filename,
  source.content_type,
  source.created,
  source.last_updated,
  source.file_upload_type_id,
  source.filesize,
  source.remote_data_flag
FROM page_builder_v2_rehearsal.source_file_upload source
JOIN page_builder_v2_rehearsal.reference_map file_mapping
  ON file_mapping.reference_type = 'FILE_UPLOAD'
 AND file_mapping.source_id = source.file_upload_id::TEXT
JOIN page_builder_v2_rehearsal.reference_map account_mapping
  ON account_mapping.reference_type = 'ACCOUNT'
 AND account_mapping.source_id = source.account_id::TEXT
JOIN page_builder_v2_rehearsal.created_local_object created
  ON created.object_type = 'FILE_UPLOAD'
 AND created.object_id = file_mapping.target_id;

INSERT INTO page_builder_v2_rehearsal.created_local_object (object_type, object_id)
SELECT 'MAILING_LIST', mapping.target_id
FROM page_builder_v2_rehearsal.reference_map mapping
LEFT JOIN mailing_list target ON target.mailing_list_id = mapping.target_id::UUID
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE mapping.reference_type = 'MAILING_LIST'
  AND target.mailing_list_id IS NULL
  AND configured.include_external_associations;

INSERT INTO mailing_list (
  mailing_list_id,
  institution_id,
  created_by_account_id,
  created,
  last_updated
)
SELECT
  list_mapping.target_id::UUID,
  institution_mapping.target_id,
  account_mapping.target_id::UUID,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_mailing_list source
JOIN page_builder_v2_rehearsal.reference_map list_mapping
  ON list_mapping.reference_type = 'MAILING_LIST'
 AND list_mapping.source_id = source.mailing_list_id::TEXT
JOIN page_builder_v2_rehearsal.reference_map institution_mapping
  ON institution_mapping.reference_type = 'INSTITUTION'
 AND institution_mapping.source_id = source.institution_id
JOIN page_builder_v2_rehearsal.reference_map account_mapping
  ON account_mapping.reference_type = 'ACCOUNT'
 AND account_mapping.source_id = source.created_by_account_id::TEXT
JOIN page_builder_v2_rehearsal.created_local_object created
  ON created.object_type = 'MAILING_LIST'
 AND created.object_id = list_mapping.target_id
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

INSERT INTO page_group (page_group_id, analytics_campaign_key)
SELECT page_group_id, analytics_campaign_key
FROM page_builder_v2_rehearsal.source_page_group;

INSERT INTO page (
  page_id,
  name,
  url_name,
  page_status_id,
  headline,
  description,
  image_file_upload_id,
  image_alt_text,
  published_date,
  deleted_flag,
  institution_id,
  parent_page_id,
  created_by_account_id,
  created,
  last_updated,
  page_group_id
)
SELECT
  source.page_id,
  source.name,
  url_mapping.target_url_name,
  source.page_status_id,
  source.headline,
  source.description,
  image_mapping.target_id::UUID,
  source.image_alt_text,
  source.published_date,
  source.deleted_flag,
  institution_mapping.target_id,
  NULL,
  account_mapping.target_id::UUID,
  source.created,
  source.last_updated,
  source.page_group_id
FROM page_builder_v2_rehearsal.source_page source
JOIN page_builder_v2_rehearsal.page_url_map url_mapping USING (page_id)
JOIN page_builder_v2_rehearsal.reference_map institution_mapping
  ON institution_mapping.reference_type = 'INSTITUTION'
 AND institution_mapping.source_id = source.institution_id
JOIN page_builder_v2_rehearsal.reference_map account_mapping
  ON account_mapping.reference_type = 'ACCOUNT'
 AND account_mapping.source_id = source.created_by_account_id::TEXT
LEFT JOIN page_builder_v2_rehearsal.reference_map image_mapping
  ON image_mapping.reference_type = 'FILE_UPLOAD'
 AND image_mapping.source_id = source.image_file_upload_id::TEXT;

UPDATE page target
SET parent_page_id = source.parent_page_id
FROM page_builder_v2_rehearsal.source_page source
WHERE target.page_id = source.page_id
  AND source.parent_page_id IS NOT NULL;

INSERT INTO page_site_location (
  page_site_location_id,
  page_id,
  site_location_id,
  display_order,
  publish_start_date,
  publish_end_date,
  call_to_action,
  created_by_account_id,
  created,
  last_updated,
  short_description,
  icon_name
)
SELECT
  source.page_site_location_id,
  source.page_id,
  source.site_location_id,
  source.display_order,
  source.publish_start_date,
  source.publish_end_date,
  source.call_to_action,
  account_mapping.target_id::UUID,
  source.created,
  source.last_updated,
  source.short_description,
  source.icon_name
FROM page_builder_v2_rehearsal.source_page_site_location source
JOIN page_builder_v2_rehearsal.reference_map account_mapping
  ON account_mapping.reference_type = 'ACCOUNT'
 AND account_mapping.source_id = source.created_by_account_id::TEXT;

INSERT INTO page_section (
  page_section_id,
  page_id,
  name,
  headline,
  description,
  background_color_id,
  deleted_flag,
  display_order,
  created_by_account_id,
  created,
  last_updated
)
SELECT
  source.page_section_id,
  source.page_id,
  source.name,
  source.headline,
  source.description,
  source.background_color_id,
  source.deleted_flag,
  source.display_order,
  account_mapping.target_id::UUID,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_section source
JOIN page_builder_v2_rehearsal.reference_map account_mapping
  ON account_mapping.reference_type = 'ACCOUNT'
 AND account_mapping.source_id = source.created_by_account_id::TEXT;

INSERT INTO page_row (
  page_row_id,
  page_section_id,
  row_type_id,
  deleted_flag,
  display_order,
  created_by_account_id,
  created,
  last_updated
)
SELECT
  source.page_row_id,
  source.page_section_id,
  source.row_type_id,
  source.deleted_flag,
  source.display_order,
  account_mapping.target_id::UUID,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row source
JOIN page_builder_v2_rehearsal.reference_map account_mapping
  ON account_mapping.reference_type = 'ACCOUNT'
 AND account_mapping.source_id = source.created_by_account_id::TEXT;

INSERT INTO page_row_column (
  page_row_column_id,
  page_row_id,
  headline,
  description,
  image_file_upload_id,
  image_alt_text,
  column_display_order,
  created,
  last_updated
)
SELECT
  source.page_row_column_id,
  source.page_row_id,
  source.headline,
  source.description,
  image_mapping.target_id::UUID,
  source.image_alt_text,
  source.column_display_order,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row_column source
LEFT JOIN page_builder_v2_rehearsal.reference_map image_mapping
  ON image_mapping.reference_type = 'FILE_UPLOAD'
 AND image_mapping.source_id = source.image_file_upload_id::TEXT;

INSERT INTO page_row_group_session (
  page_row_group_session_id,
  page_row_id,
  group_session_id,
  group_session_display_order,
  created,
  last_updated
)
SELECT
  source.page_row_group_session_id,
  source.page_row_id,
  mapping.target_id::UUID,
  source.group_session_display_order,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row_group_session source
JOIN page_builder_v2_rehearsal.reference_map mapping
  ON mapping.reference_type = 'GROUP_SESSION'
 AND mapping.source_id = source.group_session_id::TEXT
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

INSERT INTO page_row_content (
  page_row_content_id,
  page_row_id,
  content_id,
  content_display_order,
  created,
  last_updated
)
SELECT
  source.page_row_content_id,
  source.page_row_id,
  mapping.target_id::UUID,
  source.content_display_order,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row_content source
JOIN page_builder_v2_rehearsal.reference_map mapping
  ON mapping.reference_type = 'CONTENT'
 AND mapping.source_id = source.content_id::TEXT
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

INSERT INTO page_row_tag_group (
  page_row_tag_group_id,
  page_row_id,
  tag_group_id,
  created,
  last_updated
)
SELECT
  source.page_row_tag_group_id,
  source.page_row_id,
  mapping.target_id,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row_tag_group source
JOIN page_builder_v2_rehearsal.reference_map mapping
  ON mapping.reference_type = 'TAG_GROUP'
 AND mapping.source_id = source.tag_group_id
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

INSERT INTO page_row_tag (
  page_row_tag_id,
  page_row_id,
  tag_id,
  created,
  last_updated
)
SELECT
  source.page_row_tag_id,
  source.page_row_id,
  mapping.target_id,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row_tag source
JOIN page_builder_v2_rehearsal.reference_map mapping
  ON mapping.reference_type = 'TAG'
 AND mapping.source_id = source.tag_id
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

INSERT INTO page_row_mailing_list (
  page_row_mailing_list_id,
  page_row_id,
  mailing_list_id,
  title,
  description,
  created,
  last_updated
)
SELECT
  source.page_row_mailing_list_id,
  source.page_row_id,
  mapping.target_id::UUID,
  source.title,
  source.description,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row_mailing_list source
JOIN page_builder_v2_rehearsal.reference_map mapping
  ON mapping.reference_type = 'MAILING_LIST'
 AND mapping.source_id = source.mailing_list_id::TEXT
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

DO $$
DECLARE
  trigger_record RECORD;
  restore_action TEXT;
BEGIN
  FOR trigger_record IN
    SELECT table_name, trigger_name, trigger_mode
    FROM page_builder_v2_rehearsal.original_user_trigger_state
    ORDER BY table_name, trigger_name
  LOOP
    restore_action := CASE trigger_record.trigger_mode
      WHEN 'O' THEN 'ENABLE TRIGGER'
      WHEN 'D' THEN 'DISABLE TRIGGER'
      WHEN 'R' THEN 'ENABLE REPLICA TRIGGER'
      WHEN 'A' THEN 'ENABLE ALWAYS TRIGGER'
      ELSE NULL
    END;

    IF restore_action IS NULL THEN
      RAISE EXCEPTION 'Unsupported trigger mode % for %.%',
        trigger_record.trigger_mode,
        trigger_record.table_name,
        trigger_record.trigger_name;
    END IF;

    EXECUTE format(
      'ALTER TABLE %I.%I %s %I',
      'cobalt',
      trigger_record.table_name,
      restore_action,
      trigger_record.trigger_name
    );
  END LOOP;
END
$$;

CREATE TABLE page_builder_v2_rehearsal.baseline_page AS
SELECT target.*
FROM page target
JOIN page_builder_v2_rehearsal.source_page source USING (page_id);

CREATE TABLE page_builder_v2_rehearsal.baseline_page_group AS
SELECT target.*
FROM page_group target
JOIN page_builder_v2_rehearsal.source_page_group source USING (page_group_id);

CREATE TABLE page_builder_v2_rehearsal.baseline_page_site_location AS
SELECT target.*
FROM page_site_location target
JOIN page_builder_v2_rehearsal.source_page_site_location source
  USING (page_site_location_id);

CREATE TABLE page_builder_v2_rehearsal.baseline_page_section AS
SELECT target.*
FROM page_section target
JOIN page_builder_v2_rehearsal.source_page_section source USING (page_section_id);

CREATE TABLE page_builder_v2_rehearsal.baseline_page_row AS
SELECT target.*
FROM page_row target
JOIN page_builder_v2_rehearsal.source_page_row source USING (page_row_id);

CREATE TABLE page_builder_v2_rehearsal.baseline_page_row_column AS
SELECT target.*
FROM page_row_column target
JOIN page_builder_v2_rehearsal.source_page_row_column source
  USING (page_row_column_id);

CREATE TABLE page_builder_v2_rehearsal.baseline_page_row_group_session AS
SELECT target.*
FROM page_row_group_session target
JOIN page_builder_v2_rehearsal.source_page_row_group_session source
  USING (page_row_group_session_id)
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

CREATE TABLE page_builder_v2_rehearsal.baseline_page_row_content AS
SELECT target.*
FROM page_row_content target
JOIN page_builder_v2_rehearsal.source_page_row_content source
  USING (page_row_content_id)
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

CREATE TABLE page_builder_v2_rehearsal.baseline_page_row_tag_group AS
SELECT target.*
FROM page_row_tag_group target
JOIN page_builder_v2_rehearsal.source_page_row_tag_group source
  USING (page_row_tag_group_id)
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

CREATE TABLE page_builder_v2_rehearsal.baseline_page_row_tag AS
SELECT target.*
FROM page_row_tag target
JOIN page_builder_v2_rehearsal.source_page_row_tag source
  USING (page_row_tag_id)
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

CREATE TABLE page_builder_v2_rehearsal.baseline_page_row_mailing_list AS
SELECT target.*
FROM page_row_mailing_list target
JOIN page_builder_v2_rehearsal.source_page_row_mailing_list source
  USING (page_row_mailing_list_id)
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

CREATE TABLE page_builder_v2_rehearsal.baseline_file_upload AS
SELECT target.*
FROM file_upload target
JOIN page_builder_v2_rehearsal.reference_map mapping
  ON mapping.reference_type = 'FILE_UPLOAD'
 AND mapping.target_id = target.file_upload_id::TEXT;

CREATE TABLE page_builder_v2_rehearsal.baseline_mailing_list AS
SELECT target.*
FROM mailing_list target
JOIN page_builder_v2_rehearsal.reference_map mapping
  ON mapping.reference_type = 'MAILING_LIST'
 AND mapping.target_id = target.mailing_list_id::TEXT
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

CREATE TABLE page_builder_v2_rehearsal.baseline_analytics_native_event_type AS
SELECT *
FROM analytics_native_event_type;

CREATE TABLE page_builder_v2_rehearsal.expected_section_text_row AS
SELECT
  section.page_id,
  section.page_section_id AS source_page_section_id,
  section.name,
  section.headline,
  section.description,
  section.background_color_id,
  section.created_by_account_id
FROM page_builder_v2_rehearsal.baseline_page_section section
WHERE section.deleted_flag = FALSE
  AND COALESCE(
    NULLIF(BTRIM(section.headline), ''),
    NULLIF(BTRIM(section.description), '')
  ) IS NOT NULL;

CREATE TABLE page_builder_v2_rehearsal.baseline_manifest AS
SELECT 'page_group'::TEXT AS table_name, count(*)::BIGINT AS row_count
FROM page_builder_v2_rehearsal.baseline_page_group
UNION ALL SELECT 'page', count(*)
FROM page_builder_v2_rehearsal.baseline_page
UNION ALL SELECT 'page_site_location', count(*) FROM page_builder_v2_rehearsal.baseline_page_site_location
UNION ALL SELECT 'page_section', count(*) FROM page_builder_v2_rehearsal.baseline_page_section
UNION ALL SELECT 'page_row', count(*) FROM page_builder_v2_rehearsal.baseline_page_row
UNION ALL SELECT 'page_row_column', count(*) FROM page_builder_v2_rehearsal.baseline_page_row_column
UNION ALL SELECT 'page_row_group_session', count(*) FROM page_builder_v2_rehearsal.baseline_page_row_group_session
UNION ALL SELECT 'page_row_content', count(*) FROM page_builder_v2_rehearsal.baseline_page_row_content
UNION ALL SELECT 'page_row_tag_group', count(*) FROM page_builder_v2_rehearsal.baseline_page_row_tag_group
UNION ALL SELECT 'page_row_tag', count(*) FROM page_builder_v2_rehearsal.baseline_page_row_tag
UNION ALL SELECT 'page_row_mailing_list', count(*) FROM page_builder_v2_rehearsal.baseline_page_row_mailing_list
UNION ALL SELECT 'file_upload', count(*) FROM page_builder_v2_rehearsal.baseline_file_upload
UNION ALL SELECT 'mailing_list', count(*) FROM page_builder_v2_rehearsal.baseline_mailing_list
UNION ALL SELECT 'reference_map', count(*) FROM page_builder_v2_rehearsal.reference_map;

CREATE TABLE page_builder_v2_rehearsal.expected_import_page_group AS
SELECT * FROM page_builder_v2_rehearsal.source_page_group;

CREATE TABLE page_builder_v2_rehearsal.expected_import_page AS
SELECT
  source.page_id,
  source.name,
  url_mapping.target_url_name AS url_name,
  source.page_status_id,
  source.headline,
  source.description,
  image_mapping.target_id::UUID AS image_file_upload_id,
  source.image_alt_text,
  source.published_date,
  source.deleted_flag,
  institution_mapping.target_id AS institution_id,
  source.parent_page_id,
  account_mapping.target_id::UUID AS created_by_account_id,
  source.created,
  source.last_updated,
  source.page_group_id
FROM page_builder_v2_rehearsal.source_page source
JOIN page_builder_v2_rehearsal.page_url_map url_mapping USING (page_id)
JOIN page_builder_v2_rehearsal.reference_map institution_mapping
  ON institution_mapping.reference_type = 'INSTITUTION'
 AND institution_mapping.source_id = source.institution_id
JOIN page_builder_v2_rehearsal.reference_map account_mapping
  ON account_mapping.reference_type = 'ACCOUNT'
 AND account_mapping.source_id = source.created_by_account_id::TEXT
LEFT JOIN page_builder_v2_rehearsal.reference_map image_mapping
  ON image_mapping.reference_type = 'FILE_UPLOAD'
 AND image_mapping.source_id = source.image_file_upload_id::TEXT;

CREATE TABLE page_builder_v2_rehearsal.expected_import_page_site_location AS
SELECT
  source.page_site_location_id,
  source.page_id,
  source.site_location_id,
  source.display_order,
  source.publish_start_date,
  source.publish_end_date,
  source.call_to_action,
  account_mapping.target_id::UUID AS created_by_account_id,
  source.created,
  source.last_updated,
  source.short_description,
  source.icon_name
FROM page_builder_v2_rehearsal.source_page_site_location source
JOIN page_builder_v2_rehearsal.reference_map account_mapping
  ON account_mapping.reference_type = 'ACCOUNT'
 AND account_mapping.source_id = source.created_by_account_id::TEXT;

CREATE TABLE page_builder_v2_rehearsal.expected_import_page_section AS
SELECT
  source.page_section_id,
  source.page_id,
  source.name,
  source.headline,
  source.description,
  source.background_color_id,
  source.deleted_flag,
  source.display_order,
  account_mapping.target_id::UUID AS created_by_account_id,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_section source
JOIN page_builder_v2_rehearsal.reference_map account_mapping
  ON account_mapping.reference_type = 'ACCOUNT'
 AND account_mapping.source_id = source.created_by_account_id::TEXT;

CREATE TABLE page_builder_v2_rehearsal.expected_import_page_row AS
SELECT
  source.page_row_id,
  source.page_section_id,
  source.row_type_id,
  source.deleted_flag,
  source.display_order,
  account_mapping.target_id::UUID AS created_by_account_id,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row source
JOIN page_builder_v2_rehearsal.reference_map account_mapping
  ON account_mapping.reference_type = 'ACCOUNT'
 AND account_mapping.source_id = source.created_by_account_id::TEXT;

CREATE TABLE page_builder_v2_rehearsal.expected_import_page_row_column AS
SELECT
  source.page_row_column_id,
  source.page_row_id,
  source.headline,
  source.description,
  image_mapping.target_id::UUID AS image_file_upload_id,
  source.image_alt_text,
  source.column_display_order,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row_column source
LEFT JOIN page_builder_v2_rehearsal.reference_map image_mapping
  ON image_mapping.reference_type = 'FILE_UPLOAD'
 AND image_mapping.source_id = source.image_file_upload_id::TEXT;

CREATE TABLE page_builder_v2_rehearsal.expected_import_page_row_group_session AS
SELECT
  source.page_row_group_session_id,
  source.page_row_id,
  mapping.target_id::UUID AS group_session_id,
  source.group_session_display_order,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row_group_session source
JOIN page_builder_v2_rehearsal.reference_map mapping
  ON mapping.reference_type = 'GROUP_SESSION'
 AND mapping.source_id = source.group_session_id::TEXT
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

CREATE TABLE page_builder_v2_rehearsal.expected_import_page_row_content AS
SELECT
  source.page_row_content_id,
  source.page_row_id,
  mapping.target_id::UUID AS content_id,
  source.content_display_order,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row_content source
JOIN page_builder_v2_rehearsal.reference_map mapping
  ON mapping.reference_type = 'CONTENT'
 AND mapping.source_id = source.content_id::TEXT
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

CREATE TABLE page_builder_v2_rehearsal.expected_import_page_row_tag_group AS
SELECT
  source.page_row_tag_group_id,
  source.page_row_id,
  mapping.target_id AS tag_group_id,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row_tag_group source
JOIN page_builder_v2_rehearsal.reference_map mapping
  ON mapping.reference_type = 'TAG_GROUP'
 AND mapping.source_id = source.tag_group_id
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

CREATE TABLE page_builder_v2_rehearsal.expected_import_page_row_tag AS
SELECT
  source.page_row_tag_id,
  source.page_row_id,
  mapping.target_id AS tag_id,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row_tag source
JOIN page_builder_v2_rehearsal.reference_map mapping
  ON mapping.reference_type = 'TAG'
 AND mapping.source_id = source.tag_id
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

CREATE TABLE page_builder_v2_rehearsal.expected_import_page_row_mailing_list AS
SELECT
  source.page_row_mailing_list_id,
  source.page_row_id,
  mapping.target_id::UUID AS mailing_list_id,
  source.title,
  source.description,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_page_row_mailing_list source
JOIN page_builder_v2_rehearsal.reference_map mapping
  ON mapping.reference_type = 'MAILING_LIST'
 AND mapping.source_id = source.mailing_list_id::TEXT
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

CREATE TABLE page_builder_v2_rehearsal.expected_import_file_upload AS
SELECT
  file_mapping.target_id::UUID AS file_upload_id,
  account_mapping.target_id::UUID AS account_id,
  source.url,
  source.storage_key,
  source.filename,
  source.content_type,
  source.created,
  source.last_updated,
  source.file_upload_type_id,
  source.filesize,
  source.remote_data_flag
FROM page_builder_v2_rehearsal.source_file_upload source
JOIN page_builder_v2_rehearsal.reference_map file_mapping
  ON file_mapping.reference_type = 'FILE_UPLOAD'
 AND file_mapping.source_id = source.file_upload_id::TEXT
JOIN page_builder_v2_rehearsal.reference_map account_mapping
  ON account_mapping.reference_type = 'ACCOUNT'
 AND account_mapping.source_id = source.account_id::TEXT;

CREATE TABLE page_builder_v2_rehearsal.expected_import_mailing_list AS
SELECT
  list_mapping.target_id::UUID AS mailing_list_id,
  institution_mapping.target_id AS institution_id,
  account_mapping.target_id::UUID AS created_by_account_id,
  source.created,
  source.last_updated
FROM page_builder_v2_rehearsal.source_mailing_list source
JOIN page_builder_v2_rehearsal.reference_map list_mapping
  ON list_mapping.reference_type = 'MAILING_LIST'
 AND list_mapping.source_id = source.mailing_list_id::TEXT
JOIN page_builder_v2_rehearsal.reference_map institution_mapping
  ON institution_mapping.reference_type = 'INSTITUTION'
 AND institution_mapping.source_id = source.institution_id
JOIN page_builder_v2_rehearsal.reference_map account_mapping
  ON account_mapping.reference_type = 'ACCOUNT'
 AND account_mapping.source_id = source.created_by_account_id::TEXT
CROSS JOIN page_builder_v2_rehearsal.settings configured
WHERE configured.include_external_associations;

CREATE OR REPLACE FUNCTION page_builder_v2_rehearsal.table_difference_count(
  expected_table REGCLASS,
  actual_table REGCLASS
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
  mismatch_count BIGINT;
BEGIN
  EXECUTE format(
    $query$
      WITH expected AS (
        SELECT to_jsonb(expected_row) AS row_data FROM %s expected_row
      ),
      actual AS (
        SELECT to_jsonb(actual_row) AS row_data FROM %s actual_row
      ),
      differences AS (
        (SELECT row_data FROM expected EXCEPT ALL SELECT row_data FROM actual)
        UNION ALL
        (SELECT row_data FROM actual EXCEPT ALL SELECT row_data FROM expected)
      )
      SELECT count(*) FROM differences
    $query$,
    expected_table,
    actual_table
  ) INTO mismatch_count;

  RETURN mismatch_count;
END
$$;

CREATE TABLE page_builder_v2_rehearsal.import_verification_result (
  check_order INTEGER NOT NULL,
  check_name TEXT NOT NULL PRIMARY KEY,
  passed BOOLEAN NOT NULL,
  details TEXT NOT NULL
);

INSERT INTO page_builder_v2_rehearsal.import_verification_result
SELECT checked.check_order, checked.check_name, checked.mismatches = 0,
       format('%s differing row(s)', checked.mismatches)
FROM (
  SELECT 1 AS check_order, 'source_page_group_matches_import'::TEXT AS check_name,
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_page_group'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_page_group'::REGCLASS
         ) AS mismatches
  UNION ALL SELECT 2, 'source_page_matches_import_after_declared_mappings',
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_page'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_page'::REGCLASS
         )
  UNION ALL SELECT 3, 'source_page_site_location_matches_import',
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_page_site_location'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_page_site_location'::REGCLASS
         )
  UNION ALL SELECT 4, 'source_page_section_matches_import',
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_page_section'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_page_section'::REGCLASS
         )
  UNION ALL SELECT 5, 'source_page_row_matches_import',
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_page_row'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_page_row'::REGCLASS
         )
  UNION ALL SELECT 6, 'source_page_row_column_matches_import',
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_page_row_column'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_page_row_column'::REGCLASS
         )
  UNION ALL SELECT 7, 'source_group_session_associations_match_import',
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_page_row_group_session'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_page_row_group_session'::REGCLASS
         )
  UNION ALL SELECT 8, 'source_content_associations_match_import',
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_page_row_content'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_page_row_content'::REGCLASS
         )
  UNION ALL SELECT 9, 'source_tag_group_associations_match_import',
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_page_row_tag_group'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_page_row_tag_group'::REGCLASS
         )
  UNION ALL SELECT 10, 'source_tag_associations_match_import',
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_page_row_tag'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_page_row_tag'::REGCLASS
         )
  UNION ALL SELECT 11, 'source_mailing_list_associations_match_import',
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_page_row_mailing_list'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_page_row_mailing_list'::REGCLASS
         )
  UNION ALL SELECT 12, 'source_image_metadata_matches_import',
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_file_upload'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_file_upload'::REGCLASS
         )
  UNION ALL SELECT 13, 'source_mailing_list_records_match_import',
         page_builder_v2_rehearsal.table_difference_count(
           'page_builder_v2_rehearsal.expected_import_mailing_list'::REGCLASS,
           'page_builder_v2_rehearsal.baseline_mailing_list'::REGCLASS
         )
) checked;

INSERT INTO page_builder_v2_rehearsal.import_verification_result
SELECT 14, 'all_source_references_have_one_declared_mapping', count(*) = 0,
       format('%s missing or unexpected mapping key(s)', count(*))
FROM (
  (SELECT reference_type, source_id
   FROM page_builder_v2_rehearsal.source_reference_map
   EXCEPT ALL
   SELECT reference_type, source_id
   FROM page_builder_v2_rehearsal.reference_map)
  UNION ALL
  (SELECT reference_type, source_id
   FROM page_builder_v2_rehearsal.reference_map
   EXCEPT ALL
   SELECT reference_type, source_id
   FROM page_builder_v2_rehearsal.source_reference_map)
) differences;

INSERT INTO page_builder_v2_rehearsal.import_verification_result
SELECT 15, 'source_page_slugs_preserved_or_explicitly_mapped', count(*) = 0,
       format('%s undeclared page slug change(s)', count(*))
FROM page_builder_v2_rehearsal.page_url_map mapping
JOIN page_builder_v2_rehearsal.settings configured ON TRUE
WHERE (configured.url_prefix = '' AND mapping.target_url_name <> mapping.source_url_name)
   OR (configured.url_prefix <> ''
       AND mapping.target_url_name <> configured.url_prefix || mapping.source_url_name);

DO $$
DECLARE
  configured page_builder_v2_rehearsal.settings%ROWTYPE;
BEGIN
  SELECT * INTO STRICT configured FROM page_builder_v2_rehearsal.settings;

  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.source_manifest source
    FULL JOIN page_builder_v2_rehearsal.baseline_manifest baseline USING (table_name)
    WHERE baseline.row_count IS DISTINCT FROM CASE
        WHEN source.table_name IN (
          'page_row_group_session',
          'page_row_content',
          'page_row_tag_group',
          'page_row_tag',
          'page_row_mailing_list',
          'mailing_list'
        ) AND NOT configured.include_external_associations THEN 0
        ELSE source.row_count
      END
  ) THEN
    RAISE EXCEPTION 'The imported pre-migration baseline does not match the staged bundle';
  END IF;
END
$$;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM page_builder_v2_rehearsal.import_verification_result
    WHERE passed = FALSE
  ) THEN
    RAISE EXCEPTION 'The imported baseline differs from the source snapshot plus declared mappings';
  END IF;
END
$$;

COMMIT;

\echo ''
\echo 'Pre-257 fixture import complete.'
\echo 'Imported baseline row counts:'
TABLE page_builder_v2_rehearsal.baseline_manifest ORDER BY table_name;

\echo ''
\echo 'Source-to-local mapping summary:'
SELECT
  mapping.reference_type,
  count(*) AS mapping_count,
  count(*) FILTER (WHERE mapping.source_id = mapping.target_id) AS preserved_count,
  count(*) FILTER (WHERE mapping.source_id <> mapping.target_id) AS substituted_count
FROM page_builder_v2_rehearsal.reference_map mapping
GROUP BY mapping.reference_type
ORDER BY mapping.reference_type;

\echo ''
\echo 'Source-to-local mapping detail:'
SELECT
  reference_type,
  source_id,
  target_id,
  mapping_strategy,
  mapping_reason
FROM page_builder_v2_rehearsal.reference_map
ORDER BY reference_type, source_id;

\echo ''
\echo 'Source-to-import verification:'
SELECT
  CASE WHEN passed THEN 'PASS' ELSE 'FAIL' END AS result,
  check_name,
  details
FROM page_builder_v2_rehearsal.import_verification_result
ORDER BY check_order, check_name;

\echo ''
\echo 'Imported page URL mapping:'
SELECT
  source.page_id,
  source.page_status_id,
  source.deleted_flag,
  source.url_name AS source_url_name,
  target.url_name AS local_url_name
FROM page_builder_v2_rehearsal.source_page source
JOIN page target USING (page_id)
ORDER BY source.created, source.page_id;

\echo ''
\echo 'Imported page image mapping:'
SELECT
  source.page_id,
  source.image_file_upload_id AS source_image_file_upload_id,
  image_mapping.target_id::UUID AS local_image_file_upload_id,
  source_file.url AS source_image_url,
  local_file.url AS local_image_url
FROM page_builder_v2_rehearsal.source_page source
LEFT JOIN page_builder_v2_rehearsal.reference_map image_mapping
  ON image_mapping.reference_type = 'FILE_UPLOAD'
 AND image_mapping.source_id = source.image_file_upload_id::TEXT
LEFT JOIN page_builder_v2_rehearsal.source_file_upload source_file
  ON source_file.file_upload_id = source.image_file_upload_id
LEFT JOIN file_upload local_file
  ON local_file.file_upload_id = image_mapping.target_id::UUID
ORDER BY source.created, source.page_id;

\echo ''
\echo 'Imported column image mapping:'
SELECT
  source.page_row_column_id,
  source.image_file_upload_id AS source_image_file_upload_id,
  image_mapping.target_id::UUID AS local_image_file_upload_id,
  source_file.url AS source_image_url,
  local_file.url AS local_image_url
FROM page_builder_v2_rehearsal.source_page_row_column source
LEFT JOIN page_builder_v2_rehearsal.reference_map image_mapping
  ON image_mapping.reference_type = 'FILE_UPLOAD'
 AND image_mapping.source_id = source.image_file_upload_id::TEXT
LEFT JOIN page_builder_v2_rehearsal.source_file_upload source_file
  ON source_file.file_upload_id = source.image_file_upload_id
LEFT JOIN file_upload local_file
  ON local_file.file_upload_id = image_mapping.target_id::UUID
ORDER BY source.page_row_id, source.column_display_order,
  source.page_row_column_id;

\echo ''
\echo 'Next: run 03-apply-257-and-verify.sql against this same disposable database.'
