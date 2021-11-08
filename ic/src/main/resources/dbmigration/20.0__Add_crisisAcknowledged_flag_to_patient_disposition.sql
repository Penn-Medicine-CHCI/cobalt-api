-- apply changes
alter table ic.patient_disposition add column crisis_acknowledged boolean default false not null;

