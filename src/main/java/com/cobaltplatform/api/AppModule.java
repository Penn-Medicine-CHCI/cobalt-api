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

package com.cobaltplatform.api;

import com.cobaltplatform.api.cache.Cache;
import com.cobaltplatform.api.cache.CaffeineCache;
import com.cobaltplatform.api.cache.CurrentContextCache;
import com.cobaltplatform.api.cache.DistributedCache;
import com.cobaltplatform.api.cache.LocalCache;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.context.DatabaseContext;
import com.cobaltplatform.api.context.DatabaseContextExecutor;
import com.cobaltplatform.api.error.ConsoleErrorReporter;
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.error.SentryErrorReporter;
import com.cobaltplatform.api.integration.acuity.AcuitySchedulingClient;
import com.cobaltplatform.api.integration.acuity.DefaultAcuitySchedulingClient;
import com.cobaltplatform.api.integration.acuity.MockAcuitySchedulingClient;
import com.cobaltplatform.api.integration.amazon.AmazonSnsRequestValidator;
import com.cobaltplatform.api.integration.amazon.DefaultAmazonSnsRequestValidator;
import com.cobaltplatform.api.integration.bluejeans.BluejeansApi;
import com.cobaltplatform.api.integration.bluejeans.BluejeansClient;
import com.cobaltplatform.api.integration.bluejeans.DefaultBluejeansClient;
import com.cobaltplatform.api.integration.bluejeans.MockBluejeansClient;
import com.cobaltplatform.api.integration.twilio.DefaultTwilioErrorResolver;
import com.cobaltplatform.api.integration.twilio.MockTwilioRequestValidator;
import com.cobaltplatform.api.integration.twilio.TwilioErrorResolver;
import com.cobaltplatform.api.integration.twilio.TwilioRequestValidator;
import com.cobaltplatform.api.integration.way2health.DefaultWay2HealthClient;
import com.cobaltplatform.api.integration.way2health.MockWay2HealthClient;
import com.cobaltplatform.api.integration.way2health.Way2HealthClient;
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.messaging.MessageSerializer;
import com.cobaltplatform.api.messaging.call.CallMessage;
import com.cobaltplatform.api.messaging.call.CallMessageSerializer;
import com.cobaltplatform.api.messaging.email.AmazonSesEmailMessageSender;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageSerializer;
import com.cobaltplatform.api.messaging.email.MailpitEmailMessageSender;
import com.cobaltplatform.api.messaging.push.PushMessage;
import com.cobaltplatform.api.messaging.push.PushMessageSerializer;
import com.cobaltplatform.api.messaging.sms.SmsMessage;
import com.cobaltplatform.api.messaging.sms.SmsMessageSerializer;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AccountCheckInActionApiResponse.AccountCheckInActionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AccountCheckInApiResponse.AccountCheckInApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AccountSessionApiResponse.AccountSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AccountSourceApiResponse.AccountSourceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AccountStudyApiResponse.AccountStudyApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ActivityTrackingApiResponse.ActivityTrackingApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AddressApiResponse.AddressApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AdminContentApiResponse.AdminContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AdminInstitutionApiResponse.AdminInstitutionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AlertApiResponse.AlertApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AnalyticsReportGroupApiResponse.AnalyticsReportGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentTimeApiResponse.AppointmentTimeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentTypeApiResponse.AppointmentTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AssessmentApiResponse.AssessmentQuestionAnswerApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AssessmentFormApiResponse.AssessmentFormApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AvailabilityTimeApiResponse.AvailabilityTimeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.BetaFeatureAlertApiResponse.BetaFeatureAlertApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CallToActionApiResponse.CallToActionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CareResourceApiResponse.CareResourceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CareResourceLocationApiResponse.CareResourceLocationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CareResourceTagApiResponse.CareResourceTagApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ClientDeviceActivityApiResponse.ClientDeviceActivityApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ClientDeviceApiResponse.ClientDeviceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ClientDevicePushTokenApiResponse.ClientDevicePushTokenApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ClinicApiResponse.ClinicApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentAudienceTypeApiResponse.ContentAudienceTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentAudienceTypeGroupApiResponse.ContentAudienceTypeGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentFeedbackApiResponse.ContentFeedbackApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentStatusApiResponse.ContentStatusApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CountryApiResponse.CountryApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CourseApiResponse.CourseApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CourseModuleApiResponse.CourseModuleApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CourseSessionApiResponse.CourseSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CourseUnitApiResponse.CourseUnitApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CourseUnitDownloadableFileApiResponse.CourseUnitDownloadableFileApiResponseFactory;
import com.cobaltplatform.api.model.api.response.EncounterApiResponse.EncounterApiResponseFactory;
import com.cobaltplatform.api.model.api.response.EpicDepartmentApiResponse.EpicDepartmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FaqApiResponse.FaqApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FaqSubtopicApiResponse.FaqSubtopicApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FaqTopicApiResponse.FaqTopicApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FeatureApiResponse.FeatureApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FileUploadResultApiResponse.FileUploadResultApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FilterApiResponse.FilterApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FollowupApiResponse.FollowupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupRequestApiResponse.GroupRequestApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse.GroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionCollectionApiResponse.GroupSessionCollectionResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionCollectionWithGroupSessionsApiResponse.GroupSessionCollectionWithGroupSessionsResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionRequestApiResponse.GroupSessionRequestApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionReservationApiResponse.GroupSessionReservationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionResponseApiResponse.GroupSessionResponseApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionSuggestionApiResponse.GroupSessionSuggestionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionUrlValidationResultApiResponse.GroupSessionAutocompleteResultApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupTopicApiResponse.GroupTopicApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse.InstitutionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionBlurbApiResponse.InstitutionBlurbApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionFeatureInstitutionReferrerApiResponse.InstitutionFeatureInstitutionReferrerApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionLocationApiResponse.InstitutionLocationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionReferrerApiResponse.InstitutionReferrerApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionResourceApiResponse.InstitutionResourceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionResourceGroupApiResponse.InstitutionResourceGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionTeamMemberApiResponse.InstitutionTeamMemberApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InteractionInstanceApiResponse.InteractionInstanceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InteractionOptionActionApiResponse.InteractionOptionActionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InteractionOptionApiResponse.InteractionOptionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.IntroAssessmentApiResponse.IntroAssessmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.LanguageApiResponse.LanguageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.LogicalAvailabilityApiResponse.LogicalAvailabilityApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageApiResponse.PageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowApiResponse.PageRowApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowColumnApiResponse.PageRowImageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowContentApiResponse.PageRowContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowCustomOneColumnApiResponse.PageCustomOneColumnApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowCustomThreeColumnApiResponse.PageCustomThreeColumnApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowCustomTwoColumnApiResponse.PageCustomTwoColumnApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowGroupSessionApiResponse.PageRowGroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowMailingListApiResponse.PageRowMailingListApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowTagApiResponse.PageRowTagApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowTagGroupApiResponse.PageRowTagGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageSectionApiResponse.PageSectionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageSiteLocationApiResponse.PageSiteLocationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageUrlValidationResultApiResponse.PageAutocompleteResultApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderApiResponse.PatientOrderApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderAutocompleteResultApiResponse.PatientOrderAutocompleteResultApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderDiagnosisApiResponse.PatientOrderDiagnosisApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderMedicationApiResponse.PatientOrderMedicationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderNoteApiResponse.PatientOrderNoteApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderOutreachApiResponse.PatientOrderOutreachApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledMessageApiResponse.PatientOrderScheduledMessageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledMessageGroupApiResponse.PatientOrderScheduledMessageGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledOutreachApiResponse.PatientOrderScheduledOutreachApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledScreeningApiResponse.PatientOrderScheduledScreeningApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderVoicemailTaskApiResponse.PatientOrderVoicemailTaskApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PinboardNoteApiResponse.PinboardNoteApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PresignedUploadApiResponse.PresignedUploadApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse.ProviderApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderCalendarApiResponse.ProviderCalendarApiResponseFactory;
import com.cobaltplatform.api.model.api.response.QuestionApiResponse.QuestionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ReportTypeApiResponse.ReportTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ResourcePacketApiResponse.ResourcePacketApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ResourcePacketCareResourceLocationApiResponse.ResourcePacketCareResourceLocationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningAnswerApiResponse.ScreeningAnswerApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningAnswerOptionApiResponse.ScreeningAnswerOptionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningApiResponse.ScreeningApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningConfirmationPromptApiResponse.ScreeningConfirmationPromptApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningFlowApiResponse.ScreeningFlowApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningFlowVersionApiResponse.ScreeningFlowVersionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningQuestionApiResponse.ScreeningQuestionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningSessionApiResponse.ScreeningSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningTypeApiResponse.ScreeningTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningVersionApiResponse.ScreeningVersionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.SpecialtyApiResponse.SpecialtyApiResponseFactory;
import com.cobaltplatform.api.model.api.response.StudyAccountApiResponse.StudyAccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.StudyApiResponse.StudyApiResponseFactory;
import com.cobaltplatform.api.model.api.response.SupportRoleApiResponse.SupportRoleApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TagApiResponse.TagApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TagGroupApiResponse.TagGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse.TimeZoneApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TopicCenterApiResponse.TopicCenterApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TopicCenterRowApiResponse.TopicCenterRowApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TopicCenterRowTagApiResponse.TopicCenterRowTagApiResponseFactory;
import com.cobaltplatform.api.model.api.response.VideoApiResponse.VideoApiResponseFactory;
import com.cobaltplatform.api.model.api.response.VisitTypeApiResponse.VisitTypeApiResponseFactory;
import com.cobaltplatform.api.model.service.ScreeningQuestionContextId;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.AwsSecretManagerClient;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.HandlebarsTemplater;
import com.cobaltplatform.api.util.HttpLoggingInterceptor;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.JsonMapper.MappingNullability;
import com.cobaltplatform.api.util.LoggingUtility;
import com.cobaltplatform.api.util.db.ReadReplica;
import com.cobaltplatform.api.util.db.WritableMaster;
import com.cobaltplatform.api.web.filter.AuthorizationFilter;
import com.cobaltplatform.api.web.filter.DatabaseFilter;
import com.cobaltplatform.api.web.filter.DebuggingFilter;
import com.cobaltplatform.api.web.filter.MaintenanceFilter;
import com.cobaltplatform.api.web.request.CurrentContextRequestHandler;
import com.cobaltplatform.api.web.response.JsonApiResponseWriter;
import com.cobaltplatform.api.web.response.JsonPageResponseWriter;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lokalized.DefaultStrings;
import com.lokalized.LocalizedStringLoader;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.pyranid.StatementLog;
import com.pyranid.StatementLogger;
import com.soklet.converter.AbstractValueConverter;
import com.soklet.converter.ValueConversionException;
import com.soklet.converter.ValueConverterRegistry;
import com.soklet.jetty.JettyServer;
import com.soklet.util.FormatUtils;
import com.soklet.util.InstanceProvider;
import com.soklet.web.HashedUrlManifest;
import com.soklet.web.exception.MethodNotAllowedException;
import com.soklet.web.request.DefaultRequestHandler;
import com.soklet.web.request.RequestContext;
import com.soklet.web.request.RequestHandler;
import com.soklet.web.request.SokletFilter;
import com.soklet.web.response.ResponseHandler;
import com.soklet.web.response.writer.ApiResponseWriter;
import com.soklet.web.response.writer.PageResponseWriter;
import com.soklet.web.routing.Route;
import com.soklet.web.routing.RouteMatcher;
import com.soklet.web.server.FilterConfiguration;
import com.soklet.web.server.Server;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import okhttp3.OkHttpClient;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AppModule extends AbstractModule {
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Logger logger;

	public AppModule(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		this.configuration = configuration;
		this.logger = LoggerFactory.
				getLogger(getClass());
	}

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder().build(AssessmentQuestionAnswerApiResponseFactory.class));
		install(new FactoryModuleBuilder().build(AccountApiResponseFactory.class));
		install((new FactoryModuleBuilder().build(ContentApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ProviderApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AvailabilityTimeApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AppointmentApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ActivityTrackingApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(IntroAssessmentApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(SupportRoleApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ClinicApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InstitutionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AccountSourceApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(GroupSessionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(GroupSessionRequestApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(GroupSessionResponseApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(GroupSessionReservationApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PresignedUploadApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AssessmentFormApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AdminContentApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AdminInstitutionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AppointmentTypeApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(LogicalAvailabilityApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(FollowupApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(BetaFeatureAlertApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(TimeZoneApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(QuestionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InteractionInstanceApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InteractionOptionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InteractionOptionActionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(SpecialtyApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AccountSessionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ProviderCalendarApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ScreeningSessionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ScreeningQuestionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ScreeningAnswerOptionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ScreeningAnswerApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(VisitTypeApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ScreeningFlowVersionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(CallToActionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PinboardNoteApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(TopicCenterApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(TopicCenterRowApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ScreeningConfirmationPromptApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(CountryApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(LanguageApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AddressApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(TagApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(TagGroupApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(GroupRequestApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(GroupTopicApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ReportTypeApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InstitutionBlurbApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InstitutionTeamMemberApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PatientOrderApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PatientOrderNoteApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PatientOrderDiagnosisApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PatientOrderMedicationApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PatientOrderOutreachApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InstitutionLocationApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PatientOrderAutocompleteResultApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(FilterApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AppointmentTimeApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(FeatureApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(FeatureApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PatientOrderScheduledMessageApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PatientOrderScheduledMessageGroupApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AlertApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PatientOrderScheduledScreeningApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ScreeningTypeApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PatientOrderVoicemailTaskApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(GroupSessionCollectionResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ScreeningApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ScreeningFlowApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(GroupSessionAutocompleteResultApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(GroupSessionCollectionWithGroupSessionsResponseFactory.class)));
		install((new FactoryModuleBuilder().build(GroupSessionSuggestionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InstitutionResourceGroupApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InstitutionResourceApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(FaqApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(FaqTopicApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AccountCheckInApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AccountCheckInActionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(StudyAccountApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(TopicCenterRowTagApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ContentStatusApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(FileUploadResultApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(StudyApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ScreeningVersionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ClientDeviceApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ClientDevicePushTokenApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ContentFeedbackApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(EpicDepartmentApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(EncounterApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ClientDeviceActivityApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PatientOrderScheduledOutreachApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(FaqSubtopicApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AccountStudyApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ContentAudienceTypeApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ContentAudienceTypeGroupApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageSectionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageRowApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageRowContentApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageRowImageApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageRowGroupSessionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageRowTagGroupApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InstitutionReferrerApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InstitutionFeatureInstitutionReferrerApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(CareResourceTagApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(CareResourceApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(CareResourceLocationApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ResourcePacketApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ResourcePacketCareResourceLocationApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageCustomOneColumnApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageCustomTwoColumnApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageCustomThreeColumnApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageAutocompleteResultApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageSiteLocationApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageRowTagApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(PageRowMailingListApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(CourseApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(CourseModuleApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(CourseUnitApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(CourseSessionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(VideoApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(CourseUnitDownloadableFileApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AnalyticsReportGroupApiResponseFactory.class)));
	}

	@Provides
	@Singleton
	@Nonnull
	public Configuration provideConfiguration() {
		return this.configuration;
	}

	@Nonnull
	@Provides
	@Singleton
	@ReadReplica
	public Database provideReadReplicaDatabase(@Nonnull Injector injector,
																						 @ReadReplica @Nonnull DataSource dataSource,
																						 @Nonnull Provider<Optional<DatabaseContext>> databaseContextProvider) {
		requireNonNull(injector);
		requireNonNull(dataSource);
		requireNonNull(databaseContextProvider);

		return Database.forDataSource(dataSource)
				.instanceProvider(injector::getInstance)
				.statementLogger(new StatementLogger() {
					@Override
					public void log(StatementLog statementLog) {
						DatabaseContext databaseContext = databaseContextProvider.get().orElse(null);

						if (databaseContext != null)
							databaseContext.addStatementLog(statementLog);
					}
				})
				.build();
	}

	@Nonnull
	@Provides
	@Singleton
	@WritableMaster
	public Database provideWritableMasterDatabase(@Nonnull Injector injector,
																								@WritableMaster @Nonnull DataSource dataSource,
																								@Nonnull Provider<Optional<DatabaseContext>> databaseContextProvider) {
		requireNonNull(injector);
		requireNonNull(dataSource);
		requireNonNull(databaseContextProvider);

		return Database.forDataSource(dataSource)
				.instanceProvider(injector::getInstance)
				.statementLogger(new StatementLogger() {
					@Override
					public void log(StatementLog statementLog) {
						DatabaseContext databaseContext = databaseContextProvider.get().orElse(null);

						if (databaseContext != null)
							databaseContext.addStatementLog(statementLog);
					}
				})
				.build();
	}

	@Nonnull
	@Provides
	@Singleton
	@ReadReplica
	public DataSource provideReadReplicaDataSource(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		return new HikariDataSource(new HikariConfig() {
			{
				setJdbcUrl(getConfiguration().getJdbcReadReplicaUrl());
				setUsername(getConfiguration().getJdbcReadReplicaUsername());
				setPassword(getConfiguration().getJdbcReadReplicaPassword());
				setMaximumPoolSize(getConfiguration().getJdbcReadReplicaMaximumPoolSize());
				setMaxLifetime(45_000); // 45 seconds to ensure DNS switching between read replica instances
			}
		});
	}

	@Nonnull
	@Provides
	@Singleton
	@WritableMaster
	public DataSource provideWritableMasterDataSource(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		return new HikariDataSource(new HikariConfig() {
			{
				setJdbcUrl(getConfiguration().getJdbcUrl());
				setUsername(getConfiguration().getJdbcUsername());
				setPassword(getConfiguration().getJdbcPassword());
				setMaximumPoolSize(getConfiguration().getJdbcMaximumPoolSize());
				setAutoCommit(true);
			}
		});
	}

	@Provides
	@Singleton
	@Nonnull
	public Server provideServer(@Nonnull InstanceProvider instanceProvider, @Nonnull Configuration configuration) {
		requireNonNull(instanceProvider);
		requireNonNull(configuration);

		return JettyServer.forInstanceProvider(instanceProvider)
				.host(getConfiguration().getHost())
				.port(getConfiguration().getPort())
				// .staticFilesConfiguration(new StaticFilesConfiguration("/static/*", Paths.get("web/public"), getConfiguration().getStaticFileCacheStrategy()))
				.filterConfigurations(new ArrayList<FilterConfiguration>() {{
					add(new FilterConfiguration(CrossOriginFilter.class, "/*",
							new HashMap<String, String>() {{
								put(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,PUT,DELETE,PATCH");
								put(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, getConfiguration().getCorsEnabledDomains());
								put(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "*");
								put(CrossOriginFilter.EXPOSED_HEADERS_PARAM, "Content-Disposition, X-Cobalt-Checksum");
							}}));

					add(new FilterConfiguration(MaintenanceFilter.class, "/*"));
					add(new FilterConfiguration(DatabaseFilter.class, "/*"));
					add(new FilterConfiguration(AuthorizationFilter.class, "/*"));

					if (getConfiguration().getShouldEnableDebuggingHeaders())
						add(new FilterConfiguration(DebuggingFilter.class, "/*"));
				}})
				.build();
	}

	@Provides
	@Singleton
	@Nonnull
	public SokletFilter provideSokletFilter(@Nonnull RouteMatcher routeMatcher,
																					@Nonnull ResponseHandler responseHandler,
																					@Nonnull ErrorReporter errorReporter,
																					@Nonnull CurrentContextRequestHandler currentContextRequestHandler) {
		requireNonNull(routeMatcher);
		requireNonNull(responseHandler);
		requireNonNull(errorReporter);
		requireNonNull(currentContextRequestHandler);

		return new SokletFilter(routeMatcher, responseHandler) {
			@Override
			protected void handleRequest(RequestContext requestContext, FilterChain filterChain) {
				// CORS preflights should skip over current context handling
				if (requestContext.httpServletRequest().getMethod().equals("OPTIONS")) {
					super.handleRequest(requestContext, filterChain);
				} else {
					currentContextRequestHandler.handle(requestContext.httpServletRequest(), () -> {
						super.handleRequest(requestContext, filterChain);
					});
				}
			}

			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
				try {
					super.doFilter(servletRequest, servletResponse, filterChain);
				} finally {
					MDC.remove(LoggingUtility.CURRENT_CONTEXT_LOGGING_KEY);
				}
			}

			@Override
			protected void logException(HttpServletRequest httpServletRequest,
																	HttpServletResponse httpServletResponse,
																	Optional<Route> route,
																	Optional<Object> response,
																	Exception exception) {
				if (exception instanceof MethodNotAllowedException)
					getLogger().warn("Method not allowed: {}", exception.getMessage());
				else
					errorReporter.report(exception);
			}

			@Override
			protected void logRequestStart(HttpServletRequest httpServletRequest) {
				// We perform our own request logging in CurrentContextRequestHandler
			}

			@Override
			protected void logRequestEnd(HttpServletRequest httpServletRequest, long elapsedNanoTime) {
				// Slow request (10 seconds or longer)?  Log a warning regardless of whether we do our normal logging
				if (elapsedNanoTime > 10_000_000_000L)
					getLogger().warn(format("SLOW REQUEST (%.2fms): %s", (float) elapsedNanoTime / 1000000.0F, FormatUtils.httpServletRequestDescription(httpServletRequest)));

				if (shouldLog(httpServletRequest))
					super.logRequestEnd(httpServletRequest, elapsedNanoTime);
			}

			@Nonnull
			protected Boolean shouldLog(@Nonnull HttpServletRequest httpServletRequest) {
				requireNonNull(httpServletRequest);

				boolean staticFile = httpServletRequest.getRequestURI().startsWith("/static/");
				boolean healthCheck = httpServletRequest.getRequestURI().startsWith("/system/health-check");
				boolean analytics = Objects.equals(httpServletRequest.getHeader("X-Cobalt-Analytics"), "true");
				boolean performingAutorefresh = Objects.equals(httpServletRequest.getHeader("X-Cobalt-Autorefresh"), "true");

				return !staticFile && !healthCheck && !analytics && !performingAutorefresh;
			}
		};
	}

	@Provides
	@Singleton
	@Nonnull
	public ApiResponseWriter provideApiResponseWriter(@Nonnull Configuration configuration,
																										@Nonnull JsonMapper jsonMapper,
																										@Nonnull Formatter formatter,
																										@Nonnull Strings strings) {
		requireNonNull(configuration);
		requireNonNull(jsonMapper);
		requireNonNull(formatter);
		requireNonNull(strings);

		return new JsonApiResponseWriter(configuration, jsonMapper, formatter, strings);
	}

	@Provides
	@Singleton
	@Nonnull
	public AcuitySchedulingClient provideAcuitySchedulingClient(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		if (configuration.getShouldUseRealAcuity())
			return new DefaultAcuitySchedulingClient(configuration);

		return new MockAcuitySchedulingClient();
	}

	@Provides
	@Singleton
	@Nonnull
	public PageResponseWriter providePageResponseWriter(@Nonnull Configuration configuration,
																											@Nonnull JsonMapper jsonMapper,
																											@Nonnull Formatter formatter,
																											@Nonnull Strings strings) {
		requireNonNull(configuration);
		requireNonNull(jsonMapper);
		requireNonNull(formatter);
		requireNonNull(strings);

		return new JsonPageResponseWriter(configuration, jsonMapper, formatter, strings);
	}

	@Provides
	@Singleton
	@Nonnull
	public AwsSecretManagerClient provideAwsSecretManagerClient(@Nonnull Configuration configuration) {
		// Should always be available now that we have migrated away from other methods of storing secrets
		return configuration.getSecretManagerClient().get();
	}

	@Provides
	@Singleton
	@Nonnull
	public Strings provideStrings(@Nonnull CurrentContextExecutor currentContextExecutor) {
		requireNonNull(currentContextExecutor);

		final String FALLBACK_LANGUAGE_CODE = "en";
		final Locale FALLBACK_LOCALE = Locale.forLanguageTag(FALLBACK_LANGUAGE_CODE);

		return new DefaultStrings.Builder(FALLBACK_LANGUAGE_CODE, () -> LocalizedStringLoader.loadFromFilesystem(Paths.get("messages/strings")))
				.localeSupplier(() -> currentContextExecutor.getCurrentContext().isPresent() ? currentContextExecutor.getCurrentContext().get().getLocale() : FALLBACK_LOCALE)
				.build();
	}

	@Provides
	@Singleton
	@Nonnull
	public MessageSerializer<EmailMessage> provideEmailMessageSerializer(@Nonnull EmailMessageSerializer emailMessageSerializer) {
		requireNonNull(emailMessageSerializer);
		return emailMessageSerializer;
	}

	@Provides
	@Singleton
	@Nonnull
	public MessageSender<EmailMessage> provideEmailMessageSender(@Nonnull Provider<InstitutionService> institutionServiceProvider,
																															 @Nonnull Configuration configuration) {
		requireNonNull(institutionServiceProvider);
		requireNonNull(configuration);

		HandlebarsTemplater handlebarsTemplater = new HandlebarsTemplater.Builder(Paths.get("messages/email"))
				.viewsDirectoryName("views")
				.shouldCacheTemplates(configuration.getShouldCacheHandlebarsTemplates())
				.build();

		if (getConfiguration().getShouldSendRealEmailMessages())
			return new AmazonSesEmailMessageSender(institutionServiceProvider, handlebarsTemplater, configuration);

		// We now prefer MailpitEmailMessageSender to ConsoleEmailMessageSender
		return new MailpitEmailMessageSender(institutionServiceProvider, handlebarsTemplater, configuration);
	}

	@Provides
	@Singleton
	@Nonnull
	public MessageSerializer<SmsMessage> provideSmsMessageSerializer(@Nonnull SmsMessageSerializer smsMessageSerializer) {
		requireNonNull(smsMessageSerializer);
		return smsMessageSerializer;
	}

	@Provides
	@Singleton
	@Nonnull
	public MessageSerializer<CallMessage> provideCallMessageSerializer(@Nonnull CallMessageSerializer callMessageSerializer) {
		requireNonNull(callMessageSerializer);
		return callMessageSerializer;
	}

	@Provides
	@Singleton
	@Nonnull
	public MessageSerializer<PushMessage> providePushMessageSerializer(@Nonnull PushMessageSerializer pushMessageSerializer) {
		requireNonNull(pushMessageSerializer);
		return pushMessageSerializer;
	}

	@Provides
	@Singleton
	@Nonnull
	public ErrorReporter provideErrorReporter(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		if (configuration.getShouldSendErrorReports())
			return new SentryErrorReporter(configuration);

		return new ConsoleErrorReporter();
	}

	@Provides
	@Singleton
	@Nonnull
	public Way2HealthClient provideWay2HealthClient(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		if (configuration.getShouldUseRealWay2Health())
			return new DefaultWay2HealthClient(configuration.getWay2HealthEnvironment(), configuration.getWay2HealthAccessToken());

		return new MockWay2HealthClient();
	}

	@Provides
	@Singleton
	@Nonnull
	public TwilioRequestValidator provideTwilioRequestValidator(@Nonnull Configuration configuration) {
		requireNonNull(configuration);
		// TODO: enable the real validator prior to moving to prod.
		// Currently disabled during testing because Twilio subaccounts cannot validate webhooks w/o Primary account AuthToken
		// return new DefaultTwilioRequestValidator(configuration);
		return new MockTwilioRequestValidator();
	}

	@Provides
	@Singleton
	@Nonnull
	public TwilioErrorResolver provideTwilioErrorResolver() {
		return new DefaultTwilioErrorResolver();
	}

	@Provides
	@Singleton
	@Nonnull
	public AmazonSnsRequestValidator provideAmazonSnsRequestValidator(@Nonnull Configuration configuration) {
		requireNonNull(configuration);
		return new DefaultAmazonSnsRequestValidator(configuration);
	}

	@Provides
	@Nonnull
	public CurrentContextExecutor provideCurrentContextExecutor(@Nonnull ErrorReporter errorReporter) {
		requireNonNull(errorReporter);
		return new CurrentContextExecutor(errorReporter);
	}

	@Provides
	@Nonnull
	public CurrentContext provideCurrentContext(@Nonnull CurrentContextExecutor currentContextExecutor) {
		requireNonNull(currentContextExecutor);

		Optional<CurrentContext> currentContext = currentContextExecutor.getCurrentContext();

		if (!currentContext.isPresent())
			throw new IllegalStateException("Current context is not available.");

		return currentContext.get();
	}

	@Provides
	@Nonnull
	public Optional<DatabaseContext> provideDatabaseContext(@Nonnull DatabaseContextExecutor databaseContextExecutor) {
		requireNonNull(databaseContextExecutor);
		return databaseContextExecutor.getDatabaseContext();
	}

	@Provides
	@Singleton
	@Nonnull
	public HashedUrlManifest provideHashedUrlManifest() {
		return new HashedUrlManifest();
	}

	@Provides
	@Singleton
	@Nonnull
	public JsonMapper provideJsonMapper(@Nonnull Provider<CurrentContext> currentContextProvider, @Nonnull Configuration configuration) {
		requireNonNull(currentContextProvider);
		requireNonNull(configuration);

		return new JsonMapper.Builder()
				.currentContextProvider(currentContextProvider)
				.mappingFormat(getConfiguration().getJsonMappingFormat())
				.mappingNullability(MappingNullability.EXCLUDE_NULLS)
				.build();
	}

	@Provides
	@Singleton
	public RequestHandler provideRequestHandler(@Nonnull InstanceProvider instanceProvider) {
		requireNonNull(instanceProvider);

		ValueConverterRegistry valueConverterRegistry = new ValueConverterRegistry();

		// Replace default UUID parsing to also discard nonprintable characters.
		// For example, helps with parsing URLs like /something/ca99837c-6978-461c-97c0-edb506705fed%E2%80%AF
		valueConverterRegistry.remove(String.class, UUID.class);
		valueConverterRegistry.add(new AbstractValueConverter<String, UUID>() {
			@Override
			public UUID convert(String from) throws ValueConversionException {
				if (from == null)
					return null;

				// See https://howtodoinjava.com/java/regex/java-clean-ascii-text-non-printable-chars/
				from = from.trim()
						// strips off all non-ASCII characters
						.replaceAll("[^\\x00-\\x7F]", "")
						// erases all the ASCII control characters
						.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
						// removes non-printable characters from Unicode
						.replaceAll("\\p{C}", "");

				// Alternatively, we could have a regex that discards any character other than alphanumeric/hyphens,
				// but that's not quite the same semantics as "discard erroneous whitespace/nonprintables"
				// from = from.replaceAll("[^A-Za-z0-9_-]", "");

				return UUID.fromString(from);
			}
		});

		// Support for special ScreeningQuestionContextId type
		valueConverterRegistry.add(new AbstractValueConverter<String, ScreeningQuestionContextId>() {
			@Override
			public ScreeningQuestionContextId convert(String from) throws ValueConversionException {
				if (from == null)
					return null;

				return new ScreeningQuestionContextId(from);
			}
		});
		valueConverterRegistry.add(new AbstractValueConverter<ScreeningQuestionContextId, String>() {
			@Override
			public String convert(ScreeningQuestionContextId from) throws ValueConversionException {
				if (from == null)
					return null;

				return from.getIdentifier();
			}
		});

		return new DefaultRequestHandler(instanceProvider, valueConverterRegistry);
	}

	@Provides
	@Singleton
	@Nonnull
	@LocalCache
	public Cache provideLocalCache() {
		return new CaffeineCache(1_000);
	}

	@Provides
	@Singleton
	@Nonnull
	@DistributedCache
	public Cache provideDistributedCache(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		// Note: this is a hack for now to only enable Redis when running locally
		// if (configuration.getEnvironment().equals("local"))
		//	return new RedisCache(configuration.getRedisHost(), configuration.getRedisPort());

		return provideLocalCache();
	}

	@Provides
	@Nonnull
	@CurrentContextCache
	public Cache provideCurrentContextCache(@Nonnull CurrentContextExecutor currentContextExecutor) {
		requireNonNull(currentContextExecutor);

		Optional<Cache> currentContextCache = currentContextExecutor.getCurrentContextCache();

		if (!currentContextCache.isPresent())
			throw new IllegalStateException("Current context is not available.");

		return currentContextCache.get();
	}

	@Provides
	@Nonnull
	@Singleton
	public BluejeansApi bluejeansApi() {
		OkHttpClient client = new OkHttpClient.Builder()
				.connectTimeout(10, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.addInterceptor(new HttpLoggingInterceptor())
				.build();

		return new Retrofit.Builder()
				.addConverterFactory(GsonConverterFactory.create())
				.baseUrl(configuration.getBluejeansApiEndpoint())
				.client(client)
				.build()
				.create(BluejeansApi.class);
	}

	@Provides
	@Nonnull
	@Singleton
	public BluejeansClient provideBluejeansClient(@Nonnull Configuration configuration,
																								@Nonnull Injector injector) {

		if (configuration.getShouldUseRealBluejeans()) {
			return injector.getInstance(DefaultBluejeansClient.class);
		} else {
			return injector.getInstance(MockBluejeansClient.class);
		}

	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}