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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ScreeningAnswerContentHint.ScreeningAnswerContentHintId;
import com.cobaltplatform.api.model.db.ScreeningAnswerFormat.ScreeningAnswerFormatId;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningQuestionSubmissionStyle.ScreeningQuestionSubmissionStyleId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ScreeningQuestionApiResponse {
	@Nonnull
	private final UUID screeningQuestionId;
	@Nonnull
	private final UUID screeningVersionId;
	@Nonnull
	private final ScreeningAnswerFormatId screeningAnswerFormatId;
	@Nonnull
	private final ScreeningAnswerContentHintId screeningAnswerContentHintId;
	@Nonnull
	private final ScreeningQuestionSubmissionStyleId screeningQuestionSubmissionStyleId;
	@Nullable
	private final String introText;
	@Nonnull
	private final String questionText;
	@Nullable
	private final String supplementText;
	@Nullable
	private final String footerText;
	@Nonnull
	private final Integer minimumAnswerCount;
	@Nonnull
	private final String minimumAnswerCountDescription;
	@Nonnull
	private final Integer maximumAnswerCount;
	@Nonnull
	private final String maximumAnswerCountDescription;
	@Nonnull
	private final Boolean preferAutosubmit;
	@Nonnull
	private final Integer displayOrder;
	@Nonnull
	private final Map<String, Object> metadata;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ScreeningQuestionApiResponseFactory {
		@Nonnull
		ScreeningQuestionApiResponse create(@Nonnull ScreeningQuestion screeningQuestion);
	}

	@AssistedInject
	public ScreeningQuestionApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																			@Nonnull EnterprisePluginProvider enterprisePluginProvider,
																			@Nonnull Formatter formatter,
																			@Nonnull Strings strings,
																			@Assisted @Nonnull ScreeningQuestion screeningQuestion) {
		requireNonNull(currentContextProvider);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(screeningQuestion);

		CurrentContext currentContext = currentContextProvider.get();
		EnterprisePlugin enterprisePlugin = enterprisePluginProvider.enterprisePluginForInstitutionId(currentContext.getInstitutionId());
		Account account = currentContext.getAccount().orElse(null);

		this.screeningQuestionId = screeningQuestion.getScreeningQuestionId();
		this.screeningVersionId = screeningQuestion.getScreeningVersionId();
		this.screeningAnswerFormatId = screeningQuestion.getScreeningAnswerFormatId();
		this.screeningAnswerContentHintId = screeningQuestion.getScreeningAnswerContentHintId();
		this.screeningQuestionSubmissionStyleId = screeningQuestion.getScreeningQuestionSubmissionStyleId();
		this.questionText = screeningQuestion.getQuestionText();
		this.supplementText = screeningQuestion.getSupplementText();
		this.introText = screeningQuestion.getIntroText();
		this.footerText = screeningQuestion.getFooterText();
		this.minimumAnswerCount = screeningQuestion.getMinimumAnswerCount();
		this.minimumAnswerCountDescription = formatter.formatInteger(screeningQuestion.getMinimumAnswerCount());
		this.maximumAnswerCount = screeningQuestion.getMaximumAnswerCount();
		this.maximumAnswerCountDescription = formatter.formatInteger(screeningQuestion.getMaximumAnswerCount());
		this.preferAutosubmit = screeningQuestion.getPreferAutosubmit();
		this.displayOrder = screeningQuestion.getDisplayOrder();

		// Allow enterprise plugin to customize any metadata that might exist
		Map<String, Object> mutableMetadata = screeningQuestion.getMetadata() == null
				? new HashMap<>() : new HashMap<>(screeningQuestion.getMetadata());

		mutableMetadata = enterprisePlugin.customizeScreeningQuestionMetadata(account, screeningQuestion, mutableMetadata);

		this.metadata = Collections.unmodifiableMap(mutableMetadata);
	}

	@Nonnull
	public UUID getScreeningQuestionId() {
		return this.screeningQuestionId;
	}

	@Nonnull
	public UUID getScreeningVersionId() {
		return this.screeningVersionId;
	}

	@Nonnull
	public ScreeningAnswerFormatId getScreeningAnswerFormatId() {
		return this.screeningAnswerFormatId;
	}

	@Nonnull
	public ScreeningAnswerContentHintId getScreeningAnswerContentHintId() {
		return this.screeningAnswerContentHintId;
	}

	@Nonnull
	public ScreeningQuestionSubmissionStyleId getScreeningQuestionSubmissionStyleId() {
		return this.screeningQuestionSubmissionStyleId;
	}

	@Nonnull
	public Optional<String> getIntroText() {
		return Optional.ofNullable(this.introText);
	}

	@Nonnull
	public String getQuestionText() {
		return this.questionText;
	}

	@Nonnull
	public Optional<String> getSupplementText() {
		return Optional.ofNullable(this.supplementText);
	}

	@Nonnull
	public Optional<String> getFooterText() {
		return Optional.ofNullable(this.footerText);
	}

	@Nonnull
	public Integer getMinimumAnswerCount() {
		return this.minimumAnswerCount;
	}

	@Nonnull
	public String getMinimumAnswerCountDescription() {
		return this.minimumAnswerCountDescription;
	}

	@Nonnull
	public Integer getMaximumAnswerCount() {
		return this.maximumAnswerCount;
	}

	@Nonnull
	public String getMaximumAnswerCountDescription() {
		return this.maximumAnswerCountDescription;
	}

	@Nonnull
	public Boolean getPreferAutosubmit() {
		return this.preferAutosubmit;
	}

	@Nonnull
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	@Nonnull
	public Map<String, Object> getMetadata() {
		return this.metadata;
	}
}