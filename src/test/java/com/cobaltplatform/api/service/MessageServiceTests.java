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
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ScheduledMessage;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus.ScheduledMessageStatusId;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.gson.Gson;
import com.pyranid.Database;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

			Message message = createMessage();
			ZoneId timeZone = ZoneId.systemDefault();
			LocalDateTime scheduledAt = LocalDateTime.now(timeZone);

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

			Assert.assertEquals("Schedule date/times differ", scheduledAt, scheduledMessage.getScheduledAt());
			Assert.assertEquals("Timezones differ", timeZone, scheduledMessage.getTimeZone());

			Map<String, Object> metadataFromJson = new Gson().fromJson(scheduledMessage.getMetadata(), Map.class);

			Assert.assertEquals(metadataFromJson.get("exampleId").toString(), metadata.get("exampleId").toString(), "Metadatas differ");

			boolean canceled = messageService.cancelScheduledMessage(scheduledMessageId);

			Assert.assertTrue("Message was not successfully canceled", canceled);
		});
	}

	@Test
	public void findScheduledMessagesMatchingMetadata() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			MessageService messageService = app.getInjector().getInstance(MessageService.class);

			ZoneId timeZone = ZoneId.systemDefault();
			LocalDateTime scheduledAt = LocalDateTime.now(timeZone);

			Message message1 = createMessage();
			Map<String, Object> metadata1 = new HashMap<>() {{
				put("exampleId", UUID.randomUUID());
			}};

			messageService.createScheduledMessage(new CreateScheduledMessageRequest<>() {{
				setMessage(message1);
				setScheduledAt(scheduledAt);
				setTimeZone(timeZone);
				setMetadata(metadata1);
			}});

			Message message2 = createMessage();
			Map<String, Object> metadata2 = new HashMap<>() {{
				put("anotherId", UUID.randomUUID());
			}};

			messageService.createScheduledMessage(new CreateScheduledMessageRequest<>() {{
				setMessage(message2);
				setScheduledAt(scheduledAt);
				setTimeZone(timeZone);
				setMetadata(metadata2);
			}});

			List<ScheduledMessage> scheduledMessages = messageService.findScheduledMessagesMatchingMetadata(metadata1);

			Assert.assertTrue("Should have found a single scheduled message", scheduledMessages.size() == 1);

			ScheduledMessage scheduledMessage = scheduledMessages.get(0);
			Map<String, Object> metadataFromJson = new Gson().fromJson(scheduledMessage.getMetadata(), Map.class);

			Assert.assertEquals(metadataFromJson.get("exampleId").toString(), metadata1.get("exampleId").toString(), "Metadatas differ");
		});
	}

	@Test
	public void testScheduledSending() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			MessageService messageService = app.getInjector().getInstance(MessageService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();

			// Schedule it for "now" so it will be sent immediately
			ZoneId timeZone = ZoneId.systemDefault();
			LocalDateTime scheduledAt = LocalDateTime.now(timeZone);

			UUID scheduledMessageId = messageService.createScheduledMessage(new CreateScheduledMessageRequest<>() {{
				setMessage(createMessage());
				setScheduledAt(scheduledAt);
				setTimeZone(timeZone);
			}});

			// Force a commit here so the scheduled message sender task will be able to see it
			database.execute("COMMIT");

			// Wait for the scheduled message sender to notice the message and send it
			Thread.sleep(3000L);

			ScheduledMessage scheduledMessage = messageService.findScheduledMessageById(scheduledMessageId).get();

			Assert.assertEquals("Scheduled message was not successfully processed",
					ScheduledMessageStatusId.PROCESSED, scheduledMessage.getScheduledMessageStatusId());
		});
	}

	@Test
	public void testScheduledSendingTimeZones() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			MessageService messageService = app.getInjector().getInstance(MessageService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();

			// Schedule it for "now" so it will be sent immediately
			ZoneId timeZone = ZoneId.of("America/New_York");
			LocalDateTime scheduledAt = LocalDateTime.now(timeZone);

			UUID scheduledMessageId = messageService.createScheduledMessage(new CreateScheduledMessageRequest<>() {{
				setMessage(createMessage());
				setScheduledAt(scheduledAt);
				setTimeZone(timeZone);
			}});

			// Force a commit here so the scheduled message sender task will be able to see it
			database.execute("COMMIT");

			// Wait for the scheduled message sender to notice the message and send it
			Thread.sleep(3000L);

			ScheduledMessage scheduledMessage = messageService.findScheduledMessageById(scheduledMessageId).get();

			Assert.assertEquals("Scheduled message was not successfully processed",
					ScheduledMessageStatusId.PROCESSED, scheduledMessage.getScheduledMessageStatusId());
		});
	}

	@Test
	public void testScheduledSendingErrorHandling() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			MessageService messageService = app.getInjector().getInstance(MessageService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();

			// Schedule it for "now" so it will be sent immediately
			ZoneId timeZone = ZoneId.systemDefault();
			LocalDateTime scheduledAt = LocalDateTime.now(timeZone);

			UUID scheduledMessageId = messageService.createScheduledMessage(new CreateScheduledMessageRequest<>() {{
				setMessage(createMessage());
				setScheduledAt(scheduledAt);
				setTimeZone(timeZone);
			}});

			// Put junk into the serialized message field which will cause the scheduled send to fail
			database.execute("UPDATE scheduled_message SET serialized_message='{\"junk\": true}'::jsonb WHERE scheduled_message_id=?", scheduledMessageId);

			// Force a commit here so the scheduled message sender task will be able to see it
			database.execute("COMMIT");

			// Wait for the scheduled message sender to notice the message and send it
			Thread.sleep(3000L);

			ScheduledMessage scheduledMessage = messageService.findScheduledMessageById(scheduledMessageId).get();

			Assert.assertEquals("Scheduled message was not successfully transitioned to ERROR",
					ScheduledMessageStatusId.ERROR, scheduledMessage.getScheduledMessageStatusId());
		});
	}

	@Nonnull
	protected Message createMessage() {
		return new EmailMessage.Builder(InstitutionId.COBALT, EmailMessageTemplate.ACCOUNT_VERIFICATION, Locale.US)
				.toAddresses(List.of("fake@example.com"))
				.fromAddress("alsofake@example.com")
				.messageContext(new HashMap<String, Object>() {{
					put("number", 1);
					put("string", "2");
				}})
				.build();
	}
}
