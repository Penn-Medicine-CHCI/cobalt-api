BEGIN;
SELECT _v.register_patch('121-irg-images', NULL, NULL);

ALTER TABLE institution_resource_group DROP COLUMN color_id;
ALTER TABLE institution_resource_group ADD COLUMN background_color_value_id TEXT REFERENCES color_value NOT NULL DEFAULT 'P100';
ALTER TABLE institution_resource_group ADD COLUMN text_color_value_id TEXT REFERENCES color_value NOT NULL DEFAULT 'P700';
ALTER TABLE institution_resource_group ADD COLUMN image_url TEXT;

CREATE VIEW v_institution_resource_group AS
SELECT
	irg.*,
	cv.name as background_color_value_name,
	cv.color_id as background_color_id,
	icv.css_representation as background_color_value_css_representation,
	cv2.name as text_color_value_name,
	cv2.color_id as text_color_id,
	icv2.css_representation as text_color_value_css_representation
FROM
	institution_resource_group irg,
	institution_color_value icv,
	color_value cv,
	institution_color_value icv2,
	color_value cv2
WHERE
	irg.institution_id=icv.institution_id
	AND icv.color_value_id=cv.color_value_id
	AND irg.background_color_value_id=icv.color_value_id
	AND irg.institution_id=icv2.institution_id
	AND icv2.color_value_id=cv2.color_value_id
	AND irg.text_color_value_id=icv2.color_value_id;

COMMIT;