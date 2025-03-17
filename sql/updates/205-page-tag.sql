BEGIN;
SELECT _v.register_patch('205-page-tag', NULL, NULL);

CREATE TABLE page_row_tag (
    page_row_tag_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    page_row_id UUID NOT NULL REFERENCES page_row,
    tag_id VARCHAR NOT NULL REFERENCES tag,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_page_row_tag_row_tag ON page_row_tag(page_row_id, tag_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_row_tag FOR EACH ROW EXECUTE PROCEDURE set_last_updated();


INSERT INTO row_type
VALUES
('TAG', 'Tag');

CREATE VIEW v_page_row_tag
AS 
SELECT prt.page_row_tag_id, pr.page_row_id, pr.display_order, pr.page_section_id, pr.row_type_id, 
prt.tag_id, pr.created_by_account_id, pr.created, pr.last_updated
FROM page_row_tag prt, page_row pr
WHERE prt.page_row_id = pr.page_row_id
AND pr.deleted_flag = FALSE;

-- // On the web, when a custom Page is rendered.
-- // Additional data:
-- // * pageId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('PAGE_VIEW_PAGE', 'Page View (Page)');
-- // When a click occurs to access a custom Page.
-- // Additional data:
-- // * pageId (UUID)
-- // * source (String)
-- //    HOME_FEATURE: When clicked through from the homepage in one of the featured areas
-- //    NAV_FEATURE: When clicked through from the navigation featured area
-- //    NAV: When clicked through from the navigation (not in the featured area)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_PAGE', 'Clickthrough (Page)');
-- // When a Page viewer clicks through on a group session to view its detail page.
-- // Additional data:
-- // * pageId (UUID)
-- // * groupSessionId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_PAGE_GROUP_SESSION', 'Clickthrough (Page Group Session)');
-- // When a Page viewer clicks through on a piece of content to view its detail page.
-- // Additional data:
-- // * pageId (UUID)
-- // * contentId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_PAGE_CONTENT', 'Clickthrough (Page Content)');
-- // When a Page viewer clicks through on a tag group to view its Resource Library page.
-- // Additional data:
-- // * pageId (UUID)
-- // * tagGroupId (String)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_PAGE_TAG_GROUP', 'Clickthrough (Page Tag Group)');
-- // When a Page viewer clicks through on a tag to view its Resource Library page.
-- // Additional data:
-- // * pageId (UUID)
-- // * tagId (String)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_PAGE_TAG', 'Clickthrough (Page Tag)');
-- // When a Page viewer clicks through on a link contained within any WYSIWYG page component.
-- // Additional data:
-- // * pageId (UUID)
-- // * linkUrl (String, the URL linked in the anchor tag)
-- // * linkText (String, the text component of the anchor tag)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_PAGE_LINK', 'Clickthrough (Page Link)');

COMMIT;