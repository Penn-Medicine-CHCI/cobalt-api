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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.db.SystemAffinity.SystemAffinityId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ProviderFindRequest {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UUID providerId; // If provided, other values are ignored!
	@Nullable
	private LocalDate startDate; // Null means "don't limit start"
	@Nullable
	private LocalDate endDate; // Null means "don't limit end"
	@Nullable
	private Set<DayOfWeek> daysOfWeek; // Empty means "any"
	@Nullable
	private LocalTime startTime; // Null means "don't limit start"
	@Nullable
	private LocalTime endTime; // Null means "don't limit end"
	@Nullable
	private ProviderFindAvailability availability; // Null means "ALL"
	@Nullable
	private Set<SupportRoleId> supportRoleIds; // Empty means "any"
	@Nullable
	private Set<String> paymentTypeIds; // Empty means "any"
	@Nullable
	private Set<UUID> clinicIds; // Empty means "any"
	@Nullable
	private Set<VisitTypeId> visitTypeIds; // Empty means "any"
	@Nullable
	private Set<ProviderFindSupplement> supplements; // Instructions to include extra data in response
	@Nullable
	private Set<ProviderFindLicenseType> licenseTypes; // Empty means "any"
	@Nullable
	private SystemAffinityId systemAffinityId; // Null means "COBALT"
	@Nullable
	private Set<UUID> specialtyIds; // Empty means "any"
	@Nullable
	private Boolean includePastAvailability; // Provide availability for date ranges that are in the past, e.g. for reporting. Null means "don't provide"
	@Nullable
	private UUID institutionLocationId;

	public enum ProviderFindAvailability {
		ALL,
		ONLY_AVAILABLE
	}

	public enum ProviderFindSupplement {
		APPOINTMENTS,
		FOLLOWUPS
	}

	public enum ProviderFindLicenseType {
		LCSW
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(@Nullable LocalDate startDate) {
		this.startDate = startDate;
	}

	@Nullable
	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(@Nullable LocalDate endDate) {
		this.endDate = endDate;
	}

	@Nullable
	public Set<DayOfWeek> getDaysOfWeek() {
		return daysOfWeek;
	}

	public void setDaysOfWeek(@Nullable Set<DayOfWeek> daysOfWeek) {
		this.daysOfWeek = daysOfWeek;
	}

	@Nullable
	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(@Nullable LocalTime startTime) {
		this.startTime = startTime;
	}

	@Nullable
	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(@Nullable LocalTime endTime) {
		this.endTime = endTime;
	}

	@Nullable
	public ProviderFindAvailability getAvailability() {
		return availability;
	}

	public void setAvailability(@Nullable ProviderFindAvailability availability) {
		this.availability = availability;
	}

	@Nullable
	public Set<SupportRoleId> getSupportRoleIds() {
		return supportRoleIds;
	}

	public void setSupportRoleIds(@Nullable Set<SupportRoleId> supportRoleIds) {
		this.supportRoleIds = supportRoleIds;
	}

	@Nullable
	public Set<String> getPaymentTypeIds() {
		return paymentTypeIds;
	}

	public void setPaymentTypeIds(@Nullable Set<String> paymentTypeIds) {
		this.paymentTypeIds = paymentTypeIds;
	}

	@Nullable
	public Set<UUID> getClinicIds() {
		return clinicIds;
	}

	public void setClinicIds(@Nullable Set<UUID> clinicIds) {
		this.clinicIds = clinicIds;
	}

	@Nullable
	public Set<VisitTypeId> getVisitTypeIds() {
		return visitTypeIds;
	}

	public void setVisitTypeIds(@Nullable Set<VisitTypeId> visitTypeIds) {
		this.visitTypeIds = visitTypeIds;
	}

	@Nullable
	public Set<ProviderFindSupplement> getSupplements() {
		return supplements;
	}

	public void setSupplements(@Nullable Set<ProviderFindSupplement> supplements) {
		this.supplements = supplements;
	}

	@Nullable
	public Set<ProviderFindLicenseType> getLicenseTypes() {
		return licenseTypes;
	}

	public void setLicenseTypes(@Nullable Set<ProviderFindLicenseType> licenseTypes) {
		this.licenseTypes = licenseTypes;
	}

	@Nullable
	public SystemAffinityId getSystemAffinityId() {
		return systemAffinityId;
	}

	public void setSystemAffinityId(@Nullable SystemAffinityId systemAffinityId) {
		this.systemAffinityId = systemAffinityId;
	}

	@Nullable
	public Set<UUID> getSpecialtyIds() {
		return specialtyIds;
	}

	public void setSpecialtyIds(@Nullable Set<UUID> specialtyIds) {
		this.specialtyIds = specialtyIds;
	}

	@Nullable
	public Boolean getIncludePastAvailability() {
		return this.includePastAvailability;
	}

	public void setIncludePastAvailability(@Nullable Boolean includePastAvailability) {
		this.includePastAvailability = includePastAvailability;
	}

	@Nullable
	public UUID getInstitutionLocationId() {
		return institutionLocationId;
	}

	public void setInstitutionLocationId(@Nullable UUID institutionLocationId) {
		this.institutionLocationId = institutionLocationId;
	}
}
