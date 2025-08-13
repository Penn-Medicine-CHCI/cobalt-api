BEGIN;
SELECT _v.register_patch('229-courses-people-puzzle', NULL, NULL);

INSERT INTO screening_image
(screening_image_id, description)
VALUES
('PEOPLE_PUZZLE', 'People Puzzle');

END;