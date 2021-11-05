-- apply changes
alter table ic.patient add column preferred_email_has_been_updated boolean default false not null;
alter table ic.patient add column preferred_phone_has_been_updated boolean default false not null;

