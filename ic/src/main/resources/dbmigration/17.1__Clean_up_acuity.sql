-- apply changes
alter table ic.patient_disposition add column acuity_category varchar(6);
alter table ic.patient_disposition add constraint ck_patient_disposition_acuity_category check ( acuity_category in ('LOW','MEDIUM','HIGH'));

