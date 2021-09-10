/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Originally created at the University of Pennsylvania and Penn Medicine by:
 * Dr. David Asch; Dr. Lisa Bellini; Dr. Cecilia Livesey; Kelley Kugler; and Dr. Matthew Press.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
BEGIN;
SELECT _v.register_patch('000-base-creates', NULL, NULL);

CREATE FUNCTION set_last_updated() RETURNS TRIGGER AS $$
BEGIN
	NEW.last_updated := 'now';
	RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- cobalt.account_source definition

-- Drop table

-- DROP TABLE account_source;

CREATE TABLE account_source (
	account_source_id varchar NOT NULL,
	description varchar NOT NULL,
	prod_sso_url varchar NULL,
	dev_sso_url varchar NULL,
	local_sso_url varchar NULL,
	CONSTRAINT account_source_pkey PRIMARY KEY (account_source_id)
);


-- cobalt.activity_action definition

-- Drop table

-- DROP TABLE activity_action;

CREATE TABLE activity_action (
	activity_action_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT activity_action_pkey PRIMARY KEY (activity_action_id)
);


-- cobalt.activity_type definition

-- Drop table

-- DROP TABLE activity_type;

CREATE TABLE activity_type (
	activity_type_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT activity_type_pkey PRIMARY KEY (activity_type_id)
);


-- cobalt.appointment_reason_type definition

-- Drop table

-- DROP TABLE appointment_reason_type;

CREATE TABLE appointment_reason_type (
	appointment_reason_type_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT appointment_reason_type_pkey PRIMARY KEY (appointment_reason_type_id)
);


-- cobalt.approval_status definition

-- Drop table

-- DROP TABLE approval_status;

