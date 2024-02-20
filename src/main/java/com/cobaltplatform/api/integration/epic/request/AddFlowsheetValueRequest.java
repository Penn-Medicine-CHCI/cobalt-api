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

package com.cobaltplatform.api.integration.epic.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AddFlowsheetValueRequest {
	@Nullable
	private String PatientID; // e.g. 8643042971
	@Nullable
	private String PatientIDType; // e.g. UID
	@Nullable
	private String ContactID; // e.g. 100010559 (a CSN)
	@Nullable
	private String ContactIDType; // e.g. CSN
	@Nullable
	private String FlowsheetID; // e.g. 1023700050
	@Nullable
	private String FlowsheetIDType; // e.g. INTERNAL
	@Nullable
	private String UserID; // e.g. COBALT
	@Nullable
	private String UserIDType; // e.g. EXTERNAL
	@Nullable
	private String Value; // e.g. 14.3
	@Nullable
	private String Comment; // e.g. Freeform text
	@Nullable
	private Instant InstantValueToken; // e.g. "2016-03-11T09:30:14Z"
	@Nullable
	private String FlowsheetTemplateID; // e.g. 1023700051
	@Nullable
	private String FlowsheetTemplateIDType; // e.g. INTERNAL

	@Nullable
	public String getPatientID() {
		return this.PatientID;
	}

	public void setPatientID(@Nullable String patientID) {
		PatientID = patientID;
	}

	@Nullable
	public String getPatientIDType() {
		return this.PatientIDType;
	}

	public void setPatientIDType(@Nullable String patientIDType) {
		PatientIDType = patientIDType;
	}

	@Nullable
	public String getContactID() {
		return this.ContactID;
	}

	public void setContactID(@Nullable String contactID) {
		ContactID = contactID;
	}

	@Nullable
	public String getContactIDType() {
		return this.ContactIDType;
	}

	public void setContactIDType(@Nullable String contactIDType) {
		ContactIDType = contactIDType;
	}

	@Nullable
	public String getFlowsheetID() {
		return this.FlowsheetID;
	}

	public void setFlowsheetID(@Nullable String flowsheetID) {
		FlowsheetID = flowsheetID;
	}

	@Nullable
	public String getUserID() {
		return this.UserID;
	}

	public void setUserID(@Nullable String userID) {
		UserID = userID;
	}

	@Nullable
	public String getUserIDType() {
		return this.UserIDType;
	}

	public void setUserIDType(@Nullable String userIDType) {
		UserIDType = userIDType;
	}

	@Nullable
	public String getFlowsheetIDType() {
		return this.FlowsheetIDType;
	}

	public void setFlowsheetIDType(@Nullable String flowsheetIDType) {
		FlowsheetIDType = flowsheetIDType;
	}

	@Nullable
	public String getValue() {
		return this.Value;
	}

	public void setValue(@Nullable String value) {
		Value = value;
	}

	@Nullable
	public String getComment() {
		return this.Comment;
	}

	public void setComment(@Nullable String comment) {
		Comment = comment;
	}

	@Nullable
	public Instant getInstantValueToken() {
		return this.InstantValueToken;
	}

	public void setInstantValueToken(@Nullable Instant instantValueToken) {
		InstantValueToken = instantValueToken;
	}

	@Nullable
	public String getFlowsheetTemplateID() {
		return this.FlowsheetTemplateID;
	}

	public void setFlowsheetTemplateID(@Nullable String flowsheetTemplateID) {
		FlowsheetTemplateID = flowsheetTemplateID;
	}

	@Nullable
	public String getFlowsheetTemplateIDType() {
		return this.FlowsheetTemplateIDType;
	}

	public void setFlowsheetTemplateIDType(@Nullable String flowsheetTemplateIDType) {
		FlowsheetTemplateIDType = flowsheetTemplateIDType;
	}
}
