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

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class User extends Way2HealthEntity {
	@Nullable
	private BigInteger id;
	@Nullable
	private String firstName;
	@Nullable
	private String lastName;
	@Nullable
	private String name;
	@Nullable
	private String username;
	@Nullable
	private String email;
	@Nullable
	private String workPhone;
	@Nullable
	private String cellPhone;
	@Nullable
	private String homePhone;
	@Nullable
	private LocalDate dateOfBirth;
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
	private LocalDateTime lastLogin;
	@Nullable
	@SerializedName("Groups")
	private List<String> groups;
	@Nullable
	@SerializedName("StudyUsers")
	private List<StudyUser> studyUsers;
	@Nullable
	@SerializedName("Participants")
	private List<Participant> participants;
	@Nullable
	@SerializedName("Staff")
	private List<Staff> staff;

	@NotThreadSafe
	public static class StudyUser extends Way2HealthEntity {
		@Nullable
		private BigInteger studyUserId;
		@Nullable
		private String projectManager;
		@Nullable
		private String studyId;
		@Nullable
		private String status;

		@Nullable
		public BigInteger getStudyUserId() {
			return studyUserId;
		}

		public void setStudyUserId(@Nullable BigInteger studyUserId) {
			this.studyUserId = studyUserId;
		}

		@Nullable
		public String getProjectManager() {
			return projectManager;
		}

		public void setProjectManager(@Nullable String projectManager) {
			this.projectManager = projectManager;
		}

		@Nullable
		public String getStudyId() {
			return studyId;
		}

		public void setStudyId(@Nullable String studyId) {
			this.studyId = studyId;
		}

		@Nullable
		public String getStatus() {
			return status;
		}

		public void setStatus(@Nullable String status) {
			this.status = status;
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
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getUsername() {
		return username;
	}

	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	@Nullable
	public String getEmail() {
		return email;
	}

	public void setEmail(@Nullable String email) {
		this.email = email;
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
	public LocalDateTime getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(@Nullable LocalDateTime lastLogin) {
		this.lastLogin = lastLogin;
	}

	@Nullable
	public List<String> getGroups() {
		return groups;
	}

	public void setGroups(@Nullable List<String> groups) {
		this.groups = groups;
	}

	@Nullable
	public List<StudyUser> getStudyUsers() {
		return studyUsers;
	}

	public void setStudyUsers(@Nullable List<StudyUser> studyUsers) {
		this.studyUsers = studyUsers;
	}

	@Nullable
	public List<Participant> getParticipants() {
		return participants;
	}

	public void setParticipants(@Nullable List<Participant> participants) {
		this.participants = participants;
	}

	@Nullable
	public List<Staff> getStaff() {
		return staff;
	}

	public void setStaff(@Nullable List<Staff> staff) {
		this.staff = staff;
	}
}
