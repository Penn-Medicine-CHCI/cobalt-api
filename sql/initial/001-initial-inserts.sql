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
SELECT _v.register_patch('001-initial-inserts', ARRAY['000-base-creates'], NULL);

INSERT INTO cobalt."role" (role_id,description) VALUES
	 ('ADMINISTRATOR','Adminstrator'),
	 ('SUPER_ADMINISTRATOR','Super Administrator'),
	 ('MHIC','Mental Health Intake Coordinator'),
	 ('PATIENT','Patient'),
	 ('PROVIDER','Provider'),
	 ('BHS','Behavioral Health Specialist');
INSERT INTO cobalt.account_source (account_source_id,description,prod_sso_url,dev_sso_url,local_sso_url) VALUES
	 ('ANONYMOUS','Anonymous',NULL,NULL,NULL),
	 ('EMAIL_PASSWORD','Email and Password',NULL,NULL,NULL);
INSERT INTO cobalt.activity_action (activity_action_id,description) VALUES
	 ('VIEW','View'),
	 ('DOWNLOAD','Download');
INSERT INTO cobalt.activity_type (activity_type_id,description) VALUES
	 ('CONTENT','Interacting with content');
INSERT INTO cobalt.appointment_reason_type (appointment_reason_type_id,description) VALUES
	 ('MHIC_SELF_SCHEDULE_INITIAL','MHIC self-schedule of initial appointment'),
	 ('MHIC_SELF_SCHEDULE_FOLLOWUP','MHIC self-schedule of followup'),
	 ('NOT_SPECIFIED','Not specified');
INSERT INTO cobalt.approval_status (approval_status_id,description,created,last_updated) VALUES
	 ('PENDING','Pending','2020-12-04 10:58:09.404699-05','2020-12-04 10:58:09.404699-05'),
	 ('REJECTED','Rejected','2020-12-04 10:58:09.404699-05','2020-12-04 10:58:09.404699-05'),
	 ('APPROVED','Live','2020-12-04 10:58:09.404699-05','2020-12-04 10:58:09.404699-05'),
	 ('NOT_APPLICABLE','N/A','2020-12-04 10:58:09.404699-05','2020-12-04 10:58:09.404699-05'),
	 ('ARCHIVED','Archived','2020-12-04 10:58:09.404699-05','2020-12-04 10:58:09.404699-05');
INSERT INTO cobalt.assessment_type (assessment_type_id,description) VALUES
	 ('INTRO','Introductory Survey'),
	 ('PHQ4','Evidence-Based Assessment'),
	 ('GAD7','Generalized Anxiety Disorder 7-item'),
	 ('PHQ9','Patient Health Questionnaire-9'),
	 ('PCPTSD','Primary Care PTSD Screen'),
	 ('INTAKE','Clinic intake assessment');
INSERT INTO cobalt.attendance_status (attendance_status_id,description) VALUES
	 ('UNKNOWN','Unknown'),
	 ('MISSED','Missed'),
	 ('CANCELED','Canceled');
INSERT INTO cobalt.audit_log_event (audit_log_event_id,description) VALUES
	 ('PATIENT_CREATE','Create a patient'),
	 ('PATIENT_SEARCH','Search for a patient'),
	 ('APPOINTMENT_CREATE','Create an appointment'),
	 ('APPOINTMENT_CANCEL','Cancel an appointment'),
	 ('APPOINTMENT_LOOKUP','Lookup appointments'),
	 ('ACCOUNT_LOOKUP_SUCCESS','Account sign-in success'),
	 ('ACCOUNT_CREATE','Creation of an anonymous account'),
	 ('ACCOUNT_LOOKUP_FAILURE','Failed sign-in attempt');
INSERT INTO cobalt.available_status (available_status_id,description,created,last_updated) VALUES
	 ('ADDED','Live','2020-12-04 10:58:09.404699-05','2020-12-04 10:58:09.404699-05'),
	 ('AVAILABLE','Available','2020-12-04 10:58:09.404699-05','2020-12-04 10:58:09.404699-05');
INSERT INTO cobalt.beta_status (beta_status_id,description) VALUES
	 ('UNKNOWN','Unknown'),
	 ('ENABLED','Enabled'),
	 ('DISABLED','Disabled');
INSERT INTO cobalt.content_action (content_action_id,decription) VALUES
	 ('EDIT','Edit'),
	 ('APPROVE','Approve'),
	 ('REJECT','Reject'),
	 ('DELETE','Delete'),
	 ('ARCHIVE','Archive'),
	 ('UNARCHIVE','Unarchive'),
	 ('ADD','Add'),
	 ('REMOVE','Remove');
