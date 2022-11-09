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
import com.cobaltplatform.api.model.db.Race.RaceId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatchAccountRequest {
	@Nullable
	private UUID accountId;
	@Nullable
	private String emailAddress;
	@Nullable
	private boolean shouldUpdateEmailAddress;
	@Nullable
	private String phoneNumber;
	@Nullable
	private boolean shouldUpdatePhoneNumber;
	@Nullable
	private String firstName;
	@Nullable
	private boolean shouldUpdateFirstName;
	@Nullable
	private String lastName;
	@Nullable
	private boolean shouldUpdateLastName;
	@Nullable
	private String displayName;
	@Nullable
	private boolean shouldUpdateDisplayName;
	@Nullable
	private GenderIdentityId genderIdentityId;
	@Nullable
	private boolean shouldUpdateGenderIdentityId;
	@Nullable
	private EthnicityId ethnicityId;
	@Nullable
	private boolean shouldUpdateEthnicityId;
	@Nullable
	private BirthSexId birthSexId;
	@Nullable
	private boolean shouldUpdateBirthSexId;
	@Nullable
	private RaceId raceId;
	@Nullable
	private boolean shouldUpdateRaceId;
	@Nullable
	private LocalDate birthdate;
	@Nullable
	private boolean shouldUpdateBirthdate;
	@Nullable
	private UUID insuranceId;
	@Nullable
	private boolean shouldUpdateInsuranceId;
	@Nullable
	private String countryCode;
	@Nullable
	private boolean shouldUpdateCountryCode;
	@Nullable
	private String languageCode;
	@Nullable
	private boolean shouldUpdateLanguageCode;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private boolean shouldUpdateTimeZone;
	@Nullable
	private CreateAddressRequest address;
	@Nullable
	private boolean shouldUpdateAddress;

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getEmailAddress() {
		return this.emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public boolean isShouldUpdateEmailAddress() {
		return this.shouldUpdateEmailAddress;
	}

	public void setShouldUpdateEmailAddress(boolean shouldUpdateEmailAddress) {
		this.shouldUpdateEmailAddress = shouldUpdateEmailAddress;
	}

	@Nullable
	public String getPhoneNumber() {
		return this.phoneNumber;
	}

	public void setPhoneNumber(@Nullable String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public boolean isShouldUpdatePhoneNumber() {
		return this.shouldUpdatePhoneNumber;
	}

	public void setShouldUpdatePhoneNumber(boolean shouldUpdatePhoneNumber) {
		this.shouldUpdatePhoneNumber = shouldUpdatePhoneNumber;
	}

	@Nullable
	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(@Nullable String firstName) {
		this.firstName = firstName;
	}

	public boolean isShouldUpdateFirstName() {
		return this.shouldUpdateFirstName;
	}

	public void setShouldUpdateFirstName(boolean shouldUpdateFirstName) {
		this.shouldUpdateFirstName = shouldUpdateFirstName;
	}

	@Nullable
	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(@Nullable String lastName) {
		this.lastName = lastName;
	}

	public boolean isShouldUpdateLastName() {
		return this.shouldUpdateLastName;
	}

	public void setShouldUpdateLastName(boolean shouldUpdateLastName) {
		this.shouldUpdateLastName = shouldUpdateLastName;
	}

	@Nullable
	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(@Nullable String displayName) {
		this.displayName = displayName;
	}

	public boolean isShouldUpdateDisplayName() {
		return this.shouldUpdateDisplayName;
	}

	public void setShouldUpdateDisplayName(boolean shouldUpdateDisplayName) {
		this.shouldUpdateDisplayName = shouldUpdateDisplayName;
	}

	@Nullable
	public GenderIdentityId getGenderIdentityId() {
		return this.genderIdentityId;
	}

	public void setGenderIdentityId(@Nullable GenderIdentityId genderIdentityId) {
		this.genderIdentityId = genderIdentityId;
	}

	public boolean isShouldUpdateGenderIdentityId() {
		return this.shouldUpdateGenderIdentityId;
	}

	public void setShouldUpdateGenderIdentityId(boolean shouldUpdateGenderIdentityId) {
		this.shouldUpdateGenderIdentityId = shouldUpdateGenderIdentityId;
	}

	@Nullable
	public EthnicityId getEthnicityId() {
		return this.ethnicityId;
	}

	public void setEthnicityId(@Nullable EthnicityId ethnicityId) {
		this.ethnicityId = ethnicityId;
	}

	public boolean isShouldUpdateEthnicityId() {
		return this.shouldUpdateEthnicityId;
	}

	public void setShouldUpdateEthnicityId(boolean shouldUpdateEthnicityId) {
		this.shouldUpdateEthnicityId = shouldUpdateEthnicityId;
	}

	@Nullable
	public BirthSexId getBirthSexId() {
		return this.birthSexId;
	}

	public void setBirthSexId(@Nullable BirthSexId birthSexId) {
		this.birthSexId = birthSexId;
	}

	public boolean isShouldUpdateBirthSexId() {
		return this.shouldUpdateBirthSexId;
	}

	public void setShouldUpdateBirthSexId(boolean shouldUpdateBirthSexId) {
		this.shouldUpdateBirthSexId = shouldUpdateBirthSexId;
	}

	@Nullable
	public RaceId getRaceId() {
		return this.raceId;
	}

	public void setRaceId(@Nullable RaceId raceId) {
		this.raceId = raceId;
	}

	public boolean isShouldUpdateRaceId() {
		return this.shouldUpdateRaceId;
	}

	public void setShouldUpdateRaceId(boolean shouldUpdateRaceId) {
		this.shouldUpdateRaceId = shouldUpdateRaceId;
	}

	@Nullable
	public LocalDate getBirthdate() {
		return this.birthdate;
	}

	public void setBirthdate(@Nullable LocalDate birthdate) {
		this.birthdate = birthdate;
	}

	public boolean isShouldUpdateBirthdate() {
		return this.shouldUpdateBirthdate;
	}

	public void setShouldUpdateBirthdate(boolean shouldUpdateBirthdate) {
		this.shouldUpdateBirthdate = shouldUpdateBirthdate;
	}

	@Nullable
	public UUID getInsuranceId() {
		return this.insuranceId;
	}

	public void setInsuranceId(@Nullable UUID insuranceId) {
		this.insuranceId = insuranceId;
	}

	public boolean isShouldUpdateInsuranceId() {
		return this.shouldUpdateInsuranceId;
	}

	public void setShouldUpdateInsuranceId(boolean shouldUpdateInsuranceId) {
		this.shouldUpdateInsuranceId = shouldUpdateInsuranceId;
	}

	@Nullable
	public String getCountryCode() {
		return this.countryCode;
	}

	public void setCountryCode(@Nullable String countryCode) {
		this.countryCode = countryCode;
	}

	public boolean isShouldUpdateCountryCode() {
		return this.shouldUpdateCountryCode;
	}

	public void setShouldUpdateCountryCode(boolean shouldUpdateCountryCode) {
		this.shouldUpdateCountryCode = shouldUpdateCountryCode;
	}

	@Nullable
	public String getLanguageCode() {
		return this.languageCode;
	}

	public void setLanguageCode(@Nullable String languageCode) {
		this.languageCode = languageCode;
	}

	public boolean isShouldUpdateLanguageCode() {
		return this.shouldUpdateLanguageCode;
	}

	public void setShouldUpdateLanguageCode(boolean shouldUpdateLanguageCode) {
		this.shouldUpdateLanguageCode = shouldUpdateLanguageCode;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return this.timeZone;
	}

	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	public boolean isShouldUpdateTimeZone() {
		return this.shouldUpdateTimeZone;
	}

	public void setShouldUpdateTimeZone(boolean shouldUpdateTimeZone) {
		this.shouldUpdateTimeZone = shouldUpdateTimeZone;
	}

	@Nullable
	public CreateAddressRequest getAddress() {
		return this.address;
	}

	public void setAddress(@Nullable CreateAddressRequest address) {
		this.address = address;
	}

	public boolean isShouldUpdateAddress() {
		return this.shouldUpdateAddress;
	}

	public void setShouldUpdateAddress(boolean shouldUpdateAddress) {
		this.shouldUpdateAddress = shouldUpdateAddress;
	}
}
