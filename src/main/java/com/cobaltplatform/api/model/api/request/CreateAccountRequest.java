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

import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Race.RaceId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SourceSystem.SourceSystemId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.Map;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateAccountRequest {
	@Nullable
	private RoleId roleId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private AccountSourceId accountSourceId;
	@Nullable
	private SourceSystemId sourceSystemId;
	@Nullable
	private String ssoId;
	@Nullable
	private String emailAddress;
	@Nullable
	private String phoneNumber;
	@Nullable
	private String firstName;
	@Nullable
	private String lastName;
	@Nullable
	private String displayName;
	@Nullable
	private Map<String, ?> ssoAttributes;
	@Nullable
	private String ssoAttributesAsJson;
	@Nullable
	private String password;
	@Nullable
	private String epicPatientMrn;
	@Nullable
	private String epicPatientFhirId;
	@Nullable
	private GenderIdentityId genderIdentityId;
	@Nullable
	private EthnicityId ethnicityId;
	@Nullable
	private BirthSexId birthSexId;
	@Nullable
	private RaceId raceId;
	@Nullable
	private LocalDate birthdate;
	@Nullable
	private CreateAddressRequest address;

	@Nullable
	public RoleId getRoleId() {
		return roleId;
	}

	public void setRoleId(@Nullable RoleId roleId) {
		this.roleId = roleId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public AccountSourceId getAccountSourceId() {
		return accountSourceId;
	}

	public void setAccountSourceId(@Nullable AccountSourceId accountSourceId) {
		this.accountSourceId = accountSourceId;
	}

	@Nullable
	public SourceSystemId getSourceSystemId() {
		return sourceSystemId;
	}

	public void setSourceSystemId(@Nullable SourceSystemId sourceSystemId) {
		this.sourceSystemId = sourceSystemId;
	}

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
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(@Nullable String phoneNumber) {
		this.phoneNumber = phoneNumber;
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
	public Map<String, ?> getSsoAttributes() {
		return ssoAttributes;
	}

	public void setSsoAttributes(@Nullable Map<String, ?> ssoAttributes) {
		this.ssoAttributes = ssoAttributes;
	}

	@Nullable
	public String getSsoAttributesAsJson() {
		return this.ssoAttributesAsJson;
	}

	public void setSsoAttributesAsJson(@Nullable String ssoAttributesAsJson) {
		this.ssoAttributesAsJson = ssoAttributesAsJson;
	}

	@Nullable
	public String getPassword() {
		return password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}
	
	@Nullable
	public String getEpicPatientMrn() {
		return this.epicPatientMrn;
	}

	public void setEpicPatientMrn(@Nullable String epicPatientMrn) {
		this.epicPatientMrn = epicPatientMrn;
	}

	@Nullable
	public String getEpicPatientFhirId() {
		return this.epicPatientFhirId;
	}

	public void setEpicPatientFhirId(@Nullable String epicPatientFhirId) {
		this.epicPatientFhirId = epicPatientFhirId;
	}

	@Nullable
	public GenderIdentityId getGenderIdentityId() {
		return this.genderIdentityId;
	}

	public void setGenderIdentityId(@Nullable GenderIdentityId genderIdentityId) {
		this.genderIdentityId = genderIdentityId;
	}

	@Nullable
	public EthnicityId getEthnicityId() {
		return this.ethnicityId;
	}

	public void setEthnicityId(@Nullable EthnicityId ethnicityId) {
		this.ethnicityId = ethnicityId;
	}

	@Nullable
	public BirthSexId getBirthSexId() {
		return this.birthSexId;
	}

	public void setBirthSexId(@Nullable BirthSexId birthSexId) {
		this.birthSexId = birthSexId;
	}

	@Nullable
	public RaceId getRaceId() {
		return this.raceId;
	}

	public void setRaceId(@Nullable RaceId raceId) {
		this.raceId = raceId;
	}

	@Nullable
	public LocalDate getBirthdate() {
		return this.birthdate;
	}

	public void setBirthdate(@Nullable LocalDate birthdate) {
		this.birthdate = birthdate;
	}

	@Nullable
	public CreateAddressRequest getAddress() {
		return this.address;
	}

	public void setAddress(@Nullable CreateAddressRequest address) {
		this.address = address;
	}
}
