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

package com.cobaltplatform.api.integration.ipstack;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class IpstackClientTests {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void testStandardLookup() {
		IpstackClient ipstackClient = acquireIpstackClient();

		IpstackStandardLookupRequest request = IpstackStandardLookupRequest.withIpAddress("134.201.250.155")
				.hostname(true)
				.security(false)
				.language("en")
				.build();

		IpstackStandardLookupResponse response = ipstackClient.performStandardLookup(request);

		Assert.assertNotNull(response);
	}

	@Nonnull
	protected IpstackClient acquireIpstackClient() {
		final String IPSTACK_ACCESS_TOKEN_FILENAME = "resources/test/ipstack-access-token";

		try {
			Path ipstackAccessTokenFile = Path.of(IPSTACK_ACCESS_TOKEN_FILENAME);

			if (!Files.exists(ipstackAccessTokenFile) || Files.isDirectory(ipstackAccessTokenFile))
				throw new IOException(format("Please ensure %s exists", ipstackAccessTokenFile.toAbsolutePath()));

			String ipstackAccessToken = Files.readString(ipstackAccessTokenFile, StandardCharsets.UTF_8);

			return new DefaultIpstackClient(ipstackAccessToken);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

