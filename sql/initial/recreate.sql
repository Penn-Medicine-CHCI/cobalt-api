REVOKE CONNECT ON DATABASE cobalt FROM PUBLIC, cobalt;

SELECT
    pg_terminate_backend(pid)
FROM
    pg_stat_activity
WHERE
    -- don't kill my own connection!
    pid <> pg_backend_pid()
    -- don't kill the connections to other databases
    AND datname = 'cobalt'
;

-- DROP SCHEMA IF EXISTS ic CASCADE;
DROP DATABASE cobalt;
DROP ROLE cobalt;
DROP USER fdwuser;

CREATE ROLE cobalt LOGIN PASSWORD 'password' NOSUPERUSER NOINHERIT NOCREATEROLE;
CREATE DATABASE cobalt WITH OWNER = cobalt ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8';

\c cobalt;

CREATE SCHEMA cobalt
AUTHORIZATION cobalt;


ALTER USER cobalt SET search_path TO cobalt;

ALTER DATABASE cobalt SET timezone TO 'UTC';

CREATE EXTENSION "uuid-ossp" WITH SCHEMA cobalt;
CREATE EXTENSION pg_trgm WITH SCHEMA cobalt;
CREATE EXTENSION unaccent WITH SCHEMA cobalt;
CREATE EXTENSION hstore SCHEMA cobalt;
CREATE EXTENSION pg_stat_statements WITH SCHEMA cobalt;
--CREATE EXTENSION "plv8";
CREATE EXTENSION postgres_fdw;
GRANT USAGE ON FOREIGN DATA WRAPPER postgres_fdw TO cobalt;

CREATE USER fdwuser WITH PASSWORD 'password';
GRANT USAGE ON SCHEMA cobalt TO fdwUser;

ALTER USER fdwuser SET search_path TO cobalt;