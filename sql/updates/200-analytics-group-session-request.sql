BEGIN;
SELECT _v.register_patch('200-analytics-group-session-request', NULL, NULL);

-- Sneak in another analytics event type
-- // When a Topic Center page viewer clicks through on a group session request to view its detail page.
-- // Additional data:
-- // * topicCenterId (UUID)
-- // * groupSessionRequestId (UUID)
INSERT INTO analytics_native_event_type (analytics_native_event_type_id, description) VALUES ('CLICKTHROUGH_TOPIC_CENTER_GROUP_SESSION_REQUEST', 'Clickthrough (Topic Center Group Session Request)');

COMMIT;