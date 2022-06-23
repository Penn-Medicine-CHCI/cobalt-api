BEGIN;
SELECT _v.register_patch('021-screening', NULL, NULL);

CREATE TABLE screening_type (
	screening_type_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO screening_type (screening_type_id, description) VALUES ('CUSTOM', 'Custom');
INSERT INTO screening_type (screening_type_id, description) VALUES ('GAD_2', 'GAD-2');
INSERT INTO screening_type (screening_type_id, description) VALUES ('GAD_7', 'GAD-7');
INSERT INTO screening_type (screening_type_id, description) VALUES ('PHQ_4', 'PHQ-4');
INSERT INTO screening_type (screening_type_id, description) VALUES ('PHQ_8', 'PHQ-8');
INSERT INTO screening_type (screening_type_id, description) VALUES ('PHQ_9', 'PHQ-9');
INSERT INTO screening_type (screening_type_id, description) VALUES ('WHO_5', 'WHO-5');
INSERT INTO screening_type (screening_type_id, description) VALUES ('PC_PTSD_5', 'PC-PTSD-5');
INSERT INTO screening_type (screening_type_id, description) VALUES ('AUDIT_C_ALCOHOL', 'AUDIT-C (Alcohol)');
INSERT INTO screening_type (screening_type_id, description) VALUES ('CAGE_ALCOHOL', 'CAGE (Alcohol)');
INSERT INTO screening_type (screening_type_id, description) VALUES ('TICS', 'TICS (Drugs)');
INSERT INTO screening_type (screening_type_id, description) VALUES ('ISI', 'ISI');
INSERT INTO screening_type (screening_type_id, description) VALUES ('ASRM', 'ASRM');
INSERT INTO screening_type (screening_type_id, description) VALUES ('C_SSRS', 'C-SSRS');
INSERT INTO screening_type (screening_type_id, description) VALUES ('DAST_10', 'DAST-10');
INSERT INTO screening_type (screening_type_id, description) VALUES ('BPI', 'BPI');
INSERT INTO screening_type (screening_type_id, description) VALUES ('AUDIT_C', 'AUDIT-C');

-- An individual screening (set of questions and answers), e.g. PHQ-9, GAD-7, or custom screening
CREATE TABLE screening (
	screening_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id TEXT NOT NULL REFERENCES institution,
	name TEXT NOT NULL,
	active_screening_version_id UUID, -- Circular; a 'REFERENCES screening_version' is added later
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX screening_institution_name_idx ON screening (institution_id, LOWER(TRIM(name)));
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Screenings are versioned, so history is fully preserved if changes are made.
-- Only one active version of a screening can exist at a time
CREATE TABLE screening_version (
	screening_version_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	screening_id UUID NOT NULL REFERENCES screening,
	screening_type_id TEXT NOT NULL REFERENCES screening_type,
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	version_number INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	UNIQUE (screening_id, version_number)
);

ALTER TABLE screening ADD CONSTRAINT screening_active_screening_version_fk FOREIGN KEY (active_screening_version_id) REFERENCES screening_version (screening_version_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_version FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Logical "flow" manager for a set of one or more screenings.
-- Example would be "1:1 Initial Screening", which might have WHO-5, PHQ-9, GAD-7, etc. and rules about how to transition and score.
CREATE TABLE screening_template (
	screening_template_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	institution_id TEXT NOT NULL REFERENCES institution,
	active_screening_template_version_id UUID, -- Circular; a 'REFERENCES screening_template_version' is added later
	name TEXT NOT NULL,
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX screening_template_institution_name_idx ON screening_template (institution_id, LOWER(TRIM(name)));
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_template FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Screening templates are versioned, so history is fully preserved if changes are made.
-- Only one active version of a screening template can exist at a time
CREATE TABLE screening_template_version (
	screening_template_version_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	screening_template_id UUID NOT NULL REFERENCES screening_template,
	initial_screening_id UUID NOT NULL REFERENCES screening (screening_id),
	orchestration_function TEXT NOT NULL, -- Javascript code, invoked every time an answer is given to a screening question
	scoring_function TEXT NOT NULL, -- Javascript code, invoked once a screening session transitions to COMPLETE status
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE screening_template ADD CONSTRAINT screening_template_active_screening_template_version_fk FOREIGN KEY (active_screening_template_version_id) REFERENCES screening_template_version (screening_template_version_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_template_version FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE screening_session_status (
	screening_session_status_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO screening_session_status (screening_session_status_id, description) VALUES ('ACTIVE', 'Active');
INSERT INTO screening_session_status (screening_session_status_id, description) VALUES ('COMPLETE', 'Complete');
INSERT INTO screening_session_status (screening_session_status_id, description) VALUES ('ABANDONED', 'Abandoned');

-- A session ties an instance of a screening template to browser tab to track progress through it.
-- A user could have many sessions at once and they would not step on each other.
-- The created_by_account_id is who is actually taking the screenings, the target_account_id is who the results should be tied to,
-- e.g. an MHIC could take a screening on behalf of someone else
CREATE TABLE screening_session (
	screening_session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	screening_template_version_id UUID NOT NULL REFERENCES screening_template_version,
	target_account_id UUID NOT NULL REFERENCES account (account_id),
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	screening_session_status_id TEXT NOT NULL REFERENCES screening_session_status DEFAULT 'ACTIVE',
	crisis_indicated BOOLEAN NOT NULL DEFAULT FALSE,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_session FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- As output, a screening session could recommend one or more support roles
CREATE TABLE screening_session_support_role_recommendation (
	screening_session_id UUID NOT NULL REFERENCES screening_session,
	support_role_id TEXT NOT NULL REFERENCES support_role,
	PRIMARY KEY (screening_session_id, support_role_id)
);

-- Keeps track of which screening version[s] are answered during a session, and in what order
CREATE TABLE screening_session_context (
	screening_session_context_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	screening_session_id UUID NOT NULL REFERENCES screening_session,
	screening_version_id UUID NOT NULL REFERENCES screening_version,
	screening_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_session_context FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- Notes logged during orchestration, used for humans to review transitions/scoring/etc.
CREATE TABLE screening_session_note (
	screening_session_note_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	screening_session_context_id UUID NOT NULL REFERENCES screening_session_context,
	note TEXT NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_session_note FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- How are the answer[s] to the question formatted (single-select, multi-select, freeform, ...)?
CREATE TABLE screening_answer_format (
	screening_answer_format_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO screening_answer_format (screening_answer_format_id, description) VALUES ('SINGLE_SELECT', 'Single-select');
INSERT INTO screening_answer_format (screening_answer_format_id, description) VALUES ('MULTI_SELECT', 'Multi-select');
INSERT INTO screening_answer_format (screening_answer_format_id, description) VALUES ('FREEFORM_TEXT', 'Freeform text');

-- e.g. "This question is asking for a phone number" (normally used with FREEFORM_TEXT answer format)
CREATE TABLE screening_answer_content_hint (
	screening_answer_content_hint_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO screening_answer_content_hint (screening_answer_content_hint_id, description) VALUES ('NONE', 'None');
INSERT INTO screening_answer_content_hint (screening_answer_content_hint_id, description) VALUES ('FIRST_NAME', 'First Name');
INSERT INTO screening_answer_content_hint (screening_answer_content_hint_id, description) VALUES ('LAST_NAME', 'Last Name');
INSERT INTO screening_answer_content_hint (screening_answer_content_hint_id, description) VALUES ('FULL_NAME', 'Full Name');
INSERT INTO screening_answer_content_hint (screening_answer_content_hint_id, description) VALUES ('PHONE_NUMBER', 'Phone Number');
INSERT INTO screening_answer_content_hint (screening_answer_content_hint_id, description) VALUES ('EMAIL_ADDRESS', 'Email Address');

-- e.g. “How often are you feeling depressed?”
CREATE TABLE screening_question (
	screening_question_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	screening_version_id UUID NOT NULL REFERENCES screening_version,
	screening_answer_format_id TEXT NOT NULL REFERENCES screening_answer_format,
	screening_answer_content_hint_id TEXT NOT NULL REFERENCES screening_answer_content_hint DEFAULT 'NONE',
	intro_text TEXT,
	question_text TEXT NOT NULL,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_question FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- A canned answer option, e.g. “Every day”
CREATE TABLE screening_answer_option (
	screening_answer_option_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	screening_question_id UUID NOT NULL REFERENCES screening_question,
	answer_option_text TEXT, -- Display/usage depends on question format, e.g. could be single-select option value or freeform text placeholder
	score INTEGER NOT NULL,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_answer_option FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- What the user actually answers
CREATE TABLE screening_answer (
  screening_answer_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	screening_answer_option_id UUID NOT NULL REFERENCES screening_answer_option,
	screening_session_context_id UUID NOT NULL REFERENCES screening_session_context,
	text TEXT, -- Usage depends on question format, currently used to hold freeform text value entered by user
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_answer FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;