INSERT INTO cobalt.content_type (content_type_id,description,call_to_action) VALUES
	 ('VIDEO','Video','watch the video'),
	 ('AUDIO','Audio','listen'),
	 ('PODCAST','Podcast','listen'),
	 ('ARTICLE','Article','read the article'),
	 ('WORKSHEET','Worksheet','complete the worksheet'),
	 ('INT_BLOG','Internal Blog Post','read blog post'),
	 ('EXT_BLOG','External Blog Post','read blog post');
INSERT INTO cobalt.epic_appointment_filter (epic_appointment_filter_id,description) VALUES
	 ('NONE','None'),
	 ('VISIT_TYPE','Visit Type');
INSERT INTO cobalt.followup_email_status (followup_email_status_id,description) VALUES
	 ('NOT_SENT','Not Sent'),
	 ('SENDING','Sending'),
	 ('SENT','Sent'),
	 ('ERROR','Error');
INSERT INTO cobalt.group_session_request_status (group_session_request_status_id,description) VALUES
	 ('ARCHIVED','Archived'),
	 ('DELETED','Deleted'),
	 ('NEW','New'),
	 ('ADDED','Live');
INSERT INTO cobalt.group_session_scheduling_system (group_session_scheduling_system_id,description) VALUES
	 ('COBALT','Cobalt'),
	 ('EXTERNAL','External');
INSERT INTO cobalt.group_session_status (group_session_status_id,description) VALUES
	 ('NEW','New'),
	 ('ARCHIVED','Archived'),
	 ('CANCELED','Canceled'),
	 ('DELETED','Deleted'),
	 ('ADDED','Live');
INSERT INTO cobalt.group_session_system (group_session_system_id,description) VALUES
	 ('NATIVE','Native'),
	 ('ACUITY','Acuity');
INSERT INTO cobalt.login_destination (login_destination_id,description) VALUES
	 ('COBALT_PATIENT','Cobalt Patient Experience'),
	 ('PIC_PANEL','PIC Panel');
INSERT INTO cobalt.message_status (message_status_id,description) VALUES
	 ('ENQUEUED','Enqueued'),
	 ('SENT','Sent'),
	 ('ERROR','Error');
INSERT INTO cobalt.message_type (message_type_id,description) VALUES
	 ('EMAIL','Email'),
	 ('PUSH','Push notification'),
	 ('SMS','SMS'),
	 ('CALL','Phone Call');
INSERT INTO cobalt.payment_funding (payment_funding_id,description) VALUES
	 ('NO_FEE','No Fee'),
	 ('INSURANCE','Insurance'),
	 ('SELF_PAY','Self-pay');
INSERT INTO cobalt.payment_type (payment_type_id,description,display_order,payment_funding_id) VALUES
	 ('KEYSTONE_HEALTH_PLAN_EAST','Keystone Health Plan East',3,'INSURANCE'),
	 ('BLUE_SHIELD','Blue Shield',4,'INSURANCE'),
	 ('AETNA','Aetna',5,'INSURANCE'),
	 ('AETNA_STUDENT_HEALTH','Aetna Student Health',6,'INSURANCE'),
	 ('MEDICARE','Medicare',7,'INSURANCE'),
	 ('MEDICAID','Medicaid',8,'INSURANCE'),
	 ('HORIZON_BLUE_CROSS','Horizon Blue Cross',9,'INSURANCE'),
	 ('INDEPENDENCE_BLUE_CROSS','Independence Blue Cross',10,'INSURANCE'),
	 ('UNITED_HEALTH_CARE','United Health Care',11,'INSURANCE');
INSERT INTO cobalt.payment_type (payment_type_id,description,display_order,payment_funding_id) VALUES
	 ('CIGNA','Cigna',12,'INSURANCE'),
	 ('COMMUNITY_BEHAVIORAL_HEALTH','Community Behavioral Health',13,'INSURANCE'),
	 ('NO_FEE','No fee',1,'NO_FEE'),
	 ('OTHER_INSURANCE','Other insurance',15,'INSURANCE'),
	 ('SELF_PAY','Self-pay',16,'SELF_PAY');
INSERT INTO cobalt.question_type (question_type_id,description,allow_multiple_answers,requires_text_response) VALUES
	 ('RADIO','Radio Group',false,false),
	 ('DROPDOWN','Dropdown',false,false),
	 ('QUAD','Quad',false,false),
	 ('CHECKBOX','Checkbox',true,false),
	 ('TEXT','Free form text',false,true),
	 ('DATE','Date',false,true),
	 ('PHONE_NUMBER','Phone number',false,true),
	 ('HORIZONTAL_CHECKBOX','Horizontal scrolling checkboxes',true,false);
