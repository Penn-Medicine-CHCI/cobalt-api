/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Originally created at the University of Pennsylvania and Penn Medicine by:
 * Dr. David Asch; Dr. Lisa Bellini; Dr. Cecilia Livesey; Kelley Kugler; and Dr. Matthew Press.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cobaltplatform.api.messaging.email;

import com.cobaltplatform.api.DisposablePostgresTestExecutor;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.*;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class V2EmailBrandingSqlContractTests {
	@Nonnull
	private static final String PLATFORM_EMAIL_IMAGE_URL = "https://cdn-prod.cobalt.care/logos/behavior-bridge-email-logo.png";

	@Test
	public void migrationScopesBehaviorBridgeBrandingData() throws Exception {
		DisposablePostgresTestExecutor.run(database -> {
			try (Statement statement = database.createStatement()) {
				createFixture(statement);
				statement.execute("INSERT INTO institution_color_value VALUES ('COBALT_COURSES','N50','#existing')");
				statement.execute(Files.readString(Paths.get("sql/updates/260-v2-account-email-templates.sql")));
				assertEquals("Behavior Bridge", value(statement, "SELECT platform_name FROM institution WHERE institution_id='COBALT_COURSES'"));
				assertEquals("Existing production name", value(statement, "SELECT platform_name FROM institution WHERE institution_id='BEHAVIOR_BRIDGE'"));
				for (String institution : new String[]{"COBALT_COURSES","BEHAVIOR_BRIDGE"})
					assertEquals(PLATFORM_EMAIL_IMAGE_URL, value(statement, "SELECT platform_email_image_url FROM institution WHERE institution_id='" + institution + "'"));
				assertNull(value(statement, "SELECT platform_email_image_url FROM institution WHERE institution_id='COBALT'"));
				assertNull(value(statement, "SELECT email_footer_text FROM institution WHERE institution_id='COBALT_COURSES'"));
				assertEquals("#existing", color(statement, "N50"));
				assertEquals("#2D3030", color(statement, "N900"));
				assertEquals("#2F7F61", color(statement, "P500"));
				assertEquals("0", value(statement, "SELECT COUNT(*) FROM institution_color_value WHERE institution_id<>'COBALT_COURSES'"));
			}
		});
	}

	@Test
	public void postBootstrapOnlySeedsLocalCoursesInstitution() throws Exception {
		DisposablePostgresTestExecutor.run(database -> {
			try (Statement statement = database.createStatement()) {
				createFixture(statement);
				statement.execute("ALTER TABLE institution ADD COLUMN platform_email_image_url TEXT");
				statement.execute(Files.readString(Paths.get("sql/initial/post-bootstrap.sql")));
				assertEquals("Behavior Bridge", value(statement, "SELECT platform_name FROM institution WHERE institution_id='COBALT_COURSES'"));
				assertEquals(PLATFORM_EMAIL_IMAGE_URL, value(statement, "SELECT platform_email_image_url FROM institution WHERE institution_id='COBALT_COURSES'"));
				assertEquals("Existing production name", value(statement, "SELECT platform_name FROM institution WHERE institution_id='BEHAVIOR_BRIDGE'"));
				assertNull(value(statement, "SELECT platform_email_image_url FROM institution WHERE institution_id='BEHAVIOR_BRIDGE'"));
				assertEquals("Cobalt", value(statement, "SELECT platform_name FROM institution WHERE institution_id='COBALT'"));
				assertEquals("#F7F8F7", color(statement, "N50"));
				assertEquals("#2D3030", color(statement, "N900"));
				assertEquals("#2F7F61", color(statement, "P500"));
				assertEquals("0", value(statement, "SELECT COUNT(*) FROM institution_color_value WHERE institution_id<>'COBALT_COURSES'"));
			}
		});
	}

	private static void createFixture(Statement statement) throws Exception {
		statement.execute("""
				CREATE TABLE institution (institution_id TEXT PRIMARY KEY, platform_name TEXT);
				INSERT INTO institution VALUES ('COBALT_COURSES','Courses'),('BEHAVIOR_BRIDGE','Existing production name'),('COBALT','Cobalt');
				CREATE TABLE institution_color_value (
				  institution_id TEXT REFERENCES institution, color_value_id TEXT, css_representation TEXT,
				  PRIMARY KEY (institution_id,color_value_id)
				);
				""");
	}

	private static String color(Statement statement, String colorId) throws Exception {
		return value(statement, "SELECT css_representation FROM institution_color_value WHERE institution_id='COBALT_COURSES' AND color_value_id='" + colorId + "'");
	}

	private static String value(Statement statement, String query) throws Exception {
		try (ResultSet result = statement.executeQuery(query)) {
			assertTrue(result.next());
			return result.getString(1);
		}
	}
}
