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

import ca.uhn.hl7v2.model.v251.datatype.PL;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/PL
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7PersonLocation extends Hl7Object {
	@Nullable
	private String pointOfCare; // PL.1 - Point Of Care
	@Nullable
	private String room; // PL.2 - Room
	@Nullable
	private String bed; // PL.3 - Bed
	@Nullable
	private Hl7HierarchicDesignator facility; // PL.4 - Facility;
	@Nullable
	private String locationStatus; // PL.5 - Location Status
	@Nullable
	private String personLocationType; // PL.6 - Person Location Type
	@Nullable
	private String building; // PL.7 - Building
	@Nullable
	private String floor; // PL.8 - Floor
	@Nullable
	private String locationDescription; // PL.9 - Location Description
	@Nullable
	private Hl7EntityIdentifier comprehensiveLocationIdentifier; // PL.10 - Comprehensive Location Identifier
	@Nullable
	private Hl7HierarchicDesignator assigningAuthorityForLocation; // PL.11 - Assigning Authority For Location

	@Nonnull
	public static Boolean isPresent(@Nullable PL pl) {
		if (pl == null)
			return false;

		return trimToNull(pl.getPointOfCare().getValueOrEmpty()) != null
				|| trimToNull(pl.getRoom().getValueOrEmpty()) != null
				|| trimToNull(pl.getBed().getValueOrEmpty()) != null
				|| Hl7HierarchicDesignator.isPresent(pl.getFacility())
				|| trimToNull(pl.getLocationStatus().getValueOrEmpty()) != null
				|| trimToNull(pl.getPersonLocationType().getValueOrEmpty()) != null
				|| trimToNull(pl.getBuilding().getValueOrEmpty()) != null
				|| trimToNull(pl.getFloor().getValueOrEmpty()) != null
				|| trimToNull(pl.getLocationDescription().getValueOrEmpty()) != null
				|| Hl7EntityIdentifier.isPresent(pl.getComprehensiveLocationIdentifier())
				|| Hl7HierarchicDesignator.isPresent(pl.getAssigningAuthorityForLocation());
	}

	public Hl7PersonLocation() {
		// Nothing to do
	}

	public Hl7PersonLocation(@Nullable PL pl) {
		if (pl != null) {
			this.pointOfCare = trimToNull(pl.getPointOfCare().getValueOrEmpty());
			this.room = trimToNull(pl.getRoom().getValueOrEmpty());
			this.bed = trimToNull(pl.getBed().getValueOrEmpty());
			this.facility = Hl7HierarchicDesignator.isPresent(pl.getFacility()) ? new Hl7HierarchicDesignator(pl.getFacility()) : null;
			this.locationStatus = trimToNull(pl.getLocationStatus().getValueOrEmpty());
			this.personLocationType = trimToNull(pl.getPersonLocationType().getValueOrEmpty());
			this.building = trimToNull(pl.getBuilding().getValueOrEmpty());
			this.floor = trimToNull(pl.getFloor().getValueOrEmpty());
			this.locationDescription = trimToNull(pl.getLocationDescription().getValueOrEmpty());
			this.comprehensiveLocationIdentifier = Hl7EntityIdentifier.isPresent(pl.getComprehensiveLocationIdentifier()) ? new Hl7EntityIdentifier(pl.getComprehensiveLocationIdentifier()) : null;
			this.assigningAuthorityForLocation = Hl7HierarchicDesignator.isPresent(pl.getAssigningAuthorityForLocation()) ? new Hl7HierarchicDesignator(pl.getAssigningAuthorityForLocation()) : null;
		}
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
	public String getPersonLocationType() {
		return this.personLocationType;
	}

	public void setPersonLocationType(@Nullable String personLocationType) {
		this.personLocationType = personLocationType;
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

	@Nullable
	public String getLocationDescription() {
		return this.locationDescription;
	}

	public void setLocationDescription(@Nullable String locationDescription) {
		this.locationDescription = locationDescription;
	}

	@Nullable
	public Hl7EntityIdentifier getComprehensiveLocationIdentifier() {
		return this.comprehensiveLocationIdentifier;
	}

	public void setComprehensiveLocationIdentifier(@Nullable Hl7EntityIdentifier comprehensiveLocationIdentifier) {
		this.comprehensiveLocationIdentifier = comprehensiveLocationIdentifier;
	}

	@Nullable
	public Hl7HierarchicDesignator getAssigningAuthorityForLocation() {
		return this.assigningAuthorityForLocation;
	}

	public void setAssigningAuthorityForLocation(@Nullable Hl7HierarchicDesignator assigningAuthorityForLocation) {
		this.assigningAuthorityForLocation = assigningAuthorityForLocation;
	}
}