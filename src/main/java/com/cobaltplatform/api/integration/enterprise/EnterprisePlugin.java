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

package com.cobaltplatform.api.integration.enterprise;

import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.integration.epic.MyChartAccessToken;
import com.cobaltplatform.api.integration.epic.MyChartAuthenticator;
import com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest;
import com.cobaltplatform.api.integration.epic.request.ScheduleAppointmentWithInsuranceRequest;
import com.cobaltplatform.api.integration.google.GoogleAnalyticsDataClient;
import com.cobaltplatform.api.integration.google.GoogleBigQueryClient;
import com.cobaltplatform.api.integration.google.GoogleGeoClient;
import com.cobaltplatform.api.integration.google.MockGoogleAnalyticsDataClient;
import com.cobaltplatform.api.integration.google.MockGoogleBigQueryClient;
import com.cobaltplatform.api.integration.google.UnsupportedGoogleGeoClient;
import com.cobaltplatform.api.integration.hl7.model.section.Hl7OrderSection;
import com.cobaltplatform.api.integration.microsoft.MicrosoftAccessToken;
import com.cobaltplatform.api.integration.microsoft.MicrosoftAuthenticator;
import com.cobaltplatform.api.integration.microsoft.MicrosoftClient;
import com.cobaltplatform.api.integration.mixpanel.MixpanelClient;
import com.cobaltplatform.api.integration.mixpanel.MockMixpanelClient;
import com.cobaltplatform.api.integration.tableau.TableauClient;
import com.cobaltplatform.api.integration.twilio.MockTwilioRequestValidator;
import com.cobaltplatform.api.integration.twilio.TwilioRequestValidator;
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.messaging.call.CallMessage;
import com.cobaltplatform.api.messaging.call.ConsoleCallMessageSender;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.push.ConsolePushMessageSender;
import com.cobaltplatform.api.messaging.push.PushMessage;
import com.cobaltplatform.api.messaging.sms.ConsoleSmsMessageSender;
import com.cobaltplatform.api.messaging.sms.SmsMessage;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderRequest;
import com.cobaltplatform.api.model.api.request.EmailPasswordAccessTokenRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ClientDevicePushTokenType.ClientDevicePushTokenTypeId;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.model.service.CallToAction;
import com.cobaltplatform.api.model.service.CallToActionDisplayAreaId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public interface EnterprisePlugin {
	@Nonnull
	InstitutionId getInstitutionId();

	@Nonnull
	default EmailMessage customizeEmailMessage(@Nonnull EmailMessage emailMessage) {
		// No customization by default
		return emailMessage;
	}

	@Nonnull
	default Optional<String> federatedLogoutUrl(@Nullable Account account) {
		return Optional.empty();
	}

	@Nonnull
	default List<CallToAction> determineCallsToAction(@Nullable Account account,
																										@Nullable CallToActionDisplayAreaId callToActionDisplayAreaId) {
		return Collections.emptyList();
	}

	@Nonnull
	default Optional<EpicClient> epicClientForPatient(@Nonnull MyChartAccessToken myChartAccessToken) {
		return Optional.empty();
	}

	@Nonnull
	default Optional<EpicClient> epicClientForBackendService() {
		return Optional.empty();
	}

	@Nonnull
	default Optional<MyChartAuthenticator> myChartAuthenticator() {
		return Optional.empty();
	}

	@Nonnull
	default Optional<MicrosoftClient> microsoftClientForDaemon() {
		return Optional.empty();
	}

	@Nonnull
	default Optional<MicrosoftClient> microsoftClientForUser(@Nonnull MicrosoftAccessToken microsoftAccessToken) {
		return Optional.empty();
	}

	@Nonnull
	default Optional<MicrosoftAuthenticator> microsoftAuthenticator() {
		return Optional.empty();
	}

	@Nonnull
	default Optional<TableauClient> tableauClient() {
		return Optional.empty();
	}

	@Nonnull
	default Optional<MicrosoftClient> microsoftTeamsClientForDaemon() {
		return microsoftClientForDaemon();
	}

	@Nonnull
	default Optional<String> extractPatientFhirIdFromMyChartAccessToken(@Nullable MyChartAccessToken myChartAccessToken) {
		if (myChartAccessToken == null)
			return Optional.empty();

		return Optional.ofNullable((String) myChartAccessToken.getMetadata().get("patient"));
	}

	@Nonnull
	default Boolean isInstantWithinBusinessHours(@Nonnull Instant instant) {
		requireNonNull(instant);
		return instant.equals(nextInstantWithinBusinessHours(instant));
	}

	@Nonnull
	default Instant nextInstantWithinBusinessHours(@Nonnull Instant instant) {
		requireNonNull(instant);
		return instant;
	}

	@Nonnull
	default List<Content> recommendedContentForAccountId(@Nullable UUID accountId) {
		return Collections.emptyList();
	}

	@Nonnull
	default GoogleBigQueryClient googleBigQueryClient() {
		return new MockGoogleBigQueryClient();
	}

	@Nonnull
	default GoogleAnalyticsDataClient googleAnalyticsDataClient() {
		return new MockGoogleAnalyticsDataClient();
	}

	@Nonnull
	default GoogleGeoClient googleGeoClient() {
		return new UnsupportedGoogleGeoClient();
	}

	@Nonnull
	default MixpanelClient mixpanelClient() {
		return new MockMixpanelClient();
	}

	@Nonnull
	default MessageSender<PushMessage> pushMessageSenderForPushTokenTypeId(@Nonnull ClientDevicePushTokenTypeId clientDevicePushTokenTypeId) {
		return new ConsolePushMessageSender();
	}

	@Nonnull
	default MessageSender<SmsMessage> smsMessageSender() {
		return new ConsoleSmsMessageSender();
	}

	@Nonnull
	default MessageSender<CallMessage> callMessageSender() {
		return new ConsoleCallMessageSender();
	}

	@Nonnull
	default TwilioRequestValidator twilioRequestValidator() {
		return new MockTwilioRequestValidator();
	}

	@Nonnull
	default Set<UUID> analyticsClinicalScreeningFlowIds() {
		return Set.of();
	}

	// Key is screening flow ID (from above analyticsClinicalScreeningFlowIds())
	// Values are a sorted map of severity names (e.g. "Mild", "Moderate", "Severe") to counts
	@Nonnull
	default Map<UUID, SortedMap<String, Long>> analyticsClinicalScreeningSessionSeverityCountsByDescriptionByScreeningFlowId(@Nonnull Instant startTimestamp,
																																																													 @Nonnull Instant endTimestamp) {
		Set<UUID> analyticsClinicalScreeningFlowIds = analyticsClinicalScreeningFlowIds();

		return analyticsClinicalScreeningFlowIds.stream()
				.collect(Collectors.toMap(Function.identity(), ignored -> Collections.emptySortedMap()));
	}

	// For legacy data where screening flows with hard stops due to crisis still permitted users to back-button and hit "skip".
	// This would lead to a "completed=true, skipped=true, crisis_indicated=true" screening session which needs to be detected
	// to produce meaningful analytics (analytics code should not treat it as a skip).
	// Alternative would be to update all affected screening sessions in the DB to remove "skipped=true" flag.
	@Nonnull
	default Boolean analyticsClinicalScreeningFlowNeedsCrisisSkipWorkaround(@Nonnull UUID screeningFlowId) {
		requireNonNull(screeningFlowId);
		return false;
	}

	@Nonnull
	default Optional<String> fileUploadStorageKeyPrefix() {
		return Optional.empty();
	}

	default void performPatientOrderEncounterWriteback(@Nullable UUID patientOrderId,
																										 @Nullable String encounterCsn) {
		throw new UnsupportedOperationException();
	}

	default void applyCustomizationsToCreatePatientOrderRequestForHl7Order(@Nonnull CreatePatientOrderRequest request,
																																				 @Nonnull Hl7OrderSection order) {
		requireNonNull(request);
		requireNonNull(order);

		// Institutions might want to perform custom parsing of HL7 messages to create orders, e.g. freeform notes
		// fields might contain structured data that is institution-specific.
		//
		// This method gives you a hook to perform any customization needed.
		//
		// No-op by default.
	}

	// Institutions might want to tweak how a tag is displayed, e.g. change its name.
	// We support that here instead of data-driving.
	// If this is a more common scenario we might want to data-drive (e.g. a v_tag concept) in the future.
	default Tag applyCustomizationsToTag(@Nonnull Tag tag) {
		requireNonNull(tag);
		return tag;
	}

	default void applyCustomProcessingForEmailPasswordAccessTokenRequest(@Nonnull EmailPasswordAccessTokenRequest request) {
		requireNonNull(request);
		// No-op by default
	}

	default void customizeScheduleAppointmentWithInsuranceRequest(@Nonnull ScheduleAppointmentWithInsuranceRequest request,
																																@Nonnull Account account) {
		requireNonNull(request);
		requireNonNull(account);
		// No-op by default
	}

	default void customizeCancelAppointmentRequest(@Nonnull CancelAppointmentRequest request,
																								 @Nonnull Account account) {
		requireNonNull(request);
		requireNonNull(account);
		// No-op by default
	}
}