CREATE TABLE approval_status (
	approval_status_id varchar NOT NULL,
	description varchar NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT approval_status_pkey PRIMARY KEY (approval_status_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.approval_status for each row execute procedure set_last_updated();


-- cobalt.assessment_type definition

-- Drop table

-- DROP TABLE assessment_type;

CREATE TABLE assessment_type (
	assessment_type_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT assessment_type_pkey PRIMARY KEY (assessment_type_id)
);


-- cobalt.attendance_status definition

-- Drop table

-- DROP TABLE attendance_status;

CREATE TABLE attendance_status (
	attendance_status_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT attendance_status_pkey PRIMARY KEY (attendance_status_id)
);


-- cobalt.audit_log_event definition

-- Drop table

-- DROP TABLE audit_log_event;

CREATE TABLE audit_log_event (
	audit_log_event_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT audit_log_event_pkey PRIMARY KEY (audit_log_event_id)
);


-- cobalt.available_status definition

-- Drop table

-- DROP TABLE available_status;

CREATE TABLE available_status (
	available_status_id varchar NOT NULL,
	description varchar NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT available_status_pkey PRIMARY KEY (available_status_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.available_status for each row execute procedure set_last_updated();


-- cobalt.beta_feature definition

-- Drop table

-- DROP TABLE beta_feature;

CREATE TABLE beta_feature (
	beta_feature_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT beta_feature_pkey PRIMARY KEY (beta_feature_id)
);


-- cobalt.beta_status definition

-- Drop table

-- DROP TABLE beta_status;

CREATE TABLE beta_status (
	beta_status_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT beta_status_pkey PRIMARY KEY (beta_status_id)
);


-- cobalt.category definition

-- Drop table

-- DROP TABLE category;

CREATE TABLE category (
	category_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT category_pkey PRIMARY KEY (category_id)
);

-- cobalt.content_action definition

-- Drop table

-- DROP TABLE content_action;

CREATE TABLE content_action (
	content_action_id varchar NOT NULL,
	decription varchar NOT NULL,
	CONSTRAINT content_action_pkey PRIMARY KEY (content_action_id)
);


-- cobalt.content_type definition

-- Drop table

-- DROP TABLE content_type;

CREATE TABLE content_type (
	content_type_id varchar NOT NULL,
	description varchar NOT NULL,
	call_to_action varchar NOT NULL,
	CONSTRAINT content_type_pkey PRIMARY KEY (content_type_id)
);


-- cobalt.content_type_label definition

-- Drop table

-- DROP TABLE content_type_label;

CREATE TABLE content_type_label (
	content_type_label_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT content_type_label_pkey PRIMARY KEY (content_type_label_id)
);


-- cobalt.epic_appointment_filter definition

-- Drop table

-- DROP TABLE epic_appointment_filter;

CREATE TABLE epic_appointment_filter (
	epic_appointment_filter_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT epic_appointment_filter_pkey PRIMARY KEY (epic_appointment_filter_id)
);


-- cobalt.followup_email_status definition

-- Drop table

-- DROP TABLE followup_email_status;

CREATE TABLE followup_email_status (
	followup_email_status_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT followup_email_status_pkey PRIMARY KEY (followup_email_status_id)
);


-- cobalt.group_event_type definition

-- Drop table

-- DROP TABLE group_event_type;

CREATE TABLE group_event_type (
	group_event_type_id uuid NOT NULL,
	acuity_calendar_id int8 NOT NULL,
	acuity_appointment_type_id int8 NOT NULL,
	videoconference_url text NOT NULL,
	institution_id varchar NOT NULL,
	url_name varchar NOT NULL,
	image_url varchar NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT group_event_type_pkey PRIMARY KEY (group_event_type_id)
);
CREATE UNIQUE INDEX group_event_type_acuity_appointment_type_id_idx ON cobalt.group_event_type USING btree (acuity_appointment_type_id);
CREATE UNIQUE INDEX group_event_type_url_name_idx ON cobalt.group_event_type USING btree (url_name);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.group_event_type for each row execute procedure set_last_updated();


-- cobalt.group_session_request_status definition

-- Drop table

-- DROP TABLE group_session_request_status;

CREATE TABLE group_session_request_status (
	group_session_request_status_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT group_session_request_status_pkey PRIMARY KEY (group_session_request_status_id)
);


-- cobalt.group_session_scheduling_system definition

-- Drop table

-- DROP TABLE group_session_scheduling_system;

CREATE TABLE group_session_scheduling_system (
	group_session_scheduling_system_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT group_session_scheduling_system_pkey PRIMARY KEY (group_session_scheduling_system_id)
);


-- cobalt.group_session_status definition

-- Drop table

-- DROP TABLE group_session_status;

CREATE TABLE group_session_status (
	group_session_status_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT group_session_status_pkey PRIMARY KEY (group_session_status_id)
);


-- cobalt.group_session_system definition

-- Drop table

-- DROP TABLE group_session_system;

CREATE TABLE group_session_system (
	group_session_system_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT group_session_system_pkey PRIMARY KEY (group_session_system_id)
);


-- cobalt.institution definition

-- Drop table

-- DROP TABLE institution;

CREATE TABLE institution (
	institution_id varchar NOT NULL,
	"name" varchar NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	crisis_content varchar NOT NULL,
	privacy_content varchar NOT NULL,
	covid_content varchar NOT NULL,
	subdomain varchar NOT NULL,
	group_session_system_id varchar NOT NULL DEFAULT 'NATIVE'::character varying,
	time_zone varchar NOT NULL DEFAULT 'America/New_York'::character varying,
	locale varchar NOT NULL DEFAULT 'en-US'::character varying,
	require_consent_form bool NOT NULL DEFAULT false,
	consent_form_content varchar NULL,
	support_enabled bool NOT NULL DEFAULT false,
	calendar_description varchar NULL,
	well_being_content varchar NULL,
	sso_enabled bool NOT NULL DEFAULT false,
	anonymous_enabled bool NOT NULL DEFAULT true,
	email_enabled bool NOT NULL DEFAULT false,
	CONSTRAINT institution_pkey PRIMARY KEY (institution_id)
);
CREATE UNIQUE INDEX idx_institution_subdomain ON cobalt.institution USING btree (subdomain);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.institution for each row execute procedure set_last_updated();
    

-- cobalt.login_destination definition

-- Drop table

-- DROP TABLE login_destination;

CREATE TABLE login_destination (
	login_destination_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT login_destination_pkey PRIMARY KEY (login_destination_id)
);


-- cobalt.message_status definition

-- Drop table

-- DROP TABLE message_status;

CREATE TABLE message_status (
	message_status_id text NOT NULL,
	description text NOT NULL,
	CONSTRAINT message_status_pkey PRIMARY KEY (message_status_id)
);


-- cobalt.message_type definition

-- Drop table

-- DROP TABLE message_type;

CREATE TABLE message_type (
	message_type_id text NOT NULL,
	description text NOT NULL,
	CONSTRAINT message_type_pkey PRIMARY KEY (message_type_id)
);

-- cobalt.payment_funding definition

-- Drop table

-- DROP TABLE payment_funding;

CREATE TABLE payment_funding (
	payment_funding_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT payment_funding_pkey PRIMARY KEY (payment_funding_id)
);


-- cobalt.question_type definition

-- Drop table

-- DROP TABLE question_type;

CREATE TABLE question_type (
	question_type_id varchar NOT NULL,
	description varchar NOT NULL,
	allow_multiple_answers bool NOT NULL DEFAULT false,
	requires_text_response bool NOT NULL DEFAULT false,
	CONSTRAINT question_type_pkey PRIMARY KEY (question_type_id)
);


-- cobalt.recommendation_level definition

-- Drop table

-- DROP TABLE recommendation_level;

CREATE TABLE recommendation_level (
	recommendation_level_id varchar NOT NULL,
	description text NOT NULL,
	CONSTRAINT recommendation_level_pkey PRIMARY KEY (recommendation_level_id)
);


-- cobalt."role" definition

-- Drop table

-- DROP TABLE "role";

CREATE TABLE "role" (
	role_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT role_pkey PRIMARY KEY (role_id)
);


-- cobalt.scheduling_system definition

-- Drop table

-- DROP TABLE scheduling_system;

CREATE TABLE scheduling_system (
	scheduling_system_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT scheduling_system_pkey PRIMARY KEY (scheduling_system_id)
);


-- cobalt.short_url definition

-- Drop table

-- DROP TABLE short_url;

CREATE TABLE short_url (
	short_url_id int8 NOT NULL,
	encoded_identifier text NOT NULL,
	url text NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT short_url_pkey PRIMARY KEY (short_url_id)
);
CREATE UNIQUE INDEX short_url_encoded_identifier_unique_idx ON cobalt.short_url USING btree (encoded_identifier);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.short_url for each row execute procedure set_last_updated();


-- cobalt.source_system definition

-- Drop table

-- DROP TABLE source_system;

CREATE TABLE source_system (
	source_system_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT source_system_pkey PRIMARY KEY (source_system_id)
);


-- cobalt.spatial_ref_sys definition

-- Drop table

-- DROP TABLE spatial_ref_sys;

CREATE TABLE spatial_ref_sys (
	srid int4 NOT NULL,
	auth_name varchar(256) NULL,
	auth_srid int4 NULL,
	srtext varchar(2048) NULL,
	proj4text varchar(2048) NULL,
	CONSTRAINT spatial_ref_sys_pkey PRIMARY KEY (srid),
	CONSTRAINT spatial_ref_sys_srid_check CHECK (((srid > 0) AND (srid <= 998999)))
);


-- cobalt.support_role definition

-- Drop table

-- DROP TABLE support_role;

CREATE TABLE support_role (
	support_role_id varchar NOT NULL,
	description varchar NOT NULL,
	display_order int4 NOT NULL,
	CONSTRAINT support_role_pkey PRIMARY KEY (support_role_id)
);

-- cobalt.system_affinity definition

-- Drop table

-- DROP TABLE system_affinity;

CREATE TABLE system_affinity (
	system_affinity_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT system_affinity_pkey PRIMARY KEY (system_affinity_id)
);


-- cobalt.videoconference_platform definition

-- Drop table

-- DROP TABLE videoconference_platform;

CREATE TABLE videoconference_platform (
	videoconference_platform_id varchar NOT NULL,
	description varchar NOT NULL,
	CONSTRAINT videoconference_platform_pkey PRIMARY KEY (videoconference_platform_id)
);


-- cobalt.visibility definition

-- Drop table

-- DROP TABLE visibility;

CREATE TABLE visibility (
	visibility_id varchar NOT NULL,
	description varchar NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT visibility_pkey PRIMARY KEY (visibility_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.visibility for each row execute procedure set_last_updated();


-- cobalt.visit_type definition

-- Drop table

-- DROP TABLE visit_type;

CREATE TABLE visit_type (
	visit_type_id varchar NOT NULL,
	description varchar NOT NULL,
	display_order int2 NOT NULL,
	CONSTRAINT visit_type_pkey PRIMARY KEY (visit_type_id)
);


-- cobalt.account_invite definition

-- Drop table

-- DROP TABLE account_invite;

CREATE TABLE account_invite (
	account_invite_id uuid NOT NULL,
	institution_id varchar NOT NULL,
	email_address varchar NOT NULL,
	"password" varchar NOT NULL,
	account_invite_code uuid NOT NULL,
	claimed bool NOT NULL DEFAULT false,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT account_invite_pkey PRIMARY KEY (account_invite_id),
	CONSTRAINT account_invite_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.account_invite for each row execute procedure set_last_updated();


-- cobalt.appointment_reason definition

-- Drop table

-- DROP TABLE appointment_reason;

CREATE TABLE appointment_reason (
	appointment_reason_id uuid NOT NULL DEFAULT uuid_generate_v4(),
	appointment_reason_type_id varchar NOT NULL,
	institution_id varchar NOT NULL,
	description varchar NOT NULL,
	color varchar NOT NULL,
	display_order int2 NOT NULL,
	CONSTRAINT appointment_reason_pkey PRIMARY KEY (appointment_reason_id),
	CONSTRAINT appointment_reason_appointment_reason_type_id_fkey FOREIGN KEY (appointment_reason_type_id) REFERENCES appointment_reason_type(appointment_reason_type_id),
	CONSTRAINT appointment_reason_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id)
);


-- cobalt.appointment_type definition

-- Drop table

-- DROP TABLE appointment_type;

CREATE TABLE appointment_type (
	appointment_type_id uuid NOT NULL,
	acuity_appointment_type_id int8 NULL,
	"name" varchar NOT NULL,
	description varchar NULL,
	duration_in_minutes int8 NOT NULL,
	deleted bool NULL DEFAULT false,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	scheduling_system_id text NOT NULL DEFAULT 'ACUITY'::text,
	epic_visit_type_id text NULL,
	epic_visit_type_id_type text NULL,
	visit_type_id varchar NOT NULL,
	CONSTRAINT appointment_type_pkey PRIMARY KEY (appointment_type_id),
	CONSTRAINT appointment_type_visit_type_id_fkey FOREIGN KEY (visit_type_id) REFERENCES visit_type(visit_type_id),
	CONSTRAINT scheduling_system_fk FOREIGN KEY (scheduling_system_id) REFERENCES scheduling_system(scheduling_system_id)
);
CREATE UNIQUE INDEX appointment_type_acuity_appointment_type_id_idx ON cobalt.appointment_type USING btree (acuity_appointment_type_id);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.appointment_type for each row execute procedure set_last_updated();


-- cobalt.assessment definition

-- Drop table

-- DROP TABLE assessment;

CREATE TABLE assessment (
	assessment_id uuid NOT NULL,
	assessment_type_id varchar NOT NULL,
	base_question varchar NULL,
	next_assessment_id uuid NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	minimum_eligibility_score int4 NOT NULL DEFAULT 0,
	ineligible_message varchar NULL,
	answers_may_contain_pii bool NOT NULL DEFAULT false,
	CONSTRAINT assessment_pkey PRIMARY KEY (assessment_id),
	CONSTRAINT assessment_assessment_type_id_fkey FOREIGN KEY (assessment_type_id) REFERENCES assessment_type(assessment_type_id),
	CONSTRAINT assessment_next_assessment_id_fkey FOREIGN KEY (next_assessment_id) REFERENCES assessment(assessment_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.assessment for each row execute procedure set_last_updated();


-- cobalt.category_mapping definition

-- Drop table

-- DROP TABLE category_mapping;

CREATE TABLE category_mapping (
	category_id varchar NOT NULL,
	loading_category_description varchar NOT NULL,
	CONSTRAINT category_mapping_category_id_fkey FOREIGN KEY (category_id) REFERENCES category(category_id)
);
CREATE UNIQUE INDEX category_mapping_category_id_loading_category_description_idx ON cobalt.category_mapping USING btree (category_id, loading_category_description);


-- cobalt.clinic definition

-- Drop table

-- DROP TABLE clinic;

CREATE TABLE clinic (
	clinic_id uuid NOT NULL,
	description varchar NOT NULL,
	treatment_description varchar NULL,
	institution_id varchar NOT NULL,
	intake_assessment_id uuid NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	show_intake_assessment_prompt bool NOT NULL DEFAULT true,
	CONSTRAINT clinic_pkey PRIMARY KEY (clinic_id),
	CONSTRAINT clinic_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id),
	CONSTRAINT clinic_intake_assessment_id_fkey FOREIGN KEY (intake_assessment_id) REFERENCES assessment(assessment_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.clinic for each row execute procedure set_last_updated();


-- cobalt."content" definition

-- Drop table

-- DROP TABLE "content";

CREATE TABLE "content" (
	content_id uuid NOT NULL,
	content_type_id varchar NOT NULL,
	title varchar NOT NULL,
	url varchar NULL,
	date_created timestamptz NULL,
	image_url varchar NULL,
	description varchar NULL,
	author varchar NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	owner_institution_id varchar NOT NULL,
	deleted_flag bool NOT NULL DEFAULT false,
	archived_flag bool NOT NULL DEFAULT false,
	owner_institution_approval_status_id varchar NOT NULL DEFAULT 'PENDING'::character varying,
	other_institution_approval_status_id varchar NOT NULL DEFAULT 'PENDING'::character varying,
	visibility_id varchar NOT NULL,
	duration_in_minutes int4 NULL,
	content_type_label_id varchar NOT NULL,
	CONSTRAINT content_pkey PRIMARY KEY (content_id),
	CONSTRAINT nonempty_title CHECK ((length(btrim((title)::text)) > 0)),
	CONSTRAINT content_content_type_id_fkey FOREIGN KEY (content_type_id) REFERENCES content_type(content_type_id),
	CONSTRAINT content_content_type_label_id_fkey FOREIGN KEY (content_type_label_id) REFERENCES content_type_label(content_type_label_id),
	CONSTRAINT content_other_institution_approval_status_id_fkey FOREIGN KEY (other_institution_approval_status_id) REFERENCES approval_status(approval_status_id),
	CONSTRAINT content_owner_institution_approval_status_id_fkey FOREIGN KEY (owner_institution_approval_status_id) REFERENCES approval_status(approval_status_id),
	CONSTRAINT content_owner_institution_id_fkey FOREIGN KEY (owner_institution_id) REFERENCES institution(institution_id),
	CONSTRAINT content_visibility_id_fkey FOREIGN KEY (visibility_id) REFERENCES visibility(visibility_id)
);
CREATE INDEX idx_content_trgm_gin_title ON cobalt.content USING gin (title gin_trgm_ops);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.content for each row execute procedure set_last_updated();


-- cobalt.content_category definition

-- Drop table

-- DROP TABLE content_category;

CREATE TABLE content_category (
	content_category_id uuid NOT NULL,
	category_id varchar NOT NULL,
	content_id uuid NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT content_category_pkey PRIMARY KEY (content_category_id),
	CONSTRAINT content_category_category_id_fkey FOREIGN KEY (category_id) REFERENCES category(category_id),
	CONSTRAINT content_category_content_id_fkey FOREIGN KEY (content_id) REFERENCES "content"(content_id)
);
CREATE UNIQUE INDEX content_category_category_id_content_id_idx ON cobalt.content_category USING btree (category_id, content_id);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.content_category for each row execute procedure set_last_updated();


-- cobalt.crisis_contact definition

-- Drop table

-- DROP TABLE crisis_contact;

CREATE TABLE crisis_contact (
	crisis_contact_id uuid NOT NULL,
	institution_id varchar NOT NULL,
	email_address text NOT NULL,
	active bool NOT NULL DEFAULT true,
	locale text NOT NULL DEFAULT 'en-US'::text,
	time_zone text NOT NULL DEFAULT 'America/New_York'::text,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT crisis_contact_pkey PRIMARY KEY (crisis_contact_id),
	CONSTRAINT crisis_contact_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.crisis_contact for each row execute procedure set_last_updated();


-- cobalt.epic_department definition

-- Drop table

-- DROP TABLE epic_department;

CREATE TABLE epic_department (
	epic_department_id uuid NOT NULL,
	institution_id varchar NOT NULL,
	department_id varchar NOT NULL,
	department_id_type varchar NOT NULL,
	"name" varchar NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT epic_department_pkey PRIMARY KEY (epic_department_id),
	CONSTRAINT epic_department_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id)
);
CREATE UNIQUE INDEX epic_department_institution_id_department_id_department_id__idx ON cobalt.epic_department USING btree (institution_id, department_id, department_id_type);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.epic_department for each row execute procedure set_last_updated();


-- cobalt.external_group_event_type definition

-- Drop table

-- DROP TABLE external_group_event_type;

CREATE TABLE external_group_event_type (
	external_group_event_type_id uuid NOT NULL,
	institution_id varchar NOT NULL,
	"name" varchar NOT NULL,
	description varchar NOT NULL,
	url_name varchar NOT NULL,
	image_url varchar NOT NULL,
	signup_url varchar NOT NULL,
	display_order int2 NOT NULL,
	available bool NULL DEFAULT true,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT external_group_event_type_pkey PRIMARY KEY (external_group_event_type_id),
	CONSTRAINT external_group_event_type_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id)
);
CREATE UNIQUE INDEX external_group_event_type_display_order_idx ON cobalt.external_group_event_type USING btree (display_order);
CREATE UNIQUE INDEX external_group_event_type_url_name_idx ON cobalt.external_group_event_type USING btree (url_name);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.external_group_event_type for each row execute procedure set_last_updated();


-- cobalt.feedback_contact definition

-- Drop table

-- DROP TABLE feedback_contact;

CREATE TABLE feedback_contact (
	feedback_contact_id uuid NOT NULL,
	institution_id varchar NOT NULL,
	email_address text NOT NULL,
	active bool NOT NULL DEFAULT true,
	locale text NOT NULL DEFAULT 'en-US'::text,
	time_zone text NOT NULL DEFAULT 'America/New_York'::text,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT feedback_contact_pkey PRIMARY KEY (feedback_contact_id),
	CONSTRAINT feedback_contact_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id)
);


-- cobalt.institution_account_source definition

-- Drop table

-- DROP TABLE institution_account_source;

CREATE TABLE institution_account_source (
	institution_account_source_id uuid NOT NULL,
	institution_id varchar NOT NULL,
	account_source_id varchar NOT NULL,
	CONSTRAINT institution_account_source_pkey PRIMARY KEY (institution_account_source_id),
	CONSTRAINT institution_account_source_account_source_id_fkey FOREIGN KEY (account_source_id) REFERENCES account_source(account_source_id),
	CONSTRAINT institution_account_source_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id)
);
CREATE UNIQUE INDEX idx_institution_account_source ON cobalt.institution_account_source USING btree (institution_id, account_source_id);


-- cobalt.institution_assessment definition

-- Drop table

-- DROP TABLE institution_assessment;

CREATE TABLE institution_assessment (
	institution_assessment_id uuid NOT NULL,
	institution_id varchar NOT NULL,
	assessment_id uuid NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT institution_assessment_pkey PRIMARY KEY (institution_assessment_id),
	CONSTRAINT institution_assessment_assessment_id_fkey FOREIGN KEY (assessment_id) REFERENCES assessment(assessment_id),
	CONSTRAINT institution_assessment_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.institution_assessment for each row execute procedure set_last_updated();

-- cobalt.institution_content definition

-- Drop table

-- DROP TABLE institution_content;

CREATE TABLE institution_content (
	institution_content_id uuid NOT NULL,
	institution_id varchar NOT NULL,
	content_id uuid NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	approved_flag bool NOT NULL DEFAULT false,
	CONSTRAINT institution_content_pkey PRIMARY KEY (institution_content_id),
	CONSTRAINT institution_content_content_id_fkey FOREIGN KEY (content_id) REFERENCES "content"(content_id),
	CONSTRAINT institution_content_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id)
);
CREATE UNIQUE INDEX idx_institution_content_ak1 ON cobalt.institution_content USING btree (institution_id, content_id);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.institution_content for each row execute procedure set_last_updated();


-- cobalt.institution_network definition

-- Drop table

-- DROP TABLE institution_network;

CREATE TABLE institution_network (
	institution_network_id uuid NOT NULL,
	parent_institution_id varchar NOT NULL,
	related_institution_id varchar NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT institution_network_pkey PRIMARY KEY (institution_network_id),
	CONSTRAINT institution_network_parent_institution_id_fkey FOREIGN KEY (parent_institution_id) REFERENCES institution(institution_id),
	CONSTRAINT institution_network_related_institution_id_fkey FOREIGN KEY (related_institution_id) REFERENCES institution(institution_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.institution_network for each row execute procedure set_last_updated();


-- cobalt.message_log definition

-- Drop table

-- DROP TABLE message_log;

CREATE TABLE message_log (
	message_id uuid NOT NULL DEFAULT uuid_generate_v4(),
	message_type_id text NOT NULL,
	message_status_id text NOT NULL,
	serialized_message jsonb NOT NULL,
	stack_trace text NULL,
	enqueued timestamptz NULL,
	processed timestamptz NULL,
	created timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT message_log_pkey PRIMARY KEY (message_id),
	CONSTRAINT message_log_message_status_id_fkey FOREIGN KEY (message_status_id) REFERENCES message_status(message_status_id),
	CONSTRAINT message_log_message_type_id_fkey FOREIGN KEY (message_type_id) REFERENCES message_type(message_type_id)
);


-- cobalt.payment_type definition

-- Drop table

-- DROP TABLE payment_type;

CREATE TABLE payment_type (
	payment_type_id varchar NOT NULL,
	description varchar NOT NULL,
	display_order int2 NOT NULL,
	payment_funding_id varchar NOT NULL,
	CONSTRAINT payment_type_pkey PRIMARY KEY (payment_type_id),
	CONSTRAINT payment_type_payment_funding_id_fkey FOREIGN KEY (payment_funding_id) REFERENCES payment_funding(payment_funding_id)
);


-- cobalt.provider definition

-- Drop table

-- DROP TABLE provider;

CREATE TABLE provider (
	provider_id uuid NOT NULL,
	institution_id varchar NOT NULL,
	"name" varchar NOT NULL,
	title varchar NULL,
	email_address varchar NOT NULL,
	image_url varchar NULL,
	locale text NOT NULL DEFAULT 'en-US'::text,
	time_zone text NOT NULL DEFAULT 'America/New_York'::text,
	acuity_calendar_id int8 NULL,
	bluejeans_user_id int8 NULL,
	tags jsonb NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	entity text NULL,
	clinic text NULL,
	license text NULL,
	specialty text NULL,
	intake_assessment_id uuid NULL,
	active bool NULL DEFAULT true,
	scheduling_system_id text NOT NULL DEFAULT 'ACUITY'::text,
	epic_provider_id text NULL,
	epic_provider_id_type text NULL,
	videoconference_platform_id text NOT NULL DEFAULT 'BLUEJEANS'::text,
	videoconference_url text NULL,
	epic_appointment_filter_id text NOT NULL DEFAULT 'NONE'::text,
	system_affinity_id varchar NOT NULL DEFAULT 'COBALT'::character varying,
	CONSTRAINT nonempty_name CHECK ((length(btrim((name)::text)) > 0)),
	CONSTRAINT provider_pkey PRIMARY KEY (provider_id),
	CONSTRAINT provider_epic_appointment_filter_id_fkey FOREIGN KEY (epic_appointment_filter_id) REFERENCES epic_appointment_filter(epic_appointment_filter_id),
	CONSTRAINT provider_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id),
	CONSTRAINT provider_intake_assessment_id_fkey FOREIGN KEY (intake_assessment_id) REFERENCES assessment(assessment_id),
	CONSTRAINT provider_system_affinity_id_fkey FOREIGN KEY (system_affinity_id) REFERENCES system_affinity(system_affinity_id),
	CONSTRAINT scheduling_system_fk FOREIGN KEY (scheduling_system_id) REFERENCES scheduling_system(scheduling_system_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.provider for each row execute procedure set_last_updated();


-- cobalt.provider_appointment_type definition

-- Drop table

-- DROP TABLE provider_appointment_type;

CREATE TABLE provider_appointment_type (
	provider_appointment_type_id uuid NOT NULL DEFAULT uuid_generate_v4(),
	provider_id uuid NOT NULL,
	display_order int2 NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	appointment_type_id uuid NOT NULL,
	CONSTRAINT provider_appointment_type_pkey PRIMARY KEY (provider_appointment_type_id),
	CONSTRAINT appointment_type_fk FOREIGN KEY (appointment_type_id) REFERENCES appointment_type(appointment_type_id),
	CONSTRAINT provider_appointment_type_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES provider(provider_id)
);
CREATE UNIQUE INDEX provider_appointment_type_provider_id_display_order_idx ON cobalt.provider_appointment_type USING btree (provider_id, display_order);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.provider_appointment_type for each row execute procedure set_last_updated();


-- cobalt.provider_clinic definition

-- Drop table

-- DROP TABLE provider_clinic;

CREATE TABLE provider_clinic (
	provider_clinic_id uuid NOT NULL,
	provider_id uuid NOT NULL,
	clinic_id uuid NOT NULL,
	primary_clinic bool NOT NULL DEFAULT true,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT provider_clinic_pkey PRIMARY KEY (provider_clinic_id),
	CONSTRAINT provider_clinic_clinic_id_fkey FOREIGN KEY (clinic_id) REFERENCES clinic(clinic_id),
	CONSTRAINT provider_clinic_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES provider(provider_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.provider_clinic for each row execute procedure set_last_updated();


-- cobalt.provider_epic_department definition

-- Drop table

-- DROP TABLE provider_epic_department;

CREATE TABLE provider_epic_department (
	provider_epic_department_id uuid NOT NULL DEFAULT uuid_generate_v4(),
	provider_id uuid NOT NULL,
	epic_department_id uuid NOT NULL,
	display_order int2 NOT NULL,
	CONSTRAINT provider_epic_department_pkey PRIMARY KEY (provider_epic_department_id),
	CONSTRAINT provider_epic_department_epic_department_id_fkey FOREIGN KEY (epic_department_id) REFERENCES epic_department(epic_department_id),
	CONSTRAINT provider_epic_department_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES provider(provider_id)
);
CREATE UNIQUE INDEX provider_epic_department_provider_id_epic_department_id_idx ON cobalt.provider_epic_department USING btree (provider_id, epic_department_id);


-- cobalt.provider_payment_type definition

-- Drop table

-- DROP TABLE provider_payment_type;

CREATE TABLE provider_payment_type (
	provider_id uuid NOT NULL,
	payment_type_id varchar NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT provider_payment_type_pkey PRIMARY KEY (provider_id, payment_type_id),
	CONSTRAINT provider_payment_type_payment_type_id_fkey FOREIGN KEY (payment_type_id) REFERENCES payment_type(payment_type_id),
	CONSTRAINT provider_payment_type_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES provider(provider_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.provider_payment_type for each row execute procedure set_last_updated();


-- cobalt.provider_support_role definition

-- Drop table

-- DROP TABLE provider_support_role;

CREATE TABLE provider_support_role (
	provider_id uuid NOT NULL,
	support_role_id varchar NOT NULL,
	CONSTRAINT provider_support_role_pkey PRIMARY KEY (provider_id, support_role_id),
	CONSTRAINT provider_support_role_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES provider(provider_id),
	CONSTRAINT provider_support_role_support_role_id_fkey FOREIGN KEY (support_role_id) REFERENCES support_role(support_role_id)
);


-- cobalt.font_size definition

-- Drop table

-- DROP TABLE font_size;

CREATE TABLE font_size (
    font_size_id VARCHAR PRIMARY KEY,
    description VARCHAR NOT NULL
);


-- cobalt.question definition

-- Drop table

-- DROP TABLE question;

CREATE TABLE question (
	question_id uuid NOT NULL,
	assessment_id uuid NOT NULL,
	question_type_id varchar NULL,
	question_text varchar NULL,
	answer_column_count int4 NOT NULL DEFAULT 1,
	display_order int4 NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	is_root_question bool NOT NULL DEFAULT false,
	answer_required bool NOT NULL DEFAULT true,
	cms_question_text text NULL,
    font_size_id VARCHAR NOT NULL REFERENCES font_size DEFAULT 'DEFAULT',
	CONSTRAINT question_answer_column_count_check CHECK ((answer_column_count > 0)),
	CONSTRAINT question_pkey PRIMARY KEY (question_id),
	CONSTRAINT question_assessment_id_fkey FOREIGN KEY (assessment_id) REFERENCES assessment(assessment_id),
	CONSTRAINT question_question_type_id_fkey FOREIGN KEY (question_type_id) REFERENCES question_type(question_type_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.question for each row execute procedure set_last_updated();


-- cobalt.recommendation_level_to_support_role definition

-- Drop table

-- DROP TABLE recommendation_level_to_support_role;

CREATE TABLE recommendation_level_to_support_role (
	recommendation_level_id varchar NOT NULL,
	support_role_id varchar NOT NULL,
	CONSTRAINT recommendation_level_to_support_ro_recommendation_level_id_fkey FOREIGN KEY (recommendation_level_id) REFERENCES recommendation_level(recommendation_level_id),
	CONSTRAINT recommendation_level_to_support_role_support_role_id_fkey FOREIGN KEY (support_role_id) REFERENCES support_role(support_role_id)
);


-- cobalt.account definition

-- Drop table

-- DROP TABLE account;

CREATE TABLE account (
	account_id uuid NOT NULL,
	role_id varchar NOT NULL,
	institution_id varchar NOT NULL,
	account_source_id varchar NOT NULL,
	sso_id varchar NULL,
	first_name varchar NULL,
	last_name varchar NULL,
	display_name varchar NULL,
	email_address varchar NULL,
	phone_number text NULL,
	sso_attributes jsonb NULL,
	consent_form_accepted bool NOT NULL DEFAULT false,
	consent_form_accepted_date timestamptz NULL,
	locale text NOT NULL DEFAULT 'en-US'::text,
	time_zone text NOT NULL DEFAULT 'America/New_York'::text,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	epic_patient_id text NULL,
	epic_patient_id_type text NULL,
	epic_patient_created_by_cobalt bool NOT NULL DEFAULT false,
	"password" text NULL,
	source_system_id varchar NOT NULL DEFAULT 'COBALT'::character varying,
	provider_id uuid NULL,
	beta_status_id varchar NOT NULL DEFAULT 'UNKNOWN'::character varying,
	CONSTRAINT account_pkey PRIMARY KEY (account_id),
	CONSTRAINT account_account_source_id_fkey FOREIGN KEY (account_source_id) REFERENCES account_source(account_source_id),
	CONSTRAINT account_beta_status_id_fkey FOREIGN KEY (beta_status_id) REFERENCES beta_status(beta_status_id),
	CONSTRAINT account_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id),
	CONSTRAINT account_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES provider(provider_id),
	CONSTRAINT account_role_id_fkey FOREIGN KEY (role_id) REFERENCES "role"(role_id),
	CONSTRAINT account_source_system_id_fkey FOREIGN KEY (source_system_id) REFERENCES source_system(source_system_id)
);
CREATE UNIQUE INDEX idx_account_email_address ON cobalt.account USING btree (lower((email_address)::text)) WHERE ((account_source_id)::text = 'EMAIL_PASSWORD'::text);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.account for each row execute procedure set_last_updated();


-- cobalt.account_login_rule definition

-- Drop table

-- DROP TABLE account_login_rule;

CREATE TABLE account_login_rule (
	account_login_rule_id uuid NOT NULL DEFAULT uuid_generate_v4(),
	institution_id varchar NOT NULL,
	account_source_id varchar NOT NULL,
	login_destination_id varchar NOT NULL,
	role_id varchar NOT NULL,
	email_address varchar NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	provider_id uuid NULL,
	CONSTRAINT account_login_rule_pkey PRIMARY KEY (account_login_rule_id),
	CONSTRAINT account_login_rule_account_source_id_fkey FOREIGN KEY (account_source_id) REFERENCES account_source(account_source_id),
	CONSTRAINT account_login_rule_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id),
	CONSTRAINT account_login_rule_login_destination_id_fkey FOREIGN KEY (login_destination_id) REFERENCES login_destination(login_destination_id),
	CONSTRAINT account_login_rule_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES provider(provider_id),
	CONSTRAINT account_login_rule_role_id_fkey FOREIGN KEY (role_id) REFERENCES "role"(role_id)
);
CREATE UNIQUE INDEX idx_account_login_rule_unique ON cobalt.account_login_rule USING btree (institution_id, account_source_id, login_destination_id, lower((email_address)::text));

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.account_login_rule for each row execute procedure set_last_updated();


-- cobalt.account_session definition

-- Drop table

-- DROP TABLE account_session;

CREATE TABLE account_session (
	account_session_id uuid NOT NULL,
	account_id uuid NOT NULL,
	assessment_id uuid NOT NULL,
	current_flag bool NOT NULL DEFAULT true,
	complete_flag bool NOT NULL DEFAULT false,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT account_session_pkey PRIMARY KEY (account_session_id),
	CONSTRAINT account_session_account_id_fkey FOREIGN KEY (account_id) REFERENCES account(account_id),
	CONSTRAINT account_session_assessment_id_fkey FOREIGN KEY (assessment_id) REFERENCES assessment(assessment_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.account_session for each row execute procedure set_last_updated();


-- cobalt.account_session_grouping definition

-- Drop table

-- DROP TABLE account_session_grouping;

CREATE TABLE account_session_grouping (
	account_session_grouping_id uuid NULL,
	account_session_id uuid NULL,
	last_assessment bool NULL DEFAULT false,
	score int4 NULL,
	CONSTRAINT account_session_grouping_account_session_id_fkey FOREIGN KEY (account_session_id) REFERENCES account_session(account_session_id)
);


-- cobalt.account_session_grouping_all definition

-- Drop table

-- DROP TABLE account_session_grouping_all;

CREATE TABLE account_session_grouping_all (
	account_session_grouping_all_id uuid NULL,
	account_session_id uuid NULL,
	last_assessment bool NULL DEFAULT false,
	completed bool NULL,
	score int4 NULL,
	CONSTRAINT account_session_grouping_all_account_session_id_fkey FOREIGN KEY (account_session_id) REFERENCES account_session(account_session_id)
);


-- cobalt.activity_tracking definition

-- Drop table

-- DROP TABLE activity_tracking;

CREATE TABLE activity_tracking (
	activity_tracking_id uuid NOT NULL,
	account_id uuid NOT NULL,
	activity_type_id varchar NOT NULL,
	activity_action_id varchar NOT NULL,
	activity_key uuid NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT activity_tracking_pkey PRIMARY KEY (activity_tracking_id),
	CONSTRAINT activity_tracking_account_id_fkey FOREIGN KEY (account_id) REFERENCES account(account_id),
	CONSTRAINT activity_tracking_activity_action_id_fkey FOREIGN KEY (activity_action_id) REFERENCES activity_action(activity_action_id),
	CONSTRAINT activity_tracking_activity_type_id_fkey FOREIGN KEY (activity_type_id) REFERENCES activity_type(activity_type_id)
);
CREATE INDEX idx_activity_tracking_account_id_activity_key ON cobalt.activity_tracking USING btree (activity_key, account_id);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.activity_tracking for each row execute procedure set_last_updated();


-- cobalt.answer definition

-- Drop table

-- DROP TABLE answer;

CREATE TABLE answer (
	answer_id uuid NOT NULL,
	question_id uuid NULL,
	answer_text varchar NOT NULL,
	display_order int4 NOT NULL,
	answer_value int4 NOT NULL DEFAULT 0,
	crisis bool NULL DEFAULT false,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	"call" bool NULL DEFAULT false,
	next_question_id uuid NULL,
	CONSTRAINT answer_pkey PRIMARY KEY (answer_id),
	CONSTRAINT answer_next_question_id_fkey FOREIGN KEY (next_question_id) REFERENCES question(question_id),
	CONSTRAINT answer_question_id_fkey FOREIGN KEY (question_id) REFERENCES question(question_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.answer for each row execute procedure set_last_updated();

-- cobalt.answer_category definition

-- Drop table

-- DROP TABLE answer_category;

CREATE TABLE answer_category (
	answer_category_id uuid NOT NULL,
	answer_id uuid NOT NULL,
	category_id varchar NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT answer_category_pkey PRIMARY KEY (answer_category_id),
	CONSTRAINT answer_category_answer_id_fkey FOREIGN KEY (answer_id) REFERENCES answer(answer_id),
	CONSTRAINT answer_category_category_id_fkey FOREIGN KEY (category_id) REFERENCES category(category_id)
);
CREATE UNIQUE INDEX answer_category_answer_id_category_id_idx ON cobalt.answer_category USING btree (answer_id, category_id);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.answer_category for each row execute procedure set_last_updated();


-- cobalt.answer_content definition

-- Drop table

-- DROP TABLE answer_content;

CREATE TABLE answer_content (
	answer_content_id uuid NOT NULL,
	answer_id uuid NOT NULL,
	content_id uuid NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT answer_content_pkey PRIMARY KEY (answer_content_id),
	CONSTRAINT answer_content_answer_id_fkey FOREIGN KEY (answer_id) REFERENCES answer(answer_id),
	CONSTRAINT answer_content_content_id_fkey FOREIGN KEY (content_id) REFERENCES "content"(content_id)
);
CREATE UNIQUE INDEX idx_answer_content_ak1 ON cobalt.answer_content USING btree (answer_id, content_id);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.answer_content for each row execute procedure set_last_updated();


-- cobalt.appointment definition

-- Drop table

-- DROP TABLE appointment;

CREATE TABLE appointment (
	appointment_id uuid NOT NULL,
	provider_id uuid NULL,
	account_id uuid NOT NULL,
	acuity_appointment_id int8 NULL,
	acuity_appointment_type_id int8 NULL,
	acuity_class_id int8 NULL,
	bluejeans_meeting_id int8 NULL,
	videoconference_url text NULL,
	title text NOT NULL,
	start_time timestamp NOT NULL,
	end_time timestamp NOT NULL,
	duration_in_minutes int8 NOT NULL,
	time_zone text NOT NULL,
	canceled bool NOT NULL DEFAULT false,
	canceled_at timestamptz NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	epic_contact_id text NULL,
	epic_contact_id_type text NULL,
	appointment_type_id uuid NOT NULL,
	phone_number text NULL,
	videoconference_platform_id text NULL,
	appointment_reason_id uuid NOT NULL DEFAULT '3c34a096-2f4d-4091-a9d8-f0381ba079a1'::uuid,
	created_by_account_id uuid NOT NULL,
	"comment" varchar NULL,
	attendance_status_id varchar NOT NULL DEFAULT 'UNKNOWN'::character varying,
    bluejeans_participant_passcode VARCHAR,
	CONSTRAINT appointment_pkey PRIMARY KEY (appointment_id),
	CONSTRAINT appointment_account_id_fkey FOREIGN KEY (account_id) REFERENCES account(account_id),
	CONSTRAINT appointment_appointment_reason_id_fkey FOREIGN KEY (appointment_reason_id) REFERENCES appointment_reason(appointment_reason_id),
	CONSTRAINT appointment_attendance_status_id_fkey FOREIGN KEY (attendance_status_id) REFERENCES attendance_status(attendance_status_id),
	CONSTRAINT appointment_created_by_account_id_fkey FOREIGN KEY (created_by_account_id) REFERENCES account(account_id),
	CONSTRAINT appointment_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES provider(provider_id),
	CONSTRAINT appointment_type_fk FOREIGN KEY (appointment_type_id) REFERENCES appointment_type(appointment_type_id),
	CONSTRAINT videoconference_platform_fk FOREIGN KEY (videoconference_platform_id) REFERENCES videoconference_platform(videoconference_platform_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.appointment for each row execute procedure set_last_updated();


-- cobalt.assessment_viewer definition

-- Drop table

-- DROP TABLE assessment_viewer;

CREATE TABLE assessment_viewer (
	assessment_viewer_id uuid NOT NULL DEFAULT uuid_generate_v4(),
	assessment_id uuid NOT NULL,
	account_id uuid NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT assessment_viewer_pkey PRIMARY KEY (assessment_viewer_id),
	CONSTRAINT assessment_viewer_account_id_fkey FOREIGN KEY (account_id) REFERENCES account(account_id),
	CONSTRAINT assessment_viewer_assessment_id_fkey FOREIGN KEY (assessment_id) REFERENCES assessment(assessment_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.assessment_viewer for each row execute procedure set_last_updated();


-- cobalt.audit_log definition

-- Drop table

-- DROP TABLE audit_log;

CREATE TABLE audit_log (
	audit_log_id uuid NOT NULL,
	audit_log_event_id varchar NOT NULL,
	account_id uuid NULL,
	message varchar NULL,
	payload jsonb NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT audit_log_pkey PRIMARY KEY (audit_log_id),
	CONSTRAINT audit_log_account_id_fkey FOREIGN KEY (account_id) REFERENCES account(account_id),
	CONSTRAINT audit_log_audit_log_event_id_fkey FOREIGN KEY (audit_log_event_id) REFERENCES audit_log_event(audit_log_event_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.audit_log for each row execute procedure set_last_updated();


-- cobalt.beta_feature_alert definition

-- Drop table

-- DROP TABLE beta_feature_alert;

CREATE TABLE beta_feature_alert (
	account_id uuid NOT NULL,
	beta_feature_id varchar NOT NULL,
	enabled bool NOT NULL DEFAULT false,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT beta_feature_alert_pkey PRIMARY KEY (account_id, beta_feature_id),
	CONSTRAINT beta_feature_alert_account_id_fkey FOREIGN KEY (account_id) REFERENCES account(account_id),
	CONSTRAINT beta_feature_alert_beta_feature_id_fkey FOREIGN KEY (beta_feature_id) REFERENCES beta_feature(beta_feature_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.beta_feature_alert for each row execute procedure set_last_updated();


-- cobalt.followup definition

-- Drop table

-- DROP TABLE followup;

CREATE TABLE followup (
	followup_id uuid NOT NULL DEFAULT uuid_generate_v4(),
	account_id uuid NOT NULL,
	created_by_account_id uuid NOT NULL,
	provider_id uuid NOT NULL,
	appointment_reason_id uuid NOT NULL,
	followup_date date NOT NULL,
	"comment" varchar NULL,
	canceled bool NOT NULL DEFAULT false,
	canceled_at timestamptz NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT followup_pkey PRIMARY KEY (followup_id),
	CONSTRAINT followup_account_id_fkey FOREIGN KEY (account_id) REFERENCES account(account_id),
	CONSTRAINT followup_appointment_reason_id_fkey FOREIGN KEY (appointment_reason_id) REFERENCES appointment_reason(appointment_reason_id),
	CONSTRAINT followup_created_by_account_id_fkey FOREIGN KEY (created_by_account_id) REFERENCES account(account_id),
	CONSTRAINT followup_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES provider(provider_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.followup for each row execute procedure set_last_updated();


-- cobalt.group_session definition

-- Drop table

-- DROP TABLE group_session;

CREATE TABLE group_session (
	group_session_id uuid NOT NULL,
	institution_id varchar NOT NULL,
	group_session_status_id varchar NOT NULL DEFAULT 'NEW'::character varying,
	assessment_id uuid NULL,
	title varchar NOT NULL,
	description varchar NOT NULL,
	facilitator_account_id uuid NULL,
	facilitator_name varchar NOT NULL,
	facilitator_email_address varchar NOT NULL,
	image_url varchar NULL,
	videoconference_url varchar NULL,
	start_date_time timestamp NOT NULL,
	end_date_time timestamp NOT NULL,
	seats int2 NULL,
	url_name varchar NOT NULL,
	confirmation_email_content varchar NULL,
	locale text NOT NULL,
	time_zone text NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	group_session_scheduling_system_id varchar NOT NULL DEFAULT 'COBALT'::character varying,
	schedule_url varchar NULL,
	send_followup_email bool NOT NULL DEFAULT false,
	followup_email_content text NULL,
	followup_email_survey_url text NULL,
	submitter_account_id uuid NOT NULL,
	CONSTRAINT group_session_pkey PRIMARY KEY (group_session_id),
	CONSTRAINT group_session_assessment_id_fkey FOREIGN KEY (assessment_id) REFERENCES assessment(assessment_id),
	CONSTRAINT group_session_facilitator_account_id_fkey FOREIGN KEY (facilitator_account_id) REFERENCES account(account_id),
	CONSTRAINT group_session_group_session_scheduling_system_id_fkey FOREIGN KEY (group_session_scheduling_system_id) REFERENCES group_session_scheduling_system(group_session_scheduling_system_id),
	CONSTRAINT group_session_group_session_status_id_fkey FOREIGN KEY (group_session_status_id) REFERENCES group_session_status(group_session_status_id),
	CONSTRAINT group_session_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id),
	CONSTRAINT group_session_submitter_account_id_fkey FOREIGN KEY (submitter_account_id) REFERENCES account(account_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.group_session for each row execute procedure set_last_updated();


-- cobalt.group_session_request definition

-- Drop table

-- DROP TABLE group_session_request;

CREATE TABLE group_session_request (
	group_session_request_id uuid NOT NULL,
	institution_id varchar NOT NULL,
	group_session_request_status_id varchar NOT NULL DEFAULT 'ADDED'::character varying,
	title varchar NOT NULL,
	description varchar NOT NULL,
	facilitator_account_id uuid NULL,
	facilitator_name varchar NOT NULL,
	facilitator_email_address varchar NOT NULL,
	image_url varchar NULL,
	url_name varchar NOT NULL,
	custom_question_1 varchar NULL,
	custom_question_2 varchar NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	submitter_account_id uuid NOT NULL,
	CONSTRAINT group_session_request_pkey PRIMARY KEY (group_session_request_id),
	CONSTRAINT group_session_request_facilitator_account_id_fkey FOREIGN KEY (facilitator_account_id) REFERENCES account(account_id),
	CONSTRAINT group_session_request_group_session_request_status_id_fkey FOREIGN KEY (group_session_request_status_id) REFERENCES group_session_request_status(group_session_request_status_id),
	CONSTRAINT group_session_request_institution_id_fkey FOREIGN KEY (institution_id) REFERENCES institution(institution_id),
	CONSTRAINT group_session_request_submitter_account_id_fkey FOREIGN KEY (submitter_account_id) REFERENCES account(account_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.group_session_request for each row execute procedure set_last_updated();


-- cobalt.group_session_reservation definition

-- Drop table

-- DROP TABLE group_session_reservation;

CREATE TABLE group_session_reservation (
	group_session_reservation_id uuid NOT NULL,
	group_session_id uuid NOT NULL,
	account_id uuid NOT NULL,
	canceled bool NOT NULL DEFAULT false,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	followup_email_status_id varchar NOT NULL DEFAULT 'NOT_SENT'::character varying,
	followup_email_sent_timestamp timestamptz NULL,
	CONSTRAINT group_session_reservation_pkey PRIMARY KEY (group_session_reservation_id),
	CONSTRAINT group_session_reservation_account_id_fkey FOREIGN KEY (account_id) REFERENCES account(account_id),
	CONSTRAINT group_session_reservation_followup_email_status_id_fkey FOREIGN KEY (followup_email_status_id) REFERENCES followup_email_status(followup_email_status_id),
	CONSTRAINT group_session_reservation_group_session_id_fkey FOREIGN KEY (group_session_id) REFERENCES group_session(group_session_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.group_session_reservation for each row execute procedure set_last_updated();


-- cobalt.group_session_response definition

-- Drop table

-- DROP TABLE group_session_response;

CREATE TABLE group_session_response (
	group_session_response_id uuid NOT NULL,
	group_session_request_id uuid NOT NULL,
	respondent_account_id uuid NULL,
	respondent_name varchar NOT NULL,
	respondent_email_address varchar NOT NULL,
	respondent_phone_number varchar NULL,
	suggested_date date NULL,
	suggested_time varchar NULL,
	expected_participants varchar NULL,
	notes varchar NULL,
	custom_answer_1 varchar NULL,
	custom_answer_2 varchar NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT group_session_response_pkey PRIMARY KEY (group_session_response_id),
	CONSTRAINT group_session_response_group_session_request_id_fkey FOREIGN KEY (group_session_request_id) REFERENCES group_session_request(group_session_request_id),
	CONSTRAINT group_session_response_respondent_account_id_fkey FOREIGN KEY (respondent_account_id) REFERENCES account(account_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.group_session_response for each row execute procedure set_last_updated();


-- cobalt.logical_availability definition

-- Drop table

-- DROP TABLE logical_availability;

CREATE TABLE logical_availability (
	logical_availability_id uuid NOT NULL DEFAULT uuid_generate_v4(),
	provider_id uuid NOT NULL,
	start_date_time timestamp NOT NULL,
	end_date_time timestamp NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT logical_availability_pkey PRIMARY KEY (logical_availability_id),
	CONSTRAINT logical_availability_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES provider(provider_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.logical_availability for each row execute procedure set_last_updated();


-- cobalt.logical_availability_appointment_type definition

-- Drop table

-- DROP TABLE logical_availability_appointment_type;

CREATE TABLE logical_availability_appointment_type (
	logical_availability_id uuid NOT NULL,
	appointment_type_id uuid NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT logical_availability_appointment_t_logical_availability_id_fkey FOREIGN KEY (logical_availability_id) REFERENCES logical_availability(logical_availability_id),
	CONSTRAINT logical_availability_appointment_type_appointment_type_id_fkey FOREIGN KEY (appointment_type_id) REFERENCES appointment_type(appointment_type_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.logical_availability_appointment_type for each row execute procedure set_last_updated();


-- cobalt.password_reset_request definition

-- Drop table

-- DROP TABLE password_reset_request;

CREATE TABLE password_reset_request (
	password_reset_request_id uuid NOT NULL DEFAULT uuid_generate_v4(),
	account_id uuid NOT NULL,
	password_reset_token uuid NOT NULL,
	expiration_timestamp timestamp NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	CONSTRAINT password_reset_request_pkey PRIMARY KEY (password_reset_request_id),
	CONSTRAINT password_reset_request_account_id_fkey FOREIGN KEY (account_id) REFERENCES account(account_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.password_reset_request for each row execute procedure set_last_updated();


-- cobalt.provider_availability definition

-- Drop table

-- DROP TABLE provider_availability;

CREATE TABLE provider_availability (
	provider_availability_id uuid NOT NULL DEFAULT uuid_generate_v4(),
	provider_id uuid NOT NULL,
	date_time timestamp NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	appointment_type_id uuid NOT NULL,
	epic_department_id uuid NULL,
	logical_availability_id uuid NULL,
	CONSTRAINT provider_availability_pkey PRIMARY KEY (provider_availability_id),
	CONSTRAINT appointment_type_fk FOREIGN KEY (appointment_type_id) REFERENCES appointment_type(appointment_type_id),
	CONSTRAINT epic_department_fk FOREIGN KEY (epic_department_id) REFERENCES epic_department(epic_department_id),
	CONSTRAINT logical_availability_fk FOREIGN KEY (logical_availability_id) REFERENCES logical_availability(logical_availability_id),
	CONSTRAINT provider_availability_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES provider(provider_id)
);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.provider_availability for each row execute procedure set_last_updated();


-- cobalt.account_session_answer definition

-- Drop table

-- DROP TABLE account_session_answer;

CREATE TABLE account_session_answer (
	account_session_answer_id uuid NOT NULL,
	account_session_id uuid NOT NULL,
	answer_id uuid NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	last_updated timestamptz NOT NULL DEFAULT now(),
	answer_text text NULL,
	CONSTRAINT account_session_answer_pkey PRIMARY KEY (account_session_answer_id),
	CONSTRAINT account_session_answer_account_session_id_fkey FOREIGN KEY (account_session_id) REFERENCES account_session(account_session_id),
	CONSTRAINT account_session_answer_answer_id_fkey FOREIGN KEY (answer_id) REFERENCES answer(answer_id)
);
CREATE UNIQUE INDEX account_session_answer_account_session_answer_id_account_se_idx ON cobalt.account_session_answer USING btree (account_session_answer_id, account_session_id);

-- Table Triggers

create trigger set_last_updated before
insert or update on cobalt.account_session_answer for each row execute procedure set_last_updated();

-- cobalt.reporting_monthly_rollup definition

-- Drop table

-- DROP TABLE reporting_monthly_rollup;

CREATE TABLE reporting_monthly_rollup (
  institution_id VARCHAR NOT NULL REFERENCES institution,
	year SMALLINT NOT NULL,
	month SMALLINT NOT NULL,
	user_count INTEGER NOT NULL,
	apt_count INTEGER NOT NULL,
	apt_completed_count INTEGER NOT NULL,
	apt_canceled_count INTEGER NOT NULL,
	apt_avail_count INTEGER NOT NULL,
	prov_count INTEGER NOT NULL,
	created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
	last_updated TIMESTAMP WITH TIME ZONE NOT NULL,
	PRIMARY KEY (year, month)
);

-- Table Triggers

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON reporting_monthly_rollup FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

-- cobalt.reporting_weekly_rollup definition

-- Drop table

-- DROP TABLE reporting_weekly_rollup;

CREATE TABLE reporting_weekly_rollup (
  institution_id VARCHAR NOT NULL REFERENCES institution,
	year SMALLINT NOT NULL,
	month SMALLINT NOT NULL,
	week SMALLINT NOT NULL,
	user_count INTEGER NOT NULL,
	apt_count INTEGER NOT NULL,
	apt_completed_count INTEGER NOT NULL,
	apt_canceled_count INTEGER NOT NULL,
	apt_avail_count INTEGER NOT NULL,
	prov_count INTEGER NOT NULL,
	created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
	last_updated TIMESTAMP WITH TIME ZONE NOT NULL,
	PRIMARY KEY (year, month, week)
);

-- Table Triggers

CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON reporting_weekly_rollup FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

    -- cobalt.v_account_session_answer source

CREATE OR REPLACE VIEW cobalt.v_account_session_answer
AS SELECT asa.account_session_id,
    an.question_id,
    an.answer_id
   FROM account_session_answer asa,
    answer an
  WHERE asa.answer_id = an.answer_id;


-- cobalt.v_accounts_for_stats source

CREATE OR REPLACE VIEW cobalt.v_accounts_for_stats
AS SELECT account.account_id,
    account.role_id,
    account.institution_id,
    account.account_source_id,
    account.sso_id,
    account.first_name,
    account.last_name,
    account.display_name,
    account.email_address,
    account.phone_number,
    account.sso_attributes,
    account.consent_form_accepted,
    account.consent_form_accepted_date,
    account.locale,
    account.time_zone,
    account.created,
    account.last_updated
   FROM account
  WHERE account.created > '2020-04-12 22:57:27-04'::timestamp with time zone AND (account.email_address IS NULL OR lower(account.email_address::text) !~~ '%@xmog.com'::text) AND (account.phone_number IS NULL OR (account.phone_number <> ALL (ARRAY['+12156207765'::text, '+14106523451'::text, '+12155275175'::text, '+12159838469'::text])));


-- cobalt.v_admin_content source

CREATE OR REPLACE VIEW cobalt.v_admin_content
AS SELECT c.content_id,
    c.content_type_id,
    c.title,
    c.url,
    c.date_created,
    c.duration_in_minutes,
    c.image_url,
    c.description,
    c.author,
    c.created,
    c.last_updated,
    c.owner_institution_id,
    ctl.description AS content_type_label,
    ct.description AS content_type_description,
    ct.call_to_action,
    it.institution_id,
    i.name AS owner_institution,
    it.approved_flag,
    c.archived_flag,
    c.deleted_flag,
    c.content_type_label_id,
    c.visibility_id,
    c.other_institution_approval_status_id,
    c.owner_institution_approval_status_id
   FROM content c,
    content_type ct,
    institution_content it,
    institution i,
    content_type_label ctl
  WHERE c.content_type_id::text = ct.content_type_id::text AND c.content_id = it.content_id AND c.owner_institution_id::text = i.institution_id::text AND c.deleted_flag = false AND c.content_type_label_id::text = ctl.content_type_label_id::text;


-- cobalt.v_beta_feature_alert source

CREATE OR REPLACE VIEW cobalt.v_beta_feature_alert
AS SELECT a.account_id,
    bf.beta_feature_id,
    bf.description,
        CASE bfa.enabled
            WHEN true THEN 'ENABLED'::text
            WHEN false THEN 'DISABLED'::text
            ELSE 'UNKNOWN'::text
        END AS beta_feature_alert_status_id
   FROM beta_feature bf,
    account a
     LEFT JOIN beta_feature_alert bfa ON a.account_id = bfa.account_id;


-- cobalt.v_group_session source

CREATE OR REPLACE VIEW cobalt.v_group_session
AS SELECT gs.group_session_id,
    gs.institution_id,
    gs.group_session_status_id,
    gs.assessment_id,
    gs.title,
    gs.description,
    gs.facilitator_account_id,
    gs.facilitator_name,
    gs.facilitator_email_address,
    gs.image_url,
    gs.videoconference_url,
    gs.start_date_time,
    gs.end_date_time,
    gs.seats,
    gs.url_name,
    gs.confirmation_email_content,
    gs.locale,
    gs.time_zone,
    gs.created,
    gs.last_updated,
    gs.group_session_scheduling_system_id,
    gs.schedule_url,
    gs.send_followup_email,
    gs.followup_email_content,
    gs.followup_email_survey_url,
    gs.submitter_account_id,
    ( SELECT count(*) AS count
           FROM group_session_reservation gsr
          WHERE gsr.group_session_id = gs.group_session_id AND gsr.canceled = false) AS seats_reserved,
    gs.seats - (( SELECT count(*) AS count
           FROM group_session_reservation gsr
          WHERE gsr.group_session_id = gs.group_session_id AND gsr.canceled = false)) AS seats_available
   FROM group_session gs
  WHERE gs.group_session_status_id::text <> 'DELETED'::text;


-- cobalt.v_group_session_request source

CREATE OR REPLACE VIEW cobalt.v_group_session_request
AS SELECT gsr.group_session_request_id,
    gsr.institution_id,
    gsr.group_session_request_status_id,
    gsr.title,
    gsr.description,
    gsr.facilitator_account_id,
    gsr.facilitator_name,
    gsr.facilitator_email_address,
    gsr.image_url,
    gsr.url_name,
    gsr.custom_question_1,
    gsr.custom_question_2,
    gsr.created,
    gsr.last_updated,
    gsr.submitter_account_id
   FROM group_session_request gsr
  WHERE gsr.group_session_request_status_id::text <> 'DELETED'::text;


-- cobalt.v_group_session_reservation source

CREATE OR REPLACE VIEW cobalt.v_group_session_reservation
AS SELECT gsr.group_session_reservation_id,
    gsr.group_session_id,
    gsr.account_id,
    gsr.canceled,
    gsr.created,
    gsr.last_updated,
    gsr.followup_email_status_id,
    gsr.followup_email_sent_timestamp,
    a.first_name,
    a.last_name,
    a.email_address,
    a.phone_number
   FROM group_session_reservation gsr,
    account a
  WHERE gsr.canceled = false AND a.account_id = gsr.account_id;


-- cobalt.v_recommendation source

CREATE OR REPLACE VIEW cobalt.v_recommendation
AS SELECT ag.account_session_grouping_id,
    a.account_id,
    a2.assessment_type_id,
    a.created,
    ag.last_assessment,
    ag.score,
        CASE
            WHEN a2.assessment_type_id::text = 'PHQ4'::text THEN 'PEER'::text
            WHEN a2.assessment_type_id::text = 'PHQ9'::text THEN
            CASE
                WHEN ag.score < 5 THEN 'PEER_COACH'::text
                WHEN ag.score < 10 THEN 'COACH'::text
                WHEN ag.score < 20 THEN 'CLINICIAN'::text
                ELSE 'PSYCHIATRIST'::text
            END
            WHEN a2.assessment_type_id::text = 'GAD7'::text THEN
            CASE
                WHEN ag.score < 5 THEN 'PEER_COACH'::text
                WHEN ag.score < 10 THEN 'COACH'::text
                WHEN ag.score < 20 THEN 'CLINICIAN'::text
                ELSE 'PSYCHIATRIST'::text
            END
            WHEN a2.assessment_type_id::text = 'PCPTSD'::text THEN
            CASE
                WHEN ag.score < 3 THEN 'COACH_CLINICIAN'::text
                ELSE 'PSYCHIATRIST'::text
            END
            ELSE NULL::text
        END AS recommendation
   FROM account_session_grouping ag,
    account_session a,
    assessment a2,
    v_accounts_for_stats va
  WHERE a.account_id = va.account_id AND ag.account_session_id = a.account_session_id AND a.assessment_id = a2.assessment_id AND ag.last_assessment = true
  ORDER BY ag.account_session_grouping_id, a.created;


COMMIT;
