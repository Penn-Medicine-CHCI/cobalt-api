-- apply changes
alter table ic.triage_review add column disposition_id uuid;

alter table ic.triage_review add constraint fk_triage_review_disposition_id foreign key (disposition_id) references ic.patient_disposition (id) on delete restrict on update restrict;

