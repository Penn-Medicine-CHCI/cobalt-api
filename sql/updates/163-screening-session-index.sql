BEGIN;
SELECT _v.register_patch('163-screening-session-index', NULL, NULL);

--Unique index to prevent more than one screening session for a study check-in
CREATE UNIQUE INDEX idx_screening_session_account_check_in_action_id ON screening_session(account_check_in_action_id);

--Adding missing foreign key constraint
ALTER TABLE study_check_in_action ADD CONSTRAINT screening_flow_id_fkey FOREIGN KEY (screening_flow_id) REFERENCES screening_flow;


COMMIT;