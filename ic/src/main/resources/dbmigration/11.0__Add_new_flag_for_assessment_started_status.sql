-- apply changes
alter table ic.patient_disposition drop constraint if exists ck_patient_disposition_flag;
alter table ic.patient_disposition add constraint ck_patient_disposition_flag check ( flag in (0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20));
alter table ic.messages drop constraint if exists ck_messages_had_flag;
alter table ic.messages add constraint ck_messages_had_flag check ( had_flag in (0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20));
