BEGIN;
SELECT _v.register_patch('116-faqs', NULL, NULL);

ALTER TABLE institution ADD COLUMN faq_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Groups FAQs for display purposes
CREATE TABLE faq_topic (
	faq_topic_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id VARCHAR NOT NULL REFERENCES institution,
	name VARCHAR NOT NULL,
	url_name VARCHAR NOT NULL,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON faq_topic FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX faq_topic_name_unique_idx ON faq_topic USING btree (institution_id, LOWER(name));
CREATE UNIQUE INDEX faq_topic_url_name_unique_idx ON faq_topic USING btree (institution_id, LOWER(url_name));

CREATE TABLE faq (
	faq_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	faq_topic_id UUID NOT NULL REFERENCES faq_topic,
	institution_id VARCHAR NOT NULL REFERENCES institution, -- Duplicate of what's on faq_topic to make url_name constraint w/o a separate trigger
	url_name VARCHAR NOT NULL,
	question VARCHAR NOT NULL,
	answer VARCHAR NOT NULL,
	display_order INTEGER NOT NULL,  -- Ordering within the faq_topic
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON faq FOR EACH ROW EXECUTE PROCEDURE set_last_updated();
CREATE UNIQUE INDEX faq_url_name_unique_idx ON faq USING btree (institution_id, LOWER(url_name));

COMMIT;