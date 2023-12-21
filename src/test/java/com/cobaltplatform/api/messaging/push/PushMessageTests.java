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

package com.cobaltplatform.api.messaging.push;

import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PushTokenType;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

import static com.soklet.util.LoggingUtils.initializeLogback;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PushMessageTests {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void testGoogleFcmMessageSending() throws Exception {
		String pushToken = Files.readString(Path.of("resources/test/fcm-push-token"), StandardCharsets.UTF_8).trim();

		PushMessage pushMessage = new PushMessage.Builder(InstitutionId.COBALT, PushMessageTemplate.MICROINTERVENTION,
				ClientDeviceTypeId.IOS_APP, PushTokenType.PushTokenTypeId.FCM, pushToken, Locale.forLanguageTag("en-US"))
				.messageContext(Map.of("one", "two"))
				.metadata(Map.of("three", "four"))
				.build();

		String secretKeyJson = Files.readString(Path.of("resources/test/fcm-secret-key.json"), StandardCharsets.UTF_8);
		MessageSender<PushMessage> pushMessageSender = new GoogleFcmPushMessageSender(secretKeyJson);
		String identifier = pushMessageSender.sendMessage(pushMessage);
	}
}
