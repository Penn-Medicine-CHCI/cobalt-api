BEGIN;
SELECT _v.register_patch('199-pages', NULL, NULL);

CREATE TABLE page_type (
  page_type_id TEXT PRIMARY KEY,
  description TEXT NOT NULL,
  relative_base_url TEXT NOT NULL
);

CREATE TABLE page_status (
  page_status_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

CREATE TABLE background_color (
  background_color_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

CREATE TABLE row_type (
  row_type_id TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

CREATE TABLE page (
    page_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    url_name TEXT NOT NULL,
    page_type_id TEXT NOT NULL REFERENCES page_type,
    page_status_id TEXT NOT NULL REFERENCES page_status,
    headline TEXT NULL,
    description TEXT NULL,
    image_file_upload_id UUID NULL REFERENCES file_upload,
    image_alt_text TEXT NULL,
    published_date timestamptz NULL,
    deleted_flag BOOLEAN NOT NULL DEFAULT false,
    institution_id VARCHAR NOT NULL REFERENCES institution,
    parent_page_id UUID NULL REFERENCES page(page_id),
  	created_by_account_id UUID NOT NULL REFERENCES account(account_id),
  	created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
	last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE page_section (
    page_section_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    page_id UUID NOT NULL REFERENCES page,
    name TEXT NOT NULL,   
    headline TEXT NULL,
    description TEXT NULL,
    background_color_id TEXT NOT NULL REFERENCES background_color,
    deleted_flag BOOLEAN NOT NULL DEFAULT false,
    display_order SMALLINT NOT NULL,
  	created_by_account_id UUID NOT NULL REFERENCES account(account_id),
  	created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
	last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_section FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE page_row (
    page_row_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    page_section_id UUID NOT NULL REFERENCES page_section,
    row_type_id TEXT NOT NULL REFERENCES row_type,
    deleted_flag BOOLEAN NOT NULL DEFAULT false,
  	display_order SMALLINT NOT NULL,
  	created_by_account_id UUID NOT NULL REFERENCES account(account_id),
  	created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
	last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_row FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE page_row_column (
    page_row_column_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    page_row_id UUID NOT NULL REFERENCES page_row,
    headline TEXT NULL,
    description TEXT NULL,
    image_file_upload_id UUID NULL REFERENCES file_upload,
    image_alt_text TEXT NULL,    
    column_display_order SMALLINT NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_row_column FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE page_row_group_session (
    page_row_group_session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    page_row_id UUID NOT NULL REFERENCES page_row,
    group_session_id UUID NOT NULL REFERENCES group_session,
    group_session_display_order SMALLINT NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_page_row_group_session_row_session ON page_row_group_session(page_row_id, group_session_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_row_group_session FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE page_row_content (
    page_row_content_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    page_row_id UUID NOT NULL REFERENCES page_row,
    content_id UUID NOT NULL REFERENCES content,
    content_display_order SMALLINT NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_page_row_content_row_content ON page_row_content(page_row_id, content_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_row_content FOR EACH ROW EXECUTE PROCEDURE set_last_updated();

CREATE TABLE page_row_tag_group (
    page_row_tag_group_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    page_row_id UUID NOT NULL REFERENCES page_row,
    tag_group_id VARCHAR NOT NULL REFERENCES tag_group,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_page_row_tag_group_row_tag_group ON page_row_tag_group(page_row_id, tag_group_id);
CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page_row_tag_group FOR EACH ROW EXECUTE PROCEDURE set_last_updated();


INSERT INTO page_type
VALUES
('TOPIC_CENTER', 'Topic Center', 'topic-center'),
('COMMUNITY', 'Community', 'community');

INSERT INTO page_status
VALUES
('DRAFT', 'Draft'),
('LIVE', 'Live');

INSERT INTO background_color
VALUES
('WHITE', 'White'),
('NEUTRAL', 'NEUTRAL');

INSERT INTO row_type
VALUES
('RESOURCES', 'Resources'),
('GROUP_SESSIONS', 'Group Sessions'),
('TAG_GROUP', 'Tag Group'),
('ONE_COLUMN_IMAGE', 'One Column Image'),
('TWO_COLUMN_IMAGE', 'Two Column Image'),
('THREE_COLUMN_IMAGE', 'Three Column Image');

INSERT INTO file_upload_type
(file_upload_type_id, description)
VALUES
('PAGE_IMAGE', 'Page Iamge');


CREATE VIEW v_page 
AS 
SELECT p.*, fu.url as image_url
FROM page p
LEFT OUTER JOIN file_upload fu ON p.image_file_upload_id=fu.file_upload_id
WHERE p.deleted_flag = false;

CREATE VIEW v_page_section
AS 
SELECT *
FROM page_section
WHERE deleted_flag = false;

CREATE VIEW v_page_row
AS 
SELECT *
FROM page_row
WHERE deleted_flag = false;

CREATE VIEW v_page_row_column
AS 
SELECT pr.*, fu.url as image_url
FROM page_row_column pr
LEFT OUTER JOIN file_upload fu ON pr.image_file_upload_id = fu.file_upload_id;

CREATE OR REPLACE VIEW v_page_row_content
AS 
SELECT pr.*, c.title
FROM page_row_content pr, content c
WHERE pr.content_id = c.content_id;

CREATE VIEW v_page_row_group_session
AS 
SELECT *
FROM page_row_group_session;


CREATE VIEW v_page_row_tag_group
AS 
SELECT prt.page_row_tag_group_id, pr.page_row_id, pr.display_order, pr.page_section_id, pr.row_type_id, 
prt.tag_group_id, pr.created_by_account_id, pr.created, pr.last_updated
FROM page_row_tag_group prt, page_row pr
WHERE prt.page_row_id = pr.page_row_id
AND pr.deleted_flag = FALSE;

INSERT INTO account_capability_type
VALUES
('PAGE_CREATOR', 'Page Creator');

COMMIT;