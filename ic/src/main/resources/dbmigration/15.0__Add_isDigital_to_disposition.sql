-- apply changes
alter table ic.patient_disposition add column is_digital boolean default true not null;

