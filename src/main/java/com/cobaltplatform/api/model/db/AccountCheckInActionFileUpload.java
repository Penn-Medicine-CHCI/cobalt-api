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

import com.cobaltplatform.api.model.db.CheckInActionStatus.CheckInActionStatusId;
import com.cobaltplatform.api.model.db.CheckInStatus.CheckInStatusId;
import com.cobaltplatform.api.model.db.FileUploadType.FileUploadTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;


/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AccountCheckInActionFileUpload {
	// Fields are from v_account_check_in_action_file_upload
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UUID studyCheckInId;
	@Nullable
	private Integer studyCheckInNumber;
	@Nullable
	private UUID accountCheckInId;
	@Nullable
	private UUID accountStudyId;
	@Nullable
	private Instant accountCheckInStartDateTime;
	@Nullable
	private Instant accountCheckInEndDateTime;
	@Nullable
	private CheckInStatusId accountCheckInStatusId;
	@Nullable
	private Instant accountCheckInCompletedDate;
	@Nullable
	private UUID studyId;
	@Nullable
	private String studyName;
	@Nullable
	private String studyUrlName;
	@Nullable
	private UUID accountId;
	@Nullable
	private String accountUsername;
	@Nullable
	private UUID accountCheckInActionId;
	@Nullable
	private UUID studyCheckInActionId;
	@Nullable
	private CheckInActionStatusId accountCheckInActionStatusId;
	@Nullable
	private Instant accountCheckInActionCreated;
	@Nullable
	private Instant accountCheckInActionLastUpdated;
	@Nullable
	private UUID fileUploadId;
	@Nullable
	private FileUploadTypeId fileUploadTypeId;
	@Nullable
	private String fileUploadUrl;
	@Nullable
	private String fileUploadStorageKey;
	@Nullable
	private String fileUploadFilename;
	@Nullable
	private String fileUploadContentType;
	@Nullable
	private Long fileUploadFilesize;
	@Nullable
	private Instant fileUploadCreated;

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getStudyCheckInId() {
		return this.studyCheckInId;
	}

	public void setStudyCheckInId(@Nullable UUID studyCheckInId) {
		this.studyCheckInId = studyCheckInId;
	}

	@Nullable
	public Integer getStudyCheckInNumber() {
		return this.studyCheckInNumber;
	}

	public void setStudyCheckInNumber(@Nullable Integer studyCheckInNumber) {
		this.studyCheckInNumber = studyCheckInNumber;
	}

	@Nullable
	public UUID getAccountCheckInId() {
		return this.accountCheckInId;
	}

	public void setAccountCheckInId(@Nullable UUID accountCheckInId) {
		this.accountCheckInId = accountCheckInId;
	}

	@Nullable
	public UUID getAccountStudyId() {
		return this.accountStudyId;
	}

	public void setAccountStudyId(@Nullable UUID accountStudyId) {
		this.accountStudyId = accountStudyId;
	}

	@Nullable
	public Instant getAccountCheckInStartDateTime() {
		return this.accountCheckInStartDateTime;
	}

	public void setAccountCheckInStartDateTime(@Nullable Instant accountCheckInStartDateTime) {
		this.accountCheckInStartDateTime = accountCheckInStartDateTime;
	}

	@Nullable
	public Instant getAccountCheckInEndDateTime() {
		return this.accountCheckInEndDateTime;
	}

	public void setAccountCheckInEndDateTime(@Nullable Instant accountCheckInEndDateTime) {
		this.accountCheckInEndDateTime = accountCheckInEndDateTime;
	}

	@Nullable
	public CheckInStatusId getAccountCheckInStatusId() {
		return this.accountCheckInStatusId;
	}

	public void setAccountCheckInStatusId(@Nullable CheckInStatusId accountCheckInStatusId) {
		this.accountCheckInStatusId = accountCheckInStatusId;
	}

	@Nullable
	public Instant getAccountCheckInCompletedDate() {
		return this.accountCheckInCompletedDate;
	}

	public void setAccountCheckInCompletedDate(@Nullable Instant accountCheckInCompletedDate) {
		this.accountCheckInCompletedDate = accountCheckInCompletedDate;
	}

	@Nullable
	public UUID getStudyId() {
		return this.studyId;
	}

	public void setStudyId(@Nullable UUID studyId) {
		this.studyId = studyId;
	}

	@Nullable
	public String getStudyName() {
		return this.studyName;
	}

	public void setStudyName(@Nullable String studyName) {
		this.studyName = studyName;
	}

	@Nullable
	public String getStudyUrlName() {
		return this.studyUrlName;
	}

	public void setStudyUrlName(@Nullable String studyUrlName) {
		this.studyUrlName = studyUrlName;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getAccountUsername() {
		return this.accountUsername;
	}

	public void setAccountUsername(@Nullable String accountUsername) {
		this.accountUsername = accountUsername;
	}

	@Nullable
	public UUID getAccountCheckInActionId() {
		return this.accountCheckInActionId;
	}

	public void setAccountCheckInActionId(@Nullable UUID accountCheckInActionId) {
		this.accountCheckInActionId = accountCheckInActionId;
	}

	@Nullable
	public UUID getStudyCheckInActionId() {
		return this.studyCheckInActionId;
	}

	public void setStudyCheckInActionId(@Nullable UUID studyCheckInActionId) {
		this.studyCheckInActionId = studyCheckInActionId;
	}

	@Nullable
	public CheckInActionStatusId getAccountCheckInActionStatusId() {
		return this.accountCheckInActionStatusId;
	}

	public void setAccountCheckInActionStatusId(@Nullable CheckInActionStatusId accountCheckInActionStatusId) {
		this.accountCheckInActionStatusId = accountCheckInActionStatusId;
	}

	@Nullable
	public Instant getAccountCheckInActionCreated() {
		return this.accountCheckInActionCreated;
	}

	public void setAccountCheckInActionCreated(@Nullable Instant accountCheckInActionCreated) {
		this.accountCheckInActionCreated = accountCheckInActionCreated;
	}

	@Nullable
	public Instant getAccountCheckInActionLastUpdated() {
		return this.accountCheckInActionLastUpdated;
	}

	public void setAccountCheckInActionLastUpdated(@Nullable Instant accountCheckInActionLastUpdated) {
		this.accountCheckInActionLastUpdated = accountCheckInActionLastUpdated;
	}

	@Nullable
	public UUID getFileUploadId() {
		return this.fileUploadId;
	}

	public void setFileUploadId(@Nullable UUID fileUploadId) {
		this.fileUploadId = fileUploadId;
	}

	@Nullable
	public FileUploadTypeId getFileUploadTypeId() {
		return this.fileUploadTypeId;
	}

	public void setFileUploadTypeId(@Nullable FileUploadTypeId fileUploadTypeId) {
		this.fileUploadTypeId = fileUploadTypeId;
	}

	@Nullable
	public String getFileUploadUrl() {
		return this.fileUploadUrl;
	}

	public void setFileUploadUrl(@Nullable String fileUploadUrl) {
		this.fileUploadUrl = fileUploadUrl;
	}

	@Nullable
	public String getFileUploadStorageKey() {
		return this.fileUploadStorageKey;
	}

	public void setFileUploadStorageKey(@Nullable String fileUploadStorageKey) {
		this.fileUploadStorageKey = fileUploadStorageKey;
	}

	@Nullable
	public String getFileUploadFilename() {
		return this.fileUploadFilename;
	}

	public void setFileUploadFilename(@Nullable String fileUploadFilename) {
		this.fileUploadFilename = fileUploadFilename;
	}

	@Nullable
	public String getFileUploadContentType() {
		return this.fileUploadContentType;
	}

	public void setFileUploadContentType(@Nullable String fileUploadContentType) {
		this.fileUploadContentType = fileUploadContentType;
	}

	@Nullable
	public Long getFileUploadFilesize() {
		return this.fileUploadFilesize;
	}

	public void setFileUploadFilesize(@Nullable Long fileUploadFilesize) {
		this.fileUploadFilesize = fileUploadFilesize;
	}

	@Nullable
	public Instant getFileUploadCreated() {
		return this.fileUploadCreated;
	}

	public void setFileUploadCreated(@Nullable Instant fileUploadCreated) {
		this.fileUploadCreated = fileUploadCreated;
	}
}