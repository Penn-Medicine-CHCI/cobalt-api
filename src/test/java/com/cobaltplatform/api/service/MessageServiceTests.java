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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.messaging.Message;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.CreateScheduledMessageRequest;
import com.cobaltplatform.api.model.db.ScheduledMessage;
import com.google.gson.Gson;
import org.junit.Test;
import org.testng.Assert;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class MessageServiceTests {
	@Test
	public void createScheduledMessage() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			MessageService messageService = app.getInjector().getInstance(MessageService.class);

			Message message = new EmailMessage.Builder(EmailMessageTemplate.ACCOUNT_VERIFICATION, Locale.US)
					.toAddresses(List.of("fake@example.com"))
					.fromAddress("alsofake@example.com")
					.messageContext(new HashMap<String, Object>() {{
						put("number", 1);
						put("string", "2");
					}})
					.build();

			ZoneId timeZone = ZoneId.systemDefault();
			LocalDateTime scheduledAt = Instant.now()
					.plus(5, ChronoUnit.MINUTES)
					.atZone(timeZone)
					.toLocalDateTime();

			Map<String, Object> metadata = new HashMap<>() {{
				put("exampleId", UUID.randomUUID());
			}};

			UUID scheduledMessageId = messageService.createScheduledMessage(new CreateScheduledMessageRequest<>() {{
				setMessage(message);
				setScheduledAt(scheduledAt);
				setTimeZone(timeZone);
				setMetadata(metadata);
			}});

			ScheduledMessage scheduledMessage = messageService.findScheduledMessageById(scheduledMessageId).get();

			Assert.assertEquals(scheduledMessage.getScheduledAt(), scheduledAt, "Schedule date/times differ");
			Assert.assertEquals(scheduledMessage.getTimeZone(), timeZone, "Timezones differ");

			Map<String, Object> metadataFromJson = new Gson().fromJson(scheduledMessage.getMetadata(), Map.class);

			Assert.assertEquals(metadataFromJson.get("exampleId").toString(), metadata.get("exampleId").toString(), "Metadatas differ");

			boolean canceled = messageService.cancelScheduledMessage(scheduledMessageId);

			Assert.assertTrue(canceled, "Message was not successfully canceled");
		});
	}
}
