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
 account_id UUID REFERENCES account,
 start_date_time TIMESTAMP NOT NULL,
 time_zone text NOT NULL, -- e.g. 'America/New_York'
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

-- Add example interactions
INSERT INTO interaction
(interaction_id,interaction_type_id,max_interaction_count, frequency_in_minutes, institution_id,interaction_complete_message)
VALUES
('45f4082c-4d16-400e-aecd-38e87726f6d9', 'EMAIL', 3, 1440, 'COBALT', 'This case has already been completed.');

INSERT INTO interaction_option
(interaction_option_id,interaction_id, option_description, option_response,final_flag, option_order, completed_response)
VALUES
(uuid_generate_v4(), '45f4082c-4d16-400e-aecd-38e87726f6d9', 'Contacted, no response', 'We will send an email in [frequencyHoursAndMinutes] as a reminder to try again.'
	, false, 1, 'You and your team have logged [maxInteractionCount] contact attempts so we will consider this case closed. Thank you for your efforts.'),
(uuid_generate_v4(), '45f4082c-4d16-400e-aecd-38e87726f6d9', 'Assessed, no follow-up needed', 'Thank you. That took [completionTimeHoursAndMinutes] to close the case.'
	, true, 2,'Thank you. That took [completionTimeHoursAndMinutes] to close the case.' ),
(uuid_generate_v4(), '45f4082c-4d16-400e-aecd-38e87726f6d9', 'Assessed, provided resources', 'Thank you. That took [completionTimeHoursAndMinutes] to close the case.'
	, true, 3, 'Thank you. That took [completionTimeHoursAndMinutes] to close the case.');

END;