BEGIN;
SELECT _v.register_patch('225-cron', NULL, NULL);

-- Cron jobs have their own footprint event group
INSERT INTO footprint_event_group_type VALUES ('CRON_JOB', 'Cron Job');

-- Status for a specific run of a cron job
CREATE TABLE cron_job_run_status (
  cron_job_run_status_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO cron_job_run_status VALUES ('UNDEFINED', 'Undefined'); -- default status when record is created
INSERT INTO cron_job_run_status VALUES ('SUCCEEDED', 'Succeeded');
INSERT INTO cron_job_run_status VALUES ('FAILED', 'Failed');

CREATE TABLE cron_job (
  cron_job_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id TEXT NOT NULL REFERENCES institution,
  cron_expression TEXT NOT NULL, -- standard 5-field or 6-field
  time_zone TEXT NOT NULL, -- IANA TZ (e.g. 'America/New_York'), generally the same as the owning institution
  callback_type TEXT NOT NULL, -- e.g. 'SEND_EMAIL', 'RUN_SQL', etc.
  callback_payload JSONB, -- params for the job run
  next_run_at TIMESTAMPTZ,
  last_run_started_at TIMESTAMPTZ,
  last_run_finished_at TIMESTAMPTZ,
  last_run_status_id TEXT NOT NULL REFERENCES cron_job_run_status(cron_job_run_status_id) DEFAULT 'UNDEFINED',
  last_run_stack_trace TEXT,
  enabled BOOLEAN NOT NULL DEFAULT TRUE, -- if false, don't run any jobs
  created TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_updated TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (institution_id, callback_type)
);

CREATE INDEX ON cron_job (next_run_at);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON cron_job FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

COMMIT;