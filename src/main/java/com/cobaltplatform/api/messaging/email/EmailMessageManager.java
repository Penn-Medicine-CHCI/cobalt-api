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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.messaging.MessageManager;
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.messaging.MessageSerializer;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.AmazonSqsManager;
import com.cobaltplatform.api.util.Formatter;
import com.pyranid.Database;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class EmailMessageManager extends MessageManager<EmailMessage> {
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;

	public EmailMessageManager(@Nonnull EnterprisePluginProvider enterprisePluginProvider,
														 @Nonnull Provider<AccountService> accountServiceProvider,
														 @Nonnull Provider<InstitutionService> institutionServiceProvider,
														 @Nonnull Provider<CurrentContext> currentContextProvider,
														 @Nonnull Database database,
														 @Nonnull Configuration configuration,
														 @Nonnull Formatter formatter,
														 @Nonnull MessageSender<EmailMessage> messageSender,
														 @Nonnull MessageSerializer<EmailMessage> messageSerializer,
														 @Nonnull Function<AmazonSqsManager.AmazonSqsProcessingFunction, AmazonSqsManager> amazonSqsManagerProvider) {
		super(accountServiceProvider, database, configuration, formatter, messageSender, messageSerializer, amazonSqsManagerProvider);
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.currentContextProvider = currentContextProvider;
	}

	@Nonnull
	@Override
	public void enqueueMessage(@Nonnull EmailMessage emailMessage) {
		requireNonNull(emailMessage);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();

		// Add some common global fields to the email before it goes out
		Map<String, Object> messageContext = new HashMap<>(emailMessage.getMessageContext()); // Mutable copy

		// e.g. https://cobaltplatform.s3.us-east-2.amazonaws.com/local/emails/button-start-appointment@2x.jpg
		messageContext.put("staticFileUrlPrefix", format(" https://%s.s3.%s.amazonaws.com/%s/emails",
				getConfiguration().getAmazonS3BucketName(), getConfiguration().getAmazonS3Region().id(), getConfiguration().getEnvironment()));
		messageContext.put("copyrightYear", LocalDateTime.now(institution.getTimeZone()).getYear());

		// Create a new email message using the updated email message context
		emailMessage = emailMessage.toBuilder()
				.messageContext(messageContext)
				.build();

		// Hook for institutions to customize outgoing emails
		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForCurrentInstitution();
		super.enqueueMessage(enterprisePlugin.customizeEmailMessage(emailMessage));
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return enterprisePluginProvider;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}
}
