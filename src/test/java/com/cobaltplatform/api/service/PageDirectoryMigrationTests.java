package com.cobaltplatform.api.service;

import com.cobaltplatform.api.DisposablePostgresTestExecutor;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

public class PageDirectoryMigrationTests {
	@Test
	public void migrationBackfillsCategoriesPreservesHistoryAndInstallsConstraints() throws Exception {
		DisposablePostgresTestExecutor.run(database -> {
			try (Statement statement = database.createStatement()) {
				createLegacySchema(statement);
				// Migration text is executed as input; assertions inspect only database results.
				statement.execute("SELECT _v.register_patch('215-page-group',NULL,NULL)");
				statement.execute(Files.readString(Paths.get("sql/updates/289-page-directory-locations.sql")));
				try (ResultSet result = statement.executeQuery("SELECT page_location_id FROM page ORDER BY page_id")) {
					for (String expected : new String[]{"FEATURED_TOPIC","COMMUNITY","UNCATEGORIZED","UNCATEGORIZED","COMMUNITY","UNCATEGORIZED"}) {
						assertTrue(result.next());
						assertEquals(expected, result.getString(1));
					}
					assertFalse(result.next());
				}
				assertEquals(0, count(statement, "SELECT COUNT(*) FROM page WHERE created<>'2020-01-01' OR last_updated<>'2020-02-01' OR published_date<>'2020-03-01'"));
				assertEquals(7, count(statement, "SELECT COUNT(*) FROM page_site_location"));
				assertEquals(5, count(statement, "SELECT COUNT(*) FROM v_page"));
				assertEquals(4, count(statement, "SELECT COUNT(*) FROM v_page WHERE page_status_description='Published'"));
				assertEquals(1, count(statement, "SELECT COUNT(*) FROM v_page WHERE page_status_description='Copy for Editing'"));
				assertEquals(1, count(statement, "SELECT COUNT(*) FROM v_page WHERE page_location_description='Penn Communities'"));
				assertEquals(5, count(statement, "SELECT COUNT(*) FROM v_page WHERE original_create_date='2020-01-01'"));
				assertEquals(5, count(statement, "SELECT COUNT(*) FROM v_page WHERE has_been_published=TRUE"));
				assertEquals(1, count(statement, "SELECT COUNT(*) FROM _v.patches WHERE patch_name='289-page-directory-locations'"));
				assertThrows(SQLException.class, () -> statement.execute("UPDATE page SET page_location_id='INVALID'"));
				assertThrows(SQLException.class, () -> statement.execute("UPDATE page SET page_location_id=NULL"));
				statement.execute("UPDATE page SET name='Updated' WHERE url_name='page-1'");
				assertEquals(1, count(statement, "SELECT COUNT(*) FROM page WHERE url_name='page-1' AND last_updated>'2020-02-01'"));
				statement.execute("""
						INSERT INTO page (page_id,name,url_name,page_status_id,institution_id,created_by_account_id,created,last_updated,page_group_id)
						VALUES ('00000000-0000-0000-0000-000000000007','New','new','DRAFT','COBALT',
						'00000000-0000-0000-0000-000000000099',NOW(),NOW(),'00000000-0000-0000-0000-000000000098')
						""");
				assertEquals(1, count(statement, "SELECT COUNT(*) FROM v_page WHERE url_name='new' AND page_location_id='UNCATEGORIZED' AND page_status_description='Unpublished' AND has_been_published=FALSE"));
			}
		});
	}

