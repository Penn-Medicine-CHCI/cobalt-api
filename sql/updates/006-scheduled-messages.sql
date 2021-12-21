BEGIN;
SELECT _v.register_patch('006-scheduled-messages', NULL, NULL);

CREATE TABLE scheduled_message_status (
	scheduled_message_status_id TEXT PRIMARY KEY,
	description TEXT NOT NULL
);

INSERT INTO scheduled_message_status VALUES ('PENDING', 'Pending');
INSERT INTO scheduled_message_status VALUES ('PROCESSED', 'Processed');
INSERT INTO scheduled_message_status VALUES ('CANCELED', 'Canceled');
INSERT INTO scheduled_message_status VALUES ('ERROR', 'Error');

CREATE TABLE scheduled_message (
	scheduled_message_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	scheduled_message_status_id TEXT NOT NULL REFERENCES scheduled_message_status DEFAULT 'PENDING',
	message_id UUID NOT NULL,
	message_type_id TEXT NOT NULL REFERENCES message_type,
	serialized_message JSONB NOT NULL,
	scheduled_at TIMESTAMP NOT NULL,
	time_zone TEXT NOT NULL,
	stack_trace TEXT,
	metadata JSONB,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
	processed_at TIMESTAMP WITH TIME ZONE,
	canceled_at TIMESTAMP WITH TIME ZONE,
	errored_at TIMESTAMP WITH TIME ZONE
);

END;