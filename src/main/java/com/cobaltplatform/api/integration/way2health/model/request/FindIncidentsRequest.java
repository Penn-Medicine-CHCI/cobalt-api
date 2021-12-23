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

package com.cobaltplatform.api.integration.way2health.model.request;

import com.cobaltplatform.api.integration.way2health.Way2HealthGsonSupport;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigInteger;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class FindIncidentsRequest {
	@Nullable
	private BigInteger studyId;
	@Nullable
	private Integer page;
	@Nullable
	private Integer perPage;
	@Nullable
	private List<String> include;
	@Nullable
	private String status;
	@Nullable
	private String type;
	@Nullable
	private String participantId;
	@Nullable
	private String participantMrn;
	@Nullable
	private String participantMrnType;
	@Nullable
	private String orderBy;
	@Nullable
	private String groupBy;

	@Override
	public String toString() {
		return Way2HealthGsonSupport.sharedGson().toJson(this);
	}

	@Nullable
	public BigInteger getStudyId() {
		return studyId;
	}

	public void setStudyId(@Nullable BigInteger studyId) {
		this.studyId = studyId;
	}

	@Nullable
	public Integer getPage() {
		return page;
	}

	public void setPage(@Nullable Integer page) {
		this.page = page;
	}

	@Nullable
	public Integer getPerPage() {
		return perPage;
	}

	public void setPerPage(@Nullable Integer perPage) {
		this.perPage = perPage;
	}

	@Nullable
	public List<String> getInclude() {
		return include;
	}

	public void setInclude(@Nullable List<String> include) {
		this.include = include;
	}

	@Nullable
	public String getStatus() {
		return status;
	}

	public void setStatus(@Nullable String status) {
		this.status = status;
	}

	@Nullable
	public String getType() {
		return type;
	}

	public void setType(@Nullable String type) {
		this.type = type;
	}

	@Nullable
	public String getParticipantId() {
		return participantId;
	}

	public void setParticipantId(@Nullable String participantId) {
		this.participantId = participantId;
	}

	@Nullable
	public String getParticipantMrn() {
		return participantMrn;
	}

	public void setParticipantMrn(@Nullable String participantMrn) {
		this.participantMrn = participantMrn;
	}

	@Nullable
	public String getParticipantMrnType() {
		return participantMrnType;
	}

	public void setParticipantMrnType(@Nullable String participantMrnType) {
		this.participantMrnType = participantMrnType;
	}

	@Nullable
	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(@Nullable String orderBy) {
		this.orderBy = orderBy;
	}

	@Nullable
	public String getGroupBy() {
		return groupBy;
	}

	public void setGroupBy(@Nullable String groupBy) {
		this.groupBy = groupBy;
	}
}