INSERT INTO cobalt.support_role (support_role_id,description,display_order) VALUES
	 ('PEER','Peer',1),
	 ('COACH','Resilience Coach',2),
	 ('CLINICIAN','Therapist',4),
	 ('CHAPLAIN','Non-Denom Chaplain',6),
	 ('OTHER','Other',7),
	 ('PSYCHIATRIST','Psychiatrist',5),
	 ('CARE_MANAGER','EAP Intake Counselor',3),
	 ('MHIC','Mental Health Intake Coordinator',7);
INSERT INTO cobalt.recommendation_level (recommendation_level_id,description) VALUES
	 ('PSYCHIATRIST','psychiatrist'),
	 ('COACH','resilience coach'),
	 ('COACH_CLINICIAN','resilience coach'),
	 ('CLINICIAN','therapist'),
	 ('CLINICIAN_PSYCHIATRIST','therapist'),
	 ('PEER','resilience coach'),
	 ('PEER_COACH','resilience coach');
INSERT INTO cobalt.recommendation_level_to_support_role (recommendation_level_id,support_role_id) VALUES
	 ('PEER','COACH'),
	 ('PEER_COACH','COACH'),
	 ('COACH','COACH'),
	 ('COACH_CLINICIAN','COACH'),
	 ('CLINICIAN','CLINICIAN'),
	 ('CLINICIAN_PSYCHIATRIST','CLINICIAN'),
	 ('PSYCHIATRIST','PSYCHIATRIST'),
	 ('PEER','CARE_MANAGER'),
	 ('PEER_COACH','CARE_MANAGER'),
	 ('COACH','CARE_MANAGER');
INSERT INTO cobalt.recommendation_level_to_support_role (recommendation_level_id,support_role_id) VALUES
	 ('COACH_CLINICIAN','CARE_MANAGER');
INSERT INTO cobalt.scheduling_system (scheduling_system_id,description) VALUES
	 ('NONE','None'),
	 ('ACUITY','Acuity'),
	 ('EPIC','Epic'),
	 ('COBALT','Cobalt');
INSERT INTO cobalt.source_system (source_system_id,description) VALUES
	 ('COBALT','Cobalt'),
	 ('PIC','PIC');
INSERT INTO cobalt.system_affinity (system_affinity_id,description) VALUES
	 ('COBALT','Cobalt'),
	 ('PIC','PIC'),
	 ('ALL','All');
INSERT INTO cobalt.videoconference_platform (videoconference_platform_id,description) VALUES
	 ('BLUEJEANS','Bluejeans'),
	 ('EXTERNAL','External'),
	 ('TELEPHONE','Telephone'),
	 ('SWITCHBOARD','Switchboard');
INSERT INTO cobalt.visibility (visibility_id,description,created,last_updated) VALUES
	 ('PRIVATE','My institution','2020-12-04 10:58:09.404699-05','2020-12-04 10:58:09.404699-05'),
	 ('NETWORK','My network','2020-12-04 10:58:09.404699-05','2020-12-04 10:58:09.404699-05'),
	 ('PUBLIC','Publicly available','2020-12-04 10:58:09.404699-05','2020-12-04 10:58:09.404699-05');
INSERT INTO cobalt.visit_type (visit_type_id,description,display_order) VALUES
	 ('INITIAL','Initial Visit',1),
	 ('FOLLOWUP','Followup Visit',2),
	 ('OTHER','Other',3);

INSERT INTO appointment_type (appointment_type_id, acuity_appointment_type_id, name, duration_in_minutes, visit_type_id, scheduling_system_id) VALUES 
('a755e720-5ec9-4643-84d4-5b921e0d4452', '13757193', 'Initial Consult', 60,'INITIAL', 'COBALT'),
('a95820d2-07cd-45b4-a0db-cda083b4b03e', '14051518', '30-Minute Followup', 30,'FOLLOWUP', 'COBALT');


INSERT INTO cobalt.content_type_label (content_type_label_id,description) VALUES
 	('ACTION_GUIDE','Action Guide'),
	 ('EXTERNAL_BLOG_POST','External Blog Post'),
	 ('WORKSHEET','Worksheet'),
	 ('WEBINAR_SERIES','Webinar Series'),
	 ('FACT_SHEET','Fact Sheet'),
	 ('INTERNAL_BLOG_POST','Internal Blog Post'),
	 ('PODCAST','Podcast'),
	 ('ARTICLE','Article'),
	 ('INFORMATIONAL_HANDOUT','Informational Handout'),
	 ('HANDS_ON_ACTIVITIES_FOR_YOU_AND_YOUR_CHILD','Hands on Activities for you and your child');
INSERT INTO cobalt.content_type_label (content_type_label_id,description) VALUES
	('VIDEO','Video'),
	 ('RESOURCE_GUIDE','Resource Guide');

INSERT INTO font_size (font_size_id, description) VALUES 
('DEFAULT', 'Default'),
('SMALL', 'Small');

COMMIT;