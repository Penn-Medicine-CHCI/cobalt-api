# Page Builder V2 production-data rehearsal

These scripts export a faithful institution-scoped Page Builder snapshot from a
pre-257 production database, load it into a disposable local database stopped
at patch 256, apply the repository's real `257-page-builder-v2.sql`, and verify
the result.

The workflow deliberately separates two layers:

1. Raw source snapshot CSVs retain the production values and should not be
   edited.
2. The separate, editable source-to-local map handles dependency IDs that
   cannot be used locally; an explicit importer option handles slug prefixes.

The source snapshot does not mask page slugs, creator/institution IDs, image
IDs, image URLs, storage metadata, or external association IDs. Page copy,
HTML, UUIDs, timestamps, deleted records, display orders, image sharing, and
`page_group.analytics_campaign_key` are also preserved.

The export includes the full `file_upload` rows referenced by page and column
images, plus the `mailing_list` rows referenced by Page Builder associations.
It does not download image bytes.

## Scope boundaries

The following objects are not copied in full because migration 257 does not
reshape them:

- Account and institution records. Their source IDs remain in the CSVs and are
  mapped to the configured local account and institution during import.
- Content, group-session, tag, and tag-group records. Their real source IDs
  remain in the association CSVs and mapping template. The importer reuses the
  same local ID or requires an explicit replacement ID.
- Mailing-list entries.
- `page_group_email_content` and `page_group_email_group_session`.
- Historical analytics events and footprint records.

These are scope omissions, not obfuscation. No source relationship ID is
replaced by an ordinal or discarded. If a mapped content/session/tag object is
not the same object locally, its rendered card can differ; that substitution is
printed explicitly by the importer.

## Safety and prerequisites

- Prefer connecting the production scripts to a physical read replica. When no
  replica is available, they support an explicitly confirmed primary connection
  only when `default_transaction_read_only=on` was forced for the whole session.
- Use a production role with schema access and `SELECT` on the exported tables.
  For a primary, use a dedicated non-owner, non-superuser role with no write
  privileges. `TEMP` is not required. Source-data work is limited to `SELECT`
  and `COPY TO STDOUT` inside a repeatable-read, read-only transaction.
- Run the exporter from a unique empty directory approved for production data.
  `psql` writes the `COPY TO STDOUT` streams to client-side CSV files containing
  unmodified production copy, IDs, URLs, and storage keys.
- Do not run the local importer against production. It requires
  `confirm_local_import=YES`, but the operator must still verify the target.
- Use a disposable local database with patch 256 present and patch 257 absent.
- Run the local import and verifier as the normal local database/schema
  migration owner. The scripts create schemas and migration objects and must
  disable/re-enable user triggers to preserve exported `last_updated`
  timestamps instead of replacing them with `now()`.
- Do not commit the CSV bundle. The local `.gitignore` is only a final guard.

## 1. Profile the production shape

This prints aggregate shape without printing page copy. By default it requires
a physical read replica before migration 257:

```sh
psql "$PROD_DSN" -X \
  -v source_institution_id='SOURCE_INSTITUTION' \
  -f /absolute/path/to/sql/page-builder-v2-rehearsal/00-profile-production.sql
```

If no replica is available, connect with the dedicated read-only production
role and require both a connection-wide read-only default and the explicit
primary opt-in:

```sh
PGOPTIONS='-c default_transaction_read_only=on -c lock_timeout=5s -c statement_timeout=30min -c idle_in_transaction_session_timeout=5min' \
PGAPPNAME='page-builder-v2-profile' \
psql "$PROD_DSN" -X \
  -v source_institution_id='SOURCE_INSTITUTION' \
  -v confirm_read_only_primary=YES \
  -f /absolute/path/to/sql/page-builder-v2-rehearsal/00-profile-production.sql
```

The opt-in alone is insufficient: the script also verifies the connection was
forced read-only and verifies its explicit transaction remains read-only.

Require zero `active_rows_hidden_in_deleted_sections`. Such a row can become
visible when migration 257 removes its deleted section. Also review rows with
more than four columns: migration 257 normalizes order but does not remove a
historical fifth column.

