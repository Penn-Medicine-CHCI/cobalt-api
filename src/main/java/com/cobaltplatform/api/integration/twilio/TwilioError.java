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

package com.cobaltplatform.api.integration.twilio;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class TwilioError {
	@Nullable
	private Integer code;
	@Nullable
	private String message;
	@Nullable
	@SerializedName("log_level")
	private String logLevel;
	@Nullable
	@SerializedName("secondary_message")
	private String secondaryMessage;
	@Nullable
	@SerializedName("log_type")
	private String logType;
	@Nullable
	private String docs;
	@Nullable
	private String causes;
	@Nullable
	private String solutions;
	@Nullable
	private String description;
	@Nullable
	private String product;

	@Nullable
	public Integer getCode() {
		return this.code;
	}

	public void setCode(@Nullable Integer code) {
		this.code = code;
	}

	@Nullable
	public String getMessage() {
		return this.message;
	}

	public void setMessage(@Nullable String message) {
		this.message = message;
	}

	@Nullable
	public String getLogLevel() {
		return this.logLevel;
	}

	public void setLogLevel(@Nullable String logLevel) {
		this.logLevel = logLevel;
	}

	@Nullable
	public String getSecondaryMessage() {
		return this.secondaryMessage;
	}

	public void setSecondaryMessage(@Nullable String secondaryMessage) {
		this.secondaryMessage = secondaryMessage;
	}

	@Nullable
	public String getLogType() {
		return this.logType;
	}

	public void setLogType(@Nullable String logType) {
		this.logType = logType;
	}

	@Nullable
	public String getDocs() {
		return this.docs;
	}

	public void setDocs(@Nullable String docs) {
		this.docs = docs;
	}

	@Nullable
	public String getCauses() {
		return this.causes;
	}

	public void setCauses(@Nullable String causes) {
		this.causes = causes;
	}

	@Nullable
	public String getSolutions() {
		return this.solutions;
	}

	public void setSolutions(@Nullable String solutions) {
		this.solutions = solutions;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getProduct() {
		return this.product;
	}

	public void setProduct(@Nullable String product) {
		this.product = product;
	}
}