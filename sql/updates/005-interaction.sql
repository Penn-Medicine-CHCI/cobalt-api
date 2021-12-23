BEGIN;
SELECT _v.register_patch('005-interaction', NULL, NULL);

ALTER TABLE account ADD COLUMN metadata JSONB NULL;

CREATE TABLE interaction_type 
(interation_type_id VARCHAR NOT NULL PRIMARY KEY,
 interaction_type VARCHAR NOT NULL);

INSERT INTO interaction_type VALUES ('EMAIL', 'Email');

CREATE TABLE interaction
(interaction_id UUID NOT NULL PRIMARY KEY,
 interaction_type_id VARCHAR NOT NULL REFERENCES interaction_type,
 institution_id VARCHAR NOT NULL REFERENCES institution,
 max_interaction_count INTEGER NOT NULL,
 interaction_complete_message VARCHAR NOT NULL,
 frequency_in_minutes INTEGER NOT NULL,
 created timestamptz NOT NULL DEFAULT now(),
 last_updated timestamptz NOT NULL 
);

create trigger set_last_updated before
insert or update on interaction for each row execute procedure set_last_updated();

CREATE TABLE interaction_option 
(interaction_option_id UUID NOT NULL PRIMARY KEY,
 interaction_id UUID NOT NULL REFERENCES interaction,
 option_description VARCHAR NOT NULL,
 option_response VARCHAR NOT NULL,
 completed_response VARCHAR NOT NULL,
 final_flag BOOLEAN NOT NULL,
 option_order INTEGER NOT NULL,
 created timestamptz NOT NULL DEFAULT now(),
 last_updated timestamptz NOT NULL 
);

create trigger set_last_updated before
insert or update on interaction_option for each row execute procedure set_last_updated();


CREATE TABLE interaction_instance 
(interaction_instance_id UUID NOT NULL PRIMARY KEY,
 interaction_id UUID NOT NULL REFERENCES interaction,
 account_id UUID NOT NULL REFERENCES account,
 start_date_time timestamptz NOT NULL,
 time_zone text NOT NULL DEFAULT 'America/New_York'::text,
 metadata JSONB,
 completed_flag BOOLEAN NOT NULL DEFAULT false,
 completed_date timestamptz NULL,
 created timestamptz NOT NULL DEFAULT now(),
 last_updated timestamptz NOT NULL 
);

create trigger set_last_updated before
insert or update on interaction_instance for each row execute procedure set_last_updated();

CREATE TABLE interaction_option_action
(interaction_option_action_id UUID NOT NULL PRIMARY KEY,
 interaction_instance_id UUID NOT NULL REFERENCES interaction_instance,
 interaction_option_id UUID NOT NULL REFERENCES interaction_option,
 account_id UUID NOT NULL REFERENCES account,
 created timestamptz NOT NULL DEFAULT now(),
 last_updated timestamptz NOT NULL 
);

create trigger set_last_updated before
insert or update on interaction_option_action for each row execute procedure set_last_updated();


END;