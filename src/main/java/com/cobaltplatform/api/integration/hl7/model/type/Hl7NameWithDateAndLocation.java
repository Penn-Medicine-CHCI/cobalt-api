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

package com.cobaltplatform.api.integration.hl7.model.type;

import ca.uhn.hl7v2.model.v251.datatype.NDL;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/NDL
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7NameWithDateAndLocation extends Hl7Object {
	@Nullable
	private Hl7CompositeIdNumberAndNameSimplified name; // NDL.1 - Name
	@Nullable
	private Hl7TimeStamp startDateTime; // NDL.2 - Start Date/Time
	@Nullable
	private Hl7TimeStamp endDateTime; // NDL.3 - End Date/Time
	@Nullable
	private String pointOfCare; // NDL.4 - Point Of Care
	@Nullable
	private String room; // NDL.5 - Room
	@Nullable
	private String bed; // NDL.6 - Bed
	@Nullable
	private Hl7HierarchicDesignator facility; // NDL.7 - Facility
	@Nullable
	private String locationStatus; // NDL.8 - Location Status
	@Nullable
	private String patientLocationType; // NDL.9 - Patient Location Type
	@Nullable
	private String building; // NDL.10 - Building
	@Nullable
	private String floor; // NDL.11 - Floor

	@Nonnull
	public static Boolean isPresent(@Nullable NDL ndl) {
		if (ndl == null)
			return false;

		return Hl7CompositeIdNumberAndNameSimplified.isPresent(ndl.getNDLName())
				|| Hl7TimeStamp.isPresent(ndl.getStartDateTime())
				|| Hl7TimeStamp.isPresent(ndl.getEndDateTime())
				|| trimToNull(ndl.getPointOfCare().getValueOrEmpty()) != null
				|| trimToNull(ndl.getRoom().getValueOrEmpty()) != null
				|| trimToNull(ndl.getBed().getValueOrEmpty()) != null
				|| Hl7HierarchicDesignator.isPresent(ndl.getFacility())
				|| trimToNull(ndl.getLocationStatus().getValueOrEmpty()) != null
				|| trimToNull(ndl.getPatientLocationType().getValueOrEmpty()) != null
				|| trimToNull(ndl.getBuilding().getValueOrEmpty()) != null
				|| trimToNull(ndl.getFloor().getValueOrEmpty()) != null;
	}

	public Hl7NameWithDateAndLocation() {
		// Nothing to do
	}

	public Hl7NameWithDateAndLocation(@Nullable NDL ndl) {
		if (ndl != null) {
			if (Hl7CompositeIdNumberAndNameSimplified.isPresent(ndl.getNDLName()))
				this.name = new Hl7CompositeIdNumberAndNameSimplified(ndl.getNDLName());

			if (Hl7TimeStamp.isPresent(ndl.getStartDateTime()))
				this.startDateTime = new Hl7TimeStamp(ndl.getStartDateTime());

			if (Hl7TimeStamp.isPresent(ndl.getEndDateTime()))
				this.endDateTime = new Hl7TimeStamp(ndl.getEndDateTime());

			this.pointOfCare = trimToNull(ndl.getPointOfCare().getValueOrEmpty());
			this.room = trimToNull(ndl.getRoom().getValueOrEmpty());
			this.bed = trimToNull(ndl.getBed().getValueOrEmpty());

			if (Hl7HierarchicDesignator.isPresent(ndl.getFacility()))
				this.facility = new Hl7HierarchicDesignator(ndl.getFacility());

			this.locationStatus = trimToNull(ndl.getLocationStatus().getValueOrEmpty());
			this.patientLocationType = trimToNull(ndl.getPatientLocationType().getValueOrEmpty());
			this.building = trimToNull(ndl.getBuilding().getValueOrEmpty());
			this.floor = trimToNull(ndl.getFloor().getValueOrEmpty());
		}
	}

	@Nullable
	public Hl7CompositeIdNumberAndNameSimplified getName() {
		return this.name;
	}

	public void setName(@Nullable Hl7CompositeIdNumberAndNameSimplified name) {
		this.name = name;
	}

	@Nullable
	public Hl7TimeStamp getStartDateTime() {
		return this.startDateTime;
	}

	public void setStartDateTime(@Nullable Hl7TimeStamp startDateTime) {
		this.startDateTime = startDateTime;
	}

	@Nullable
	public Hl7TimeStamp getEndDateTime() {
		return this.endDateTime;
	}

	public void setEndDateTime(@Nullable Hl7TimeStamp endDateTime) {
		this.endDateTime = endDateTime;
	}

	@Nullable
	public String getPointOfCare() {
		return this.pointOfCare;
	}

	public void setPointOfCare(@Nullable String pointOfCare) {
		this.pointOfCare = pointOfCare;
	}

	@Nullable
	public String getRoom() {
		return this.room;
	}

	public void setRoom(@Nullable String room) {
		this.room = room;
	}

	@Nullable
	public String getBed() {
		return this.bed;
	}

	public void setBed(@Nullable String bed) {
		this.bed = bed;
	}

	@Nullable
	public Hl7HierarchicDesignator getFacility() {
		return this.facility;
	}

	public void setFacility(@Nullable Hl7HierarchicDesignator facility) {
		this.facility = facility;
	}

	@Nullable
	public String getLocationStatus() {
		return this.locationStatus;
	}

	public void setLocationStatus(@Nullable String locationStatus) {
		this.locationStatus = locationStatus;
	}

	@Nullable
	public String getPatientLocationType() {
		return this.patientLocationType;
	}

	public void setPatientLocationType(@Nullable String patientLocationType) {
		this.patientLocationType = patientLocationType;
	}

	@Nullable
	public String getBuilding() {
		return this.building;
	}

	public void setBuilding(@Nullable String building) {
		this.building = building;
	}

	@Nullable
	public String getFloor() {
		return this.floor;
	}

	public void setFloor(@Nullable String floor) {
		this.floor = floor;
	}
}