## 2. Export the faithful source bundle

Run from a unique empty directory because the client-side output paths are
relative to the current directory and file writes are not transactional:

```sh
EXPORT_ROOT='/path/on/approved-volume'
EXPORT_DIR="$(mktemp -d "$EXPORT_ROOT/page-builder-v2-rehearsal.XXXXXX")"
chmod 700 "$EXPORT_DIR"
cd "$EXPORT_DIR"
umask 077

psql "$PROD_DSN" -X \
  -v source_institution_id='SOURCE_INSTITUTION' \
  -f /absolute/path/to/sql/page-builder-v2-rehearsal/01-export-production.sql
```

For the read-only-primary fallback, use the same connection protections:

```sh
PGOPTIONS='-c default_transaction_read_only=on -c lock_timeout=5s -c statement_timeout=30min -c idle_in_transaction_session_timeout=5min' \
PGAPPNAME='page-builder-v2-export' \
psql "$PROD_DSN" -X \
  -v source_institution_id='SOURCE_INSTITUTION' \
  -v confirm_read_only_primary=YES \
  -f /absolute/path/to/sql/page-builder-v2-rehearsal/01-export-production.sql
```

This mode cannot write database data, but its reads still run on the primary.
Run it during an acceptable operational window. As with a replica export,
discard the directory after any error or nonzero exit; only
`99-export-complete.csv` marks a complete bundle.

All source-data reads use one repeatable-read, read-only snapshot. The exporter
writes 20 files, including:

- `01` through `11`: page groups, pages, sections, rows, columns, and raw
  association IDs.
- `12-file-upload.csv`: complete database rows for referenced page images.
- `13-mailing-list.csv`: referenced mailing-list records, without entries.
- `70-reference-map.csv`: editable identity-map template.
- `80` through `90`: manifest and diagnostic reports.
- `99-export-complete.csv`: completion marker written last.

The export includes all selected institution pages, including drafts, editing
copies, deleted data, parents, and all members of selected page groups. It
aborts if a parent or page group crosses the institution boundary.

Replica lag affects freshness but not consistency within the fixed snapshot. A
long standby query can be canceled by a recovery conflict; if that happens,
discard the partial directory and retry from a new empty one.

## 3. Resolve local-only ID differences

`70-reference-map.csv` starts with `target_id` equal to `source_id`. Leave an
identity mapping unchanged when that object exists locally. If a referenced
content, group session, tag, or tag group does not exist locally, change only
that row's `target_id` to the intended local equivalent.

Example:

```csv
reference_type,source_id,target_id
"CONTENT","production-content-uuid","local-content-uuid"
```

Supported reference types are:

- `ACCOUNT`
- `INSTITUTION`
- `FILE_UPLOAD`
- `CONTENT`
- `GROUP_SESSION`
- `TAG_GROUP`
- `TAG`
- `MAILING_LIST`

Account and institution targets in this file are superseded by the import
command's `target_account_id` and `target_institution_id`; their source IDs are
still retained and reported. Distinct source image/external IDs cannot silently
collapse onto one target ID.

The importer handles referenced images and mailing lists specially:

- If an exact mapped `file_upload` exists, it is reused.
- Otherwise the importer creates it with the mapped ID, original URL, storage
  key, filename, type, size, flags, and timestamps. Only its account FK is
  mapped.
- An inconsistent file ID, URL, or storage-key collision aborts the import.
- A missing mailing list is recreated with the mapped ID and original
  timestamps; institution and creator FKs are mapped.

Content mappings must resolve to a local `LIVE` record. Group-session mappings
must resolve to an `ADDED` record in the target institution. Tags and tag groups
must exist locally. The importer never chooses arbitrary fallback objects.

## 4. Recreate a disposable local database through patch 256

Use the normal local recreation sequence but omit migration 257. Confirm:

```sql
SELECT patch_name
FROM _v.patches
WHERE patch_name IN (
  '256-autism-clinic-penn-updates',
  '257-page-builder-v2'
)
ORDER BY patch_name;
```

Patch 256 must be present and patch 257 absent. The importer enforces both.

