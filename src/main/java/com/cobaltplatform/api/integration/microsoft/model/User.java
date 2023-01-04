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

package com.cobaltplatform.api.integration.microsoft.model;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class User {
	@Nullable
	private String id;
	@Nullable
	private String userPrincipalName;
	@Nullable
	private String givenName;
	@Nullable
	private String surname;
	@Nullable
	private String displayName;
	@Nullable
	private String jobTitle;
	@Nullable
	private String mail;
	@Nullable
	private List<String> businessPhones;
	@Nullable
	private String mobilePhone;
	@Nullable
	private String officeLocation;
	@Nullable
	private String preferredLanguage;

	@Nullable
	public String getId() {
		return this.id;
	}

	public void setId(@Nullable String id) {
		this.id = id;
	}

	@Nullable
	public String getUserPrincipalName() {
		return this.userPrincipalName;
	}

	public void setUserPrincipalName(@Nullable String userPrincipalName) {
		this.userPrincipalName = userPrincipalName;
	}

	@Nullable
	public String getGivenName() {
		return this.givenName;
	}

	public void setGivenName(@Nullable String givenName) {
		this.givenName = givenName;
	}

	@Nullable
	public String getSurname() {
		return this.surname;
	}

	public void setSurname(@Nullable String surname) {
		this.surname = surname;
	}

	@Nullable
	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(@Nullable String displayName) {
		this.displayName = displayName;
	}

	@Nullable
	public String getJobTitle() {
		return this.jobTitle;
	}

	public void setJobTitle(@Nullable String jobTitle) {
		this.jobTitle = jobTitle;
	}

	@Nullable
	public String getMail() {
		return this.mail;
	}

	public void setMail(@Nullable String mail) {
		this.mail = mail;
	}

	@Nullable
	public List<String> getBusinessPhones() {
		return this.businessPhones;
	}

	public void setBusinessPhones(@Nullable List<String> businessPhones) {
		this.businessPhones = businessPhones;
	}

	@Nullable
	public String getMobilePhone() {
		return this.mobilePhone;
	}

	public void setMobilePhone(@Nullable String mobilePhone) {
		this.mobilePhone = mobilePhone;
	}

	@Nullable
	public String getOfficeLocation() {
		return this.officeLocation;
	}

	public void setOfficeLocation(@Nullable String officeLocation) {
		this.officeLocation = officeLocation;
	}

	@Nullable
	public String getPreferredLanguage() {
		return this.preferredLanguage;
	}

	public void setPreferredLanguage(@Nullable String preferredLanguage) {
		this.preferredLanguage = preferredLanguage;
	}
}
