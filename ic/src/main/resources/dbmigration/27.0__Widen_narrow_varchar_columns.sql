-- apply changes
alter table ic.contact alter column note type varchar(10000) using note::varchar(10000);
alter table ic.specialty_care_scheduling alter column notes type varchar(10000) using notes::varchar(10000);
alter table ic.triage_review alter column comment type varchar(10000) using comment::varchar(10000);
