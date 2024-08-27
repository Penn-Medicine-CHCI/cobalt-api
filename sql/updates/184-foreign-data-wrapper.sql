BEGIN;
SELECT _v.register_patch('184-non-local-foreign-data-wrapper', NULL, NULL);

CREATE SERVER cobalt_remote
FOREIGN DATA WRAPPER postgres_fdw
OPTIONS (host '1.1.1.1', port '5432', dbname 'cobalt'); 

CREATE USER MAPPING FOR cobalt_app
SERVER cobalt_remote
OPTIONS (user 'fdwuser', password 'password goes here');

COMMIT;