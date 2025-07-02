BEGIN;
SELECT _v.register_patch('222-anonymous-implicit-controls', NULL, NULL);

-- If provided, only create anonymous implicit accounts for URLs that match this regex.
-- Example: '\butm_source=testing\b' would match any URL with a `utm_source=testing` query parameter
ALTER TABLE institution ADD COLUMN anonymous_implicit_url_path_regex TEXT;

-- If provided, any anonymous implicit access tokens before this timestamp are considered invalid
ALTER TABLE institution ADD COLUMN anonymous_implicit_access_tokens_valid_after TIMESTAMPTZ;

END;