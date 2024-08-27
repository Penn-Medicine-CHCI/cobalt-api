BEGIN;

CREATE SERVER cobalt_remote
FOREIGN DATA WRAPPER postgres_fdw
OPTIONS (host '1.1.1.1', port '5432', dbname 'cobalt'); 

CREATE USER MAPPING FOR cobalt
SERVER cobalt_remote
OPTIONS (user 'fdwuser', password 'password goes here');

COMMIT;