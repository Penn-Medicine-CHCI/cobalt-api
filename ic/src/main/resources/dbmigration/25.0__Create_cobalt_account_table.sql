-- apply changes
create table ic.cobalt_account (
  account_id                    uuid not null,
  provider_id                   uuid,
  role_id                       varchar(255) not null,
  first_name                    varchar(255),
  last_name                     varchar(255),
  display_name                  varchar(255),
  email_address                 varchar(255),
  created_dt                    timestamptz not null,
  updated_dt                    timestamptz not null,
  constraint uq_cobalt_account_provider_id unique (provider_id),
  constraint pk_cobalt_account primary key (account_id)
);

