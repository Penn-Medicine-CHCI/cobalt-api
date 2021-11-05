-- apply changes
alter table ic.messages add column succeeded boolean default false not null;

