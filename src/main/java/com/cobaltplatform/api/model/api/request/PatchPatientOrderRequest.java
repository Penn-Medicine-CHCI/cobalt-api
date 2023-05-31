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

import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.PatientOrderCarePreference.PatientOrderCarePreferenceId;
import com.cobaltplatform.api.model.db.Race.RaceId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatchPatientOrderRequest {
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID accountId;
	@Nullable
	private String patientFirstName;
	@Nullable
	private boolean shouldUpdatePatientFirstName;
	@Nullable
	private String patientLastName;
	@Nullable
	private boolean shouldUpdatePatientLastName;
	@Nullable
	private String patientEmailAddress;
	@Nullable
	private boolean shouldUpdatePatientEmailAddress;
	@Nullable
	private String patientPhoneNumber;
	@Nullable
	private boolean shouldUpdatePatientPhoneNumber;
	@Nullable
	private String patientLanguageCode;
	@Nullable
	private boolean shouldUpdatePatientLanguageCode;
	@Nullable
	private LocalDate patientBirthdate;
	@Nullable
	private boolean shouldUpdatePatientBirthdate;
	@Nullable
	private EthnicityId patientEthnicityId;
	@Nullable
	private boolean shouldUpdatePatientEthnicityId;
	@Nullable
	private RaceId patientRaceId;
	@Nullable
	private boolean shouldUpdatePatientRaceId;
	@Nullable
	private GenderIdentityId patientGenderIdentityId;
	@Nullable
	private boolean shouldUpdatePatientGenderIdentityId;
	@Nullable
	private BirthSexId patientBirthSexId;
	@Nullable
	private boolean shouldUpdatePatientBirthSexId;
	@Nullable
	private CreateAddressRequest patientAddress;
	@Nullable
	private boolean shouldUpdatePatientAddress;
	@Nullable
	private UUID patientOrderInsurancePlanId;
	@Nullable
	private boolean shouldUpdatePatientOrderInsurancePlanId;
	@Nullable
	private Boolean patientDemographicsConfirmed;
	@Nullable
	private boolean shouldUpdatePatientDemographicsConfirmed;
	@Nullable
	private PatientOrderCarePreferenceId patientOrderCarePreferenceId;
	@Nullable
	private boolean shouldUpdatePatientOrderCarePreferenceId;
	@Nullable
	private Integer inPersonCareRadius;
	@Nullable
	private boolean shouldUpdateInPersonCareRadius;

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getPatientFirstName() {
		return this.patientFirstName;
	}

	public void setPatientFirstName(@Nullable String patientFirstName) {
		this.patientFirstName = patientFirstName;
	}

	public boolean isShouldUpdatePatientFirstName() {
		return this.shouldUpdatePatientFirstName;
	}

	public void setShouldUpdatePatientFirstName(boolean shouldUpdatePatientFirstName) {
		this.shouldUpdatePatientFirstName = shouldUpdatePatientFirstName;
	}

	@Nullable
	public String getPatientLastName() {
		return this.patientLastName;
	}

	public void setPatientLastName(@Nullable String patientLastName) {
		this.patientLastName = patientLastName;
	}

	public boolean isShouldUpdatePatientLastName() {
		return this.shouldUpdatePatientLastName;
	}

	public void setShouldUpdatePatientLastName(boolean shouldUpdatePatientLastName) {
		this.shouldUpdatePatientLastName = shouldUpdatePatientLastName;
	}

	@Nullable
	public String getPatientEmailAddress() {
		return this.patientEmailAddress;
	}

	public void setPatientEmailAddress(@Nullable String patientEmailAddress) {
		this.patientEmailAddress = patientEmailAddress;
	}

	public boolean isShouldUpdatePatientEmailAddress() {
		return this.shouldUpdatePatientEmailAddress;
	}

	public void setShouldUpdatePatientEmailAddress(boolean shouldUpdatePatientEmailAddress) {
		this.shouldUpdatePatientEmailAddress = shouldUpdatePatientEmailAddress;
	}

	@Nullable
	public String getPatientPhoneNumber() {
		return this.patientPhoneNumber;
	}

	public void setPatientPhoneNumber(@Nullable String patientPhoneNumber) {
		this.patientPhoneNumber = patientPhoneNumber;
	}

	public boolean isShouldUpdatePatientPhoneNumber() {
		return this.shouldUpdatePatientPhoneNumber;
	}

	public void setShouldUpdatePatientPhoneNumber(boolean shouldUpdatePatientPhoneNumber) {
		this.shouldUpdatePatientPhoneNumber = shouldUpdatePatientPhoneNumber;
	}

	@Nullable
	public String getPatientLanguageCode() {
		return this.patientLanguageCode;
	}

	public void setPatientLanguageCode(@Nullable String patientLanguageCode) {
		this.patientLanguageCode = patientLanguageCode;
	}

	public boolean isShouldUpdatePatientLanguageCode() {
		return this.shouldUpdatePatientLanguageCode;
	}

	public void setShouldUpdatePatientLanguageCode(boolean shouldUpdatePatientLanguageCode) {
		this.shouldUpdatePatientLanguageCode = shouldUpdatePatientLanguageCode;
	}

	@Nullable
	public LocalDate getPatientBirthdate() {
		return this.patientBirthdate;
	}

	public void setPatientBirthdate(@Nullable LocalDate patientBirthdate) {
		this.patientBirthdate = patientBirthdate;
	}

	public boolean isShouldUpdatePatientBirthdate() {
		return this.shouldUpdatePatientBirthdate;
	}

	public void setShouldUpdatePatientBirthdate(boolean shouldUpdatePatientBirthdate) {
		this.shouldUpdatePatientBirthdate = shouldUpdatePatientBirthdate;
	}

	@Nullable
	public EthnicityId getPatientEthnicityId() {
		return this.patientEthnicityId;
	}

	public void setPatientEthnicityId(@Nullable EthnicityId patientEthnicityId) {
		this.patientEthnicityId = patientEthnicityId;
	}

	public boolean isShouldUpdatePatientEthnicityId() {
		return this.shouldUpdatePatientEthnicityId;
	}

	public void setShouldUpdatePatientEthnicityId(boolean shouldUpdatePatientEthnicityId) {
		this.shouldUpdatePatientEthnicityId = shouldUpdatePatientEthnicityId;
	}

	@Nullable
	public RaceId getPatientRaceId() {
		return this.patientRaceId;
	}

	public void setPatientRaceId(@Nullable RaceId patientRaceId) {
		this.patientRaceId = patientRaceId;
	}

	public boolean isShouldUpdatePatientRaceId() {
		return this.shouldUpdatePatientRaceId;
	}

	public void setShouldUpdatePatientRaceId(boolean shouldUpdatePatientRaceId) {
		this.shouldUpdatePatientRaceId = shouldUpdatePatientRaceId;
	}

	@Nullable
	public GenderIdentityId getPatientGenderIdentityId() {
		return this.patientGenderIdentityId;
	}

	public void setPatientGenderIdentityId(@Nullable GenderIdentityId patientGenderIdentityId) {
		this.patientGenderIdentityId = patientGenderIdentityId;
	}

	public boolean isShouldUpdatePatientGenderIdentityId() {
		return this.shouldUpdatePatientGenderIdentityId;
	}

	public void setShouldUpdatePatientGenderIdentityId(boolean shouldUpdatePatientGenderIdentityId) {
		this.shouldUpdatePatientGenderIdentityId = shouldUpdatePatientGenderIdentityId;
	}

	@Nullable
	public BirthSexId getPatientBirthSexId() {
		return this.patientBirthSexId;
	}

	public void setPatientBirthSexId(@Nullable BirthSexId patientBirthSexId) {
		this.patientBirthSexId = patientBirthSexId;
	}

	public boolean isShouldUpdatePatientBirthSexId() {
		return this.shouldUpdatePatientBirthSexId;
	}

	public void setShouldUpdatePatientBirthSexId(boolean shouldUpdatePatientBirthSexId) {
		this.shouldUpdatePatientBirthSexId = shouldUpdatePatientBirthSexId;
	}

	@Nullable
	public CreateAddressRequest getPatientAddress() {
		return this.patientAddress;
	}

	public void setPatientAddress(@Nullable CreateAddressRequest patientAddress) {
		this.patientAddress = patientAddress;
	}

	public boolean isShouldUpdatePatientAddress() {
		return this.shouldUpdatePatientAddress;
	}

	public void setShouldUpdatePatientAddress(boolean shouldUpdatePatientAddress) {
		this.shouldUpdatePatientAddress = shouldUpdatePatientAddress;
	}

	@Nullable
	public UUID getPatientOrderInsurancePlanId() {
		return this.patientOrderInsurancePlanId;
	}

	public void setPatientOrderInsurancePlanId(@Nullable UUID patientOrderInsurancePlanId) {
		this.patientOrderInsurancePlanId = patientOrderInsurancePlanId;
	}

	public boolean isShouldUpdatePatientOrderInsurancePlanId() {
		return this.shouldUpdatePatientOrderInsurancePlanId;
	}

	public void setShouldUpdatePatientOrderInsurancePlanId(boolean shouldUpdatePatientOrderInsurancePlanId) {
		this.shouldUpdatePatientOrderInsurancePlanId = shouldUpdatePatientOrderInsurancePlanId;
	}

	@Nullable
	public Boolean getPatientDemographicsConfirmed() {
		return this.patientDemographicsConfirmed;
	}

	public void setPatientDemographicsConfirmed(@Nullable Boolean patientDemographicsConfirmed) {
		this.patientDemographicsConfirmed = patientDemographicsConfirmed;
	}

	public boolean isShouldUpdatePatientDemographicsConfirmed() {
		return this.shouldUpdatePatientDemographicsConfirmed;
	}

	public void setShouldUpdatePatientDemographicsConfirmed(boolean shouldUpdatePatientDemographicsConfirmed) {
		this.shouldUpdatePatientDemographicsConfirmed = shouldUpdatePatientDemographicsConfirmed;
	}

	@Nullable
	public PatientOrderCarePreferenceId getPatientOrderCarePreferenceId() {
		return this.patientOrderCarePreferenceId;
	}

	public void setPatientOrderCarePreferenceId(@Nullable PatientOrderCarePreferenceId patientOrderCarePreferenceId) {
		this.patientOrderCarePreferenceId = patientOrderCarePreferenceId;
	}

	public boolean isShouldUpdatePatientOrderCarePreferenceId() {
		return this.shouldUpdatePatientOrderCarePreferenceId;
	}

	public void setShouldUpdatePatientOrderCarePreferenceId(boolean shouldUpdatePatientOrderCarePreferenceId) {
		this.shouldUpdatePatientOrderCarePreferenceId = shouldUpdatePatientOrderCarePreferenceId;
	}

	@Nullable
	public Integer getInPersonCareRadius() {
		return this.inPersonCareRadius;
	}

	public void setInPersonCareRadius(@Nullable Integer inPersonCareRadius) {
		this.inPersonCareRadius = inPersonCareRadius;
	}

	public boolean isShouldUpdateInPersonCareRadius() {
		return this.shouldUpdateInPersonCareRadius;
	}

	public void setShouldUpdateInPersonCareRadius(boolean shouldUpdateInPersonCareRadius) {
		this.shouldUpdateInPersonCareRadius = shouldUpdateInPersonCareRadius;
	}
}
