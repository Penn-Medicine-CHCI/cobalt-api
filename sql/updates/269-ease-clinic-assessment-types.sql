BEGIN;
SELECT _v.register_patch('269-ease-clinic-assessment-types', NULL, NULL);

-- PROMIS Satisfaction with Participation in Social Roles 8a, version 1.0.
-- Keep the version in the identifier because PROMIS v1.0 and v2.0 scores are
-- not comparable.
INSERT INTO screening_type (
  screening_type_id,
  description,
  overall_score_maximum
) VALUES (
  'PROMIS_PARTICIPATION_SOCIAL_ROLES_8A_V1',
  'PROMIS Satisfaction with Participation in Social Roles 8a v1.0',
  40
)
ON CONFLICT (screening_type_id) DO UPDATE
SET description=EXCLUDED.description,
    overall_score_maximum=EXCLUDED.overall_score_maximum;

INSERT INTO flowsheet_type (flowsheet_type_id, description) VALUES
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_1', 'PROMIS Satisfaction with Participation in Social Roles 8a v1.0 Question 1'),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_2', 'PROMIS Satisfaction with Participation in Social Roles 8a v1.0 Question 2'),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_3', 'PROMIS Satisfaction with Participation in Social Roles 8a v1.0 Question 3'),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_4', 'PROMIS Satisfaction with Participation in Social Roles 8a v1.0 Question 4'),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_5', 'PROMIS Satisfaction with Participation in Social Roles 8a v1.0 Question 5'),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_6', 'PROMIS Satisfaction with Participation in Social Roles 8a v1.0 Question 6'),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_7', 'PROMIS Satisfaction with Participation in Social Roles 8a v1.0 Question 7'),
  ('PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_8', 'PROMIS Satisfaction with Participation in Social Roles 8a v1.0 Question 8')
ON CONFLICT (flowsheet_type_id) DO UPDATE
SET description=EXCLUDED.description;

COMMIT;
