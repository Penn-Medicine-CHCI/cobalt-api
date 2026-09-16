\set ON_ERROR_STOP on

BEGIN;

DO $$
DECLARE
	v_epic_department_id UUID;
	v_expected_provider_count INTEGER := 4;
	v_mapped_provider_count INTEGER;
BEGIN
	SELECT epic_department_id
	INTO v_epic_department_id
	FROM epic_department
	WHERE institution_id='COBALT_IC'
	AND department_id='9999'
	AND name='ZZZTest Department';

	IF v_epic_department_id IS NULL THEN
		RAISE EXCEPTION 'The local COBALT_IC ZZZTest Department fixture is required.';
	END IF;

	-- BlueJeans is retired and native booking now rejects it. These are local
	-- fixtures, so use external videoconferencing without making a live API call.
	UPDATE provider
	SET videoconference_platform_id='EXTERNAL'
	WHERE institution_id='COBALT_IC'
	AND email_address IN (
		'agrayson+mhp@xmog.com',
		'jess.gaito+mhp@xmog.com',
		'maa+mhp@xmog.com',
		'mark+mhp@xmog.com'
	);

	INSERT INTO provider_epic_department (
		provider_id,
		epic_department_id,
		display_order
	)
	SELECT
		provider.provider_id,
		v_epic_department_id,
		ROW_NUMBER() OVER (ORDER BY provider.name)::SMALLINT
	FROM provider
	WHERE provider.institution_id='COBALT_IC'
	AND provider.email_address IN (
		'agrayson+mhp@xmog.com',
		'jess.gaito+mhp@xmog.com',
		'maa+mhp@xmog.com',
		'mark+mhp@xmog.com'
	)
	ON CONFLICT (provider_id, epic_department_id) DO UPDATE
	SET display_order=EXCLUDED.display_order;

	SELECT COUNT(*)
	INTO v_mapped_provider_count
	FROM provider_epic_department
	WHERE epic_department_id=v_epic_department_id
	AND provider_id IN (
		SELECT provider_id
		FROM provider
		WHERE institution_id='COBALT_IC'
		AND email_address IN (
			'agrayson+mhp@xmog.com',
			'jess.gaito+mhp@xmog.com',
			'maa+mhp@xmog.com',
			'mark+mhp@xmog.com'
		)
	);

	IF v_mapped_provider_count != v_expected_provider_count THEN
		RAISE EXCEPTION 'Expected % local COBALT_IC MHP fixture mappings for ZZZTest Department, found %',
			v_expected_provider_count,
			v_mapped_provider_count;
	END IF;
END $$;

COMMIT;
