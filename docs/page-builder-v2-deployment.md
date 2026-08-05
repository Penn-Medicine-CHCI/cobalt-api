# Page Builder V2 Deployment

Page Builder V2 changes persisted row-type values and destructively reshapes page sections. It must not be deployed as a normal rolling API/web release. An API instance from before this change cannot deserialize rows created with the new row types, and rollback after editors create V2 rows requires restoring the database as well as the application.

## Required release sequence

1. Disable page-builder writes and place the affected web experience in maintenance mode.
2. Drain all pre-V2 API instances and take an external PostgreSQL snapshot. The audit tables created by migration 257 are useful for targeted recovery, but are not a substitute for a database snapshot.
3. Run migration `257-page-builder-v2.sql` and the immediately following Page Builder migrations.
4. Deploy the V2 API and web application as one maintenance-window release.
5. Run the verification queries below and smoke-test both a migrated public page and the admin builder before removing maintenance mode.

Do not allow old and new API instances to serve traffic concurrently during this release. Do not roll only the application back after V2 rows have been created; restore the pre-migration database snapshot or perform a deliberate data downgrade at the same time.

## Verification queries

Every page with active sections must have exactly one after the reshape:

```sql
SELECT p.page_id, count(ps.page_section_id)
FROM page p
LEFT JOIN page_section ps
  ON ps.page_id = p.page_id
 AND ps.deleted_flag = FALSE
WHERE p.deleted_flag = FALSE
GROUP BY p.page_id
HAVING count(ps.page_section_id) <> 1;
```

Column display positions must be unique and contiguous per row:

```sql
WITH ordered_columns AS (
  SELECT
    page_row_id,
    column_display_order,
    row_number() OVER (
      PARTITION BY page_row_id
      ORDER BY column_display_order, page_row_column_id
    ) - 1 AS expected_display_order
  FROM page_row_column
)
SELECT *
FROM ordered_columns
WHERE column_display_order <> expected_display_order;
```

No row may retain a reference to a removed section:

```sql
SELECT pr.page_row_id
FROM page_row pr
LEFT JOIN page_section ps ON ps.page_section_id = pr.page_section_id
WHERE ps.page_section_id IS NULL;
```

All three queries must return zero rows.
