-- apply changes
alter table ic.patient add column uid varchar(255);

alter table ic.patient_disposition drop constraint if exists ck_patient_disposition_diagnosis;
alter table ic.patient_disposition add constraint ck_patient_disposition_diagnosis check ( diagnosis in ('100','0','6','4','40','3','1','11','5','7','2','9','10','8','-2','-1'));