	@Test
	public void migrationBackfillsPublicationEvidenceWithoutChangingTimestamps() throws Exception {
		DisposablePostgresTestExecutor.run(database -> {
			try (Statement statement = database.createStatement()) {
				createLegacySchema(statement);
				statement.execute("SELECT _v.register_patch('215-page-group',NULL,NULL)");
				statement.execute("UPDATE page SET published_date=NULL WHERE url_name='page-2'");
				statement.execute("UPDATE page SET page_status_id='DRAFT',published_date=NULL WHERE url_name IN ('page-3','page-5')");
				statement.execute("UPDATE page SET page_status_id='DRAFT' WHERE url_name='page-4'");
				statement.execute("CREATE TEMP TABLE expected_page_history AS SELECT * FROM page");
				statement.execute(Files.readString(Paths.get("sql/updates/289-page-directory-locations.sql")));
				try (ResultSet result = statement.executeQuery("SELECT has_been_published FROM page ORDER BY page_id")) {
					for (boolean expected : new boolean[]{true,true,false,true,false,true}) {
						assertTrue(result.next());
						assertEquals(expected, result.getBoolean(1));
					}
					assertFalse(result.next());
				}
				assertEquals(0, count(statement, """
						SELECT COUNT(*) FROM page p JOIN expected_page_history e USING (page_id)
						WHERE p.created IS DISTINCT FROM e.created OR p.last_updated IS DISTINCT FROM e.last_updated
						OR p.published_date IS DISTINCT FROM e.published_date
						"""));
				assertEquals(4, count(statement, "SELECT COUNT(*) FROM v_page WHERE has_been_published=TRUE"));
				assertEquals(7, count(statement, "SELECT COUNT(*) FROM page_site_location"));
				assertEquals(1, count(statement, "SELECT COUNT(*) FROM _v.patches WHERE patch_name='289-page-directory-locations'"));
				assertThrows(SQLException.class, () -> statement.execute("UPDATE page SET has_been_published=NULL"));
				statement.execute("""
						INSERT INTO page (page_id,name,url_name,page_status_id,institution_id,created_by_account_id,created,last_updated,page_group_id)
						VALUES ('00000000-0000-0000-0000-000000000007','New','new','DRAFT','COBALT',
						'00000000-0000-0000-0000-000000000099',NOW(),NOW(),'00000000-0000-0000-0000-000000000098')
						""");
				assertEquals(1, count(statement, "SELECT COUNT(*) FROM v_page WHERE url_name='new' AND has_been_published=FALSE"));
				statement.execute("UPDATE page SET last_updated='2000-01-01' WHERE url_name='new'");
				assertEquals(1, count(statement, "SELECT COUNT(*) FROM page WHERE url_name='new' AND last_updated>'2000-01-01'"));
			}
		});
	}

	private static void createLegacySchema(Statement statement) throws SQLException {
		statement.execute("""
				CREATE TABLE page_status (page_status_id TEXT PRIMARY KEY, description TEXT NOT NULL);
				INSERT INTO page_status VALUES ('DRAFT','Draft'),('LIVE','Live'),('COPY_FOR_EDITING','Copy for Editing');
				CREATE TABLE file_upload (file_upload_id UUID PRIMARY KEY, url TEXT NOT NULL);
				CREATE TABLE page (
				  page_id UUID PRIMARY KEY, name TEXT NOT NULL, url_name TEXT NOT NULL,
				  page_status_id TEXT NOT NULL REFERENCES page_status, headline TEXT, description TEXT,
				  image_file_upload_id UUID REFERENCES file_upload, image_alt_text TEXT, published_date TIMESTAMPTZ,
				  deleted_flag BOOLEAN NOT NULL DEFAULT FALSE, institution_id TEXT NOT NULL,
				  parent_page_id UUID, created_by_account_id UUID NOT NULL, created TIMESTAMPTZ NOT NULL,
				  last_updated TIMESTAMPTZ NOT NULL, page_group_id UUID NOT NULL
				);
				CREATE FUNCTION set_last_updated() RETURNS TRIGGER LANGUAGE plpgsql
				AS $$ BEGIN NEW.last_updated = NOW(); RETURN NEW; END; $$;
				CREATE TABLE page_site_location (page_id UUID REFERENCES page, site_location_id TEXT, display_order SMALLINT);
				CREATE VIEW v_page AS
				SELECT p.*, MIN(p.created) OVER (PARTITION BY p.page_group_id) AS original_create_date, fu.url AS image_url
				FROM page p LEFT JOIN file_upload fu ON p.image_file_upload_id=fu.file_upload_id
				WHERE p.deleted_flag=FALSE;
				INSERT INTO page
				SELECT ('00000000-0000-0000-0000-' || LPAD(n::TEXT,12,'0'))::UUID,
				       'Page ' || n, 'page-' || n, CASE WHEN n=6 THEN 'COPY_FOR_EDITING' ELSE 'LIVE' END,
				       'Headline','Description',NULL,NULL,'2020-03-01',n=5,'COBALT',NULL,
				       '00000000-0000-0000-0000-000000000099','2020-01-01','2020-02-01',
				       '00000000-0000-0000-0000-000000000098'
				FROM generate_series(1,6) n;
				INSERT INTO page_site_location VALUES
				('00000000-0000-0000-0000-000000000001','FEATURED_TOPIC',1),
				('00000000-0000-0000-0000-000000000001','RESOURCE',2),
				('00000000-0000-0000-0000-000000000002','COMMUNITY',1),
				('00000000-0000-0000-0000-000000000003','FEATURED_TOPIC',1),
				('00000000-0000-0000-0000-000000000003','COMMUNITY',2),
				('00000000-0000-0000-0000-000000000004','RESOURCE',1),
				('00000000-0000-0000-0000-000000000005','COMMUNITY',1);
				CREATE TRIGGER set_last_updated BEFORE INSERT OR UPDATE ON page
				FOR EACH ROW EXECUTE FUNCTION set_last_updated();
				""");
	}

	private static int count(Statement statement, String query) throws SQLException {
		try (ResultSet result = statement.executeQuery(query)) {
			result.next();
			return result.getInt(1);
		}
	}
}
