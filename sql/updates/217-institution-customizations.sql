BEGIN;
SELECT _v.register_patch('217-institution-customizations', NULL, NULL);

-- Correct an earlier modeling miss: videos should know which institution owns them
ALTER TABLE video ADD COLUMN institution_id TEXT NOT NULL REFERENCES institution DEFAULT 'COBALT';

-- Add nullable customization hooks for institutions to support custom branding/blurbs/etc.

-- Top-left nav header logo
ALTER TABLE institution ADD COLUMN header_logo_url TEXT;

-- Footer logo, right above "Powered by Cobalt Innovations, Inc."
ALTER TABLE institution ADD COLUMN footer_logo_url TEXT;

-- Copy/image for when patients first enter the site.  Currently ignored for IC institutions.
ALTER TABLE institution ADD COLUMN hero_title TEXT;
ALTER TABLE institution ADD COLUMN hero_description TEXT;
ALTER TABLE institution ADD COLUMN hero_image_url TEXT;

-- "Sign in" screen logo at top left of screen
ALTER TABLE institution ADD COLUMN sign_in_logo_url TEXT;

-- Large "Sign in" screen logo on right side of screen
ALTER TABLE institution ADD COLUMN sign_in_large_logo_url TEXT;

-- Large "Sign in" screen logo background image, sits underneath the large logo
ALTER TABLE institution ADD COLUMN sign_in_large_logo_background_url TEXT;

-- Additional "Sign in" screen branding logo shown over "Welcome to Cobalt" title text, e.g. if customer is whitelabeling
ALTER TABLE institution ADD COLUMN sign_in_branding_logo_url TEXT;

-- e.g. "Welcome to Cobalt"
ALTER TABLE institution ADD COLUMN sign_in_title TEXT;

-- e.g. "Cobalt is a mental health and wellness platform created for [Institution name] faculty and staff"
ALTER TABLE institution ADD COLUMN sign_in_description TEXT;

-- e.g. "Select your sign in method to continue." or "Click 'Sign In With MyChart' below, then enter your details to sign in."
ALTER TABLE institution ADD COLUMN sign_in_direction TEXT;

-- Whether the "Crisis support" button on the sign-in screen is visible
ALTER TABLE institution ADD COLUMN sign_in_crisis_button_visible BOOLEAN NOT NULL DEFAULT FALSE;

-- Whether the "If you are in crisis" box on the sign-in screen is visible
ALTER TABLE institution ADD COLUMN sign_in_crisis_section_visible BOOLEAN NOT NULL DEFAULT FALSE;

-- If there is a marketing video to be shown on the sign-in screen
ALTER TABLE institution ADD COLUMN sign_in_video_id UUID REFERENCES video(video_id);

-- Copy to show on the "play video" button
-- e.g. "Watch our video"
ALTER TABLE institution ADD COLUMN sign_in_video_cta TEXT;

-- If sign_in_privacy_overview is present, then show the "About your privacy" box on the sign-in screen.
-- If sign_in_privacy_detail is present, then show the "Learn more about your privacy" link in the "privacy" box.
-- Both can include HTML.
ALTER TABLE institution ADD COLUMN sign_in_privacy_overview TEXT;
ALTER TABLE institution ADD COLUMN sign_in_privacy_detail TEXT;

-- Should we show the "personalized quote" area of the homepage (bubble with headshot and blurb)?
ALTER TABLE institution ADD COLUMN sign_in_quote_visible BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institution ADD COLUMN sign_in_quote_title TEXT; -- e.g. "Welcome to Cobalt"
ALTER TABLE institution ADD COLUMN sign_in_quote_blurb TEXT; -- e.g. "Hi! I'm Dr. Example Person, the Director of Cobalt. I am a Clinical Psychologist and Clinical Assistant...".  Can include HTML
ALTER TABLE institution ADD COLUMN sign_in_quote_detail TEXT; -- e.g. the rest of the blurb above, if applicable.  Can include HTML.  If this is non-null, a "Read More" link should be shown and this copy is rendered in a modal.

-- Add a new "Resource" site location, which can will be included in the Browse Resources navigation under the other resource content
INSERT INTO site_location
VALUES ('RESOURCE', 'Resource', '');

-- e.g. "ICON_WELL_BEING", needs to match FE's enum definition.
-- Useful for "Resource" site location pages, where they normally have an icon next to them
ALTER TABLE page_site_location ADD COLUMN icon_name TEXT;

-- To support "My Events" as a feature, need to create a new navigation header
INSERT INTO navigation_header (navigation_header_id, name)
VALUES ('MY_PROFILE', 'My Profile');

-- Now support "My Events" as a feature
INSERT INTO feature (feature_id, navigation_header_id, name, url_name)
VALUES ('MY_EVENTS', 'MY_PROFILE', 'My Events', '/my-calendar');

-- Enable "My Events" for all non-IC institutions by default
INSERT INTO institution_feature (institution_id, feature_id, description, display_order, nav_visible, landing_page_visible)
SELECT institution_id, 'MY_EVENTS', 'Your booked appointments, group session seats, and more will be available here.', 1, TRUE, FALSE
FROM institution
WHERE integrated_care_enabled=FALSE;

COMMIT;

