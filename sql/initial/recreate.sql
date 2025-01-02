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

--Revoke all of the Foreign Data Wrapper permissions so the cobalt role can be dropped
REVOKE ALL ON FUNCTION postgres_fdw_handler() FROM cobalt;
REVOKE ALL ON FUNCTION postgres_fdw_validator(text[], oid) FROM cobalt;
REVOKE ALL ON FUNCTION postgres_fdw_get_connections() FROM cobalt;
REVOKE ALL ON FUNCTION postgres_fdw_disconnect(text) FROM cobalt;
REVOKE ALL ON FUNCTION postgres_fdw_disconnect_all() FROM cobalt;

--Reubild the template database. This is really only needed if we're swiching BD images/versions
UPDATE pg_database SET datallowconn = 'false' WHERE datname = 'template1';
ALTER DATABASE template1 IS_TEMPLATE = false;
DROP DATABASE template1;
CREATE DATABASE template1 WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8';
UPDATE pg_database SET datistemplate = true WHERE datname = 'template1';
ALTER DATABASE template1 ALLOW_CONNECTIONS = true;

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
CREATE EXTENSION postgis WITH SCHEMA cobalt;
GRANT USAGE ON FOREIGN DATA WRAPPER postgres_fdw TO cobalt; 

CREATE USER fdwuser WITH PASSWORD 'password';
GRANT USAGE ON SCHEMA cobalt TO fdwUser;

ALTER USER fdwuser SET search_path TO cobalt;