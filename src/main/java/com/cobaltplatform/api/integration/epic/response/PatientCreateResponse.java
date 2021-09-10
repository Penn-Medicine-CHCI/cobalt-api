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

package com.cobaltplatform.api.integration.epic.response;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientCreateResponse {
	@Nullable
	private String NationalIdentifier;
	@Nullable
	private String DateOfBirth;
	@Nullable
	private String Gender;
	@Nullable
	private String MaritalStatus;
	@Nullable
	private String Race;
	@Nullable
	private String Religion;
	@Nullable
	private List<PatientID> PatientIDs;

	@NotThreadSafe
	public static class PatientID {
		@Nullable
		private String ID;
		@Nullable
		private String Type;

		@Nullable
		public String getID() {
			return ID;
		}

		public void setID(@Nullable String ID) {
			this.ID = ID;
		}

		@Nullable
		public String getType() {
			return Type;
		}

		public void setType(@Nullable String type) {
			Type = type;
		}
	}

	@Nullable
	public String getNationalIdentifier() {
		return NationalIdentifier;
	}

	public void setNationalIdentifier(@Nullable String nationalIdentifier) {
		NationalIdentifier = nationalIdentifier;
	}

	@Nullable
	public String getDateOfBirth() {
		return DateOfBirth;
	}

	public void setDateOfBirth(@Nullable String dateOfBirth) {
		DateOfBirth = dateOfBirth;
	}

	@Nullable
	public String getGender() {
		return Gender;
	}

	public void setGender(@Nullable String gender) {
		Gender = gender;
	}

	@Nullable
	public String getMaritalStatus() {
		return MaritalStatus;
	}

	public void setMaritalStatus(@Nullable String maritalStatus) {
		MaritalStatus = maritalStatus;
	}

	@Nullable
	public String getRace() {
		return Race;
	}

	public void setRace(@Nullable String race) {
		Race = race;
	}

	@Nullable
	public String getReligion() {
		return Religion;
	}

	public void setReligion(@Nullable String religion) {
		Religion = religion;
	}

	@Nullable
	public List<PatientID> getPatientIDs() {
		return PatientIDs;
	}

	public void setPatientIDs(@Nullable List<PatientID> patientIDs) {
		PatientIDs = patientIDs;
	}
}
