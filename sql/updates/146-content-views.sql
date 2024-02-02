BEGIN;
SELECT _v.register_patch('146-content-views', NULL, NULL);

CREATE INDEX idx_activity_tracking_context ON activity_tracking (context);
CREATE INDEX idx_activity_account_id ON activity_tracking (account_id);

COMMIT;