## 5. Import the source snapshot and declared mappings

Run from the directory containing the 20 CSV files:

```sh
psql "$LOCAL_DSN" -X \
  -v confirm_local_import=YES \
  -v target_institution_id='LOCAL_INSTITUTION' \
  -v target_account_id='LOCAL_ADMIN_ACCOUNT_UUID' \
  -v include_external_associations=true \
  -f /absolute/path/to/sql/page-builder-v2-rehearsal/02-import-local-pre257.sql
```

Page slugs are preserved exactly by default. If a clean database is not
possible, an explicit `-v url_prefix='rehearsal-'` is supported and recorded in
`page_builder_v2_rehearsal.page_url_map`. An active LIVE slug collision aborts
instead of silently rewriting a page.

The import:

- Keeps all page-owned UUIDs exactly and fails if they collide.
- Retains the raw source tables under
  `page_builder_v2_rehearsal.source_*`.
- Resolves every mapped dependency ID through
  `page_builder_v2_rehearsal.reference_map`, including identity mappings.
- Temporarily disables only user triggers inside the import transaction so
  source timestamps remain exact; foreign-key constraint triggers stay active.
- Creates an expected baseline from source data plus declared mappings and
  compares it bidirectionally with every imported table.
- Runs 15 source-to-local fidelity checks and aborts if any fail.
- Prints mapping summaries/details, slug mappings, and page/column image URLs.

`include_external_associations` is the default for each association family.
Each family can be overridden independently with:

- `include_group_session_associations`
- `include_content_associations`
- `include_tag_group_associations`
- `include_tag_associations`
- `include_mailing_list_associations`

For example, when local content and group-session records are unavailable but
the exported taxonomy IDs exist locally, retain the singleton relationships
needed to render `TAG` and `TAG_GROUP` rows:

```sh
psql "$LOCAL_DSN" -X \
  -v confirm_local_import=YES \
  -v target_institution_id='LOCAL_INSTITUTION' \
  -v target_account_id='LOCAL_ADMIN_ACCOUNT_UUID' \
  -v include_external_associations=false \
  -v include_tag_associations=true \
  -v include_tag_group_associations=true \
  -v include_mailing_list_associations=true \
  -f /absolute/path/to/sql/page-builder-v2-rehearsal/02-import-local-pre257.sql
```

Active `TAG` and `TAG_GROUP` rows must retain exactly one corresponding
association; the importer aborts instead of creating a fixture the page API
cannot render. Mailing-list dependencies are fully bundled and can normally be
enabled even when content and group-session associations are omitted. Raw CSVs
and source IDs for any disabled family remain staged for inspection.

## 6. Apply migration 257 and verify

```sh
psql "$LOCAL_DSN" -X \
  -f /absolute/path/to/sql/page-builder-v2-rehearsal/03-apply-257-and-verify.sql
```

The verifier applies the repository's actual migration with `\ir`, leaves the
migrated disposable database available for inspection, and fails if any check
does not pass. Checks include:

- The 15 source-to-local fidelity results must all pass.
- All ten migration backup tables exactly match the imported pre-migration
  Page Builder rows.
- Page, site-location, and referenced file-upload records remain unchanged;
  external association and mailing-list records remain unchanged when those
  associations were included in the import.
- Original slugs and active page/column image URLs still resolve through the
  migrated views according to the declared mappings; deleted pages remain
  excluded from `v_page`.
- Every original row and column survives; only intended V2 reshaping and
  deterministic column-order normalization are allowed.
- Keeper sections, generated text rows, row/column counts, ordering, orphan
  checks, and the four-column invariant are exact.
- V2 schema objects, lookups, defaults, constraints, triggers, and view
  contracts have their expected definitions and behavior.
- Every pre-257 native analytics event type remains unchanged and the CTA event
  is the only addition.

After SQL verification, use the printed source/local URL and image reports to
smoke-test a migrated public page and the admin builder.

## Cleanup

Drop or recreate the disposable database after review. Delete the CSV bundle
when it is no longer needed. These scripts are a migration rehearsal, not a
production backup or restore mechanism.
