BEGIN;
SELECT _v.register_patch('019-provider-find-indexing', NULL, NULL);

-- We were actually missing the PK declaration here...
ALTER TABLE logical_availability_appointment_type ADD PRIMARY KEY (logical_availability_id, appointment_type_id);

-- Perf improvement
CREATE INDEX logical_availability_provider_id_idx ON logical_availability(provider_id);

END;