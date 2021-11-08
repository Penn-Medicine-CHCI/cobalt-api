-- apply changes
alter table if exists ic.messages drop constraint if exists fk_messages_patient_id;
alter table ic.messages add column had_flag integer;
alter table ic.messages add constraint ck_messages_had_flag check ( had_flag in (0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19));
alter table ic.messages add column disposition_id uuid;

