BEGIN;
SELECT _v.register_patch(
	'267-local-only-cobalt-homepage-features',
	ARRAY['266-local-only-cobalt-employer-onboarding-single-question'],
	NULL
);

-- Keep local COBALT discovery navigation aligned with the shared dev fixture:
-- six primary features. Provider-booking-only fixtures remain available through
-- their direct routes without crowding either the homepage or header menus.
UPDATE institution_feature
SET nav_visible=(feature_id IN (
		'THERAPY',
		'MEDICATION_PRESCRIBER',
		'GROUP_SESSIONS',
		'SELF_HELP_RESOURCES',
		'SPIRITUAL_SUPPORT',
		'CRISIS_SUPPORT'
	)),
	landing_page_visible=(feature_id IN (
		'THERAPY',
		'MEDICATION_PRESCRIBER',
		'GROUP_SESSIONS',
		'SELF_HELP_RESOURCES',
		'SPIRITUAL_SUPPORT',
		'CRISIS_SUPPORT'
	)),
	subtitle_override=CASE
		WHEN feature_id='GROUP_SESSIONS' THEN NULL
		ELSE subtitle_override
	END
WHERE institution_id='COBALT';

DO $verify$
BEGIN
	IF (
		SELECT ARRAY_AGG(feature_id::TEXT ORDER BY display_order, feature_id)
		FROM institution_feature
		WHERE institution_id='COBALT'
		AND landing_page_visible IS TRUE
	) IS DISTINCT FROM ARRAY[
		'THERAPY',
		'MEDICATION_PRESCRIBER',
		'GROUP_SESSIONS',
		'SELF_HELP_RESOURCES',
		'SPIRITUAL_SUPPORT',
		'CRISIS_SUPPORT'
	]::TEXT[] THEN
		RAISE EXCEPTION 'Local COBALT homepage must expose exactly the six expected features in display order';
	END IF;

	IF (
		SELECT ARRAY_AGG(feature_id::TEXT ORDER BY display_order, feature_id)
		FROM institution_feature
		WHERE institution_id='COBALT'
		AND nav_visible IS TRUE
	) IS DISTINCT FROM ARRAY[
		'THERAPY',
		'MEDICATION_PRESCRIBER',
		'GROUP_SESSIONS',
		'SELF_HELP_RESOURCES',
		'SPIRITUAL_SUPPORT',
		'CRISIS_SUPPORT'
	]::TEXT[] THEN
		RAISE EXCEPTION 'Local COBALT header navigation must expose exactly the six expected features in display order';
	END IF;
END
$verify$;

COMMIT;
