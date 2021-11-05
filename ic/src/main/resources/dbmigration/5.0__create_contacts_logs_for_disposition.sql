-- apply changes
create table ic.contact (
  id                            uuid not null,
  disposition_id                uuid,
  authored_by                   varchar(255),
  referring_location            varchar(1),
  call_result                   varchar(1),
  note                          varchar(255),
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  deleted                       boolean default false not null,
  constraint ck_contact_referring_location check ( referring_location in ('0','1','2','3','4','5','6','7','8')),
  constraint ck_contact_call_result check ( call_result in ('0','1','2','3','4','5','6','7','8','9')),
  constraint pk_contact primary key (id)
);

create index ix_contact_disposition_id on ic.contact (disposition_id);
alter table ic.contact add constraint fk_contact_disposition_id foreign key (disposition_id) references ic.patient_disposition (id) on delete restrict on update restrict;

