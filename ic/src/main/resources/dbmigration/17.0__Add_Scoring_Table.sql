-- apply changes
create table ic.scoring (
  id                            uuid not null,
  assessment_id                 uuid,
  score                         bigint not null,
  acuity                        varchar(6),
  questionnaire_type            varchar(17),
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  deleted                       boolean default false not null,
  constraint ck_scoring_acuity check ( acuity in ('LOW','MEDIUM','HIGH')),
  constraint ck_scoring_questionnaire_type check ( questionnaire_type in ('INFO','SYMPTOMS','DIAGNOSES','MILITARY','CSSRS','PHQ9','GAD7','ISI','PREPTSD','PTSD5','ASRM','PRIME5','SIMPLEPAIN','BPI','SIMPLEDRUGALCOHOL','OPIOIDSCREEN','DAST10','AUDITC','GOALS')),
  constraint pk_scoring primary key (id)
);

create index ix_scoring_assessment_id on ic.scoring (assessment_id);
alter table ic.scoring add constraint fk_scoring_assessment_id foreign key (assessment_id) references ic.assessment (id) on delete restrict on update restrict;

