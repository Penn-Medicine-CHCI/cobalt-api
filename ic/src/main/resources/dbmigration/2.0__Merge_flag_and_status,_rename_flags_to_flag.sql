-- apply changes
alter table ic.patient_disposition add column flag integer default 11 not null;
