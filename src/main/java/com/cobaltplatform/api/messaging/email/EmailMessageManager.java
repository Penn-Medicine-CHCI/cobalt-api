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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.util.AmazonSqsManager;
import com.cobaltplatform.api.util.Formatter;
import com.pyranid.Database;
import com.cobaltplatform.api.messaging.MessageManager;
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.messaging.MessageSerializer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import java.util.function.Function;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class EmailMessageManager extends MessageManager<EmailMessage> {
	public EmailMessageManager(@Nonnull Provider<AccountService> accountServiceProvider,
														 @Nonnull Database database,
														 @Nonnull Configuration configuration,
														 @Nonnull Formatter formatter,
														 @Nonnull MessageSender<EmailMessage> messageSender,
														 @Nonnull MessageSerializer<EmailMessage> messageSerializer,
														 @Nonnull Function<AmazonSqsManager.AmazonSqsProcessingFunction, AmazonSqsManager> amazonSqsManagerProvider) {
		super(accountServiceProvider, database, configuration, formatter, messageSender, messageSerializer, amazonSqsManagerProvider);
	}
}
