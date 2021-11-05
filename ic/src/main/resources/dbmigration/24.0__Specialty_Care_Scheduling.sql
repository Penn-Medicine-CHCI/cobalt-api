-- apply changes
create table ic.specialty_care_scheduling (
  id                            uuid not null,
  disposition_id                uuid,
  agency                        varchar(255) not null,
  date                          date not null,
  time                          time,
  attendance_confirmed          boolean default false not null,
  notes                         varchar(255),
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  deleted                       boolean default false not null,
  constraint uq_specialty_care_scheduling_disposition_id unique (disposition_id),
  constraint pk_specialty_care_scheduling primary key (id)
);

alter table ic.specialty_care_scheduling add constraint fk_specialty_care_scheduling_disposition_id foreign key (disposition_id) references ic.patient_disposition (id) on delete restrict on update restrict;

