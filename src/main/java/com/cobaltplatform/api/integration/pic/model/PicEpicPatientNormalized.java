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

package com.cobaltplatform.api.integration.pic.model;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PicEpicPatientNormalized {
	@Nullable
	private String ssoId;
	@Nullable
	private String epicPatientId;
	@Nullable
	private String epicPatientIdType;
	@Nullable
	private String emailAddress;
	@Nullable
	private String firstName;
	@Nullable
	private String lastName;
	@Nullable
	private String displayName;
	@Nullable
	private String phoneNumber;

	@Nullable
	public String getSsoId() {
		return ssoId;
	}

	public void setSsoId(@Nullable String ssoId) {
		this.ssoId = ssoId;
	}

	@Nullable
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Nullable
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(@Nullable String firstName) {
		this.firstName = firstName;
	}

	@Nullable
	public String getLastName() {
		return lastName;
	}

	public void setLastName(@Nullable String lastName) {
		this.lastName = lastName;
	}

	@Nullable
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(@Nullable String displayName) {
		this.displayName = displayName;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(@Nullable String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Nullable
	public String getEpicPatientId() {
		return epicPatientId;
	}

	public void setEpicPatientId(@Nullable String epicPatientId) {
		this.epicPatientId = epicPatientId;
	}

	@Nullable
	public String getEpicPatientIdType() {
		return epicPatientIdType;
	}

	public void setEpicPatientIdType(@Nullable String epicPatientIdType) {
		this.epicPatientIdType = epicPatientIdType;
	}
}
