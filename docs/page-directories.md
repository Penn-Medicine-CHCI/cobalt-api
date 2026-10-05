# Published page directories

Directory membership belongs to each page version. It is separate from scheduled
`page_site_location` placements used for homepage spotlights and curated navigation.
All endpoints below require authentication and use the current account's institution.

## Locations and settings

`GET /page-locations` returns options in this order:

```json
{
  "pageLocations": [
    {
      "pageLocationId": "FEATURED_TOPIC",
      "description": "Featured Topics",
      "directoryRelativeUrl": "/featured-topics"
    },
    {
      "pageLocationId": "COMMUNITY",
      "description": "Penn Communities",
      "directoryRelativeUrl": "/communities"
    },
    {
      "pageLocationId": "UNCATEGORIZED",
      "description": "Uncategorized"
    }
  ]
}
```

`POST /pages` and `PUT /pages/{pageId}/settings` accept optional `pageLocationId`
alongside their existing fields. Omitted or null values default to Uncategorized
on creation and preserve the existing category on updates. Unsupported IDs are
rejected. Existing page-management authorization applies.

Page detail and summary responses include `pageLocationId`,
`pageLocationDescription`, `pageStatusDescription`, and `hasBeenPublished`. Status IDs remain `LIVE`,
`DRAFT`, and `COPY_FOR_EDITING`; their descriptions are Published, Unpublished,
and Copy for Editing respectively.

Editing copies and standalone duplicates inherit the source category. Changes to
an editing copy become visible in directories when that version is published.
Unpublishing or deleting a page removes it from directory results.

`hasBeenPublished` supports the Settings modal's URL-change warning. It becomes
true on publication and remains true after unpublishing. Editing copies inherit
the flag; standalone duplicates start false with no publication date. The
frontend shows the warning when this flag is true and the friendly URL differs
from the saved URL.

## Admin actions

| Action | API |
| --- | --- |
| Create | `POST /pages` with name, URL name, and optional location |
| Edit | `POST /pages/{pageIdentifier}/edit` returns the editable page ID |
| Duplicate | `PUT /pages/{pageId}/duplicate` with a new name and URL name |
| Settings | `PUT /pages/{pageId}/settings` with name, URL name, and optional location |
| View on Cobalt | Navigate to the returned `relativeUrl`; data comes from `GET /pages/published/{pageIdentifier}` |
| Unpublish | `PUT /pages/{pageId}/unpublish` |
| Delete | `DELETE /pages/{pageId}` |
| Copy subscriber emails | `GET /pages/{pageIdentifier}/page-row-mailing-lists`, selecting email entries |
| Download subscriber emails | `GET /page-row-mailing-lists/csv?pageId={pageId}&mailingListEntryTypeId=EMAIL_ADDRESS` |

Name and friendly URL are required on creation and settings saves. Duplicate
creates an Unpublished page in a separate page group; editing a Published page
resumes the current admin's most recently saved, non-deleted editing copy for
that published version, or creates one if none exists. Other admins keep
separate editing copies. Publishing a new version starts a fresh editing copy
on the next edit. The Unpublished menu exposes Delete, while the
Published menu exposes View on Cobalt and Unpublish. These workflows retain
page-management authorization and institution boundaries.

Subscriber responses include only active subscriptions; the admin subscriber
count excludes unsubscribed entries. CSV downloads retain their gzip attachment
response and Mailing List, Type, and Value columns. The optional
`mailingListEntryTypeId` filter restricts exported records to that type;
omitting it preserves the export of all subscribed types. Empty lists return
a CSV header without data rows.

Changing a Published page's URL in Settings makes the new URL effective
immediately and the previous URL unavailable. Unpublish immediately removes the
page from public lookups, directories, and curated navigation. Delete removes
the selected page from the admin list and public lookups using the existing
deletion flag; no restore action is exposed in this workflow.

## Directory listing

```http
GET /pages/published?pageLocationId=FEATURED_TOPIC&searchQuery=anxiety&pageNumber=0&pageSize=25
```

`pageLocationId` is required. The response contains `pages`, `totalCount`, and
`totalCountDescription`. Each page uses the existing summary contract, including
`pageId`, `name`, `relativeUrl`, `imageUrl`, and `imageAltText`; missing optional
image values may be omitted. Summary responses do not require `pageSections`.

Only published, non-deleted pages in the requested category and current
institution are returned. Search matches name, headline, or URL name without
regard to case. Results sort by case-insensitive page name, then page ID, before
pagination. The frontend should preserve this order.

Pagination is zero-based. The default page size is 25 and the maximum is 100.
Negative page numbers normalize to zero; nonpositive sizes use the default.
Totals describe the entire filtered result, even when the requested page is
empty. An explicit Uncategorized query is supported, but Uncategorized pages
appear in neither frontend directory.

Placement schedules do not control directory visibility. Categorizing a page
does not add a navigation entry or create/change spotlight placements. Community
navigation remains sourced from `institution.additionalNavigationItems` in its
configured order. The frontend's Monthly Spotlight label continues using the
existing Featured Topic placement source and legacy routes.

## Curated Community navigation

For page-based institutions, a `page_site_location` record with
`site_location_id = 'COMMUNITY'` selects that page version for the Community
dropdown. Add placements only after the institution supplies its selections.
There is no selection UI or additional selection flag.

Configure the existing placement fields in data:

- `page_id`: the selected page version in the institution.
- `display_order`: the curated navigation order (use distinct values).
- `short_description`: supporting text, including the established supported HTML.
- `publish_start_date` / `publish_end_date`: optional visibility window; the start
  is inclusive and the end is exclusive.
- `call_to_action` and `created_by_account_id`: existing required placement fields.

Only live, non-deleted pages whose placement window is active appear in navigation.
The page category remains independent: `page_location_id = 'COMMUNITY'` includes
a published page in `/communities`, whether or not it has a Community placement.
Categorizing a page does not select it for the dropdown. Editing copies inherit
placements, including order, supporting text, and schedules; publishing the copy
replaces the navigation entry with the newly published version.

`institution.additionalNavigationItems` includes optional `shortDescription` from
the placement. Legacy topic-center navigation retains its configured selection,
order, routes, and analytics, and uses `nav_description` for `shortDescription`.
Release this additive API response change before the consuming frontend. No new
migration or example Community selections are needed.

## Migration and rollout

1. Apply the single feature migration, `sql/updates/289-page-directory-locations.sql`,
   before releasing this API.
2. Release the API, then the frontend consuming these contracts.
3. Verify authenticated directory searches, settings saves, and publication in
   the deployed environment. Keep existing curated community selections until
   the organization administrator supplies replacements.

Migration 289 adds the lookup, defaulted page category, durable publication history,
view metadata, directory index, and status descriptions in one transaction.
Its category backfill uses existing Featured Topic and
Community placements: exactly one distinct placement category determines the
category; both or neither becomes Uncategorized. Resource placements are ignored.
Existing page timestamps and placement records are preserved.

Publication history is backfilled from retained publication dates and
Published/editing-copy statuses without changing existing timestamps.
Historical publication of old Unpublished pages cannot be recovered if all
evidence was already cleared before this migration.

Focused behavioral checks:

```shell
mvn -Dtest=PageDirectoryMigrationTests,PageDirectoryRequestTests,PageDirectoryTests,PageAdminActionsTests test
```

Run `mvn test` with the local dependencies available for the complete backend
suite. Migration tests apply real SQL to disposable databases; directory service
tests use transactions that roll back their fixtures.
