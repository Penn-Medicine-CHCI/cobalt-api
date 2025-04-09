BEGIN;
SELECT _v.register_patch('208-ic-institution-overrides', NULL, NULL);

-- Introduce support for the concept of business hours, which enables fine-grained "opened/closed" calculations.
-- For example, provider availability might be capped to anything at least 48 business hours out,
-- which means taking into account weekends, holidays, and special exceptions when performing the calculation

CREATE TYPE DAY_OF_WEEK AS ENUM ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY');

-- Currently business hours are institution-specific, but in the future we might have department/clinic-specific business hours
CREATE TABLE business_hour (
  business_hour_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
  institution_id TEXT NOT NULL REFERENCES institution(institution_id),
  day_of_week DAY_OF_WEEK NOT NULL,
  open_time TIME NOT NULL,
  close_time TIME NOT NULL
);

CREATE OR REPLACE FUNCTION validate_business_hours()
RETURNS trigger AS
$$
BEGIN
    -- Check if open_time is not before close_time.
    IF NEW.open_time >= NEW.close_time THEN
        RAISE EXCEPTION 'Invalid business hour: open_time (%) must be before close_time (%).',
                        NEW.open_time, NEW.close_time;
    END IF;
    RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_business_hours
BEFORE INSERT OR UPDATE ON business_hour
FOR EACH ROW
EXECUTE FUNCTION validate_business_hours();

-- Permit overrides for business hours - for example, closing early on Christmas Eve or an unexpected "we are totally closed today".
CREATE TABLE business_hour_override (
  business_hour_override_id UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
  business_hour_id UUID NOT NULL REFERENCES business_hour(business_hour_id),
  date DATE NOT NULL,
  open_time TIME, -- Use NULL to indicate that we never open
  close_time TIME, -- Must use NULL if override_open_time is NULL (enforced by trigger below)
  description VARCHAR(255),
  UNIQUE (business_hour_id, date) -- Can't have multiple overrides for the dame date
);

-- Verify that open/close overrides are legal
CREATE FUNCTION validate_business_hour_override()
RETURNS trigger AS
$$
BEGIN
    -- Ensure that either both open_time and close_time are NULL or both are not null.
    IF (NEW.open_time IS NULL AND NEW.close_time IS NOT NULL)
       OR (NEW.open_time IS NOT NULL AND NEW.close_time IS NULL) THEN
        RAISE EXCEPTION 'Both open_time and close_time must be either NULL (closed) or non-null (open).';
    END IF;

    -- If both are non-null, ensure that the open time is before the close time.
    IF NEW.open_time IS NOT NULL AND NEW.close_time IS NOT NULL THEN
        IF NEW.open_time >= NEW.close_time THEN
            RAISE EXCEPTION 'open_time (%) must be before close_time (%).',
                NEW.open_time, NEW.close_time;
        END IF;
    END IF;

    RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_business_hour_override
BEFORE INSERT OR UPDATE ON business_hour_override
FOR EACH ROW
EXECUTE FUNCTION validate_business_hour_override();

CREATE TABLE holiday (
  holiday_id TEXT PRIMARY KEY,
  description VARCHAR(255) NOT NULL,
  country_code VARCHAR(2) NOT NULL, -- ISO 3166, e.g. 'US'
  display_order INTEGER NOT NULL
);

INSERT INTO holiday (holiday_id, description, country_code, display_order) VALUES
  ('NEW_YEARS_DAY', 'New Year''s Day', 'US', 1), -- January 1
  ('MLK_DAY', 'MLK Day', 'US', 2), -- Third Monday in January
  ('PRESIDENTS_DAY', 'Presidents'' Day', 'US', 3), -- Third Monday in February
  ('MEMORIAL_DAY', 'Memorial Day', 'US', 4), -- Last Monday in May
  ('JUNETEENTH', 'Juneteenth', 'US', 5), -- June 19
  ('INDEPENDENCE_DAY', 'Independence Day', 'US', 6), -- July 4
  ('LABOR_DAY', 'Labor Day', 'US', 7), -- First Monday in September
  ('INDIGENOUS_PEOPLES_DAY', 'Indigenous Peoples'' Day', 'US', 8), -- Second Monday in October
  ('VETERANS_DAY', 'Veterans Day', 'US', 9), -- November 11
  ('THANKSGIVING', 'Thanksgiving', 'US', 10), -- Fourth Thursday in November
  ('CHRISTMAS', 'Christmas', 'US', 11); -- December 25

-- Currently holidays are institution-specific, but in the future we might have department/clinic-specific holidays
CREATE TABLE institution_holiday (
  institution_id TEXT NOT NULL REFERENCES institution(institution_id),
  holiday_id TEXT NOT NULL REFERENCES holiday(holiday_id),
  PRIMARY KEY(institution_id, holiday_id)
);

COMMIT;