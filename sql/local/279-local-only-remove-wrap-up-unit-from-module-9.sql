BEGIN;
SELECT _v.register_patch(
  '279-local-only-remove-wrap-up-unit-from-module-9',
  ARRAY['278-local-only-cobalt-care-navigator-slots'],
  NULL
);

-- Remove the local Managing Challenging Behaviors Wrap Up unit from module 9. Unit-linked course
-- session status and associations must be removed first because their foreign
-- keys do not cascade. The screening flow itself remains intact for reuse by
-- the Keep Learning with Behavior Bridge unit.
DO $remove_wrap_up_unit$
DECLARE
  v_course_unit_id UUID;
  v_course_unit_title TEXT;
  v_screening_flow_id UUID;
  v_updated_klwbb_count INTEGER;
  v_deleted_session_status_count BIGINT;
BEGIN
  BEGIN
    SELECT course_unit.course_unit_id, course_unit.title, course_unit.screening_flow_id
    INTO STRICT v_course_unit_id, v_course_unit_title, v_screening_flow_id
    FROM course
    JOIN course_module
      ON course_module.course_id=course.course_id
    JOIN course_unit
      ON course_unit.course_module_id=course_module.course_module_id
    WHERE course.title='Parenting Education for Managing Challenging Behaviors'
      AND course_module.display_order=9
      AND REGEXP_REPLACE(LOWER(course_unit.title), '[^a-z0-9]+', ' ', 'g') LIKE '%wrap up%'
      AND EXISTS (
        SELECT 1
        FROM institution_course
        WHERE institution_course.course_id=course.course_id
          AND institution_course.institution_id='COBALT_COURSES'
      );
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RAISE EXCEPTION 'No local Managing Challenging Behaviors Wrap Up unit was found in module 9.';
    WHEN TOO_MANY_ROWS THEN
      RAISE EXCEPTION 'More than one local Wrap Up unit matched module 9; no rows were removed.';
  END;

  IF v_screening_flow_id IS NULL THEN
    RAISE EXCEPTION 'Wrap Up unit % (%) has no screening flow to move to KLWBB.',
      v_course_unit_title, v_course_unit_id;
  END IF;

  UPDATE course_unit AS keep_learning
  SET screening_flow_id=v_screening_flow_id
  FROM course_unit AS wrap_up
  JOIN course_module AS wrap_up_module
    ON wrap_up_module.course_module_id=wrap_up.course_module_id
  JOIN course_module AS keep_learning_module
    ON keep_learning_module.course_id=wrap_up_module.course_id
  WHERE wrap_up.course_unit_id=v_course_unit_id
    AND keep_learning.course_module_id=keep_learning_module.course_module_id
    AND REGEXP_REPLACE(LOWER(keep_learning.title), '[^a-z0-9]+', ' ', 'g') LIKE '%keep learning with behavior bridge%'
    AND (keep_learning.screening_flow_id IS NULL OR keep_learning.screening_flow_id=v_screening_flow_id);
  GET DIAGNOSTICS v_updated_klwbb_count = ROW_COUNT;

  IF v_updated_klwbb_count <> 1 THEN
    RAISE EXCEPTION 'Expected to move the screening flow to one KLWBB unit, but updated % rows.',
      v_updated_klwbb_count;
  END IF;

  DELETE FROM course_unit_dependency
  WHERE determinant_course_unit_id=v_course_unit_id
     OR dependent_course_unit_id=v_course_unit_id;

  DELETE FROM course_unit_downloadable_file
  WHERE course_unit_id=v_course_unit_id;

  DELETE FROM course_unit_content
  WHERE course_unit_id=v_course_unit_id;

  DELETE FROM course_session_unit
  WHERE course_unit_id=v_course_unit_id;
  GET DIAGNOSTICS v_deleted_session_status_count = ROW_COUNT;

  DELETE FROM course_unit
  WHERE course_unit_id=v_course_unit_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Wrap Up unit % (%) was not deleted.', v_course_unit_title, v_course_unit_id;
  END IF;

  RAISE NOTICE 'Removed Wrap Up unit % (%); deleted % course-session unit status rows.',
    v_course_unit_title, v_course_unit_id, v_deleted_session_status_count;
END
$remove_wrap_up_unit$;

COMMIT;
