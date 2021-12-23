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

package com.cobaltplatform.api.integration.way2health.model.entity;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Participant extends Way2HealthEntity {
	@Nullable
	private BigInteger id;
	@Nullable
	private BigInteger studyId;
	@Nullable
	private String name;
	@Nullable
	private String longId;
	@Nullable
	private LocalDate startDate;
	@Nullable
	private String status;
	@Nullable
	private String armId;
	@Nullable
	private String cohortId;
	@Nullable
	private Integer lotteryNumber;
	@Nullable
	private String externalId;
	@Nullable
	private String mrn;
	@Nullable
	private String mrnType;
	@Nullable
	private String csn;
	@Nullable
	private String providerNpi;
	@Nullable
	private String paymentPreference;
	@Nullable
	private String email;
	@Nullable
	private String firstName;
	@Nullable
	private String lastName;
	@Nullable
	private String workPhone;
	@Nullable
	private String cellPhone;
	@Nullable
	private String homePhone;
	@Nullable
	private String streetAddress;
	@Nullable
	private String streetAddress2;
	@Nullable
	private String city;
	@Nullable
	private String stateRegion;
	@Nullable
	private String postalCode;
	@Nullable
	private LocalDate dateOfBirth;
	@Nullable
	private String timeZone; // e.g. an offset like "-5.0", not an ID like "America/New_York"
	@Nullable
	private Boolean notifyEmail;
	@Nullable
	private Boolean notifyText;
	@Nullable
	private Boolean notifyVoice;
	@Nullable
	private String notifyVoiceChoice;
	@Nullable
	private String preferredLanguage;
	@Nullable
	private String locale; // e.g. a description like "English", not a language tag like "en-US"
	@Nullable
	private Arm arm;
	@Nullable
	private List<Incident> incidents;
	@Nullable
	private List<EnrollmentStep> enrollmentSteps;
	@Nullable
	private List<TextMessage> textMessages;
	@Nullable
	private List<Variable> variables;

	@NotThreadSafe
	public static class Arm extends Way2HealthEntity {
		@Nullable
		private BigInteger id;
		@Nullable
		private String name;
		@Nullable
		private Boolean enablePortal;

		@Nullable
		public BigInteger getId() {
			return id;
		}

		public void setId(@Nullable BigInteger id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public Boolean getEnablePortal() {
			return enablePortal;
		}

		public void setEnablePortal(@Nullable Boolean enablePortal) {
			this.enablePortal = enablePortal;
		}
	}

	@Nullable
	public BigInteger getId() {
		return id;
	}

	public void setId(@Nullable BigInteger id) {
		this.id = id;
	}

	@Nullable
	public BigInteger getStudyId() {
		return studyId;
	}

	public void setStudyId(@Nullable BigInteger studyId) {
		this.studyId = studyId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getLongId() {
		return longId;
	}

	public void setLongId(@Nullable String longId) {
		this.longId = longId;
	}

	@Nullable
	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(@Nullable LocalDate startDate) {
		this.startDate = startDate;
	}

	@Nullable
	public String getStatus() {
		return status;
	}

	public void setStatus(@Nullable String status) {
		this.status = status;
	}

	@Nullable
	public String getArmId() {
		return armId;
	}

	public void setArmId(@Nullable String armId) {
		this.armId = armId;
	}

	@Nullable
	public String getCohortId() {
		return cohortId;
	}

	public void setCohortId(@Nullable String cohortId) {
		this.cohortId = cohortId;
	}

	@Nullable
	public Integer getLotteryNumber() {
		return lotteryNumber;
	}

	public void setLotteryNumber(@Nullable Integer lotteryNumber) {
		this.lotteryNumber = lotteryNumber;
	}

	@Nullable
	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(@Nullable String externalId) {
		this.externalId = externalId;
	}

	@Nullable
	public String getMrn() {
		return mrn;
	}

	public void setMrn(@Nullable String mrn) {
		this.mrn = mrn;
	}

	@Nullable
	public String getMrnType() {
		return mrnType;
	}

	public void setMrnType(@Nullable String mrnType) {
		this.mrnType = mrnType;
	}

	@Nullable
	public String getCsn() {
		return csn;
	}

	public void setCsn(@Nullable String csn) {
		this.csn = csn;
	}

	@Nullable
	public String getProviderNpi() {
		return providerNpi;
	}

	public void setProviderNpi(@Nullable String providerNpi) {
		this.providerNpi = providerNpi;
	}

	@Nullable
	public String getPaymentPreference() {
		return paymentPreference;
	}

	public void setPaymentPreference(@Nullable String paymentPreference) {
		this.paymentPreference = paymentPreference;
	}

	@Nullable
	public String getEmail() {
		return email;
	}

	public void setEmail(@Nullable String email) {
		this.email = email;
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
	public String getWorkPhone() {
		return workPhone;
	}

	public void setWorkPhone(@Nullable String workPhone) {
		this.workPhone = workPhone;
	}

	@Nullable
	public String getCellPhone() {
		return cellPhone;
	}

	public void setCellPhone(@Nullable String cellPhone) {
		this.cellPhone = cellPhone;
	}

	@Nullable
	public String getHomePhone() {
		return homePhone;
	}

	public void setHomePhone(@Nullable String homePhone) {
		this.homePhone = homePhone;
	}

	@Nullable
	public String getStreetAddress() {
		return streetAddress;
	}

	public void setStreetAddress(@Nullable String streetAddress) {
		this.streetAddress = streetAddress;
	}

	@Nullable
	public String getStreetAddress2() {
		return streetAddress2;
	}

	public void setStreetAddress2(@Nullable String streetAddress2) {
		this.streetAddress2 = streetAddress2;
	}

	@Nullable
	public String getCity() {
		return city;
	}

	public void setCity(@Nullable String city) {
		this.city = city;
	}

	@Nullable
	public String getStateRegion() {
		return stateRegion;
	}

	public void setStateRegion(@Nullable String stateRegion) {
		this.stateRegion = stateRegion;
	}

	@Nullable
	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(@Nullable String postalCode) {
		this.postalCode = postalCode;
	}

	@Nullable
	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(@Nullable LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	@Nullable
	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(@Nullable String timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public Boolean getNotifyEmail() {
		return notifyEmail;
	}

	public void setNotifyEmail(@Nullable Boolean notifyEmail) {
		this.notifyEmail = notifyEmail;
	}

	@Nullable
	public Boolean getNotifyText() {
		return notifyText;
	}

	public void setNotifyText(@Nullable Boolean notifyText) {
		this.notifyText = notifyText;
	}

	@Nullable
	public Boolean getNotifyVoice() {
		return notifyVoice;
	}

	public void setNotifyVoice(@Nullable Boolean notifyVoice) {
		this.notifyVoice = notifyVoice;
	}

	@Nullable
	public String getNotifyVoiceChoice() {
		return notifyVoiceChoice;
	}

	public void setNotifyVoiceChoice(@Nullable String notifyVoiceChoice) {
		this.notifyVoiceChoice = notifyVoiceChoice;
	}

	@Nullable
	public String getPreferredLanguage() {
		return preferredLanguage;
	}

	public void setPreferredLanguage(@Nullable String preferredLanguage) {
		this.preferredLanguage = preferredLanguage;
	}

	@Nullable
	public String getLocale() {
		return locale;
	}

	public void setLocale(@Nullable String locale) {
		this.locale = locale;
	}

	@Nullable
	public Arm getArm() {
		return arm;
	}

	public void setArm(@Nullable Arm arm) {
		this.arm = arm;
	}

	@Nullable
	public List<Incident> getIncidents() {
		return incidents;
	}

	public void setIncidents(@Nullable List<Incident> incidents) {
		this.incidents = incidents;
	}

	@Nullable
	public List<EnrollmentStep> getEnrollmentSteps() {
		return enrollmentSteps;
	}

	public void setEnrollmentSteps(@Nullable List<EnrollmentStep> enrollmentSteps) {
		this.enrollmentSteps = enrollmentSteps;
	}

	@Nullable
	public List<TextMessage> getTextMessages() {
		return textMessages;
	}

	public void setTextMessages(@Nullable List<TextMessage> textMessages) {
		this.textMessages = textMessages;
	}

	@Nullable
	public List<Variable> getVariables() {
		return variables;
	}

	public void setVariables(@Nullable List<Variable> variables) {
		this.variables = variables;
	}
}
