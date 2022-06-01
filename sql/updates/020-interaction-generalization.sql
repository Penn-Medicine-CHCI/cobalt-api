BEGIN;
SELECT _v.register_patch('020-interaction-generalization', NULL, NULL);

INSERT INTO interaction_type
VALUES
('SI', 'Suicide Ideation'),
('ROLE_REQUEST', 'Role Request'),
('APPOINTMENT_PATIENT', 'Appointment Patient'),
('APPOINTMENT_PROVIDER', 'Appointment Provider');

ALTER TABLE interaction_type RENAME COLUMN interation_type_id TO interaction_type_id;

-- Update old EMAIL interactions
UPDATE interaction SET interaction_type_id = 'SI' WHERE interaction_complete_message = 'This case has already been completed.';
UPDATE interaction SET interaction_type_id = 'ROLE_REQUEST' WHERE interaction_complete_message = 'Role request process has been completed.';

DELETE FROM interaction_type WHERE interaction_type_id = 'EMAIL';

CREATE TABLE provider_interaction
(provider_interaction_id UUID PRIMARY KEY,
provider_id UUID NOT NULL REFERENCES provider,
interaction_id UUID NOT NULL REFERENCES interaction,
created timestamptz NOT NULL DEFAULT now(),
last_updated timestamptz NOT NULL DEFAULT now()
);

create trigger set_last_updated before
insert or update on provider_interaction for each row execute procedure set_last_updated();

ALTER TABLE appointment ADD COLUMN interaction_instance_id UUID NULL REFERENCES interaction_instance;

ALTER TABLE interaction ADD COLUMN send_offset_in_minutes INTEGER NOT NULL DEFAULT 0;

ALTER TABLE interaction ADD COLUMN message_template VARCHAR NULL;

ALTER TABLE interaction ADD COLUMN message_template_body VARCHAR NULL;

UPDATE interaction SET message_template = 'INTERACTION_REMINDER';

ALTER TABLE interaction ALTER COLUMN message_template SET NOT NULL;

CREATE TABLE appointment_interaction_instance
(appointment_interaction_instance_id UUID PRIMARY KEY,
appointment_id UUID NOT NULL REFERENCES appointment,
interaction_instance_id UUID NOT NULL REFERENCES interaction_instance,
created timestamptz NOT NULL DEFAULT now(),
last_updated timestamptz NOT NULL DEFAULT now()
);

create trigger set_last_updated before
insert or update on appointment_interaction_instance for each row execute procedure set_last_updated();

END;