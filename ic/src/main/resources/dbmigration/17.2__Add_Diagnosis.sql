-- apply changes
alter table ic.patient_disposition add column diagnosis varchar(3);
alter table ic.patient_disposition add constraint ck_patient_disposition_diagnosis check ( diagnosis in ('100','0','6','4','3','1','11','5','7','9','10','8','2','-2','-1'));

