BEGIN;
SELECT _v.register_patch('174-faq-enhancements', NULL, NULL);

-- FAQs can optionally specify a "short" answer, e.g. for display on a list page
ALTER TABLE faq ADD COLUMN short_answer TEXT;

-- FAQs can specify whether their answer should be ellipsized.
-- e.g. a short-ish answer for display on a list page probably should not be ellipsized
ALTER TABLE faq ADD COLUMN permit_ellipsizing BOOLEAN NOT NULL DEFAULT TRUE;

-- FAQs can now have subtopics
CREATE TABLE faq_subtopic (
	faq_subtopic_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	faq_id UUID NOT NULL REFERENCES faq,
	name VARCHAR NOT NULL,
	url_name VARCHAR NOT NULL,
  description VARCHAR NOT NULL,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON faq_subtopic FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX faq_subtopic_name_unique_idx ON faq_subtopic USING btree (faq_id, LOWER(name));
CREATE UNIQUE INDEX faq_subtopic_url_name_unique_idx ON faq_subtopic USING btree (faq_id, LOWER(url_name));

-- Features can now have optional subtitles, which are overridable per-institution
ALTER TABLE feature ADD COLUMN subtitle VARCHAR;
ALTER TABLE institution_feature ADD COLUMN subtitle_override VARCHAR;

COMMIT;