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

import com.cobaltplatform.api.model.db.PatientOrderCareType.PatientOrderCareTypeId;
import com.cobaltplatform.api.model.db.PatientOrderIntakeScreeningStatus.PatientOrderIntakeScreeningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderOutreachType.PatientOrderOutreachTypeId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledOutreachReason.PatientOrderScheduledOutreachReasonId;
import com.cobaltplatform.api.model.db.PatientOrderScreeningStatus.PatientOrderScreeningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderTriageSource.PatientOrderTriageSourceId;
import com.cobaltplatform.api.model.db.PatientOrderTriageStatus.PatientOrderTriageStatusId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.service.PatientOrderContactTypeId;
import com.cobaltplatform.api.model.service.PatientOrderEncounterDocumentationStatusId;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrder extends RawPatientOrder {
	// See RawPatientOrder for only those fields in patient_order
	// This type is RawPatientOrder + fields included in v_patient_order
	@Nullable
	private PatientOrderScreeningStatusId patientOrderScreeningStatusId;
	@Nullable
	private PatientOrderCareTypeId patientOrderCareTypeId;
	@Nullable
	private String patientOrderCareTypeDescription;
	@Nullable
	private Integer totalOutreachCount;
	@Nullable
	private LocalDateTime mostRecentTotalOutreachDateTime;
	@Nullable
	private Integer outreachCount;
	@Nullable
	private LocalDateTime mostRecentOutreachDateTime;
	@Nullable
	private Integer scheduledMessageGroupDeliveredCount;
	@Nullable
	private LocalDateTime mostRecentDeliveredScheduledMessageGroupDateTime;
	@Nullable
	private UUID mostRecentScreeningSessionId;
	@Nullable
	private Instant mostRecentScreeningSessionCreatedAt;
	@Nullable
	private UUID mostRecentScreeningSessionCreatedByAccountId;
	@Nullable
	private RoleId mostRecentScreeningSessionCreatedByAccountRoleId;
	@Nullable
	private String mostRecentScreeningSessionCreatedByAccountFirstName;
	@Nullable
	private String mostRecentScreeningSessionCreatedByAccountLastName;
	@Nullable
	private Boolean mostRecentScreeningSessionCompleted;
	@Nullable
	private Instant mostRecentScreeningSessionCompletedAt;
	@Nullable
	private Boolean mostRecentScreeningSessionAppearsAbandoned;
	@Nullable
	private UUID mostRecentIntakeScreeningSessionId;
	@Nullable
	private Instant mostRecentIntakeScreeningSessionCreatedAt;
	@Nullable
	private UUID mostRecentIntakeScreeningSessionCreatedByAccountId;
	@Nullable
	private RoleId mostRecentIntakeScreeningSessionCreatedByAccountRoleId;
	@Nullable
	@DatabaseColumn("most_recent_intake_screening_session_created_by_account_fn")
	private String mostRecentIntakeScreeningSessionCreatedByAccountFirstName;
	@Nullable
	@DatabaseColumn("most_recent_intake_screening_session_created_by_account_ln")
	private String mostRecentIntakeScreeningSessionCreatedByAccountLastName;
	@Nullable
	private Boolean mostRecentIntakeScreeningSessionCompleted;
	@Nullable
	private Instant mostRecentIntakeScreeningSessionCompletedAt;
	@Nullable
	private Boolean mostRecentIntakeScreeningSessionByPatient;
	@Nullable
	private Boolean mostRecentIntakeScreeningSessionAppearsAbandoned;
	@Nullable
	private PatientOrderIntakeScreeningStatusId patientOrderIntakeScreeningStatusId;
	@Nullable
	private String patientOrderIntakeScreeningStatusDescription;
	@Nullable
	private String panelAccountFirstName;
	@Nullable
	private String panelAccountLastName;
	@Nullable
	private String patientOrderScreeningStatusDescription;
	@Nullable
	private String patientOrderDispositionDescription;
	@Nullable
	private String patientOrderTriageStatusDescription;
	@Nullable
	private String patientOrderClosureReasonDescription;
	@Nullable
	private Integer patientAgeOnOrderDate;
	@Nullable
	private Boolean patientBelowAgeThreshold;
	@Nullable
	private Instant mostRecentEpisodeClosedAt;
	@Nullable
	private Boolean mostRecentEpisodeClosedWithinDateThreshold;
	@Nullable
	private UUID patientOrderScheduledScreeningId;
	@Nullable
	private LocalDateTime patientOrderScheduledScreeningScheduledDateTime;
	@Nullable
	private String patientOrderScheduledScreeningCalendarUrl;
	@Nullable
	private UUID appointmentId;
	@Nullable
	private LocalDateTime appointmentStartTime;
	@Nullable
	private UUID providerId;
	@Nullable
	private String providerName;
	@Nullable
	private UUID mostRecentPatientOrderVoicemailTaskId;
	@Nullable
	private Boolean mostRecentPatientOrderVoicemailTaskCompleted;
	@Nullable
	private String reasonForReferral;
	@Nullable
	@DatabaseColumn("patient_address_street_address_1")
	private String patientAddressStreetAddress1;
	@Nullable
	private String patientAddressLocality;
	@Nullable
	private String patientAddressRegion;
	@Nullable
	private String patientAddressPostalCode;
	@Nullable
	private String patientAddressCountryCode;
	@Nullable
	private Boolean patientAddressRegionAccepted;
	@Nullable
	private Boolean patientDemographicsCompleted;
	@Nullable
	private Boolean patientDemographicsAccepted;
	@Nullable
	private LocalDateTime resourceCheckInScheduledAtDateTime;
	@Nullable
	private String patientOrderResourceCheckInResponseStatusDescription;
	@Nullable
	private Boolean resourceCheckInResponseNeeded;
	@Nullable
	private Boolean mostRecentScreeningSessionByPatient;
	@Nullable
	private Boolean appointmentScheduledByPatient;
	@Nullable
	private PatientOrderTriageSourceId patientOrderTriageSourceId;
	@Nullable
	private String patientOrderTriageReason;
	@Nullable
	private Boolean appointmentScheduled;
	@Nullable
	private Boolean testPatientOrder;
	@Nullable
	private Integer episodeDurationInDays;
	@Nullable
	private Boolean mostRecentIntakeAndClinicalScreeningsSatisfied;
	@Nullable
	private String epicDepartmentName;
	@Nullable
	private String epicDepartmentDepartmentId;
	@Nullable
	private PatientOrderEncounterDocumentationStatusId patientOrderEncounterDocumentationStatusId;
	@Nullable
	private UUID nextScheduledOutreachId;
	@Nullable
	private LocalDateTime nextScheduledOutreachScheduledAtDateTime;
	@Nullable
	private PatientOrderOutreachTypeId nextScheduledOutreachTypeId;
	@Nullable
	private PatientOrderScheduledOutreachReasonId nextScheduledOutreachReasonId;
	@Nullable
	private Instant lastContactedAt;
	@Nullable
	private PatientOrderContactTypeId nextContactTypeId;
	@Nullable
	private PatientOrderTriageStatusId patientOrderTriageStatusId;
	@Nullable
	private LocalDateTime nextContactScheduledAt;
	@Nullable
	private Instant mostRecentMessageDeliveredAt;
	@Nullable
	private Boolean outreachFollowupNeeded;
	@Nullable
	private Boolean patientDemographicsConfirmed;

	@Nullable
	public PatientOrderScreeningStatusId getPatientOrderScreeningStatusId() {
		return this.patientOrderScreeningStatusId;
	}

	public void setPatientOrderScreeningStatusId(@Nullable PatientOrderScreeningStatusId patientOrderScreeningStatusId) {
		this.patientOrderScreeningStatusId = patientOrderScreeningStatusId;
	}

	@Nullable
	public PatientOrderCareTypeId getPatientOrderCareTypeId() {
		return this.patientOrderCareTypeId;
	}

	public void setPatientOrderCareTypeId(@Nullable PatientOrderCareTypeId patientOrderCareTypeId) {
		this.patientOrderCareTypeId = patientOrderCareTypeId;
	}

	@Nullable
	public String getPatientOrderCareTypeDescription() {
		return this.patientOrderCareTypeDescription;
	}

	public void setPatientOrderCareTypeDescription(@Nullable String patientOrderCareTypeDescription) {
		this.patientOrderCareTypeDescription = patientOrderCareTypeDescription;
	}

	@Nullable
	public Integer getTotalOutreachCount() {
		return this.totalOutreachCount;
	}

	public void setTotalOutreachCount(@Nullable Integer totalOutreachCount) {
		this.totalOutreachCount = totalOutreachCount;
	}

	@Nullable
	public LocalDateTime getMostRecentTotalOutreachDateTime() {
		return this.mostRecentTotalOutreachDateTime;
	}

	public void setMostRecentTotalOutreachDateTime(@Nullable LocalDateTime mostRecentTotalOutreachDateTime) {
		this.mostRecentTotalOutreachDateTime = mostRecentTotalOutreachDateTime;
	}

	@Nullable
	public Integer getOutreachCount() {
		return this.outreachCount;
	}

	public void setOutreachCount(@Nullable Integer outreachCount) {
		this.outreachCount = outreachCount;
	}

	@Nullable
	public LocalDateTime getMostRecentOutreachDateTime() {
		return this.mostRecentOutreachDateTime;
	}

	public void setMostRecentOutreachDateTime(@Nullable LocalDateTime mostRecentOutreachDateTime) {
		this.mostRecentOutreachDateTime = mostRecentOutreachDateTime;
	}

	@Nullable
	public Integer getScheduledMessageGroupDeliveredCount() {
		return this.scheduledMessageGroupDeliveredCount;
	}

	public void setScheduledMessageGroupDeliveredCount(@Nullable Integer scheduledMessageGroupDeliveredCount) {
		this.scheduledMessageGroupDeliveredCount = scheduledMessageGroupDeliveredCount;
	}

	@Nullable
	public LocalDateTime getMostRecentDeliveredScheduledMessageGroupDateTime() {
		return this.mostRecentDeliveredScheduledMessageGroupDateTime;
	}

	public void setMostRecentDeliveredScheduledMessageGroupDateTime(@Nullable LocalDateTime mostRecentDeliveredScheduledMessageGroupDateTime) {
		this.mostRecentDeliveredScheduledMessageGroupDateTime = mostRecentDeliveredScheduledMessageGroupDateTime;
	}

	@Nullable
	public UUID getMostRecentScreeningSessionId() {
		return this.mostRecentScreeningSessionId;
	}

	public void setMostRecentScreeningSessionId(@Nullable UUID mostRecentScreeningSessionId) {
		this.mostRecentScreeningSessionId = mostRecentScreeningSessionId;
	}

	@Nullable
	public Instant getMostRecentScreeningSessionCreatedAt() {
		return this.mostRecentScreeningSessionCreatedAt;
	}

	public void setMostRecentScreeningSessionCreatedAt(@Nullable Instant mostRecentScreeningSessionCreatedAt) {
		this.mostRecentScreeningSessionCreatedAt = mostRecentScreeningSessionCreatedAt;
	}

	@Nullable
	public UUID getMostRecentScreeningSessionCreatedByAccountId() {
		return this.mostRecentScreeningSessionCreatedByAccountId;
	}

	public void setMostRecentScreeningSessionCreatedByAccountId(@Nullable UUID mostRecentScreeningSessionCreatedByAccountId) {
		this.mostRecentScreeningSessionCreatedByAccountId = mostRecentScreeningSessionCreatedByAccountId;
	}

	@Nullable
	public RoleId getMostRecentScreeningSessionCreatedByAccountRoleId() {
		return this.mostRecentScreeningSessionCreatedByAccountRoleId;
	}

	public void setMostRecentScreeningSessionCreatedByAccountRoleId(@Nullable RoleId mostRecentScreeningSessionCreatedByAccountRoleId) {
		this.mostRecentScreeningSessionCreatedByAccountRoleId = mostRecentScreeningSessionCreatedByAccountRoleId;
	}

	@Nullable
	public String getMostRecentScreeningSessionCreatedByAccountFirstName() {
		return this.mostRecentScreeningSessionCreatedByAccountFirstName;
	}

	public void setMostRecentScreeningSessionCreatedByAccountFirstName(@Nullable String mostRecentScreeningSessionCreatedByAccountFirstName) {
		this.mostRecentScreeningSessionCreatedByAccountFirstName = mostRecentScreeningSessionCreatedByAccountFirstName;
	}

	@Nullable
	public String getMostRecentScreeningSessionCreatedByAccountLastName() {
		return this.mostRecentScreeningSessionCreatedByAccountLastName;
	}

	public void setMostRecentScreeningSessionCreatedByAccountLastName(@Nullable String mostRecentScreeningSessionCreatedByAccountLastName) {
		this.mostRecentScreeningSessionCreatedByAccountLastName = mostRecentScreeningSessionCreatedByAccountLastName;
	}

	@Nullable
	public Boolean getMostRecentScreeningSessionCompleted() {
		return this.mostRecentScreeningSessionCompleted;
	}

	public void setMostRecentScreeningSessionCompleted(@Nullable Boolean mostRecentScreeningSessionCompleted) {
		this.mostRecentScreeningSessionCompleted = mostRecentScreeningSessionCompleted;
	}

	@Nullable
	public Instant getMostRecentScreeningSessionCompletedAt() {
		return this.mostRecentScreeningSessionCompletedAt;
	}

	public void setMostRecentScreeningSessionCompletedAt(@Nullable Instant mostRecentScreeningSessionCompletedAt) {
		this.mostRecentScreeningSessionCompletedAt = mostRecentScreeningSessionCompletedAt;
	}

	@Nullable
	public Boolean getMostRecentScreeningSessionAppearsAbandoned() {
		return this.mostRecentScreeningSessionAppearsAbandoned;
	}

	public void setMostRecentScreeningSessionAppearsAbandoned(@Nullable Boolean mostRecentScreeningSessionAppearsAbandoned) {
		this.mostRecentScreeningSessionAppearsAbandoned = mostRecentScreeningSessionAppearsAbandoned;
	}

	@Nullable
	public UUID getMostRecentIntakeScreeningSessionId() {
		return this.mostRecentIntakeScreeningSessionId;
	}

	public void setMostRecentIntakeScreeningSessionId(@Nullable UUID mostRecentIntakeScreeningSessionId) {
		this.mostRecentIntakeScreeningSessionId = mostRecentIntakeScreeningSessionId;
	}

	@Nullable
	public Instant getMostRecentIntakeScreeningSessionCreatedAt() {
		return this.mostRecentIntakeScreeningSessionCreatedAt;
	}

	public void setMostRecentIntakeScreeningSessionCreatedAt(@Nullable Instant mostRecentIntakeScreeningSessionCreatedAt) {
		this.mostRecentIntakeScreeningSessionCreatedAt = mostRecentIntakeScreeningSessionCreatedAt;
	}

	@Nullable
	public UUID getMostRecentIntakeScreeningSessionCreatedByAccountId() {
		return this.mostRecentIntakeScreeningSessionCreatedByAccountId;
	}

	public void setMostRecentIntakeScreeningSessionCreatedByAccountId(@Nullable UUID mostRecentIntakeScreeningSessionCreatedByAccountId) {
		this.mostRecentIntakeScreeningSessionCreatedByAccountId = mostRecentIntakeScreeningSessionCreatedByAccountId;
	}

	@Nullable
	public RoleId getMostRecentIntakeScreeningSessionCreatedByAccountRoleId() {
		return this.mostRecentIntakeScreeningSessionCreatedByAccountRoleId;
	}

	public void setMostRecentIntakeScreeningSessionCreatedByAccountRoleId(@Nullable RoleId mostRecentIntakeScreeningSessionCreatedByAccountRoleId) {
		this.mostRecentIntakeScreeningSessionCreatedByAccountRoleId = mostRecentIntakeScreeningSessionCreatedByAccountRoleId;
	}

	@Nullable
	public String getMostRecentIntakeScreeningSessionCreatedByAccountFirstName() {
		return this.mostRecentIntakeScreeningSessionCreatedByAccountFirstName;
	}

	public void setMostRecentIntakeScreeningSessionCreatedByAccountFirstName(@Nullable String mostRecentIntakeScreeningSessionCreatedByAccountFirstName) {
		this.mostRecentIntakeScreeningSessionCreatedByAccountFirstName = mostRecentIntakeScreeningSessionCreatedByAccountFirstName;
	}

	@Nullable
	public String getMostRecentIntakeScreeningSessionCreatedByAccountLastName() {
		return this.mostRecentIntakeScreeningSessionCreatedByAccountLastName;
	}

	public void setMostRecentIntakeScreeningSessionCreatedByAccountLastName(@Nullable String mostRecentIntakeScreeningSessionCreatedByAccountLastName) {
		this.mostRecentIntakeScreeningSessionCreatedByAccountLastName = mostRecentIntakeScreeningSessionCreatedByAccountLastName;
	}

	@Nullable
	public Boolean getMostRecentIntakeScreeningSessionCompleted() {
		return this.mostRecentIntakeScreeningSessionCompleted;
	}

	public void setMostRecentIntakeScreeningSessionCompleted(@Nullable Boolean mostRecentIntakeScreeningSessionCompleted) {
		this.mostRecentIntakeScreeningSessionCompleted = mostRecentIntakeScreeningSessionCompleted;
	}

	@Nullable
	public Instant getMostRecentIntakeScreeningSessionCompletedAt() {
		return this.mostRecentIntakeScreeningSessionCompletedAt;
	}

	public void setMostRecentIntakeScreeningSessionCompletedAt(@Nullable Instant mostRecentIntakeScreeningSessionCompletedAt) {
		this.mostRecentIntakeScreeningSessionCompletedAt = mostRecentIntakeScreeningSessionCompletedAt;
	}

	@Nullable
	public Boolean getMostRecentIntakeScreeningSessionByPatient() {
		return this.mostRecentIntakeScreeningSessionByPatient;
	}

	public void setMostRecentIntakeScreeningSessionByPatient(@Nullable Boolean mostRecentIntakeScreeningSessionByPatient) {
		this.mostRecentIntakeScreeningSessionByPatient = mostRecentIntakeScreeningSessionByPatient;
	}

	@Nullable
	public Boolean getMostRecentIntakeScreeningSessionAppearsAbandoned() {
		return this.mostRecentIntakeScreeningSessionAppearsAbandoned;
	}

	public void setMostRecentIntakeScreeningSessionAppearsAbandoned(@Nullable Boolean mostRecentIntakeScreeningSessionAppearsAbandoned) {
		this.mostRecentIntakeScreeningSessionAppearsAbandoned = mostRecentIntakeScreeningSessionAppearsAbandoned;
	}

	@Nullable
	public PatientOrderIntakeScreeningStatusId getPatientOrderIntakeScreeningStatusId() {
		return this.patientOrderIntakeScreeningStatusId;
	}

	public void setPatientOrderIntakeScreeningStatusId(@Nullable PatientOrderIntakeScreeningStatusId patientOrderIntakeScreeningStatusId) {
		this.patientOrderIntakeScreeningStatusId = patientOrderIntakeScreeningStatusId;
	}

	@Nullable
	public String getPatientOrderIntakeScreeningStatusDescription() {
		return this.patientOrderIntakeScreeningStatusDescription;
	}

	public void setPatientOrderIntakeScreeningStatusDescription(@Nullable String patientOrderIntakeScreeningStatusDescription) {
		this.patientOrderIntakeScreeningStatusDescription = patientOrderIntakeScreeningStatusDescription;
	}

	@Nullable
	public String getPanelAccountFirstName() {
		return this.panelAccountFirstName;
	}

	public void setPanelAccountFirstName(@Nullable String panelAccountFirstName) {
		this.panelAccountFirstName = panelAccountFirstName;
	}

	@Nullable
	public String getPanelAccountLastName() {
		return this.panelAccountLastName;
	}

	public void setPanelAccountLastName(@Nullable String panelAccountLastName) {
		this.panelAccountLastName = panelAccountLastName;
	}

	@Nullable
	public String getPatientOrderScreeningStatusDescription() {
		return this.patientOrderScreeningStatusDescription;
	}

	public void setPatientOrderScreeningStatusDescription(@Nullable String patientOrderScreeningStatusDescription) {
		this.patientOrderScreeningStatusDescription = patientOrderScreeningStatusDescription;
	}

	@Nullable
	public String getPatientOrderDispositionDescription() {
		return this.patientOrderDispositionDescription;
	}

	public void setPatientOrderDispositionDescription(@Nullable String patientOrderDispositionDescription) {
		this.patientOrderDispositionDescription = patientOrderDispositionDescription;
	}

	@Nullable
	public PatientOrderTriageStatusId getPatientOrderTriageStatusId() {
		return this.patientOrderTriageStatusId;
	}


	public void setPatientOrderTriageStatusId(@Nullable PatientOrderTriageStatusId patientOrderTriageStatusId) {
		this.patientOrderTriageStatusId = patientOrderTriageStatusId;
	}

	@Nullable
	public String getPatientOrderTriageStatusDescription() {
		return this.patientOrderTriageStatusDescription;
	}

	public void setPatientOrderTriageStatusDescription(@Nullable String patientOrderTriageStatusDescription) {
		this.patientOrderTriageStatusDescription = patientOrderTriageStatusDescription;
	}

	@Nullable
	public String getPatientOrderClosureReasonDescription() {
		return this.patientOrderClosureReasonDescription;
	}

	public void setPatientOrderClosureReasonDescription(@Nullable String patientOrderClosureReasonDescription) {
		this.patientOrderClosureReasonDescription = patientOrderClosureReasonDescription;
	}

	@Nullable
	public Integer getPatientAgeOnOrderDate() {
		return this.patientAgeOnOrderDate;
	}

	public void setPatientAgeOnOrderDate(@Nullable Integer patientAgeOnOrderDate) {
		this.patientAgeOnOrderDate = patientAgeOnOrderDate;
	}

	@Nullable
	public Boolean getPatientBelowAgeThreshold() {
		return this.patientBelowAgeThreshold;
	}

	public void setPatientBelowAgeThreshold(@Nullable Boolean patientBelowAgeThreshold) {
		this.patientBelowAgeThreshold = patientBelowAgeThreshold;
	}

	@Nullable
	public Instant getMostRecentEpisodeClosedAt() {
		return this.mostRecentEpisodeClosedAt;
	}

	public void setMostRecentEpisodeClosedAt(@Nullable Instant mostRecentEpisodeClosedAt) {
		this.mostRecentEpisodeClosedAt = mostRecentEpisodeClosedAt;
	}

	@Nullable
	public Boolean getMostRecentEpisodeClosedWithinDateThreshold() {
		return this.mostRecentEpisodeClosedWithinDateThreshold;
	}

	public void setMostRecentEpisodeClosedWithinDateThreshold(@Nullable Boolean mostRecentEpisodeClosedWithinDateThreshold) {
		this.mostRecentEpisodeClosedWithinDateThreshold = mostRecentEpisodeClosedWithinDateThreshold;
	}

	@Nullable
	public UUID getPatientOrderScheduledScreeningId() {
		return this.patientOrderScheduledScreeningId;
	}

	public void setPatientOrderScheduledScreeningId(@Nullable UUID patientOrderScheduledScreeningId) {
		this.patientOrderScheduledScreeningId = patientOrderScheduledScreeningId;
	}

	@Nullable
	public LocalDateTime getPatientOrderScheduledScreeningScheduledDateTime() {
		return this.patientOrderScheduledScreeningScheduledDateTime;
	}

	public void setPatientOrderScheduledScreeningScheduledDateTime(@Nullable LocalDateTime patientOrderScheduledScreeningScheduledDateTime) {
		this.patientOrderScheduledScreeningScheduledDateTime = patientOrderScheduledScreeningScheduledDateTime;
	}

	@Nullable
	public String getPatientOrderScheduledScreeningCalendarUrl() {
		return this.patientOrderScheduledScreeningCalendarUrl;
	}

	public void setPatientOrderScheduledScreeningCalendarUrl(@Nullable String patientOrderScheduledScreeningCalendarUrl) {
		this.patientOrderScheduledScreeningCalendarUrl = patientOrderScheduledScreeningCalendarUrl;
	}

	@Nullable
	public UUID getAppointmentId() {
		return this.appointmentId;
	}

	public void setAppointmentId(@Nullable UUID appointmentId) {
		this.appointmentId = appointmentId;
	}

	@Nullable
	public LocalDateTime getAppointmentStartTime() {
		return this.appointmentStartTime;
	}

	public void setAppointmentStartTime(@Nullable LocalDateTime appointmentStartTime) {
		this.appointmentStartTime = appointmentStartTime;
	}

	@Nullable
	public UUID getProviderId() {
		return this.providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public String getProviderName() {
		return this.providerName;
	}

	public void setProviderName(@Nullable String providerName) {
		this.providerName = providerName;
	}

	@Nullable
	public UUID getMostRecentPatientOrderVoicemailTaskId() {
		return this.mostRecentPatientOrderVoicemailTaskId;
	}

	public void setMostRecentPatientOrderVoicemailTaskId(@Nullable UUID mostRecentPatientOrderVoicemailTaskId) {
		this.mostRecentPatientOrderVoicemailTaskId = mostRecentPatientOrderVoicemailTaskId;
	}

	@Nullable
	public Boolean getMostRecentPatientOrderVoicemailTaskCompleted() {
		return this.mostRecentPatientOrderVoicemailTaskCompleted;
	}

	public void setMostRecentPatientOrderVoicemailTaskCompleted(@Nullable Boolean mostRecentPatientOrderVoicemailTaskCompleted) {
		this.mostRecentPatientOrderVoicemailTaskCompleted = mostRecentPatientOrderVoicemailTaskCompleted;
	}

	@Nullable
	public String getReasonForReferral() {
		return this.reasonForReferral;
	}

	public void setReasonForReferral(@Nullable String reasonForReferral) {
		this.reasonForReferral = reasonForReferral;
	}

	@Nullable
	public String getPatientAddressStreetAddress1() {
		return this.patientAddressStreetAddress1;
	}

	public void setPatientAddressStreetAddress1(@Nullable String patientAddressStreetAddress1) {
		this.patientAddressStreetAddress1 = patientAddressStreetAddress1;
	}

	@Nullable
	public String getPatientAddressLocality() {
		return this.patientAddressLocality;
	}

	public void setPatientAddressLocality(@Nullable String patientAddressLocality) {
		this.patientAddressLocality = patientAddressLocality;
	}

	@Nullable
	public String getPatientAddressRegion() {
		return this.patientAddressRegion;
	}

	public void setPatientAddressRegion(@Nullable String patientAddressRegion) {
		this.patientAddressRegion = patientAddressRegion;
	}

	@Nullable
	public String getPatientAddressPostalCode() {
		return this.patientAddressPostalCode;
	}

	public void setPatientAddressPostalCode(@Nullable String patientAddressPostalCode) {
		this.patientAddressPostalCode = patientAddressPostalCode;
	}

	@Nullable
	public String getPatientAddressCountryCode() {
		return this.patientAddressCountryCode;
	}

	public void setPatientAddressCountryCode(@Nullable String patientAddressCountryCode) {
		this.patientAddressCountryCode = patientAddressCountryCode;
	}

	@Nullable
	public Boolean getPatientAddressRegionAccepted() {
		return this.patientAddressRegionAccepted;
	}

	public void setPatientAddressRegionAccepted(@Nullable Boolean patientAddressRegionAccepted) {
		this.patientAddressRegionAccepted = patientAddressRegionAccepted;
	}

	@Nullable
	public Boolean getPatientDemographicsCompleted() {
		return this.patientDemographicsCompleted;
	}

	public void setPatientDemographicsCompleted(@Nullable Boolean patientDemographicsCompleted) {
		this.patientDemographicsCompleted = patientDemographicsCompleted;
	}

	@Nullable
	public Boolean getPatientDemographicsAccepted() {
		return this.patientDemographicsAccepted;
	}

	public void setPatientDemographicsAccepted(@Nullable Boolean patientDemographicsAccepted) {
		this.patientDemographicsAccepted = patientDemographicsAccepted;
	}

	@Nullable
	public LocalDateTime getResourceCheckInScheduledAtDateTime() {
		return this.resourceCheckInScheduledAtDateTime;
	}

	public void setResourceCheckInScheduledAtDateTime(@Nullable LocalDateTime resourceCheckInScheduledAtDateTime) {
		this.resourceCheckInScheduledAtDateTime = resourceCheckInScheduledAtDateTime;
	}

	@Nullable
	public String getPatientOrderResourceCheckInResponseStatusDescription() {
		return this.patientOrderResourceCheckInResponseStatusDescription;
	}

	public void setPatientOrderResourceCheckInResponseStatusDescription(@Nullable String patientOrderResourceCheckInResponseStatusDescription) {
		this.patientOrderResourceCheckInResponseStatusDescription = patientOrderResourceCheckInResponseStatusDescription;
	}

	@Nullable
	public Boolean getResourceCheckInResponseNeeded() {
		return this.resourceCheckInResponseNeeded;
	}

	public void setResourceCheckInResponseNeeded(@Nullable Boolean resourceCheckInResponseNeeded) {
		this.resourceCheckInResponseNeeded = resourceCheckInResponseNeeded;
	}

	@Nullable
	public Boolean getMostRecentScreeningSessionByPatient() {
		return this.mostRecentScreeningSessionByPatient;
	}

	public void setMostRecentScreeningSessionByPatient(@Nullable Boolean mostRecentScreeningSessionByPatient) {
		this.mostRecentScreeningSessionByPatient = mostRecentScreeningSessionByPatient;
	}

	@Nullable
	public Boolean getAppointmentScheduledByPatient() {
		return this.appointmentScheduledByPatient;
	}

	public void setAppointmentScheduledByPatient(@Nullable Boolean appointmentScheduledByPatient) {
		this.appointmentScheduledByPatient = appointmentScheduledByPatient;
	}

	@Nullable
	public PatientOrderTriageSourceId getPatientOrderTriageSourceId() {
		return this.patientOrderTriageSourceId;
	}

	public void setPatientOrderTriageSourceId(@Nullable PatientOrderTriageSourceId patientOrderTriageSourceId) {
		this.patientOrderTriageSourceId = patientOrderTriageSourceId;
	}

	@Nullable
	public String getPatientOrderTriageReason() {
		return this.patientOrderTriageReason;
	}

	public void setPatientOrderTriageReason(@Nullable String patientOrderTriageReason) {
		this.patientOrderTriageReason = patientOrderTriageReason;
	}

	@Nullable
	public Boolean getAppointmentScheduled() {
		return this.appointmentScheduled;
	}

	public void setAppointmentScheduled(@Nullable Boolean appointmentScheduled) {
		this.appointmentScheduled = appointmentScheduled;
	}

	@Nullable
	public Boolean getTestPatientOrder() {
		return this.testPatientOrder;
	}

	public void setTestPatientOrder(@Nullable Boolean testPatientOrder) {
		this.testPatientOrder = testPatientOrder;
	}

	@Nullable
	public Integer getEpisodeDurationInDays() {
		return this.episodeDurationInDays;
	}

	public void setEpisodeDurationInDays(@Nullable Integer episodeDurationInDays) {
		this.episodeDurationInDays = episodeDurationInDays;
	}

	@Nullable
	public Boolean getMostRecentIntakeAndClinicalScreeningsSatisfied() {
		return this.mostRecentIntakeAndClinicalScreeningsSatisfied;
	}

	public void setMostRecentIntakeAndClinicalScreeningsSatisfied(@Nullable Boolean mostRecentIntakeAndClinicalScreeningsSatisfied) {
		this.mostRecentIntakeAndClinicalScreeningsSatisfied = mostRecentIntakeAndClinicalScreeningsSatisfied;
	}

	@Nullable
	public String getEpicDepartmentName() {
		return this.epicDepartmentName;
	}

	public void setEpicDepartmentName(@Nullable String epicDepartmentName) {
		this.epicDepartmentName = epicDepartmentName;
	}

	@Nullable
	public String getEpicDepartmentDepartmentId() {
		return this.epicDepartmentDepartmentId;
	}

	public void setEpicDepartmentDepartmentId(@Nullable String epicDepartmentDepartmentId) {
		this.epicDepartmentDepartmentId = epicDepartmentDepartmentId;
	}

	@Nullable
	public PatientOrderEncounterDocumentationStatusId getPatientOrderEncounterDocumentationStatusId() {
		return this.patientOrderEncounterDocumentationStatusId;
	}

	public void setPatientOrderEncounterDocumentationStatusId(@Nullable PatientOrderEncounterDocumentationStatusId patientOrderEncounterDocumentationStatusId) {
		this.patientOrderEncounterDocumentationStatusId = patientOrderEncounterDocumentationStatusId;
	}

	@Nullable
	public UUID getNextScheduledOutreachId() {
		return this.nextScheduledOutreachId;
	}

	public void setNextScheduledOutreachId(@Nullable UUID nextScheduledOutreachId) {
		this.nextScheduledOutreachId = nextScheduledOutreachId;
	}

	@Nullable
	public LocalDateTime getNextScheduledOutreachScheduledAtDateTime() {
		return this.nextScheduledOutreachScheduledAtDateTime;
	}

	public void setNextScheduledOutreachScheduledAtDateTime(@Nullable LocalDateTime nextScheduledOutreachScheduledAtDateTime) {
		this.nextScheduledOutreachScheduledAtDateTime = nextScheduledOutreachScheduledAtDateTime;
	}

	@Nullable
	public PatientOrderOutreachTypeId getNextScheduledOutreachTypeId() {
		return this.nextScheduledOutreachTypeId;
	}

	public void setNextScheduledOutreachTypeId(@Nullable PatientOrderOutreachTypeId nextScheduledOutreachTypeId) {
		this.nextScheduledOutreachTypeId = nextScheduledOutreachTypeId;
	}

	@Nullable
	public PatientOrderScheduledOutreachReasonId getNextScheduledOutreachReasonId() {
		return this.nextScheduledOutreachReasonId;
	}

	public void setNextScheduledOutreachReasonId(@Nullable PatientOrderScheduledOutreachReasonId nextScheduledOutreachReasonId) {
		this.nextScheduledOutreachReasonId = nextScheduledOutreachReasonId;
	}

	@Nullable
	public Instant getLastContactedAt() {
		return this.lastContactedAt;
	}

	public void setLastContactedAt(@Nullable Instant lastContactedAt) {
		this.lastContactedAt = lastContactedAt;
	}

	@Nullable
	public PatientOrderContactTypeId getNextContactTypeId() {
		return this.nextContactTypeId;
	}

	public void setNextContactTypeId(@Nullable PatientOrderContactTypeId nextContactTypeId) {
		this.nextContactTypeId = nextContactTypeId;
	}

	@Nullable
	public LocalDateTime getNextContactScheduledAt() {
		return this.nextContactScheduledAt;
	}

	public void setNextContactScheduledAt(@Nullable LocalDateTime nextContactScheduledAt) {
		this.nextContactScheduledAt = nextContactScheduledAt;
	}

	@Nullable
	public Instant getMostRecentMessageDeliveredAt() {
		return this.mostRecentMessageDeliveredAt;
	}

	public void setMostRecentMessageDeliveredAt(@Nullable Instant mostRecentMessageDeliveredAt) {
		this.mostRecentMessageDeliveredAt = mostRecentMessageDeliveredAt;
	}

	@Nullable
	public Boolean getOutreachFollowupNeeded() {
		return this.outreachFollowupNeeded;
	}

	public void setOutreachFollowupNeeded(@Nullable Boolean outreachFollowupNeeded) {
		this.outreachFollowupNeeded = outreachFollowupNeeded;
	}

	@Nullable
	public Boolean getPatientDemographicsConfirmed() {
		return this.patientDemographicsConfirmed;
	}

	public void setPatientDemographicsConfirmed(@Nullable Boolean patientDemographicsConfirmed) {
		this.patientDemographicsConfirmed = patientDemographicsConfirmed;
	}
}