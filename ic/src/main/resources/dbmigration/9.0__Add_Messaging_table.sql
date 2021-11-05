-- apply changes
create table ic.messages (
  id                            uuid not null,
  patient_id                    uuid,
  attempt                       smallint not null,
  body                          varchar(255),
  address                       varchar(255),
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  deleted                       boolean default false not null,
  constraint pk_messages primary key (id)
);

create index ix_messages_patient_id on ic.messages (patient_id);
alter table ic.messages add constraint fk_messages_patient_id foreign key (patient_id) references ic.patient (id) on delete restrict on update restrict;

