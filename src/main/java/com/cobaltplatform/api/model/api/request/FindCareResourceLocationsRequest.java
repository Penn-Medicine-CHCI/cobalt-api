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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.Set;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class FindCareResourceLocationsRequest {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private Integer pageNumber;
	@Nullable
	private Integer pageSize;
	@Nullable
	private OrderBy orderBy;
	@Nullable
	private String search;
	@Nullable
	private Boolean wheelchairAccess;
	@Nullable
	private Integer searchRadiusMiles;
	@Nullable
	private Set<String> payorIds;
	@Nullable
	private Set<String> specialtyIds;
	@Nullable
	private Set<String> therapyTypeIds;
	@Nullable
	private Set<String> populationServedIds;
	@Nullable
	private Set<String> genderIds;
	@Nullable
	private Set<String> ethnicityIds;
	@Nullable
	private Set<String> languageIds;
	@Nullable
	private Set<String> facilityTypes;

	public enum OrderBy {
		NAME_ASC,
		NAME_DESC
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public Integer getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(@Nullable Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	@Nullable
	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(@Nullable Integer pageSize) {
		this.pageSize = pageSize;
	}

	@Nullable
	public OrderBy getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(@Nullable OrderBy orderBy) {
		this.orderBy = orderBy;
	}

	@Nullable
	public String getSearch() {
		return search;
	}

	public void setSearch(@Nullable String search) {
		this.search = search;
	}

	@Nullable
	public Boolean getWheelchairAccess() {
		return wheelchairAccess;
	}

	public void setWheelchairAccess(@Nullable Boolean wheelchairAccess) {
		this.wheelchairAccess = wheelchairAccess;
	}

	@Nullable
	public Set<String> getPayorIds() {
		return payorIds;
	}

	public void setPayorIds(@Nullable Set<String> payorIds) {
		this.payorIds = payorIds;
	}

	@Nullable
	public Set<String> getSpecialtyIds() {
		return specialtyIds;
	}

	public void setSpecialtyIds(@Nullable Set<String> specialtyIds) {
		this.specialtyIds = specialtyIds;
	}

	@Nullable
	public Set<String> getTherapyTypeIds() {
		return therapyTypeIds;
	}

	public void setTherapyTypeIds(@Nullable Set<String> therapyTypeIds) {
		this.therapyTypeIds = therapyTypeIds;
	}

	@Nullable
	public Set<String> getPopulationServedIds() {
		return populationServedIds;
	}

	public void setPopulationServedIds(@Nullable Set<String> populationServedIds) {
		this.populationServedIds = populationServedIds;
	}

	@Nullable
	public Set<String> getGenderIds() {
		return genderIds;
	}

	public void setGenderIds(@Nullable Set<String> genderIds) {
		this.genderIds = genderIds;
	}

	@Nullable
	public Set<String> getEthnicityIds() {
		return ethnicityIds;
	}

	public void setEthnicityIds(@Nullable Set<String> ethnicityIds) {
		this.ethnicityIds = ethnicityIds;
	}

	@Nullable
	public Set<String> getLanguageIds() {
		return languageIds;
	}

	public void setLanguageIds(@Nullable Set<String> languageIds) {
		this.languageIds = languageIds;
	}

	@Nullable
	public Set<String> getFacilityTypes() {
		return facilityTypes;
	}

	public void setFacilityTypes(@Nullable Set<String> facilityTypes) {
		this.facilityTypes = facilityTypes;
	}

	@Nullable
	public Integer getSearchRadiusMiles() {
		return searchRadiusMiles;
	}

	public void setSearchRadiusMiles(@Nullable Integer searchRadiusMiles) {
		this.searchRadiusMiles = searchRadiusMiles;
	}
}