BEGIN;
SELECT _v.register_patch('209-support-banner', NULL, NULL);

CREATE TABLE display_type (
  display_type_id VARCHAR NOT NULL PRIMARY KEY,
  description VARCHAR NOT NULL
);

INSERT INTO display_type (display_type_id, description) VALUES
  ('PRIMARY', 'Primary'),
  ('SECONDARY', 'Secondary'),
  ('SUCCESS', 'Success'),
  ('DANGER', 'Danger'),
  ('WARNING', 'Warning'),
  ('INFO', 'Info'),
  ('DARK', 'Dark'),
  ('LIGHT', 'Light');

ALTER TABLE institution_feature ADD COLUMN banner_message VARCHAR; -- Can include HTML
ALTER TABLE institution_feature ADD COLUMN banner_message_display_type_id VARCHAR REFERENCES display_type(display_type_id);

CREATE OR REPLACE FUNCTION check_banner_message_consistency()
RETURNS TRIGGER AS $$
BEGIN
  IF (NEW.banner_message IS NULL AND NEW.banner_message_display_type_id IS NOT NULL) OR
     (NEW.banner_message IS NOT NULL AND NEW.banner_message_display_type_id IS NULL) THEN
    RAISE EXCEPTION 'Both banner_message and banner_message_display_type_id must be either present or null together.';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_banner_message_consistency
BEFORE INSERT OR UPDATE ON institution_feature
FOR EACH ROW
EXECUTE FUNCTION check_banner_message_consistency();

COMMIT;