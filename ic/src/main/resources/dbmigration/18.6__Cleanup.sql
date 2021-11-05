-- apply changes
alter table ic.patient_disposition drop column responses;

drop table if exists ic.questionnaire_definition cascade;
