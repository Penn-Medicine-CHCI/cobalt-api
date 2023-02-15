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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.PatientOrderNoteApiResponse.PatientOrderNoteApiResponseFactory;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderStatus.PatientOrderStatusId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class PatientOrderApiResponse {
	@Nonnull
	private final UUID patientOrderId;
	@Nullable
	private final PatientOrderStatusId patientOrderStatusId;
	@Nullable
	private final UUID patientAccountId;
	@Nullable
	private final UUID patientAddressId;
	@Nullable
	private final UUID panelAccountId;
	@Nullable
	private final String encounterDepartmentId;
	@Nullable
	private final String encounterDepartmentIdType;
	@Nullable
	private final String encounterDepartmentName;
	@Nullable
	private final String referringPracticeId;
	@Nullable
	private final String referringPracticeIdType;
	@Nullable
	private final String referringPracticeName;
	@Nullable
	private final String orderingProviderId;
	@Nullable
	private final String orderingProviderIdType;
	@Nullable
	private final String orderingProviderLastName;
	@Nullable
	private final String orderingProviderFirstName;
	@Nullable
	private final String orderingProviderMiddleName;
	@Nullable
	private final String orderingProviderDisplayName;
	@Nullable
	private final String billingProviderId;
	@Nullable
	private final String billingProviderIdType;
	@Nullable
	private final String billingProviderLastName;
	@Nullable
	private final String billingProviderFirstName;
	@Nullable
	private final String billingProviderMiddleName;
	@Nullable
	private final String billingProviderDisplayName;
	@Nullable
	private final String patientLastName;
	@Nullable
	private final String patientFirstName;
	@Nullable
	private final String patientDisplayName;
	@Nullable
	private final String patientMrn;
	@Nullable
	private final String patientId;
	@Nullable
	private final String patientIdType;
	@Nullable
	private final BirthSexId patientBirthSexId;
	@Nullable
	private final LocalDate patientBirthdate;
	@Nullable
	private final String patientBirthdateDescription;
	@Nullable
	private final String primaryPayorId;
	@Nullable
	private final String primaryPayorName;
	@Nullable
	private final String primaryPlanId;
	@Nullable
	private final String primaryPlanName;
	@Nullable
	private final LocalDate orderDate;
	@Nullable
	private final String orderDateDescription;
	@Nullable
	private final Integer orderAgeInMinutes;
	@Nullable
	private final String orderAgeInMinutesDescription;
	@Nullable
	private final String orderId;
	@Nullable
	private final String routing;
	@Nullable
	private final String reasonForReferral;
	@Nullable
	private final String associatedDiagnosis;
	@Nullable
	private final String callbackPhoneNumber;
	@Nullable
	private final String callbackPhoneNumberDescription;
	@Nullable
	private final String preferredContactHours;
	@Nullable
	private final String comments;
	@Nullable
	private final String ccRecipients;
	@Nullable
	private final String lastActiveMedicationOrderSummary;
	@Nullable
	private final String medications;
	@Nullable
	private final String recentPsychotherapeuticMedications;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PatientOrderApiResponseFactory {
		@Nonnull
		PatientOrderApiResponse create(@Nonnull PatientOrder patientOrder);
	}

	@AssistedInject
	public PatientOrderApiResponse(@Nonnull PatientOrderNoteApiResponseFactory patientOrderNoteApiResponseFactory,
																 @Nonnull Formatter formatter,
																 @Nonnull Strings strings,
																 @Nonnull Provider<CurrentContext> currentContextProvider,
																 @Assisted @Nonnull PatientOrder patientOrder) {
		requireNonNull(patientOrderNoteApiResponseFactory);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(patientOrder);

		CurrentContext currentContext = currentContextProvider.get();

		this.patientOrderId = patientOrder.getPatientOrderId();
		this.patientOrderStatusId = patientOrder.getPatientOrderStatusId();
		this.patientAccountId = patientOrder.getPatientAccountId();
		this.patientAddressId = patientOrder.getPatientAddressId();
		this.panelAccountId = patientOrder.getPanelAccountId();
		this.encounterDepartmentId = patientOrder.getEncounterDepartmentId();
		this.encounterDepartmentIdType = patientOrder.getEncounterDepartmentIdType();
		this.encounterDepartmentName = patientOrder.getEncounterDepartmentName();
		this.referringPracticeId = patientOrder.getReferringPracticeId();
		this.referringPracticeIdType = patientOrder.getReferringPracticeIdType();
		this.referringPracticeName = patientOrder.getReferringPracticeName();
		this.orderingProviderId = patientOrder.getOrderingProviderId();
		this.orderingProviderIdType = patientOrder.getOrderingProviderIdType();
		this.orderingProviderLastName = patientOrder.getOrderingProviderLastName();
		this.orderingProviderFirstName = patientOrder.getOrderingProviderFirstName();
		this.orderingProviderMiddleName = patientOrder.getOrderingProviderMiddleName();
		this.orderingProviderDisplayName = formatDisplayName(patientOrder.getOrderingProviderFirstName(), patientOrder.getOrderingProviderMiddleName(), patientOrder.getOrderingProviderLastName()).orElse(null);
		this.billingProviderId = patientOrder.getBillingProviderId();
		this.billingProviderIdType = patientOrder.getBillingProviderIdType();
		this.billingProviderLastName = patientOrder.getBillingProviderLastName();
		this.billingProviderFirstName = patientOrder.getBillingProviderFirstName();
		this.billingProviderMiddleName = patientOrder.getBillingProviderMiddleName();
		this.billingProviderDisplayName = formatDisplayName(patientOrder.getBillingProviderFirstName(), patientOrder.getBillingProviderMiddleName(), patientOrder.getBillingProviderLastName()).orElse(null);
		this.patientLastName = patientOrder.getPatientLastName();
		this.patientFirstName = patientOrder.getPatientFirstName();
		this.patientDisplayName = formatDisplayName(patientOrder.getPatientFirstName(), null, patientOrder.getPatientLastName()).orElse(null);
		this.patientMrn = patientOrder.getPatientMrn();
		this.patientId = patientOrder.getPatientId();
		this.patientIdType = patientOrder.getPatientIdType();
		this.patientBirthSexId = patientOrder.getPatientBirthSexId();
		this.patientBirthdate = patientOrder.getPatientBirthdate();
		this.patientBirthdateDescription = patientOrder.getPatientBirthdate() == null ? null : formatter.formatDate(patientOrder.getPatientBirthdate(), FormatStyle.MEDIUM);
		this.primaryPayorId = patientOrder.getPrimaryPayorId();
		this.primaryPayorName = patientOrder.getPrimaryPayorName();
		this.primaryPlanId = patientOrder.getPrimaryPlanId();
		this.primaryPlanName = patientOrder.getPrimaryPlanName();
		this.orderDate = patientOrder.getOrderDate();
		this.orderDateDescription = patientOrder.getOrderDate() == null ? null : formatter.formatDate(patientOrder.getOrderDate(), FormatStyle.MEDIUM);
		this.orderAgeInMinutes = patientOrder.getOrderAgeInMinutes();
		this.orderAgeInMinutesDescription = patientOrder.getOrderAgeInMinutes() == null ? null : formatter.formatNumber(patientOrder.getOrderAgeInMinutes());
		this.orderId = patientOrder.getOrderId();
		this.routing = patientOrder.getRouting();
		this.reasonForReferral = patientOrder.getReasonForReferral();
		this.associatedDiagnosis = patientOrder.getAssociatedDiagnosis();
		this.callbackPhoneNumber = patientOrder.getCallbackPhoneNumber();
		this.callbackPhoneNumberDescription = patientOrder.getCallbackPhoneNumber() == null ? null : formatter.formatPhoneNumber(patientOrder.getCallbackPhoneNumber(), currentContext.getLocale());
		this.preferredContactHours = patientOrder.getPreferredContactHours();
		this.comments = patientOrder.getComments();
		this.ccRecipients = patientOrder.getCcRecipients();
		this.lastActiveMedicationOrderSummary = patientOrder.getLastActiveMedicationOrderSummary();
		this.medications = patientOrder.getMedications();
		this.recentPsychotherapeuticMedications = patientOrder.getRecentPsychotherapeuticMedications();
	}

	@Nonnull
	protected Optional<String> formatDisplayName(@Nullable String firstName,
																							 @Nullable String middleName,
																							 @Nullable String lastName) {
		firstName = trimToNull(firstName);
		middleName = trimToNull(middleName);
		lastName = trimToNull(lastName);

		if (firstName != null && middleName != null && lastName != null)
			return Optional.of(format("%s, %s %s", lastName, firstName, middleName));

		if (firstName != null && lastName != null)
			return Optional.of(format("%s, %s", lastName, firstName));

		if (middleName != null && lastName != null)
			return Optional.of(format("%s, %s", lastName, middleName));

		if (middleName != null && firstName != null)
			return Optional.of(format("%s %s", firstName, middleName));

		if (lastName != null)
			return Optional.of(lastName);

		if (firstName != null)
			return Optional.of(firstName);

		return Optional.empty();
	}

	@Nonnull
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	@Nullable
	public PatientOrderStatusId getPatientOrderStatusId() {
		return this.patientOrderStatusId;
	}

	@Nullable
	public UUID getPatientAccountId() {
		return this.patientAccountId;
	}

	@Nullable
	public UUID getPatientAddressId() {
		return this.patientAddressId;
	}

	@Nullable
	public UUID getPanelAccountId() {
		return this.panelAccountId;
	}

	@Nullable
	public String getEncounterDepartmentId() {
		return this.encounterDepartmentId;
	}

	@Nullable
	public String getEncounterDepartmentIdType() {
		return this.encounterDepartmentIdType;
	}

	@Nullable
	public String getEncounterDepartmentName() {
		return this.encounterDepartmentName;
	}

	@Nullable
	public String getReferringPracticeId() {
		return this.referringPracticeId;
	}

	@Nullable
	public String getReferringPracticeIdType() {
		return this.referringPracticeIdType;
	}

	@Nullable
	public String getReferringPracticeName() {
		return this.referringPracticeName;
	}

	@Nullable
	public String getOrderingProviderId() {
		return this.orderingProviderId;
	}

	@Nullable
	public String getOrderingProviderIdType() {
		return this.orderingProviderIdType;
	}

	@Nullable
	public String getOrderingProviderLastName() {
		return this.orderingProviderLastName;
	}

	@Nullable
	public String getOrderingProviderFirstName() {
		return this.orderingProviderFirstName;
	}

	@Nullable
	public String getOrderingProviderMiddleName() {
		return this.orderingProviderMiddleName;
	}

	@Nullable
	public String getOrderingProviderDisplayName() {
		return this.orderingProviderDisplayName;
	}

	@Nullable
	public String getBillingProviderId() {
		return this.billingProviderId;
	}

	@Nullable
	public String getBillingProviderIdType() {
		return this.billingProviderIdType;
	}

	@Nullable
	public String getBillingProviderLastName() {
		return this.billingProviderLastName;
	}

	@Nullable
	public String getBillingProviderFirstName() {
		return this.billingProviderFirstName;
	}

	@Nullable
	public String getBillingProviderMiddleName() {
		return this.billingProviderMiddleName;
	}

	@Nullable
	public String getBillingProviderDisplayName() {
		return this.billingProviderDisplayName;
	}

	@Nullable
	public String getPatientLastName() {
		return this.patientLastName;
	}

	@Nullable
	public String getPatientFirstName() {
		return this.patientFirstName;
	}

	@Nullable
	public String getPatientDisplayName() {
		return this.patientDisplayName;
	}

	@Nullable
	public String getPatientMrn() {
		return this.patientMrn;
	}

	@Nullable
	public String getPatientId() {
		return this.patientId;
	}

	@Nullable
	public String getPatientIdType() {
		return this.patientIdType;
	}

	@Nullable
	public BirthSexId getPatientBirthSexId() {
		return this.patientBirthSexId;
	}

	@Nullable
	public LocalDate getPatientBirthdate() {
		return this.patientBirthdate;
	}

	@Nullable
	public String getPatientBirthdateDescription() {
		return this.patientBirthdateDescription;
	}

	@Nullable
	public String getPrimaryPayorId() {
		return this.primaryPayorId;
	}

	@Nullable
	public String getPrimaryPayorName() {
		return this.primaryPayorName;
	}

	@Nullable
	public String getPrimaryPlanId() {
		return this.primaryPlanId;
	}

	@Nullable
	public String getPrimaryPlanName() {
		return this.primaryPlanName;
	}

	@Nullable
	public LocalDate getOrderDate() {
		return this.orderDate;
	}

	@Nullable
	public String getOrderDateDescription() {
		return this.orderDateDescription;
	}

	@Nullable
	public Integer getOrderAgeInMinutes() {
		return this.orderAgeInMinutes;
	}

	@Nullable
	public String getOrderAgeInMinutesDescription() {
		return this.orderAgeInMinutesDescription;
	}

	@Nullable
	public String getOrderId() {
		return this.orderId;
	}

	@Nullable
	public String getRouting() {
		return this.routing;
	}

	@Nullable
	public String getReasonForReferral() {
		return this.reasonForReferral;
	}

	@Nullable
	public String getAssociatedDiagnosis() {
		return this.associatedDiagnosis;
	}

	@Nullable
	public String getCallbackPhoneNumber() {
		return this.callbackPhoneNumber;
	}

	@Nullable
	public String getCallbackPhoneNumberDescription() {
		return this.callbackPhoneNumberDescription;
	}

	@Nullable
	public String getPreferredContactHours() {
		return this.preferredContactHours;
	}

	@Nullable
	public String getComments() {
		return this.comments;
	}

	@Nullable
	public String getCcRecipients() {
		return this.ccRecipients;
	}

	@Nullable
	public String getLastActiveMedicationOrderSummary() {
		return this.lastActiveMedicationOrderSummary;
	}

	@Nullable
	public String getMedications() {
		return this.medications;
	}

	@Nullable
	public String getRecentPsychotherapeuticMedications() {
		return this.recentPsychotherapeuticMedications;
	}
}