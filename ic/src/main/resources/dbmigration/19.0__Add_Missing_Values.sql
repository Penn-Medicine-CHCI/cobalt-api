-- apply changes
alter table ic.patient_disposition drop constraint if exists ck_patient_disposition_diagnosis;
alter table ic.patient_disposition add constraint ck_patient_disposition_diagnosis check ( diagnosis in ('100','0','6','4','40','3','1','11','5','7','9','10','8','2','-2','-1'));
alter table ic.scoring drop constraint if exists ck_scoring_questionnaire_type;
alter table ic.scoring add constraint ck_scoring_questionnaire_type check ( questionnaire_type in ('INFO','SYMPTOMS','DIAGNOSES','MILITARY','CSSRS_SHORT','CSSRS','PHQ9','GAD7','ISI','PREPTSD','PTSD5','ASRM','PRIME5','SIMPLEPAIN','BPI','SIMPLEDRUGALCOHOL','OPIOIDSCREEN','DAST10','AUDITC','GOALS'));
