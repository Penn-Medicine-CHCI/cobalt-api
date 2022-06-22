BEGIN;
SELECT _v.register_patch('021-screening', NULL, NULL);

CREATE TABLE screening_version_status (
	screening_version_status_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO screening_version_status (screening_version_status_id, description) VALUES ('DRAFT', 'Draft');
INSERT INTO screening_version_status (screening_version_status_id, description) VALUES ('PUBLISHED', 'Published');
INSERT INTO screening_version_status (screening_version_status_id, description) VALUES ('ARCHIVED', 'Archived');

CREATE TABLE screening_type (
	screening_type_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO screening_type (screening_type_id, description) VALUES ('OTHER', 'Other');
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

CREATE TABLE screening (
	screening_id UUID PRIMARY KEY,
	institution_id TEXT NOT NULL REFERENCES institution,
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE screening_version (
	screening_version_id UUID PRIMARY KEY,
	screening_id UUID NOT NULL REFERENCES screening,
	screening_version_status_id TEXT NOT NULL REFERENCES screening_version_status,
	screening_type_id TEXT NOT NULL REFERENCES screening_type,
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	version_number INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	UNIQUE (screening_id, version_number)
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_version FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- A session is tied to browser tab.  You can have many sessions at once and not step on each other.  The created_by_account_id is who is
-- actually taking the survey, the target_account_id is who the results should be tied to
-- (e.g. an MHIC could take an assessment on behalf of someone else)
CREATE TABLE screening_session (
	screening_session_id UUID PRIMARY KEY,
	target_account_id UUID NOT NULL REFERENCES account (account_id),
	created_by_account_id UUID NOT NULL REFERENCES account (account_id),
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_session FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- How are the answer[s] to the question formatted (single-select, multi-select, freeform, ...)?
CREATE TABLE screening_answer_format (
	screening_answer_format_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO screening_answer_format (screening_answer_format_id, description) VALUES ('SINGLE_SELECT', 'Single-select');
INSERT INTO screening_answer_format (screening_answer_format_id, description) VALUES ('MULTI_SELECT', 'Multi-select');
INSERT INTO screening_answer_format (screening_answer_format_id, description) VALUES ('FREEFORM_TEXT', 'Freeform text');

-- e.g. "This question is asking for a phone number" (normally used with FREEFORM_TEXT answer format)
CREATE TABLE screening_question_content_hint (
	screening_question_content_hint_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO screening_question_content_hint (screening_question_content_hint_id, description) VALUES ('NONE', 'None');
INSERT INTO screening_question_content_hint (screening_question_content_hint_id, description) VALUES ('FIRST_NAME', 'First Name');
INSERT INTO screening_question_content_hint (screening_question_content_hint_id, description) VALUES ('LAST_NAME', 'Last Name');
INSERT INTO screening_question_content_hint (screening_question_content_hint_id, description) VALUES ('FULL_NAME', 'Full Name');
INSERT INTO screening_question_content_hint (screening_question_content_hint_id, description) VALUES ('PHONE_NUMBER', 'Phone Number');
INSERT INTO screening_question_content_hint (screening_question_content_hint_id, description) VALUES ('EMAIL_ADDRESS', 'Email Address');

-- e.g. “How often are you feeling depressed?”
CREATE TABLE screening_question (
	screening_question_id UUID PRIMARY KEY,
	screening_version_id UUID NOT NULL REFERENCES screening_version,
	screening_answer_format_id TEXT NOT NULL REFERENCES screening_answer_format,
	screening_question_content_hint_id TEXT NOT NULL REFERENCES screening_question_content_hint,
	display_order INTEGER NOT NULL,
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_question FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- A canned answer option, e.g. “Every day”
CREATE TABLE screening_answer_option (
	screening_answer_option_id UUID PRIMARY KEY,
	screening_question_id UUID NOT NULL REFERENCES screening_question,
	text TEXT, -- Usage depends on question format, e.g. single-select option value or freeform text placeholder value
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_answer_option FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- What the user actually answers
CREATE TABLE screening_answer (
  screening_answer_id UUID PRIMARY KEY,
	screening_answer_option_id UUID NOT NULL REFERENCES screening_answer_option,
	screening_session_id UUID NULL REFERENCES screening_session,
	text TEXT, -- Usage depends on question format, currently used to hold freeform text value entered by user
	created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	updated TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON screening_answer FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;