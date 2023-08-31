BEGIN;
SELECT _v.register_patch('117-fhir-institution-updates', NULL, NULL);

ALTER TABLE institution ADD COLUMN external_contact_us_url TEXT;
ALTER TABLE institution ADD COLUMN mychart_instructions_url TEXT;

INSERT INTO color VALUES ('NEUTRAL', 'Neutral');
INSERT INTO color VALUES ('SEMANTIC_ATTENTION', 'Semantic Attention');

CREATE TABLE color_value (
  color_value_id TEXT PRIMARY KEY,
  color_id VARCHAR NOT NULL REFERENCES color,
  name VARCHAR NOT NULL
);

CREATE UNIQUE INDEX color_value_unique_idx ON color_value USING btree (color_id, name);

INSERT INTO color_value(color_value_id, color_id, name) VALUES
  ('N0', 'NEUTRAL', 'n0'),
  ('N50', 'NEUTRAL', 'n50'),
  ('N75', 'NEUTRAL', 'n75'),
  ('N100', 'NEUTRAL', 'n100'),
  ('N300', 'NEUTRAL', 'n300'),
  ('N500', 'NEUTRAL', 'n500'),
  ('N700', 'NEUTRAL', 'n700'),
  ('N900', 'NEUTRAL', 'n900'),

  ('P50', 'BRAND_PRIMARY', 'p50'),
  ('P100', 'BRAND_PRIMARY', 'p100'),
  ('P300', 'BRAND_PRIMARY', 'p300'),
  ('P500', 'BRAND_PRIMARY', 'p500'),
  ('P700', 'BRAND_PRIMARY', 'p700'),
  ('P900', 'BRAND_PRIMARY', 'p900'),

  ('A50', 'BRAND_ACCENT', 'a50'),
  ('A100', 'BRAND_ACCENT', 'a100'),
  ('A300', 'BRAND_ACCENT', 'a300'),
  ('A500', 'BRAND_ACCENT', 'a500'),
  ('A700', 'BRAND_ACCENT', 'a700'),
  ('A900', 'BRAND_ACCENT', 'a900'),

  ('D50', 'SEMANTIC_DANGER', 'd50'),
  ('D100', 'SEMANTIC_DANGER', 'd100'),
  ('D300', 'SEMANTIC_DANGER', 'd300'),
  ('D500', 'SEMANTIC_DANGER', 'd500'),
  ('D700', 'SEMANTIC_DANGER', 'd700'),
  ('D900', 'SEMANTIC_DANGER', 'd900'),

  ('W50', 'SEMANTIC_WARNING', 'w50'),
  ('W100', 'SEMANTIC_WARNING', 'w100'),
  ('W300', 'SEMANTIC_WARNING', 'w300'),
  ('W500', 'SEMANTIC_WARNING', 'w500'),
  ('W700', 'SEMANTIC_WARNING', 'w700'),
  ('W900', 'SEMANTIC_WARNING', 'w900'),

  ('S50', 'SEMANTIC_SUCCESS', 's50'),
  ('S100', 'SEMANTIC_SUCCESS', 's100'),
  ('S300', 'SEMANTIC_SUCCESS', 's300'),
  ('S500', 'SEMANTIC_SUCCESS', 's500'),
  ('S700', 'SEMANTIC_SUCCESS', 's700'),
  ('S900', 'SEMANTIC_SUCCESS', 's900'),

  ('I50', 'SEMANTIC_INFO', 'i50'),
  ('I100', 'SEMANTIC_INFO', 'i100'),
  ('I300', 'SEMANTIC_INFO', 'i300'),
  ('I500', 'SEMANTIC_INFO', 'i500'),
  ('I700', 'SEMANTIC_INFO', 'i700'),
  ('I900', 'SEMANTIC_INFO', 'i900'),

  ('T50', 'SEMANTIC_INFO', 't50'),
  ('T100', 'SEMANTIC_INFO', 't100'),
  ('T300', 'SEMANTIC_INFO', 't300'),
  ('T500', 'SEMANTIC_INFO', 't500'),
  ('T700', 'SEMANTIC_INFO', 't700'),
  ('T900', 'SEMANTIC_INFO', 't900');

CREATE TABLE institution_color_value (
  institution_id VARCHAR NOT NULL REFERENCES institution,
  color_value_id VARCHAR NOT NULL REFERENCES color_value,
  css_representation VARCHAR NOT NULL,
  created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (institution_id, color_value_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_color_value FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE VIEW v_institution_color_value AS
SELECT icv.*, cv.name
FROM institution_color_value icv, color_value cv
WHERE icv.color_value_id=cv.color_value_id;

COMMIT;