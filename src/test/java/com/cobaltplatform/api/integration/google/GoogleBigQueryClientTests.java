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

package com.cobaltplatform.api.integration.google;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.soklet.util.LoggingUtils.initializeLogback;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class GoogleBigQueryClientTests {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void testGoogleBigQuery() throws Exception {
		String serviceAccountPrivateKeyJson = Files.readString(
				Path.of("resources/test/bigquery-service-account-private-key.json"), StandardCharsets.UTF_8);

		GoogleBigQueryClient googleBigQueryClient = new DefaultGoogleBigQueryClient(serviceAccountPrivateKeyJson);
		googleBigQueryClient.test();
	}
}
