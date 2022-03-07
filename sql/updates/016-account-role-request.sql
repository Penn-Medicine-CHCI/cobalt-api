BEGIN;
SELECT _v.register_patch('016-account-role-request', NULL, NULL);

INSERT INTO audit_log_event VALUES ('ACCOUNT_ROLE_REQUEST', 'Account requested to add a role');

-- Add interaction for role requests
INSERT INTO interaction
(interaction_id,interaction_type_id,max_interaction_count, frequency_in_minutes, institution_id,interaction_complete_message)
VALUES
('f2d728b1-076d-433e-9166-df2a1237d2ef', 'EMAIL', 3, 1440, 'COBALT', 'Role request process has been completed.');

INSERT INTO interaction_option
(interaction_option_id,interaction_id, option_description, option_response,final_flag, option_order, completed_response)
VALUES
(uuid_generate_v4(), 'f2d728b1-076d-433e-9166-df2a1237d2ef', 'Role request approved and executed', 'Thank you for handling this request.'
	, true, 1, 'Thank you. The request was processed in [completionTimeHoursAndMinutes].'),
(uuid_generate_v4(), 'f2d728b1-076d-433e-9166-df2a1237d2ef', 'Role request rejected', 'Thank you for handling this request.'
	, true, 2, 'Thank you. The request was processed in [completionTimeHoursAndMinutes].' );

UPDATE institution
SET metadata = jsonb_set(metadata, '{defaultRoleRequestInteractionId}', '"f2d728b1-076d-433e-9166-df2a1237d2ef"', true)
WHERE institution_id = 'COBALT';

END;