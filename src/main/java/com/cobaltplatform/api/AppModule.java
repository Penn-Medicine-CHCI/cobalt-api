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
import com.cobaltplatform.api.messaging.call.ConsoleCallMessageSender;
import com.cobaltplatform.api.messaging.call.TwilioCallMessageSender;
import com.cobaltplatform.api.messaging.email.AmazonSesEmailMessageSender;
import com.cobaltplatform.api.messaging.email.ConsoleEmailMessageSender;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageSerializer;
import com.cobaltplatform.api.messaging.sms.ConsoleSmsMessageSender;
import com.cobaltplatform.api.messaging.sms.SmsMessage;
import com.cobaltplatform.api.messaging.sms.SmsMessageSerializer;
import com.cobaltplatform.api.messaging.sms.TwilioSmsMessageSender;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AccountCheckInApiResponse.AccountCheckInApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AccountSessionApiResponse.AccountSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AccountSourceApiResponse.AccountSourceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ActivityTrackingApiResponse.ActivityTrackingApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AddressApiResponse.AddressApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AdminAvailableContentApiResponse.AdminAvailableContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AdminContentApiResponse.AdminContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AdminInstitutionApiResponse.AdminInstitutionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AlertApiResponse.AlertApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentTimeApiResponse.AppointmentTimeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentTypeApiResponse.AppointmentTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AssessmentApiResponse.AssessmentQuestionAnswerApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AssessmentFormApiResponse.AssessmentFormApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AvailabilityTimeApiResponse.AvailabilityTimeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.BetaFeatureAlertApiResponse.BetaFeatureAlertApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CallToActionApiResponse.CallToActionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ClinicApiResponse.ClinicApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CountryApiResponse.CountryApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FaqApiResponse.FaqApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FaqTopicApiResponse.FaqTopicApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FeatureApiResponse.FeatureApiResponseFactory;
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
import com.cobaltplatform.api.model.api.response.InstitutionLocationApiResponse.InstitutionLocationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionResourceApiResponse.InstitutionResourceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionResourceGroupApiResponse.InstitutionResourceGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionTeamMemberApiResponse.InstitutionTeamMemberApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InteractionInstanceApiResponse.InteractionInstanceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InteractionOptionActionApiResponse.InteractionOptionActionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InteractionOptionApiResponse.InteractionOptionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.IntroAssessmentApiResponse.IntroAssessmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.LanguageApiResponse.LanguageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.LogicalAvailabilityApiResponse.LogicalAvailabilityApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderApiResponse.PatientOrderApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderAutocompleteResultApiResponse.PatientOrderAutocompleteResultApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderDiagnosisApiResponse.PatientOrderDiagnosisApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderMedicationApiResponse.PatientOrderMedicationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderNoteApiResponse.PatientOrderNoteApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderOutreachApiResponse.PatientOrderOutreachApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledMessageApiResponse.PatientOrderScheduledMessageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledMessageGroupApiResponse.PatientOrderScheduledMessageGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledScreeningApiResponse.PatientOrderScheduledScreeningApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderVoicemailTaskApiResponse.PatientOrderVoicemailTaskApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PinboardNoteApiResponse.PinboardNoteApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PresignedUploadApiResponse.PresignedUploadApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse.ProviderApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderCalendarApiResponse.ProviderCalendarApiResponseFactory;
import com.cobaltplatform.api.model.api.response.QuestionApiResponse.QuestionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ReportTypeApiResponse.ReportTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningAnswerApiResponse.ScreeningAnswerApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningAnswerOptionApiResponse.ScreeningAnswerOptionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningApiResponse.ScreeningApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningConfirmationPromptApiResponse.ScreeningConfirmationPromptApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningFlowApiResponse.ScreeningFlowApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningFlowVersionApiResponse.ScreeningFlowVersionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningQuestionApiResponse.ScreeningQuestionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningSessionApiResponse.ScreeningSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningTypeApiResponse.ScreeningTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.SpecialtyApiResponse.SpecialtyApiResponseFactory;
import com.cobaltplatform.api.model.api.response.SupportRoleApiResponse.SupportRoleApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TagApiResponse.TagApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TagGroupApiResponse.TagGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse.TimeZoneApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TopicCenterApiResponse.TopicCenterApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TopicCenterRowApiResponse.TopicCenterRowApiResponseFactory;
import com.cobaltplatform.api.model.api.response.VisitTypeApiResponse.VisitTypeApiResponseFactory;
import com.cobaltplatform.api.model.service.ScreeningQuestionContextId;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.HandlebarsTemplater;
import com.cobaltplatform.api.util.HttpLoggingInterceptor;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.JsonMapper.MappingNullability;
import com.cobaltplatform.api.util.Normalizer;
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
import com.soklet.util.InstanceProvider;
import com.soklet.web.HashedUrlManifest;
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
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
		this.logger = LoggerFactory.getLogger(getClass());
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
		install((new FactoryModuleBuilder().build(AdminAvailableContentApiResponseFactory.class)));
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
	}

	@Provides
	@Singleton
	@Nonnull
	public Configuration provideConfiguration() {
		return this.configuration;
	}

	@Provides
	@Singleton
	@Nonnull
	public Database provideDatabase(@Nonnull Injector injector,
																	@Nonnull DataSource dataSource,
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

	@Provides
	@Singleton
	@Nonnull
	public DataSource provideDataSource(@Nonnull Configuration configuration) {
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
			protected void logException(HttpServletRequest httpServletRequest,
																	HttpServletResponse httpServletResponse,
																	Optional<Route> route,
																	Optional<Object> response,
																	Exception exception) {
				errorReporter.report(exception);
			}

			@Override
			protected void logRequestStart(HttpServletRequest httpServletRequest) {
				if (shouldLog(httpServletRequest))
					super.logRequestStart(httpServletRequest);
			}

			@Override
			protected void logRequestEnd(HttpServletRequest httpServletRequest, long elapsedNanoTime) {
				if (shouldLog(httpServletRequest))
					super.logRequestEnd(httpServletRequest, elapsedNanoTime);
			}

			@Nonnull
			protected Boolean shouldLog(@Nonnull HttpServletRequest httpServletRequest) {
				requireNonNull(httpServletRequest);

				boolean staticFile = httpServletRequest.getRequestURI().startsWith("/static/");
				boolean healthCheck = httpServletRequest.getRequestURI().startsWith("/system/health-check");

				return !staticFile && !healthCheck;
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
	public MessageSender<EmailMessage> provideEmailMessageSender(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		if (getConfiguration().getShouldSendRealEmailMessages()) {
			HandlebarsTemplater handlebarsTemplater = new HandlebarsTemplater(Paths.get("messages/email"), configuration, "views");
			return new AmazonSesEmailMessageSender(handlebarsTemplater, configuration);
		}

		return new ConsoleEmailMessageSender();
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
	public MessageSender<SmsMessage> provideSmsMessageSender(@Nonnull Configuration configuration,
																													 @Nonnull Normalizer normalizer) {
		requireNonNull(configuration);
		requireNonNull(normalizer);

		if (getConfiguration().getShouldSendRealSmsMessages()) {
			HandlebarsTemplater handlebarsTemplater = new HandlebarsTemplater(Paths.get("messages/sms"), configuration);
			return new TwilioSmsMessageSender(handlebarsTemplater, normalizer, configuration);
		}

		return new ConsoleSmsMessageSender();
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
	public MessageSender<CallMessage> provideCallMessageSender(@Nonnull Configuration configuration,
																														 @Nonnull Normalizer normalizer) {
		requireNonNull(configuration);
		requireNonNull(normalizer);

		if (getConfiguration().getShouldSendRealCallMessages()) {
			HandlebarsTemplater handlebarsTemplater = new HandlebarsTemplater(Paths.get("messages/call"), configuration);
			return new TwilioCallMessageSender(handlebarsTemplater, normalizer, configuration);
		}

		return new ConsoleCallMessageSender();
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