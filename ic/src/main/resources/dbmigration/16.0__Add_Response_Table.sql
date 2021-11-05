-- apply changes
create table ic.response_item (
  id                            uuid not null,
  assessment_id                 uuid,
  link_id                       varchar(255),
  string_value                  varchar(255),
  boolean_value                 boolean,
  code_system                   varchar(255),
  code_value                    varchar(255),
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  deleted                       boolean default false not null,
  constraint pk_response_item primary key (id)
);

create index ix_response_item_assessment_id on ic.response_item (assessment_id);
alter table ic.response_item add constraint fk_response_item_assessment_id foreign key (assessment_id) references ic.assessment (id) on delete restrict on update restrict;

