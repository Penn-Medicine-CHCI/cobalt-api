DO
$do$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ic') THEN
      CREATE ROLE ic LOGIN PASSWORD 'password' NOSUPERUSER NOINHERIT NOCREATEROLE;
   END IF;
END
$do$;

\c cobalt;

DROP SCHEMA IF EXISTS ic CASCADE;

CREATE SCHEMA ic AUTHORIZATION ic;
ALTER USER ic SET search_path TO ic;

grant connect on database cobalt to ic;

alter default privileges for role ic in schema ic grant all on tables to ic;
alter default privileges for role ic in schema ic grant all on sequences to ic;
alter default privileges for role ic in schema ic grant all on functions to ic;
