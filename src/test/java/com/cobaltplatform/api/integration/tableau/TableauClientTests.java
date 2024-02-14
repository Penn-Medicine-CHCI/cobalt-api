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

package com.cobaltplatform.api.integration.tableau;

import com.cobaltplatform.api.integration.tableau.request.AccessTokenRequest;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.soklet.util.LoggingUtils.initializeLogback;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class TableauClientTests {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void acquireAccessToken() throws Exception {
		String apiBaseUrl = Files.readString(Path.of("resources/test/tableau-api-base-url"), StandardCharsets.UTF_8);
		String contentUrl = Files.readString(Path.of("resources/test/tableau-content-url"), StandardCharsets.UTF_8);
		String emailAddress = Files.readString(Path.of("resources/test/tableau-email-address"), StandardCharsets.UTF_8);
		String serializedDirectTrustCredential = Files.readString(Path.of("resources/test/tableau-direct-trust-credentials.json"), StandardCharsets.UTF_8);

		TableauClient tableauClient = new DefaultTableauClient(apiBaseUrl, TableauDirectTrustCredential.deserialize(serializedDirectTrustCredential));

		String jwt = tableauClient.generateDirectTrustJwt(new AccessTokenRequest.Builder(emailAddress)
				.scopes(List.of("tableau:views:embed"))
				.claims(Map.of("https://tableau.com/oda", "true"))
				.build());

		System.out.println(jwt);

		String accessToken = tableauClient.authenticateUsingDirectTrustJwt(jwt, contentUrl);

		System.out.println(accessToken);
	}
}
