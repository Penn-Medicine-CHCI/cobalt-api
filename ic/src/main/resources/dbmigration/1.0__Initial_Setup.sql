-- apply changes
create table ic.assessment (
  id                            uuid not null,
  patient_id                    uuid,
  status                        varchar(11),
  due                           timestamptz,
  authored_by                   varchar(255),
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  deleted                       boolean default false not null,
  constraint ck_assessment_status check ( status in ('NOT_STARTED','IN_PROGRESS','COMPLETED','STALE')),
  constraint pk_assessment primary key (id)
);

create table ic.patient (
  id                            uuid not null,
  logged_in_dt                  varchar[],
  fhir_id                       varchar(255),
  fhir_provider                 varchar(255),
  goals                         varchar[],
  preferred_first_name          varchar(255),
  preferred_last_name           varchar(255),
  preferred_email               varchar(255),
  preferred_phone_number        varchar(255),
  preferred_gender              varchar(255),
  mrn                           varchar(255),
  cobalt_account_id             uuid,
  orders                        varchar[],
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  deleted                       boolean default false not null,
  constraint uq_patient_fhir_id unique (fhir_id),
  constraint uq_patient_cobalt_account_id unique (cobalt_account_id),
  constraint pk_patient primary key (id)
);

create table ic.patient_disposition (
  id                            uuid not null,
  patient_id                    uuid,
  status                        integer,
  outcome                       jsonb,
  responses                     uuid[],
  notes                         jsonb,
  flags                         jsonb,
  triage_review_id              uuid,
  acuity                        jsonb,
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  deleted                       boolean default false not null,
  constraint ck_patient_disposition_status check ( status in (0,1,2,3,4,5,6,7)),
  constraint uq_patient_disposition_triage_review_id unique (triage_review_id),
  constraint pk_patient_disposition primary key (id)
);

create table ic.questionnaire_definition (
  id                            uuid not null,
  questionnaire_id              varchar(255),
  filename                      varchar(255),
  questionnaire_version         varchar(255),
  link_id                       varchar(255),
  questionnaire_fhir            jsonb,
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  deleted                       boolean default false not null,
  constraint pk_questionnaire_definition primary key (id)
);

create table ic.questionnaire_response (
  id                            uuid not null,
  questionnaire_type            varchar(255),
  response                      jsonb,
  assessment_id                 uuid,
  patient_id                    uuid,
  score                         bigint not null,
  acuity                        varchar(6),
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  deleted                       boolean default false not null,
  constraint ck_questionnaire_response_acuity check ( acuity in ('LOW','MEDIUM','HIGH')),
  constraint pk_questionnaire_response primary key (id)
);

create table ic.referral_order_report (
  order_id                      varchar(255) not null,
  encounter_dept_name           varchar(255),
  encounter_dept_id             varchar(255),
  referring_practice            varchar(255),
  referring_practice_second     varchar(255),
  ordering_provider             varchar(255),
  billing_provider              varchar(255),
  last_name                     varchar(255),
  first_name                    varchar(255),
  mrn                           varchar(255),
  uid                           varchar(255),
  sex                           varchar(255),
  date_of_birth                 varchar(255),
  primary_payor                 varchar(255),
  primary_plan                  varchar(255),
  order_date                    varchar(255),
  age_of_order                  varchar(255),
  ccbh_order_routing            varchar(255),
  reasons_for_referral          varchar(255),
  dx                            varchar(255),
  order_associated_diagnosis    varchar(255),
  call_back_number              varchar(255),
  preferred_contact_hours       varchar(255),
  order_comments                varchar(255),
  img_cc_recipients             varchar(255),
  patient_address_line1         varchar(255),
  patient_address_line2         varchar(255),
  city                          varchar(255),
  patient_state                 varchar(255),
  patient_zip_code              varchar(255),
  ccbh_last_active_med_order_summary varchar(255),
  ccbh_medications_list         varchar(255),
  psychotherapeutic_med_lst_two_weeks varchar(255),
  constraint pk_referral_order_report primary key (order_id)
);

create table ic.triage_review (
  id                            uuid not null,
  bhp_reviewed_dt               timestamptz,
  psychiatrist_reviewed_dt      timestamptz,
  comment                       varchar(255),
  needs_focused_review          boolean,
  patient_id                    uuid not null,
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  deleted                       boolean default false not null,
  constraint pk_triage_review primary key (id)
);

create index ix_assessment_patient_id on ic.assessment (patient_id);
alter table ic.assessment add constraint fk_assessment_patient_id foreign key (patient_id) references ic.patient (id) on delete restrict on update restrict;

create index ix_patient_disposition_patient_id on ic.patient_disposition (patient_id);
alter table ic.patient_disposition add constraint fk_patient_disposition_patient_id foreign key (patient_id) references ic.patient (id) on delete restrict on update restrict;

alter table ic.patient_disposition add constraint fk_patient_disposition_triage_review_id foreign key (triage_review_id) references ic.triage_review (id) on delete restrict on update restrict;

create index ix_questionnaire_response_assessment_id on ic.questionnaire_response (assessment_id);
alter table ic.questionnaire_response add constraint fk_questionnaire_response_assessment_id foreign key (assessment_id) references ic.assessment (id) on delete restrict on update restrict;

create index ix_questionnaire_response_patient_id on ic.questionnaire_response (patient_id);
alter table ic.questionnaire_response add constraint fk_questionnaire_response_patient_id foreign key (patient_id) references ic.patient (id) on delete restrict on update restrict;

create index ix_triage_review_patient_id on ic.triage_review (patient_id);
alter table ic.triage_review add constraint fk_triage_review_patient_id foreign key (patient_id) references ic.patient (id) on delete restrict on update restrict;

