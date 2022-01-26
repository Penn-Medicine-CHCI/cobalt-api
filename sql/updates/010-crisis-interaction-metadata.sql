BEGIN;
SELECT _v.register_patch('010-crisis-interaction-metadata', NULL, NULL);

ALTER TABLE interaction_instance ADD COLUMN hipaa_compliant_metadata JSONB NULL;

-- Add example interaction for in-app crisis
INSERT INTO interaction
(interaction_id,interaction_type_id,max_interaction_count, frequency_in_minutes, institution_id,interaction_complete_message)
VALUES
('848f9384-99a1-4289-9fa0-5f005121cd8f', 'EMAIL', 3, 1440, 'COBALT', 'This case has already been completed.');

INSERT INTO interaction_option
(interaction_option_id,interaction_id, option_description, option_response,final_flag, option_order, completed_response)
VALUES
(uuid_generate_v4(), '848f9384-99a1-4289-9fa0-5f005121cd8f', 'Contacted, no response', 'We will send an email in [frequencyHoursAndMinutes] as a reminder to try again.'
	, false, 1, 'You and your team have logged [maxInteractionCount] contact attempts so we will consider this case closed. Thank you for your efforts.'),
(uuid_generate_v4(), '848f9384-99a1-4289-9fa0-5f005121cd8f', 'Assessed, no follow-up needed', 'Thank you. That took [completionTimeHoursAndMinutes] to close the case.'
	, true, 2,'Thank you. That took [completionTimeHoursAndMinutes] to close the case.' ),
(uuid_generate_v4(), '848f9384-99a1-4289-9fa0-5f005121cd8f', 'Assessed, provided resources', 'Thank you. That took [completionTimeHoursAndMinutes] to close the case.'
	, true, 3, 'Thank you. That took [completionTimeHoursAndMinutes] to close the case.');

UPDATE institution
SET metadata = '{"defaultCrisisInteractionId": "848f9384-99a1-4289-9fa0-5f005121cd8f", "way2HealthIncidentTrackingConfigs": [{"enabled": true, "studyId": 715, "type": "Medical Emergency: Suicide Ideation", "interactionId": "45f4082c-4d16-400e-aecd-38e87726f6d9"}]}'::jsonb
WHERE institution_id = 'COBALT';

UPDATE account
SET metadata = '{"interactionIds": ["45f4082c-4d16-400e-aecd-38e87726f6d9", "848f9384-99a1-4289-9fa0-5f005121cd8f"]}'::jsonb
WHERE role_id='ADMINISTRATOR' AND institution_id='COBALT';

END;