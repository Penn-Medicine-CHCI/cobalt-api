BEGIN;
SELECT _v.register_patch(
	'265-onboarding-screening-presentation-and-cancellation-reasons',
	ARRAY['262-care-navigator', '264-provider-institution-referrer'],
	NULL
);

ALTER TABLE account_source
ADD COLUMN onboarding_screening_presentation_id TEXT NOT NULL DEFAULT 'LARGE_MODAL';

COMMENT ON COLUMN account_source.onboarding_screening_presentation_id IS
'Onboarding screening presentation identifier. LARGE_MODAL uses the existing UI; clients should fall back to it for unrecognized identifiers.';

-- Initial enterprise and local-development configuration; future presentation changes are data updates.
UPDATE account_source
SET onboarding_screening_presentation_id='SMALL_MODAL'
WHERE account_source_id IN ('COBALT_SSO', 'PENN_SSO', 'PENN_KEY_SSO');

INSERT INTO care_encounter_cancellation_reason (
	care_encounter_cancellation_reason_id,
	description,
	display_order,
	freeform_text_required
)
VALUES
	('PATIENT_REQUESTED', 'Patient requested cancellation', 1, FALSE),
	('CARE_DELIVERED_DURING_CALL', 'Care delivered during call', 2, FALSE),
	('UNABLE_TO_REACH_PATIENT', 'Unable to reach patient', 3, FALSE),
	('SCHEDULING_CONFLICT', 'Scheduling conflict', 4, FALSE),
	('DUPLICATE_BOOKING', 'Duplicate booking', 5, FALSE),
	('FOLLOW_UP_COMPLETED', 'Follow-up completed', 6, FALSE),
	('OTHER', 'Other', 7, TRUE)
ON CONFLICT (care_encounter_cancellation_reason_id) DO UPDATE
SET description=EXCLUDED.description,
	display_order=EXCLUDED.display_order,
	freeform_text_required=EXCLUDED.freeform_text_required;

-- Preserve the original meaning of cancellations created with the retired
-- placeholder reason instead of reclassifying their historical data.
UPDATE care_encounter
SET care_encounter_cancellation_reason_id='OTHER',
	care_encounter_cancellation_reason_other_text='Care navigation is no longer needed'
WHERE care_encounter_cancellation_reason_id='NO_LONGER_NEEDED';

DELETE FROM care_encounter_cancellation_reason
WHERE care_encounter_cancellation_reason_id='NO_LONGER_NEEDED';

COMMIT;
