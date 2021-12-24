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
public class Staff extends Way2HealthEntity {
	@Nullable
	private BigInteger id;
	@Nullable
	private BigInteger accountId;
	@Nullable
	private BigInteger studyId;
	@Nullable
	private String name;
	@Nullable
	private LocalDate dateOfBirth;
	@Nullable
	private String streetAddress;
	@Nullable
	private String streetAddress2;
	@Nullable
	private String status;
	@Nullable
	private BigInteger notificationGroupId;
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
	private List<Incident> incidents;
	@Nullable
	private List<StudyGroup> studyGroups;

	@NotThreadSafe
	public static class StudyGroup extends Way2HealthEntity {
		@Nullable
		private BigInteger id;
		@Nullable
		private String name;

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
	}

	@Nullable
	public BigInteger getId() {
		return id;
	}

	public void setId(@Nullable BigInteger id) {
		this.id = id;
	}

	@Nullable
	public BigInteger getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable BigInteger accountId) {
		this.accountId = accountId;
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
	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(@Nullable LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
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
	public String getStatus() {
		return status;
	}

	public void setStatus(@Nullable String status) {
		this.status = status;
	}

	@Nullable
	public BigInteger getNotificationGroupId() {
		return notificationGroupId;
	}

	public void setNotificationGroupId(@Nullable BigInteger notificationGroupId) {
		this.notificationGroupId = notificationGroupId;
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
	public List<Incident> getIncidents() {
		return incidents;
	}

	public void setIncidents(@Nullable List<Incident> incidents) {
		this.incidents = incidents;
	}

	@Nullable
	public List<StudyGroup> getStudyGroups() {
		return studyGroups;
	}

	public void setStudyGroups(@Nullable List<StudyGroup> studyGroups) {
		this.studyGroups = studyGroups;
	}
}
