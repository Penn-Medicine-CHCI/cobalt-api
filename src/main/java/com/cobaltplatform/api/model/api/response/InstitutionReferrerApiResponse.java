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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionReferrer;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.WebUtility;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InstitutionReferrerApiResponse {
	@Nonnull
	private final UUID institutionReferrerId;
	@Nonnull
	private final InstitutionId fromInstitutionId;
	@Nonnull
	private final InstitutionId toInstitutionId;
	@Nullable
	private final UUID intakeScreeningFlowId;
	@Nonnull
	private final String urlName;
	@Nonnull
	private final String title;
	@Nonnull
	private final String description; // Can include HTML
	@Nonnull
	private final String pageContent; // Content for the referrer page (e.g. markup with list of FAQs), should be HTML
	@Nullable
	private final String ctaTitle; // CTA to be displayed on the institution referrer page
	@Nullable
	private final String ctaDescription; // CTA to be displayed on the institution referrer page.  Can include HTML
	@Nonnull
	private final Map<String, Object> metadata;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InstitutionReferrerApiResponseFactory {
		@Nonnull
		InstitutionReferrerApiResponse create(@Nonnull InstitutionReferrer institutionReferrer,
																					@Assisted("appointmentTypeId") @Nullable UUID appointmentTypeId);
	}

	@AssistedInject
	public InstitutionReferrerApiResponse(@Nonnull Formatter formatter,
																				@Nonnull Strings strings,
																				@Assisted @Nonnull InstitutionReferrer institutionReferrer,
																				@Assisted("appointmentTypeId") @Nullable UUID appointmentTypeId) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(institutionReferrer);

		this.institutionReferrerId = institutionReferrer.getInstitutionReferrerId();
		this.fromInstitutionId = institutionReferrer.getFromInstitutionId();
		this.toInstitutionId = institutionReferrer.getToInstitutionId();
		this.intakeScreeningFlowId = institutionReferrer.getIntakeScreeningFlowId();
		this.urlName = institutionReferrer.getUrlName();
		this.title = institutionReferrer.getTitle();
		this.description = institutionReferrer.getDescription();
		this.pageContent = institutionReferrer.getPageContent();
		this.ctaTitle = institutionReferrer.getCtaTitle();
		this.ctaDescription = institutionReferrer.getCtaDescription();
		this.metadata = Collections.unmodifiableMap(metadataFor(institutionReferrer, appointmentTypeId));
	}

	@Nonnull
	protected static Map<String, Object> metadataFor(@Nonnull InstitutionReferrer institutionReferrer,
																									 @Nullable UUID appointmentTypeId) {
		requireNonNull(institutionReferrer);

		Map<String, Object> metadata = copyStringKeyedMap(institutionReferrer.getMetadata());

		if (appointmentTypeId == null)
			return metadata;

		applyAppointmentTypeIdToBookingMetadata(metadata, appointmentTypeId);

		Object resultScreensMetadata = metadata.get("resultScreens");

		if (resultScreensMetadata instanceof Map<?, ?> resultScreensById) {
			Map<String, Object> resultScreens = copyStringKeyedMap(resultScreensById);

			for (Entry<String, Object> entry : resultScreens.entrySet()) {
				if (entry.getValue() instanceof Map<?, ?> resultScreenMetadata) {
					Map<String, Object> resultScreen = copyStringKeyedMap(resultScreenMetadata);
					applyAppointmentTypeIdToBookingMetadata(resultScreen, appointmentTypeId);
					entry.setValue(resultScreen);
				}
			}

			metadata.put("resultScreens", resultScreens);
		}

		return metadata;
	}

	protected static void applyAppointmentTypeIdToBookingMetadata(@Nonnull Map<String, Object> metadata,
																																@Nonnull UUID appointmentTypeId) {
		requireNonNull(metadata);
		requireNonNull(appointmentTypeId);

		Object bookingMetadata = metadata.get("booking");

		if (!(bookingMetadata instanceof Map<?, ?> bookingMetadataByKey))
			return;

		Map<String, Object> booking = copyStringKeyedMap(bookingMetadataByKey);
		booking.put("appointmentTypeId", appointmentTypeId.toString());

		Object path = booking.get("path");

		if (path instanceof String && trimToNull((String) path) != null)
			booking.put("path", pathWithAppointmentTypeId((String) path, appointmentTypeId));

		metadata.put("booking", booking);
	}

	@Nonnull
	protected static Map<String, Object> copyStringKeyedMap(@Nonnull Map<?, ?> source) {
		requireNonNull(source);

		Map<String, Object> copy = new LinkedHashMap<>(source.size());

		for (Entry<?, ?> entry : source.entrySet())
			if (entry.getKey() != null)
				copy.put(entry.getKey().toString(), entry.getValue());

		return copy;
	}

	@Nonnull
	protected static String pathWithAppointmentTypeId(@Nonnull String path,
																										@Nonnull UUID appointmentTypeId) {
		requireNonNull(path);
		requireNonNull(appointmentTypeId);

		if (trimToNull(path) == null)
			return path;

		return appendQueryParameters(pathWithoutQueryParameter(path, "appointmentTypeId"),
				Map.of("appointmentTypeId", appointmentTypeId.toString()));
	}

	@Nonnull
	protected static String pathWithoutQueryParameter(@Nonnull String path,
																									 @Nonnull String parameterName) {
		requireNonNull(path);
		requireNonNull(parameterName);

		int fragmentIndex = path.indexOf('#');
		String fragment = fragmentIndex >= 0 ? path.substring(fragmentIndex) : "";
		String pathWithoutFragment = fragmentIndex >= 0 ? path.substring(0, fragmentIndex) : path;
		int queryIndex = pathWithoutFragment.indexOf('?');

		if (queryIndex < 0)
			return path;

		String pathPrefix = pathWithoutFragment.substring(0, queryIndex);
		String query = pathWithoutFragment.substring(queryIndex + 1);
		StringBuilder updatedQuery = new StringBuilder();

		for (String queryParameter : query.split("&", -1)) {
			if (trimToNull(queryParameter) == null || parameterName.equals(queryParameterName(queryParameter)))
				continue;

			if (updatedQuery.length() > 0)
				updatedQuery.append("&");

			updatedQuery.append(queryParameter);
		}

		return updatedQuery.length() == 0
				? pathPrefix + fragment
				: pathPrefix + "?" + updatedQuery + fragment;
	}

	@Nonnull
	protected static String queryParameterName(@Nonnull String queryParameter) {
		requireNonNull(queryParameter);

		int valueSeparatorIndex = queryParameter.indexOf('=');

		return valueSeparatorIndex < 0 ? queryParameter : queryParameter.substring(0, valueSeparatorIndex);
	}

	@Nonnull
	protected static String appendQueryParameters(@Nonnull String path,
																								@Nonnull Map<String, String> queryParameters) {
		requireNonNull(path);
		requireNonNull(queryParameters);

		if (queryParameters.isEmpty())
			return path;

		int fragmentIndex = path.indexOf('#');
		String fragment = fragmentIndex >= 0 ? path.substring(fragmentIndex) : "";
		String pathWithoutFragment = fragmentIndex >= 0 ? path.substring(0, fragmentIndex) : path;
		String separator = pathWithoutFragment.contains("?")
				? (pathWithoutFragment.endsWith("?") || pathWithoutFragment.endsWith("&") ? "" : "&")
				: "?";

		return pathWithoutFragment + separator + WebUtility.urlEncode(queryParameters) + fragment;
	}

	@Nonnull
	public UUID getInstitutionReferrerId() {
		return this.institutionReferrerId;
	}

	@Nonnull
	public InstitutionId getFromInstitutionId() {
		return this.fromInstitutionId;
	}

	@Nonnull
	public InstitutionId getToInstitutionId() {
		return this.toInstitutionId;
	}

	@Nonnull
	public Optional<UUID> getIntakeScreeningFlowId() {
		return Optional.ofNullable(this.intakeScreeningFlowId);
	}

	@Nonnull
	public String getUrlName() {
		return this.urlName;
	}

	@Nonnull
	public String getTitle() {
		return this.title;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}

	@Nonnull
	public String getPageContent() {
		return this.pageContent;
	}

	@Nonnull
	public Optional<String> getCtaTitle() {
		return Optional.ofNullable(this.ctaTitle);
	}

	@Nonnull
	public Optional<String> getCtaDescription() {
		return Optional.ofNullable(this.ctaDescription);
	}

	@Nonnull
	public Map<String, Object> getMetadata() {
		return this.metadata;
	}
}
