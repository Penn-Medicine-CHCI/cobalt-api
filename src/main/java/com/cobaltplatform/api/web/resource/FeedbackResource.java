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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.FeedbackRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.FeedbackContact;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FeedbackTypeId;
import com.cobaltplatform.api.service.MessageService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.ValidationUtility;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.ApiResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify LLC.
 */
@Singleton
@Resource
@ThreadSafe
public class FeedbackResource {
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final MessageService messageService;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Formatter formatter;

	@Inject
	public FeedbackResource(@Nonnull RequestBodyParser requestBodyParser,
													@Nonnull MessageService messageService,
													@Nonnull Provider<CurrentContext> currentContextProvider,
													@Nonnull DatabaseProvider databaseProvider,
													@Nonnull Strings strings,
													@Nonnull Formatter formatter) {
		this.requestBodyParser = requestBodyParser;
		this.messageService = messageService;
		this.currentContextProvider = currentContextProvider;
		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.formatter = formatter;
	}

	@POST("/feedback")
	@AuthenticationRequired
	public ApiResponse submitFeedback(@Nonnull @RequestBody String body) {
		FeedbackRequest feedbackRequest = getRequestBodyParser().parse(body, FeedbackRequest.class);
		Account account = getCurrentContext().getAccount().get();

		FeedbackTypeId feedbackTypeId = feedbackRequest.getFeedbackTypeId();
		String feedback = trimToNull(feedbackRequest.getFeedback());
		String emailAddress = trimToNull(feedbackRequest.getEmailAddress());

		ValidationException validationException = new ValidationException();

		if (feedback == null)
			validationException.add(new FieldError("feedback", getStrings().get("Feedback is required.")));

		if (emailAddress != null && !ValidationUtility.isValidEmailAddress(emailAddress))
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is invalid.")));

		if (validationException.hasErrors())
			throw validationException;

		if(feedbackTypeId == null)
			feedbackTypeId = FeedbackTypeId.OTHER;

		feedback = format("Reason for contacting: %s\n\n%s", feedbackTypeId.name(), feedback);

		List<FeedbackContact> contacts = getDatabase().queryForList("SELECT * from feedback_contact WHERE institution_id = ? AND active = ?",
				FeedbackContact.class, account.getInstitutionId(), true);

		String phoneNumber = getFormatter().formatPhoneNumber(account.getPhoneNumber(), account.getLocale());

		Map<String, Object> messageContext = new HashMap<>();
		messageContext.put("accountDescription", format("%s %s %s - %s", account.getDisplayName(),
				account.getEmailAddress() == null ? getStrings().get("[no email address]") : account.getEmailAddress(),
				phoneNumber == null ? getStrings().get("[no phone number]") : phoneNumber,
				account.getAccountId()));
		messageContext.put("institutionDescription", account.getInstitutionId());
		messageContext.put("feedback", feedback);
		messageContext.put("emailAddress", emailAddress);

		for (FeedbackContact feedbackContact : contacts) {
			getMessageService().enqueueMessage(new EmailMessage.Builder(feedbackContact.getInstitutionId(), EmailMessageTemplate.USER_FEEDBACK, feedbackContact.getLocale())
					.toAddresses(new ArrayList<>() {{
						add(feedbackContact.getEmailAddress());
					}})
					.messageContext(messageContext)
					.build());
		}

		return new ApiResponse(new HashMap<String, Object>());
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
	}

	@Nonnull
	protected MessageService getMessageService() {
		return this.messageService;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}
}
