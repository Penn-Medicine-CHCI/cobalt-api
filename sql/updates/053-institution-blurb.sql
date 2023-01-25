BEGIN;
SELECT _v.register_patch('053-institution-blurb', NULL, NULL);

CREATE TABLE institution_blurb_type (
  institution_blurb_type_id VARCHAR PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO institution_blurb_type VALUES ('INTRO', 'Intro');
INSERT INTO institution_blurb_type VALUES ('TEAM', 'Team');
INSERT INTO institution_blurb_type VALUES ('ABOUT', 'About');

CREATE TABLE institution_team_member (
	institution_team_member_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id VARCHAR NOT NULL REFERENCES institution,
	name VARCHAR NOT NULL,
	title VARCHAR NOT NULL,
	image_url VARCHAR NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_team_member FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE institution_blurb (
  institution_blurb_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id VARCHAR NOT NULL REFERENCES institution,
	institution_blurb_type_id VARCHAR NOT NULL REFERENCES institution_blurb_type,
	title TEXT,
	description TEXT,
	short_description TEXT,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX institution_blurb_type_unique_idx ON institution_blurb USING btree (institution_id, institution_blurb_type_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_blurb FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE institution_blurb_team_member (
	institution_blurb_id UUID NOT NULL REFERENCES institution_blurb,
	institution_team_member_id UUID NOT NULL REFERENCES institution_team_member,
	display_order INTEGER NOT NULL DEFAULT 1,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	PRIMARY KEY (institution_blurb_id, institution_team_member_id)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON institution_blurb_team_member FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;