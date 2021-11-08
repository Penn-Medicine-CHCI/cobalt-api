-- apply changes
create table ic.disposition_note (
  disposition_note_id           uuid not null,
  note                          TEXT not null,
  disposition_id                uuid,
  cobalt_account_account_id     uuid,
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  deleted                       boolean default false not null,
  constraint pk_disposition_note primary key (disposition_note_id)
);

create index ix_disposition_note_disposition_id on ic.disposition_note (disposition_id);
alter table ic.disposition_note add constraint fk_disposition_note_disposition_id foreign key (disposition_id) references ic.patient_disposition (id) on delete restrict on update restrict;

create index ix_disposition_note_cobalt_account_account_id on ic.disposition_note (cobalt_account_account_id);
alter table ic.disposition_note add constraint fk_disposition_note_cobalt_account_account_id foreign key (cobalt_account_account_id) references ic.cobalt_account (account_id) on delete restrict on update restrict;

