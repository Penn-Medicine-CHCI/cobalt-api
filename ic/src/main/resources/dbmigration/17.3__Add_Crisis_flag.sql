-- apply changes
alter table ic.patient_disposition add column crisis boolean default false not null;

