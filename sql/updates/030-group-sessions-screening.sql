BEGIN;
SELECT _v.register_patch('030-group-sessions-screening', NULL, NULL);

-- Each institution can optionally have a screening flow ID that can be triggered prior to viewing group sessions
ALTER TABLE institution ADD COLUMN group_sessions_screening_flow_id UUID REFERENCES screening_flow;

COMMIT;