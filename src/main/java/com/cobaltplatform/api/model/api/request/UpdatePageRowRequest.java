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

import com.cobaltplatform.api.model.db.BackgroundColor.BackgroundColorId;
import com.cobaltplatform.api.model.db.PageRow.PaddingId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class UpdatePageRowRequest {
	@Nullable
	private UUID pageRowId;
	@Nullable
	private String name;
	@Nullable
	private BackgroundColorId backgroundColorId;
	@Nullable
	private PaddingId paddingId;

	@Nullable
	public UUID getPageRowId() {
		return pageRowId;
	}

	public void setPageRowId(@Nullable UUID pageRowId) {
		this.pageRowId = pageRowId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public BackgroundColorId getBackgroundColorId() {
		return backgroundColorId;
	}

	public void setBackgroundColorId(@Nullable BackgroundColorId backgroundColorId) {
		this.backgroundColorId = backgroundColorId;
	}

	@Nullable
	public PaddingId getPaddingId() {
		return paddingId;
	}

	public void setPaddingId(@Nullable PaddingId paddingId) {
		this.paddingId = paddingId;
	}
}
