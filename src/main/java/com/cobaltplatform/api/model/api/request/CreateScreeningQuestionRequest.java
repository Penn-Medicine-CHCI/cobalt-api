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

package com.cobaltplatform.api.model.api.request;

import com.cobaltplatform.api.model.db.FontSize.FontSizeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateScreeningQuestionRequest {
	@Nullable
	private String question;
	@Nullable
	private FontSizeId fontSizeId;

	@Nullable
	public String getQuestion() {
		return question;
	}

	public void setQuestion(@Nullable String question) {
		this.question = question;
	}

	@Nullable
	public FontSizeId getFontSizeId() {
		return fontSizeId;
	}

	public void setFontSizeId(@Nullable FontSizeId fontSizeId) {
		this.fontSizeId = fontSizeId;
	}
}
