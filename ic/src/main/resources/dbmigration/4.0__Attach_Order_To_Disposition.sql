-- apply changes
alter table ic.patient add constraint uq_patient_mrn unique  (mrn);
alter table ic.referral_order_report add column disposition_id uuid;

create index ix_referral_order_report_disposition_id on ic.referral_order_report (disposition_id);
alter table ic.referral_order_report add constraint fk_referral_order_report_disposition_id foreign key (disposition_id) references ic.patient_disposition (id) on delete restrict on update restrict;

