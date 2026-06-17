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

import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.AppointmentBookingLevel.AppointmentBookingLevelId;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ClinicApiResponse {
	@Nonnull
	private final UUID clinicId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nullable
	private final UUID intakeAssessmentId;
	@Nonnull
	private final String name;
	@Nonnull
	private final String description;
	@Nullable
	private final String treatmentDescription;
	@Nullable
	private final Boolean showIntakeAssessmentPrompt;
	@Nullable
	private final AppointmentBookingLevelId appointmentBookingLevelId;
	@Nullable
	private final String phoneNumber;
	@Nullable
	private final String phoneNumberDescription;
	@Nullable
	private final String formattedPhoneNumber;
	@Nullable
	private final String imageUrl;
	@Nonnull
	private final List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalities;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ClinicApiResponseFactory {
		@Nonnull
		ClinicApiResponse create(@Nonnull Clinic clinic);
	}

	@AssistedInject
	public ClinicApiResponse(@Nonnull ProviderService providerService,
													 @Nonnull Formatter formatter,
													 @Nonnull Strings strings,
													 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
													 @Assisted @Nonnull Clinic clinic) {
		requireNonNull(providerService);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(clinic);

		this.clinicId = clinic.getClinicId();
		this.institutionId = clinic.getInstitutionId();
		this.intakeAssessmentId = clinic.getIntakeAssessmentId();
		this.name = clinic.getDescription();
		this.description = clinic.getDescription();
		this.treatmentDescription = clinic.getTreatmentDescription();
		this.showIntakeAssessmentPrompt = clinic.getShowIntakeAssessmentPrompt();
		this.appointmentBookingLevelId = clinic.getAppointmentBookingLevelId();
		this.phoneNumber = clinic.getPhoneNumber();
		this.phoneNumberDescription = formatter.formatPhoneNumber(clinic.getPhoneNumber(), clinic.getLocale());
		this.formattedPhoneNumber = this.phoneNumberDescription;
		this.imageUrl = clinic.getImageUrl();
		this.supportedAppointmentModalities = supportedAppointmentModalitiesFor(providerService.findProvidersByClinicId(clinic.getClinicId()), strings);
	}

	@Nonnull
	protected List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalitiesFor(@Nonnull List<Provider> providers,
																																													 @Nonnull Strings strings) {
		requireNonNull(providers);
		requireNonNull(strings);

		Set<ProviderAppointmentModalityId> providerAppointmentModalityIds = new HashSet<>();

		for (Provider provider : providers)
			providerAppointmentModalityIds.addAll(ProviderAppointmentModalitySupport.providerAppointmentModalityIdsFor(provider));

		return ProviderAppointmentModalitySupport.providerAppointmentModalityApiResponsesFor(providerAppointmentModalityIds, strings);
	}

	@Nonnull
	public UUID getClinicId() {
		return clinicId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	@Nullable
	public UUID getIntakeAssessmentId() {
		return intakeAssessmentId;
	}

	@Nonnull
	public String getName() {
		return this.name;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}

	@Nullable
	public String getTreatmentDescription() {
		return treatmentDescription;
	}

	@Nullable
	public Boolean getShowIntakeAssessmentPrompt() {
		return showIntakeAssessmentPrompt;
	}

	@Nullable
	public AppointmentBookingLevelId getAppointmentBookingLevelId() {
		return this.appointmentBookingLevelId;
	}

	@Nullable
	public String getPhoneNumber() {
		return this.phoneNumber;
	}

	@Nullable
	public String getPhoneNumberDescription() {
		return this.phoneNumberDescription;
	}

	@Nullable
	public String getFormattedPhoneNumber() {
		return formattedPhoneNumber;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	@Nonnull
	public List<ProviderAppointmentModalityApiResponse> getSupportedAppointmentModalities() {
		return this.supportedAppointmentModalities;
	}
}
