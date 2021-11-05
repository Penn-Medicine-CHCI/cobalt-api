BEGIN;

create table ic.assessment_status_change (
  id UUID NOT NULL,
  assessment_id UUID NOT NULL REFERENCES assessment(id),
  account_id UUID REFERENCES cobalt_account,
  patient_id UUID REFERENCES patient(id),
  old_status VARCHAR,
  new_status VARCHAR NOT NULL,
  created_dt timestamptz not null,
  constraint ck_assessment_status_change_old_status check ( old_status in ('NOT_STARTED','IN_PROGRESS','COMPLETED','STALE')),
  constraint ck_assessment_status_change_new_status check ( new_status in ('NOT_STARTED','IN_PROGRESS','COMPLETED','STALE'))
);

COMMIT;