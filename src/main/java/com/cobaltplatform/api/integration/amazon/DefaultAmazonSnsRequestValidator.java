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

package com.cobaltplatform.api.integration.amazon;

import com.cobaltplatform.api.Configuration;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public class DefaultAmazonSnsRequestValidator implements AmazonSnsRequestValidator {
	@Nonnull
	private final Configuration configuration;

	public DefaultAmazonSnsRequestValidator(@Nonnull Configuration configuration) {
		requireNonNull(configuration);
		this.configuration = configuration;
	}

	@Nonnull
	public Boolean validateRequest() {
		// TODO: actually implement per https://docs.aws.amazon.com/sns/latest/dg/SendMessageToHttp.prepare.html
		// and https://docs.aws.amazon.com/sns/latest/dg/sns-verify-signature-of-message.html
		return true;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}
}
