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

import com.cobaltplatform.api.model.db.Interaction;
import com.cobaltplatform.api.model.db.InteractionInstance;
import com.cobaltplatform.api.model.db.InteractionOption;
import com.cobaltplatform.api.service.InteractionService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.LinkGenerator;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InteractionOptionApiResponse {
	@Nullable
	private UUID interactionOptionId;
	@Nullable
	private UUID interactionId;
	@Nullable
	private String optionDescription;
	@Nullable
	private String optionResponse;
	@Nullable
	private Boolean finalFlag;
	@Nullable
	private Integer optionOrder;
	@Nullable
	private String optionUrl;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InteractionOptionApiResponseFactory {
		@Nonnull
		InteractionOptionApiResponse create(@Nonnull InteractionOption interactionOption,
																				@Nonnull InteractionInstance interactionInstance);
	}

	@AssistedInject
	public InteractionOptionApiResponse(@Nonnull Formatter formatter,
																			@Nonnull Strings strings,
																			@Nonnull InteractionService interactionService,
																			@Nonnull LinkGenerator linkGenerator,
																			@Assisted @Nonnull InteractionOption interactionOption,
																			@Assisted @Nonnull InteractionInstance interactionInstance) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(interactionOption);
		requireNonNull(interactionService);
		requireNonNull(interactionInstance);

		Interaction interaction = interactionService.findInteractionById(interactionOption.getInteractionId()).get();

		this.interactionOptionId = interactionOption.getInteractionOptionId();
		this.interactionId = interactionOption.getInteractionId();
		this.finalFlag = interactionOption.getFinalFlag();
		this.optionOrder = interactionOption.getOptionOrder();
		this.optionUrl = linkGenerator.generateInteractionOptionLink(interaction.getInstitutionId(), interactionOption, interactionInstance);
		this.optionDescription = interactionOption.getOptionDescription();
		this.optionResponse = interactionInstance.getCompletedFlag() ? interactionService.formatInteractionOptionResponseMessage(interactionInstance, interactionOption.getCompletedResponse()) :
				interactionService.formatInteractionOptionResponseMessage(interactionInstance, interactionOption.getOptionResponse());
	}
}