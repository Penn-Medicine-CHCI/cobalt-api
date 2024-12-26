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

package com.cobaltplatform.api.model.db;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ResourcePacket {
	@Nullable
	private UUID resourcePacketId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID addressId;
	@Nullable
	private Integer travelRadius;
	@Nullable
	private String travelRadiusDistanceUnitId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getResourcePacketId() {
		return resourcePacketId;
	}

	public void setResourcePacketId(@Nullable UUID resourcePacketId) {
		this.resourcePacketId = resourcePacketId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public UUID getAddressId() {
		return addressId;
	}

	public void setAddressId(@Nullable UUID addressId) {
		this.addressId = addressId;
	}

	@Nullable
	public Integer getTravelRadius() {
		return travelRadius;
	}

	public void setTravelRadius(@Nullable Integer travelRadius) {
		this.travelRadius = travelRadius;
	}

	@Nullable
	public String getTravelRadiusDistanceUnitId() {
		return travelRadiusDistanceUnitId;
	}

	public void setTravelRadiusDistanceUnitId(@Nullable String travelRadiusDistanceUnitId) {
		this.travelRadiusDistanceUnitId = travelRadiusDistanceUnitId;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}