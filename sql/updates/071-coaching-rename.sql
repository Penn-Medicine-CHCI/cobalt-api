BEGIN;
SELECT _v.register_patch('071-coaching-rename', NULL, NULL);

UPDATE support_role SET description = 'Wellness Coaching' WHERE support_role_id='COACH';
UPDATE feature SET name='Wellness Coaching' WHERE feature_id='COACHING';
UPDATE institution_feature SET description='If you’d like to connect with an emotional wellness coach, Coping First Aid is a free resource for you. Flexible appointments are available now with lay health coaches supervised by licensed mental health professionals. You can receive one-to-one support with a trained coach to work with you on coping and resilience strategies' WHERE feature_id='COACHING';

COMMIT;