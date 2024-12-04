BEGIN;
SELECT _v.register_patch('195-analytics-additional-events', NULL, NULL);

-- // When a Topic Center page viewer clicks through on a group session to view its detail page.
-- // Additional data:
-- // * topicCenterId (UUID)
-- // * groupSessionId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_TOPIC_CENTER_GROUP_SESSION', 'Clickthrough (Topic Center Group Session)');
-- // When a Topic Center page viewer clicks through on a piece of content to view its detail page.
-- // Additional data:
-- // * topicCenterId (UUID)
-- // * contentId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_TOPIC_CENTER_CONTENT', 'Clickthrough (Topic Center Content)');
-- // When a Topic Center page viewer clicks through on a tag group to view its Resource Library page.
-- // Additional data:
-- // * topicCenterId (UUID)
-- // * tagGroupId (String)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_TOPIC_CENTER_TAG_GROUP', 'Clickthrough (Topic Center Tag Group)');
-- // When a Topic Center page viewer clicks through on a tag to view its Resource Library page.
-- // Additional data:
-- // * topicCenterId (UUID)
-- // * tagId (String)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_TOPIC_CENTER_TAG', 'Clickthrough (Topic Center Tag)');
-- // When a Topic Center page viewer clicks through on a link contained within a "pinboard note" component.
-- // Additional data:
-- // * topicCenterId (UUID)
-- // * pinboardNoteId (UUID)
-- // * linkUrl (String, the URL linked in the anchor tag)
-- // * linkText (String, the text component of the anchor tag)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_TOPIC_CENTER_PINBOARD_NOTE_LINK', 'Clickthrough (Topic Center Pinboard Note Link)');

COMMIT;