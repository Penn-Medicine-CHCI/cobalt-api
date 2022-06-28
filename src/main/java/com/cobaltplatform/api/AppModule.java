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
import com.cobaltplatform.api.integration.bluejeans.BluejeansApi;
import com.cobaltplatform.api.integration.bluejeans.BluejeansClient;
import com.cobaltplatform.api.integration.bluejeans.DefaultBluejeansClient;
import com.cobaltplatform.api.integration.bluejeans.MockBluejeansClient;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.epic.DefaultEpicClient;
import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.integration.epic.MockEpicClient;
import com.cobaltplatform.api.integration.ic.DefaultIcClient;
import com.cobaltplatform.api.integration.ic.IcClient;
import com.cobaltplatform.api.integration.ic.MockIcClient;
import com.cobaltplatform.api.integration.way2health.DefaultWay2HealthClient;
import com.cobaltplatform.api.integration.way2health.MockWay2HealthClient;
import com.cobaltplatform.api.integration.way2health.Way2HealthClient;
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.messaging.MessageSerializer;
import com.cobaltplatform.api.messaging.call.CallMessage;
import com.cobaltplatform.api.messaging.call.CallMessageManager;
import com.cobaltplatform.api.messaging.call.CallMessageSerializer;
import com.cobaltplatform.api.messaging.call.ConsoleCallMessageSender;
import com.cobaltplatform.api.messaging.call.TwilioCallMessageSender;
import com.cobaltplatform.api.messaging.email.AmazonSesEmailMessageSender;
import com.cobaltplatform.api.messaging.email.ConsoleEmailMessageSender;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.messaging.email.EmailMessageSerializer;
import com.cobaltplatform.api.messaging.sms.ConsoleSmsMessageSender;
import com.cobaltplatform.api.messaging.sms.SmsMessage;
import com.cobaltplatform.api.messaging.sms.SmsMessageManager;
import com.cobaltplatform.api.messaging.sms.SmsMessageSerializer;
import com.cobaltplatform.api.messaging.sms.TwilioSmsMessageSender;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AccountSessionApiResponse.AccountSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AccountSourceApiResponse.AccountSourceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ActivityTrackingApiResponse.ActivityTrackingApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AdminAvailableContentApiResponse.AdminAvailableContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AdminContentApiResponse.AdminContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AdminInstitutionApiResponse.AdminInstitutionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentTypeApiResponse.AppointmentTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AssessmentApiResponse.AssessmentQuestionAnswerApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AssessmentFormApiResponse.AssessmentFormApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AvailabilityTimeApiResponse.AvailabilityTimeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.BetaFeatureAlertApiResponse.BetaFeatureAlertApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ClinicApiResponse.ClinicApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ExternalGroupEventTypeApiResponse.ExternalGroupEventTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FollowupApiResponse.FollowupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupEventApiResponse.GroupEventApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse.GroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionRequestApiResponse.GroupSessionRequestApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionReservationApiResponse.GroupSessionReservationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionResponseApiResponse.GroupSessionResponseApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse.InstitutionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InteractionInstanceApiResponse.InteractionInstanceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InteractionOptionActionApiResponse.InteractionOptionActionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InteractionOptionApiResponse.InteractionOptionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.IntroAssessmentApiResponse.IntroAssessmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.LogicalAvailabilityApiResponse.LogicalAvailabilityApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PresignedUploadApiResponse.PresignedUploadApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse.ProviderApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderCalendarApiResponse.ProviderCalendarApiResponseFactory;
import com.cobaltplatform.api.model.api.response.QuestionApiResponse.QuestionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ReportingChartApiResponse.ReportingChartApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ReportingChartApiResponse.ReportingChartElementApiResponse.ReportingChartElementApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ReportingChartApiResponse.ReportingChartMetricApiResponse.ReportingChartMetricApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningSessionApiResponse.ScreeningSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.SpecialtyApiResponse.SpecialtyApiResponseFactory;
import com.cobaltplatform.api.model.api.response.SupportRoleApiResponse.SupportRoleApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse.TimeZoneApiResponseFactory;
import com.cobaltplatform.api.model.qualifier.AuditLogged;
import com.cobaltplatform.api.model.qualifier.NotAuditLogged;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AuditLogService;
import com.cobaltplatform.api.util.AmazonSqsManager;
import com.cobaltplatform.api.util.Authenticator;
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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import com.soklet.jetty.JettyServer;
import com.soklet.util.InstanceProvider;
import com.soklet.web.HashedUrlManifest;
import com.soklet.web.request.RequestContext;
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
		install((new FactoryModuleBuilder().build(GroupEventApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AvailabilityTimeApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AppointmentApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ActivityTrackingApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(IntroAssessmentApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(SupportRoleApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ExternalGroupEventTypeApiResponseFactory.class)));
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
		install((new FactoryModuleBuilder().build(ReportingChartApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ReportingChartMetricApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ReportingChartElementApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InteractionInstanceApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InteractionOptionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(InteractionOptionActionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(SpecialtyApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(AccountSessionApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ProviderCalendarApiResponseFactory.class)));
		install((new FactoryModuleBuilder().build(ScreeningSessionApiResponseFactory.class)));
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
								put(CrossOriginFilter.EXPOSED_HEADERS_PARAM, "Content-Disposition");
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
				currentContextRequestHandler.handle(requestContext.httpServletRequest(), () -> {
					super.handleRequest(requestContext, filterChain);
				});
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

	@AuditLogged
	@Provides
	@Singleton
	@Nonnull
	public EpicClient provideAuditLoggedEpicClient(@Nonnull Configuration configuration,
																								 @Nonnull Provider<CurrentContext> currentContextProvider,
																								 @Nonnull Normalizer normalizer,
																								 @Nonnull AuditLogService auditLogService) {
		requireNonNull(configuration);
		requireNonNull(currentContextProvider);
		requireNonNull(normalizer);
		requireNonNull(auditLogService);

		if (configuration.getShouldUseRealEpic())
			return new DefaultEpicClient(configuration, auditLogService, currentContextProvider, normalizer, "com.cobaltplatform.api.integration.epic.sync.http");

		return new MockEpicClient();
	}

	@NotAuditLogged
	@Provides
	@Singleton
	@Nonnull
	public EpicClient provideNotAuditLoggedEpicClient(@Nonnull Configuration configuration,
																										@Nonnull Normalizer normalizer) {
		requireNonNull(configuration);
		requireNonNull(normalizer);

		if (configuration.getShouldUseRealEpic())
			return new DefaultEpicClient(configuration, null, null, normalizer, "com.cobaltplatform.api.integration.epic.sync.http");

		return new MockEpicClient();
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
	public EmailMessageManager provideEmailMessageManager(@Nonnull EnterprisePluginProvider enterprisePluginProvider,
																												@Nonnull Provider<AccountService> accountServiceProvider,
																												@Nonnull Database database,
																												@Nonnull Configuration configuration,
																												@Nonnull Formatter formatter,
																												@Nonnull MessageSender<EmailMessage> messageSender,
																												@Nonnull MessageSerializer<EmailMessage> messageSerializer) {
		requireNonNull(enterprisePluginProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(formatter);
		requireNonNull(messageSender);
		requireNonNull(messageSerializer);

		return new EmailMessageManager(enterprisePluginProvider, accountServiceProvider, database, configuration, formatter, messageSender, messageSerializer, (processingFunction) -> {
			return new AmazonSqsManager.Builder(getConfiguration().getAmazonSqsEmailMessageQueueName(),
					getConfiguration().getAmazonSqsRegion())
					.useLocalstack(getConfiguration().getAmazonUseLocalstack())
					.localstackPort(getConfiguration().getAmazonLocalstackPort())
					.queueWaitTimeSeconds(getConfiguration().getAmazonSqsEmailMessageQueueWaitTimeSeconds())
					.processingThreadCount(3)
					.processingThreadFactory(new ThreadFactoryBuilder().setNameFormat("sqs-email-processing-task-%d").build())
					.processingFunction(processingFunction)
					.build();
		});
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
	public SmsMessageManager provideSmsMessageManager(@Nonnull Provider<AccountService> accountServiceProvider,
																										@Nonnull Database database,
																										@Nonnull Configuration configuration,
																										@Nonnull Formatter formatter,
																										@Nonnull MessageSender<SmsMessage> messageSender,
																										@Nonnull MessageSerializer<SmsMessage> messageSerializer) {
		requireNonNull(accountServiceProvider);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(formatter);
		requireNonNull(messageSender);
		requireNonNull(messageSerializer);

		return new SmsMessageManager(accountServiceProvider, database, configuration, formatter, messageSender, messageSerializer, (processingFunction) -> {
			return new AmazonSqsManager.Builder(getConfiguration().getAmazonSqsSmsMessageQueueName(),
					getConfiguration().getAmazonSqsRegion())
					.useLocalstack(getConfiguration().getAmazonUseLocalstack())
					.localstackPort(getConfiguration().getAmazonLocalstackPort())
					.queueWaitTimeSeconds(getConfiguration().getAmazonSqsSmsMessageQueueWaitTimeSeconds())
					.processingThreadCount(2)
					.processingThreadFactory(new ThreadFactoryBuilder().setNameFormat("sqs-sms-processing-task-%d").build())
					.processingFunction(processingFunction)
					.build();
		});
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
	public CallMessageManager provideCallMessageManager(@Nonnull Provider<AccountService> accountServiceProvider,
																											@Nonnull Database database,
																											@Nonnull Configuration configuration,
																											@Nonnull Formatter formatter,
																											@Nonnull MessageSender<CallMessage> messageSender,
																											@Nonnull MessageSerializer<CallMessage> messageSerializer) {
		requireNonNull(accountServiceProvider);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(formatter);
		requireNonNull(messageSender);
		requireNonNull(messageSerializer);

		return new CallMessageManager(accountServiceProvider, database, configuration, formatter, messageSender, messageSerializer, (processingFunction) -> {
			return new AmazonSqsManager.Builder(getConfiguration().getAmazonSqsCallMessageQueueName(),
					getConfiguration().getAmazonSqsRegion())
					.useLocalstack(getConfiguration().getAmazonUseLocalstack())
					.localstackPort(getConfiguration().getAmazonLocalstackPort())
					.queueWaitTimeSeconds(getConfiguration().getAmazonSqsSmsMessageQueueWaitTimeSeconds())
					.processingThreadCount(2)
					.processingThreadFactory(new ThreadFactoryBuilder().setNameFormat("sqs-call-processing-task-%d").build())
					.processingFunction(processingFunction)
					.build();
		});
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
	public IcClient provideIcClient(@Nonnull Authenticator authenticator,
																	@Nonnull Configuration configuration) {
		requireNonNull(authenticator);
		requireNonNull(configuration);

		if (configuration.getShouldUseRealIc())
			return new DefaultIcClient(authenticator, configuration);

		return new MockIcClient();
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