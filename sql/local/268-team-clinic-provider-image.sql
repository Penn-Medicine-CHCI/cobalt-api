BEGIN;
SELECT _v.register_patch(
	'268-local-only-team-clinic-provider-image',
	ARRAY['265-local-only-team-clinic-referral-provider-seed'],
	NULL
);

DO $$
DECLARE
	v_image_url CONSTANT TEXT := 'https://cdn-prod.cobalt.care/providers/penn/penn-provider-placeholder-centered.png';
	v_provider_id UUID;
BEGIN
	SELECT provider.provider_id
	INTO v_provider_id
	FROM provider
	JOIN provider_institution_referrer
		ON provider_institution_referrer.provider_id=provider.provider_id
	JOIN institution_referrer
		ON institution_referrer.institution_referrer_id=provider_institution_referrer.institution_referrer_id
	WHERE provider.institution_id='COBALT'
	AND provider.url_name='team-clinic'
	AND institution_referrer.from_institution_id='COBALT'
	AND institution_referrer.to_institution_id='COBALT_IC_SELF_REFERRAL'
	AND institution_referrer.url_name='team-clinic-pilot';

	IF v_provider_id IS NULL THEN
		RAISE EXCEPTION 'The local TEAM Clinic referral-backed provider is required.';
	END IF;

	UPDATE provider
	SET image_url=v_image_url
	WHERE provider_id=v_provider_id;

	IF NOT EXISTS (
		SELECT 1
		FROM provider
		WHERE provider_id=v_provider_id
		AND image_url=v_image_url
	) THEN
		RAISE EXCEPTION 'The local TEAM Clinic provider image update failed.';
	END IF;
END $$;

COMMIT;
