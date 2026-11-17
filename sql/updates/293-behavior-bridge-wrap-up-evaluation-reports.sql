BEGIN;
SELECT _v.register_patch(
  '293-behavior-bridge-wrap-up-evaluation-reports',
  NULL,
  NULL
);

-- Deploy the API first so every instance recognizes both report type enum values.
INSERT INTO report_type (report_type_id, description, display_order)
VALUES
  ('COURSE_PARENTING_TEENS_WRAP_UP', 'Analytics - Parenting Teens Wrap Up', 123),
  ('COURSE_UNDERSTANDING_TRAUMA_WRAP_UP', 'Analytics - Understanding Trauma Wrap Up', 124),
  ('COURSE_BUILDING_HEALTHY_SLEEP_WRAP_UP', 'Analytics - Building Healthy Sleep Wrap Up', 125);

COMMIT;
