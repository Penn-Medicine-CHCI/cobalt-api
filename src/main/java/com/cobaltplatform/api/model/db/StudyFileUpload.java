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
public class StudyFileUpload {
	// Fields are from v_study_file_upload
	@Nullable
	private InstitutionId institutionId;
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
	private UUID accountStudyId;
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
	public UUID getAccountStudyId() {
		return this.accountStudyId;
	}

	public void setAccountStudyId(@Nullable UUID accountStudyId) {
		this.accountStudyId = accountStudyId;
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