BEGIN;
SELECT _v.register_patch(
	'270-local-only-team-clinic-medication-prescriber',
	ARRAY[
		'265-local-only-team-clinic-referral-provider-seed',
		'267-local-only-cobalt-homepage-features'
	],
	NULL
);

-- Make the local referral-backed TEAM Clinic provider discoverable from the
-- Medication Prescriber feature, matching the PENN Provider Booking V2 setup.
-- Reuse the institution referrer instead of duplicating the provider or
-- assigning the clinic an inaccurate PSYCHIATRIST support role.
DO $$
DECLARE
	v_institution_id CONSTANT TEXT := 'COBALT';
	v_referrer_url_name CONSTANT TEXT := 'team-clinic-pilot';
	v_target_association_id_seed CONSTANT UUID := 'c0ba1f00-0000-4000-8000-000000000002';
	v_referrer_id UUID;
	v_source_association_id UUID;
	v_target_association_id UUID;
	v_target_feature_id UUID;
	v_cta_title TEXT;
	v_cta_description TEXT;
	v_display_order INTEGER;
BEGIN
	IF (
		SELECT COUNT(*)
		FROM institution_feature
		WHERE institution_id=v_institution_id
		AND feature_id='MEDICATION_PRESCRIBER'
	) <> 1 THEN
		RAISE EXCEPTION 'Expected exactly one local COBALT Medication Prescriber institution feature.';
	END IF;

	SELECT institution_feature_id
	INTO v_target_feature_id
	FROM institution_feature
	WHERE institution_id=v_institution_id
	AND feature_id='MEDICATION_PRESCRIBER';

	IF (
		SELECT COUNT(*)
		FROM institution_referrer
		WHERE from_institution_id=v_institution_id
		AND url_name=v_referrer_url_name
	) <> 1 THEN
		RAISE EXCEPTION 'Expected exactly one local COBALT TEAM Clinic institution referrer.';
	END IF;

	SELECT institution_referrer_id
	INTO v_referrer_id
	FROM institution_referrer
	WHERE from_institution_id=v_institution_id
	AND url_name=v_referrer_url_name;

	IF (
		SELECT COUNT(*)
		FROM institution_feature_institution_referrer
		JOIN institution_feature
			ON institution_feature.institution_feature_id=institution_feature_institution_referrer.institution_feature_id
		WHERE institution_feature_institution_referrer.institution_referrer_id=v_referrer_id
		AND institution_feature.institution_id=v_institution_id
		AND institution_feature.feature_id='THERAPY'
	) <> 1 THEN
		RAISE EXCEPTION 'Expected exactly one local COBALT Therapy association for the TEAM Clinic institution referrer.';
	END IF;

	SELECT
		institution_feature_institution_referrer.institution_feature_institution_referrer_id,
		institution_feature_institution_referrer.cta_title,
		institution_feature_institution_referrer.cta_description,
		institution_feature_institution_referrer.display_order
	INTO
		v_source_association_id,
		v_cta_title,
		v_cta_description,
		v_display_order
	FROM institution_feature_institution_referrer
	JOIN institution_feature
		ON institution_feature.institution_feature_id=institution_feature_institution_referrer.institution_feature_id
	WHERE institution_feature_institution_referrer.institution_referrer_id=v_referrer_id
	AND institution_feature.institution_id=v_institution_id
	AND institution_feature.feature_id='THERAPY';

	IF EXISTS (
		SELECT 1
		FROM institution_feature_institution_referrer
		WHERE institution_feature_institution_referrer_id=v_target_association_id_seed
		AND (
			institution_feature_id<>v_target_feature_id
			OR institution_referrer_id<>v_referrer_id
		)
	) THEN
		RAISE EXCEPTION 'The deterministic local TEAM Clinic Medication Prescriber association ID is already in use.';
	END IF;

	SELECT institution_feature_institution_referrer_id
	INTO v_target_association_id
	FROM institution_feature_institution_referrer
	WHERE institution_feature_id=v_target_feature_id
	AND institution_referrer_id=v_referrer_id;

	IF v_target_association_id IS NULL THEN
		v_target_association_id := v_target_association_id_seed;

		INSERT INTO institution_feature_institution_referrer (
			institution_feature_institution_referrer_id,
			institution_feature_id,
			institution_referrer_id,
			cta_title,
			cta_description,
			display_order
		) VALUES (
			v_target_association_id,
			v_target_feature_id,
			v_referrer_id,
			v_cta_title,
			v_cta_description,
			v_display_order
		);
	ELSE
		UPDATE institution_feature_institution_referrer
		SET cta_title=v_cta_title,
			cta_description=v_cta_description,
			display_order=v_display_order
		WHERE institution_feature_institution_referrer_id=v_target_association_id;
	END IF;

	INSERT INTO institution_feature_referrer_location (
		institution_feature_institution_referrer_id,
		institution_location_id
	)
	SELECT
		v_target_association_id,
		institution_location_id
	FROM institution_feature_referrer_location
	WHERE institution_feature_institution_referrer_id=v_source_association_id
	ON CONFLICT (institution_feature_institution_referrer_id, institution_location_id) DO NOTHING;

	INSERT INTO institution_feature_referrer_account_source (
		institution_feature_institution_referrer_id,
		account_source_id
	)
	SELECT
		v_target_association_id,
		account_source_id
	FROM institution_feature_referrer_account_source
	WHERE institution_feature_institution_referrer_id=v_source_association_id
	ON CONFLICT (institution_feature_institution_referrer_id, account_source_id) DO NOTHING;

	IF (
		SELECT COUNT(*)
		FROM institution_feature_institution_referrer
		WHERE institution_feature_id=v_target_feature_id
		AND institution_referrer_id=v_referrer_id
	) <> 1 THEN
		RAISE EXCEPTION 'Local TEAM Clinic Medication Prescriber association verification failed.';
	END IF;

	IF NOT EXISTS (
		SELECT 1
		FROM provider
		JOIN provider_institution_referrer
			ON provider_institution_referrer.provider_id=provider.provider_id
		WHERE provider.institution_id=v_institution_id
		AND provider.active=TRUE
		AND provider.url_name='team-clinic'
		AND provider_institution_referrer.institution_referrer_id=v_referrer_id
	) THEN
		RAISE EXCEPTION 'The active local referral-backed TEAM Clinic provider is required.';
	END IF;

	IF EXISTS (
		(
			SELECT institution_location_id
			FROM institution_feature_referrer_location
			WHERE institution_feature_institution_referrer_id=v_source_association_id
			EXCEPT
			SELECT institution_location_id
			FROM institution_feature_referrer_location
			WHERE institution_feature_institution_referrer_id=v_target_association_id
		)
		UNION ALL
		(
			SELECT institution_location_id
			FROM institution_feature_referrer_location
			WHERE institution_feature_institution_referrer_id=v_target_association_id
			EXCEPT
			SELECT institution_location_id
			FROM institution_feature_referrer_location
			WHERE institution_feature_institution_referrer_id=v_source_association_id
		)
	) THEN
		RAISE EXCEPTION 'Local TEAM Clinic feature associations must have matching location visibility.';
	END IF;

	IF EXISTS (
		(
			SELECT account_source_id
			FROM institution_feature_referrer_account_source
			WHERE institution_feature_institution_referrer_id=v_source_association_id
			EXCEPT
			SELECT account_source_id
			FROM institution_feature_referrer_account_source
			WHERE institution_feature_institution_referrer_id=v_target_association_id
		)
		UNION ALL
		(
			SELECT account_source_id
			FROM institution_feature_referrer_account_source
			WHERE institution_feature_institution_referrer_id=v_target_association_id
			EXCEPT
			SELECT account_source_id
			FROM institution_feature_referrer_account_source
			WHERE institution_feature_institution_referrer_id=v_source_association_id
		)
	) THEN
		RAISE EXCEPTION 'Local TEAM Clinic feature associations must have matching account-source visibility.';
	END IF;
END $$;

COMMIT;
