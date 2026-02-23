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

package com.cobaltplatform.api.integration.epic;

import com.cobaltplatform.api.util.JsonMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class EpicAppointmentBookingErrorResolver {
	@Nonnull
	private static final String EPIC_RESPONSE_BODY_MARKER;
	@Nonnull
	private static final Pattern EPIC_HTTP_STATUS_PATTERN;
	@Nonnull
	private static final Pattern EPIC_COMMAND_AND_DETAILS_PATTERN;
	@Nonnull
	private static final Pattern EPIC_DETAILS_CODE_PATTERN;
	@Nonnull
	private static final Pattern EPIC_DETAILS_VALUE_PATTERN;
	@Nonnull
	private static final Pattern EPIC_APPOINTMENT_WARNING_DETAILS_VALUES_PATTERN;
	@Nonnull
	private static final Pattern EPIC_APPOINTMENT_WARNING_DETAILS_CODE_PATTERN;
	@Nonnull
	private static final Map<String, EpicAppointmentWarningType> EPIC_APPOINTMENT_WARNING_DETAIL_CODE_TO_TYPE;

	@Nonnull
	private final JsonMapper jsonMapper;

	static {
		EPIC_RESPONSE_BODY_MARKER = "Response body was\n";
		EPIC_HTTP_STATUS_PATTERN = Pattern.compile("Bad HTTP response\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
		EPIC_COMMAND_AND_DETAILS_PATTERN = Pattern.compile("command:\\s*([^\\s]+)\\s+details:\\s*(.+)", Pattern.CASE_INSENSITIVE);
		EPIC_DETAILS_CODE_PATTERN = Pattern.compile("code:\\s*([A-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);
		EPIC_DETAILS_VALUE_PATTERN = Pattern.compile("Details:\\s*([A-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);
		EPIC_APPOINTMENT_WARNING_DETAILS_VALUES_PATTERN = Pattern.compile("Details:\\s*(.+)", Pattern.CASE_INSENSITIVE);
		EPIC_APPOINTMENT_WARNING_DETAILS_CODE_PATTERN = Pattern.compile("(\\d+)");
		EPIC_APPOINTMENT_WARNING_DETAIL_CODE_TO_TYPE = Map.of(
				// Known EPIC APTWARN detail codes observed during ScheduleWithInsurance booking failures.
				"25", EpicAppointmentWarningType.TIMESLOT_UNAVAILABLE,
				"27", EpicAppointmentWarningType.PICK_DIFFERENT_TIME,
				"34", EpicAppointmentWarningType.PICK_DIFFERENT_TIME
		);
	}

	public EpicAppointmentBookingErrorResolver() {
		this(new JsonMapper());
	}

	public EpicAppointmentBookingErrorResolver(@Nonnull JsonMapper jsonMapper) {
		requireNonNull(jsonMapper);
		this.jsonMapper = jsonMapper;
	}

	@Nonnull
	public EpicAppointmentBookingErrorResolution resolve(@Nullable EpicException epicException) {
		return resolve(epicException == null ? null : epicException.getMessage());
	}

	@Nonnull
	public EpicAppointmentBookingErrorResolution resolve(@Nullable String epicExceptionMessage) {
		EpicAppointmentBookingErrorDetails errorDetails = parseEpicAppointmentBookingErrorDetails(epicExceptionMessage).orElse(null);
		EpicAppointmentWarningType appointmentWarningType = errorDetails == null ? null : errorDetails.resolveAppointmentWarningType().orElse(null);
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("epicAppointmentBookingError", true);

		if (errorDetails != null) {
			if (errorDetails.getHttpStatus() != null)
				metadata.put("epicHttpStatus", errorDetails.getHttpStatus());
			if (errorDetails.getCommand() != null)
				metadata.put("epicCommand", errorDetails.getCommand());
			if (errorDetails.getErrorCode() != null)
				metadata.put("epicErrorCode", errorDetails.getErrorCode());
			if (errorDetails.getErrorDetailCode() != null)
				metadata.put("epicErrorDetailCode", errorDetails.getErrorDetailCode());
			if (errorDetails.getErrorDetailCodes().size() > 0)
				metadata.put("epicErrorDetailCodes", errorDetails.getErrorDetailCodes());

			if (appointmentWarningType != null)
				metadata.put("epicAppointmentWarningType", appointmentWarningType.name());
		}

		EpicAppointmentBookingFailureType failureType;

		if (appointmentWarningType == EpicAppointmentWarningType.TIMESLOT_UNAVAILABLE) {
			metadata.put("appointmentTimeslotUnavailable", true);
			failureType = EpicAppointmentBookingFailureType.TIMESLOT_UNAVAILABLE;
		} else if (appointmentWarningType == EpicAppointmentWarningType.PICK_DIFFERENT_TIME) {
			failureType = EpicAppointmentBookingFailureType.PICK_DIFFERENT_TIME;
		} else if (errorDetails != null && errorDetails.isMissingRequiredPatientData()) {
			failureType = EpicAppointmentBookingFailureType.MISSING_REQUIRED_PATIENT_DATA;
		} else if (errorDetails != null && errorDetails.isEpicTemporarilyUnavailable()) {
			failureType = EpicAppointmentBookingFailureType.EPIC_TEMPORARILY_UNAVAILABLE;
		} else if (errorDetails != null && errorDetails.isAppointmentWarning()) {
			failureType = EpicAppointmentBookingFailureType.APPOINTMENT_WARNING;
		} else {
			failureType = EpicAppointmentBookingFailureType.UNKNOWN;
		}

		return new EpicAppointmentBookingErrorResolution(
				failureType,
				metadata,
				appointmentWarningType,
				errorDetails
		);
	}

	@Nonnull
	protected Optional<EpicAppointmentBookingErrorDetails> parseEpicAppointmentBookingErrorDetails(@Nullable String epicExceptionMessage) {
		if (trimToNull(epicExceptionMessage) == null)
			return Optional.empty();

		Integer httpStatus = null;
		Matcher httpStatusMatcher = EPIC_HTTP_STATUS_PATTERN.matcher(epicExceptionMessage);

		if (httpStatusMatcher.find()) {
			try {
				httpStatus = Integer.valueOf(httpStatusMatcher.group(1));
			} catch (Exception ignored) {
				// Ignored
			}
		}

		String responseBody = extractEpicResponseBody(epicExceptionMessage).orElse(null);
		String epicServiceExceptionMessage = null;

		if (responseBody != null) {
			try {
				Map<String, Object> responseBodyAsMap = getJsonMapper().fromJson(responseBody, Map.class);
				epicServiceExceptionMessage = firstNonNull(
						stringValue(responseBodyAsMap, "ExceptionMessage"),
						stringValue(responseBodyAsMap, "Message")
				);
			} catch (Exception ignored) {
				// Ignored
			}
		}

		String messageToParse = firstNonNull(epicServiceExceptionMessage, trimToNull(epicExceptionMessage));
		String command = null;
		String details = null;
		String errorCode = null;
		String errorDetailCode = null;
		List<String> errorDetailCodes = List.of();

		if (messageToParse != null) {
			Matcher commandAndDetailsMatcher = EPIC_COMMAND_AND_DETAILS_PATTERN.matcher(messageToParse);

			if (commandAndDetailsMatcher.find()) {
				command = trimToNull(commandAndDetailsMatcher.group(1));
				details = trimToNull(commandAndDetailsMatcher.group(2));
			}
		}

		if (details != null) {
			Matcher detailsCodeMatcher = EPIC_DETAILS_CODE_PATTERN.matcher(details);
			if (detailsCodeMatcher.find())
				errorCode = trimToNull(detailsCodeMatcher.group(1));

			Matcher detailsValueMatcher = EPIC_DETAILS_VALUE_PATTERN.matcher(details);
			if (detailsValueMatcher.find())
				errorDetailCode = trimToNull(detailsValueMatcher.group(1));

			if ("APTWARN".equalsIgnoreCase(trimToNull(errorCode))) {
				errorDetailCodes = parseEpicAppointmentWarningDetailCodes(details);

				if (errorDetailCodes.size() > 0)
					errorDetailCode = errorDetailCodes.get(0);
			}
		}

		return Optional.of(new EpicAppointmentBookingErrorDetails(
				httpStatus,
				trimToNull(epicExceptionMessage),
				trimToNull(epicServiceExceptionMessage),
				command,
				details,
				errorCode,
				errorDetailCode,
				errorDetailCodes
		));
	}

	@Nonnull
	protected List<String> parseEpicAppointmentWarningDetailCodes(@Nullable String details) {
		if (trimToNull(details) == null)
			return List.of();

		String detailsValues = trimToNull(details);
		Matcher detailsValuesMatcher = EPIC_APPOINTMENT_WARNING_DETAILS_VALUES_PATTERN.matcher(details);

		if (detailsValuesMatcher.find())
			detailsValues = trimToNull(detailsValuesMatcher.group(1));

		if (detailsValues == null)
			return List.of();

		List<String> detailCodes = new ArrayList<>();
		Matcher detailCodeMatcher = EPIC_APPOINTMENT_WARNING_DETAILS_CODE_PATTERN.matcher(detailsValues);

		while (detailCodeMatcher.find()) {
			String detailCode = trimToNull(detailCodeMatcher.group(1));

			if (detailCode == null)
				continue;

			if (!detailCodes.contains(detailCode))
				detailCodes.add(detailCode);
		}

		return Collections.unmodifiableList(detailCodes);
	}

	@Nonnull
	protected Optional<String> extractEpicResponseBody(@Nullable String epicExceptionMessage) {
		if (trimToNull(epicExceptionMessage) == null)
			return Optional.empty();

		int markerIndex = epicExceptionMessage.indexOf(EPIC_RESPONSE_BODY_MARKER);

		if (markerIndex < 0)
			return Optional.empty();

		String responseBody = trimToNull(epicExceptionMessage.substring(markerIndex + EPIC_RESPONSE_BODY_MARKER.length()));
		return Optional.ofNullable(responseBody);
	}

	@Nullable
	protected String stringValue(@Nullable Map<String, Object> map,
															 @Nonnull String key) {
		requireNonNull(key);

		if (map == null)
			return null;

		Object value = map.get(key);
		if (value == null)
			return null;

		return trimToNull(value.toString());
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}

	public enum EpicAppointmentBookingFailureType {
		TIMESLOT_UNAVAILABLE,
		PICK_DIFFERENT_TIME,
		MISSING_REQUIRED_PATIENT_DATA,
		EPIC_TEMPORARILY_UNAVAILABLE,
		APPOINTMENT_WARNING,
		UNKNOWN
	}

	public enum EpicAppointmentWarningType {
		TIMESLOT_UNAVAILABLE,
		PICK_DIFFERENT_TIME
	}

	@ThreadSafe
	public static class EpicAppointmentBookingErrorResolution {
		@Nonnull
		private final EpicAppointmentBookingFailureType failureType;
		@Nonnull
		private final Map<String, Object> metadata;
		@Nullable
		private final EpicAppointmentWarningType warningType;
		@Nullable
		private final EpicAppointmentBookingErrorDetails errorDetails;

		public EpicAppointmentBookingErrorResolution(@Nonnull EpicAppointmentBookingFailureType failureType,
																						 @Nullable Map<String, Object> metadata,
																						 @Nullable EpicAppointmentWarningType warningType,
																						 @Nullable EpicAppointmentBookingErrorDetails errorDetails) {
			requireNonNull(failureType);
			this.failureType = failureType;
			this.metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
			this.warningType = warningType;
			this.errorDetails = errorDetails;
		}

		@Nonnull
		public EpicAppointmentBookingFailureType getFailureType() {
			return this.failureType;
		}

		@Nonnull
		public Map<String, Object> getMetadata() {
			return this.metadata;
		}

		@Nullable
		public EpicAppointmentWarningType getWarningType() {
			return this.warningType;
		}

		@Nonnull
		public Optional<EpicAppointmentBookingErrorDetails> getErrorDetails() {
			return Optional.ofNullable(this.errorDetails);
		}
	}

	@ThreadSafe
	public static class EpicAppointmentBookingErrorDetails {
		@Nullable
		private final Integer httpStatus;
		@Nullable
		private final String topLevelExceptionMessage;
		@Nullable
		private final String epicServiceExceptionMessage;
		@Nullable
		private final String command;
		@Nullable
		private final String details;
		@Nullable
		private final String errorCode;
		@Nullable
		private final String errorDetailCode;
		@Nonnull
		private final List<String> errorDetailCodes;

		public EpicAppointmentBookingErrorDetails(@Nullable Integer httpStatus,
																							@Nullable String topLevelExceptionMessage,
																							@Nullable String epicServiceExceptionMessage,
																							@Nullable String command,
																							@Nullable String details,
																							@Nullable String errorCode,
																							@Nullable String errorDetailCode,
																							@Nullable List<String> errorDetailCodes) {
			this.httpStatus = httpStatus;
			this.topLevelExceptionMessage = topLevelExceptionMessage;
			this.epicServiceExceptionMessage = epicServiceExceptionMessage;
			this.command = command;
			this.details = details;
			this.errorCode = errorCode;
			this.errorDetailCode = errorDetailCode;
			this.errorDetailCodes = errorDetailCodes == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(errorDetailCodes));
		}

		public boolean isAppointmentWarning() {
			return "APTWARN".equalsIgnoreCase(trimToNull(getErrorCode()));
		}

		@Nonnull
		public Optional<EpicAppointmentWarningType> resolveAppointmentWarningType() {
			if (!isAppointmentWarning())
				return Optional.empty();

			for (String errorDetailCode : getErrorDetailCodes()) {
				EpicAppointmentWarningType warningType = EPIC_APPOINTMENT_WARNING_DETAIL_CODE_TO_TYPE.get(errorDetailCode);

				if (warningType != null)
					return Optional.of(warningType);
			}

			return Optional.empty();
		}

		public boolean isMissingRequiredPatientData() {
			String normalizedCommand = trimToNull(getCommand());
			if (normalizedCommand != null && normalizedCommand.toUpperCase(Locale.ENGLISH).startsWith("NO-"))
				return true;

			String message = firstNonNull(trimToNull(getEpicServiceExceptionMessage()), trimToNull(getDetails()));
			return message != null && message.toUpperCase(Locale.ENGLISH).contains("IS REQUIRED");
		}

		public boolean isEpicTemporarilyUnavailable() {
			if (getHttpStatus() != null && getHttpStatus() >= 500)
				return true;

			String message = firstNonNull(trimToNull(getTopLevelExceptionMessage()), trimToNull(getEpicServiceExceptionMessage()));
			return message != null && message.toUpperCase(Locale.ENGLISH).contains("UNABLE TO CALL EPIC ENDPOINT");
		}

		@Nullable
		public Integer getHttpStatus() {
			return this.httpStatus;
		}

		@Nullable
		public String getTopLevelExceptionMessage() {
			return this.topLevelExceptionMessage;
		}

		@Nullable
		public String getEpicServiceExceptionMessage() {
			return this.epicServiceExceptionMessage;
		}

		@Nullable
		public String getCommand() {
			return this.command;
		}

		@Nullable
		public String getDetails() {
			return this.details;
		}

		@Nullable
		public String getErrorCode() {
			return this.errorCode;
		}

		@Nullable
		public String getErrorDetailCode() {
			return this.errorDetailCode;
		}

		@Nonnull
		public List<String> getErrorDetailCodes() {
			return this.errorDetailCodes;
		}
	}
}
