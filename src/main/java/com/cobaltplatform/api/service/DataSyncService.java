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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.AdvisoryLock;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class DataSyncService implements AutoCloseable {
	@Nonnull
	private static final Long BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;
	@Nonnull
	private final Provider<BackgroundSyncTask> backgroundSyncTaskProvider;
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<SystemService> systemServiceProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final Object backgroundTaskLock;
	@Nonnull
	private Boolean backgroundTaskStarted;
	@Nullable
	private ScheduledExecutorService backgroundTaskExecutorService;

	static {
		BACKGROUND_TASK_INTERVAL_IN_SECONDS = 60L;
		BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS = 10L;
	}

	@Inject
	public DataSyncService(@Nonnull Provider<BackgroundSyncTask> backgroundSyncTaskProvider,
												 @Nonnull Provider<InstitutionService> institutionServiceProvider,
												 @Nonnull Provider<AccountService> accountServiceProvider,
												 @Nonnull Provider<SystemService> systemServiceProvider,
												 @Nonnull DatabaseProvider databaseProvider,
												 @Nonnull ErrorReporter errorReporter,
												 @Nonnull Configuration configuration,
												 @Nonnull Strings strings) {
		requireNonNull(backgroundSyncTaskProvider);
		requireNonNull(institutionServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(systemServiceProvider);
		requireNonNull(databaseProvider);
		requireNonNull(errorReporter);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.backgroundSyncTaskProvider = backgroundSyncTaskProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.systemServiceProvider = systemServiceProvider;
		this.databaseProvider = databaseProvider;
		this.errorReporter = errorReporter;
		this.configuration = configuration;
		this.strings = strings;
		this.backgroundTaskLock = new Object();
		this.backgroundTaskStarted = false;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void close() throws Exception {
		stopBackgroundTask();
	}

	@Nonnull
	public Boolean startBackgroundTask() {
		synchronized (getBackgroundTaskLock()) {
			if (isBackgroundTaskStarted())
				return false;

			getLogger().trace("Starting data sync background task...");

			this.backgroundTaskExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("data-sync-background-task").build());
			this.backgroundTaskStarted = true;

			getBackgroundTaskExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getBackgroundSyncTaskProvider().get().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to complete data sync background task - will retry in %s seconds", String.valueOf(getBackgroundTaskIntervalInSeconds())), e);
					}
				}
			}, getBackgroundTaskInitialDelayInSeconds(), getBackgroundTaskIntervalInSeconds(), TimeUnit.SECONDS);

			getLogger().trace("Data sync background task started.");

			return true;
		}
	}

	@Nonnull
	public Boolean stopBackgroundTask() {
		synchronized (getBackgroundTaskLock()) {
			if (!isBackgroundTaskStarted())
				return false;

			getLogger().trace("Stopping data sync background task...");

			getBackgroundTaskExecutorService().get().shutdownNow();
			this.backgroundTaskExecutorService = null;
			this.backgroundTaskStarted = false;

			getLogger().trace("Data sync background task stopped.");

			return true;
		}
	}

	@Nonnull
	public void syncData() {
		getSystemService().performAdvisoryLockOperationIfAvailable(AdvisoryLock.DATA_SYNC, () -> {
			Account serviceAccount = getAccountService().findServiceAccountByInstitutionId(InstitutionId.COBALT).get();

			getDatabase().execute("""
					INSERT INTO institution
					(institution_id,
					name,
					created,
					last_updated,
					group_session_system_id,
					time_zone,
					locale,
					require_consent_form,
					support_enabled,
					sso_enabled,
					anonymous_enabled,
					email_enabled,
					access_token_expiration_in_minutes,
					access_token_short_expiration_in_minutes,
					anon_access_token_expiration_in_minutes,
					anon_access_token_short_expiration_in_minutes,
					metadata,
					email_signup_enabled,
					support_email_address,
					recommend_group_session_requests,
					immediate_access_enabled,
					contact_us_enabled,
					integrated_care_enabled,
					epic_backend_service_auth_type_id,
					user_submitted_content_enabled,
					user_submitted_group_session_enabled,
					user_submitted_group_session_request_enabled,
					recommended_content_enabled,
					group_session_requests_enabled,
					group_session_reservation_default_followup_time_of_day,
					group_session_reservation_default_followup_day_offset,
					appointment_reservation_default_reminder_time_of_day,
					appointment_reservation_default_reminder_day_offset,
					group_session_reservation_default_reminder_minutes_offset,
					features_enabled,
					mychart_name,
					anonymous_account_expiration_strategy_id,
					epic_patient_mrn_type_name,
					epic_fhir_appointment_find_cache_expiration_in_seconds,
					epic_fhir_enabled,
					faq_enabled,
					google_bigquery_sync_enabled,
					mixpanel_sync_enabled,
					sharing_content,
					microsoft_teams_enabled,
					tableau_enabled,
					google_fcm_push_notifications_enabled,
					call_messages_enabled,
					sms_messages_enabled,
					epic_provider_slot_booking_sync_enabled,
					appointment_feedback_survey_enabled,
					appointment_feedback_survey_delay_in_minutes)
					(SELECT institution_id,
					        name,
					        created,
					        last_updated,
					        group_session_system_id,
					        time_zone,
					        locale,
					        require_consent_form,
					        support_enabled,
					        sso_enabled,
					        anonymous_enabled,
					        email_enabled,
					        access_token_expiration_in_minutes,
					        access_token_short_expiration_in_minutes,
					        anon_access_token_expiration_in_minutes,
					        anon_access_token_short_expiration_in_minutes,
					        metadata,
					        email_signup_enabled,
					        support_email_address,
					        recommend_group_session_requests,
					        immediate_access_enabled,
					        contact_us_enabled,
					        integrated_care_enabled,
					        epic_backend_service_auth_type_id,
					        user_submitted_content_enabled,
					        user_submitted_group_session_enabled,
					        user_submitted_group_session_request_enabled,
					        recommended_content_enabled,
					        group_session_requests_enabled,
					        group_session_reservation_default_followup_time_of_day,
					        group_session_reservation_default_followup_day_offset,
					        appointment_reservation_default_reminder_time_of_day,
					        appointment_reservation_default_reminder_day_offset,
					        group_session_reservation_default_reminder_minutes_offset,
					        features_enabled,
					        mychart_name,
					        anonymous_account_expiration_strategy_id,
					        epic_patient_mrn_type_name,
					        epic_fhir_appointment_find_cache_expiration_in_seconds,
					        epic_fhir_enabled,
					        faq_enabled,
					        google_bigquery_sync_enabled,
					        mixpanel_sync_enabled,
					        sharing_content,
					        microsoft_teams_enabled,
					        tableau_enabled,
					        google_fcm_push_notifications_enabled,
					        call_messages_enabled,
					        sms_messages_enabled,
					        epic_provider_slot_booking_sync_enabled,
					        appointment_feedback_survey_enabled,
					        appointment_feedback_survey_delay_in_minutes
					        FROM remote_institution ri
					        WHERE ri.institution_id NOT IN
					        (SELECT i.institution_id
					        FROM institution i))
					""");

			//Pull over any file upload rows that are used by content that we'll be pulling over
			getDatabase().execute("""
					INSERT INTO file_upload
					(file_upload_id, account_id, url, storage_key,
					filename, content_type, file_upload_type_id, filesize)
					(SELECT rfu.file_upload_id, ?, rfu.url, rfu.storage_key,
					rfu.filename, rfu.content_type, rfu.file_upload_type_id, rfu.filesize 
					FROM remote_file_upload rfu 
					WHERE rfu.file_upload_id NOT IN
					(SELECT fu.file_upload_id
					FROM file_upload fu)
					AND rfu.file_upload_id IN
					(SELECT rc1.file_upload_id 
					FROM remote_content rc1
					WHERE rc1.shared_flag=TRUE
					AND rc1.published=TRUE
				  AND rc1.deleted_flag=FALSE
					UNION ALL
					SELECT rc2.image_file_upload_id 
					FROM remote_content rc2
					WHERE rc2.shared_flag=TRUE
					AND rc2.published=TRUE
				  AND rc2.deleted_flag=FALSE));""", serviceAccount.getAccountId());

			//Pull in any new tags that do not exist in this database instance
			getDatabase().execute("""
					INSERT INTO tag 
					(tag_id, name, url_name, description, en_search_vector, tag_group_id)
					(SELECT rt.tag_id, rt.name, rt.url_name, rt.description, rt.en_search_vector, rt.tag_group_id
					FROM remote_tag rt 
					WHERE rt.tag_id NOT IN 
					(SELECT t.tag_id FROM tag t))""");

			//Pull over any content data that is shared and does not exist in this database instance
			getDatabase().execute("""
					INSERT INTO content 
					(content_id, content_type_id, title, url, date_created, description, author,
					 owner_institution_id, deleted_flag, duration_in_minutes, en_search_vector, never_embed, shared_flag,
					 search_terms, publish_start_date, publish_end_date, publish_recurring, published, file_upload_id,
					 image_file_upload_id, remote_data_flag)
					(SELECT rc.content_id, content_type_id, title, url, date_created, description, author,
					 owner_institution_id, deleted_flag, duration_in_minutes, en_search_vector, never_embed, shared_flag,
					 search_terms, publish_start_date, publish_end_date, publish_recurring, published, file_upload_id,
					 image_file_upload_id, 'TRUE'
					FROM remote_content rc
					WHERE rc.content_id NOT IN 
					(SELECT c.content_id 
					FROM content c)
					 AND rc.shared_flag=TRUE 
					 AND rc.published=TRUE
					 AND rc.deleted_flag=FALSE)""");

			//Pull over any content_tag data that we do not have in this database instance
			getDatabase().execute("""
					INSERT INTO tag_content 
					(tag_content_id, tag_id, content_id)
					(SELECT rtc.tag_content_id, rtc.tag_id, rtc.content_id 
					FROM remote_tag_content rtc, remote_content rc 
					WHERE rtc.content_id = rc.content_id
					AND rc.shared_flag=TRUE
					AND rc.published=TRUE
					AND rc.deleted_flag=FALSE
					AND rtc.tag_content_id NOT IN 
					(SELECT tc.tag_content_id 
					FROM tag_content tc, content c
					WHERE tc.content_id = c.content_id
					AND c.shared_flag=TRUE 
					AND c.published=TRUE
					AND c.deleted_flag=FALSE)
					AND NOT EXISTS
					(SELECT 'X'
					FROM tag_content tc2
					WHERE rtc.tag_id = tc2.tag_id
					 AND rtc.content_id = tc2.content_id))""");

			//Remove any tags that are no longer associated to content that we have synced over
			getDatabase().execute("""
					DELETE FROM tag_content
					WHERE tag_content_id NOT IN
					(SELECT rtc.tag_content_id 
					FROM remote_tag_content rtc)
					AND content_id IN
					(SELECT c.content_id
					FROM content c
					WHERE remote_data_flag = true)""");

			//Update any content attributes that may have changed.
			getDatabase().execute("""
					UPDATE content 
					SET content_type_id =rc.content_type_id,
					    title=rc.title,
					    url=rc.url,
					    date_created=rc.date_created,
					    description=rc.description,
					    author =rc.author,
					    deleted_flag =rc.deleted_flag,
					    duration_in_minutes=rc.duration_in_minutes,
					    en_search_vector=rc.en_search_vector,
					    never_embed =rc.never_embed,
					    shared_flag =rc.shared_flag,
					    search_terms=rc.search_terms,
					    publish_start_date=rc.publish_start_date,
					    publish_end_date=rc.publish_end_date,
					    publish_recurring =rc.publish_recurring,
					    published =rc.published,
					    file_upload_id =rc.file_upload_id,
					    image_file_upload_id=rc.image_file_upload_id
					FROM remote_content rc
					WHERE content.content_id = rc.content_id
					AND content.remote_data_flag = TRUE""");
		});


	}

	@ThreadSafe
	protected static class BackgroundSyncTask implements Runnable {
		@Nonnull
		private final Provider<DataSyncService> dataSyncServiceProvider;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final ErrorReporter errorReporter;
		@Nonnull
		private final DatabaseProvider databaseProvider;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public BackgroundSyncTask(@Nonnull Provider<DataSyncService> dataSyncServiceProvider,
															@Nonnull CurrentContextExecutor currentContextExecutor,
															@Nonnull ErrorReporter errorReporter,
															@Nonnull DatabaseProvider databaseProvider,
															@Nonnull Configuration configuration) {
			requireNonNull(dataSyncServiceProvider);
			requireNonNull(currentContextExecutor);
			requireNonNull(errorReporter);
			requireNonNull(databaseProvider);
			requireNonNull(configuration);

			this.dataSyncServiceProvider = dataSyncServiceProvider;
			this.currentContextExecutor = currentContextExecutor;
			this.errorReporter = errorReporter;
			this.databaseProvider = databaseProvider;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			CurrentContext currentContext = new CurrentContext.Builder(InstitutionId.COBALT,
					getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

			getCurrentContextExecutor().execute(currentContext, () -> {
				try {
					getDataSyncService().syncData();
				} catch (Exception e) {
					getLogger().error("Unable to sync data", e);
					getErrorReporter().report(e);
				}
			});
		}

		@Nonnull
		protected DataSyncService getDataSyncService() {
			return this.dataSyncServiceProvider.get();
		}

		@Nonnull
		protected CurrentContextExecutor getCurrentContextExecutor() {
			return this.currentContextExecutor;
		}

		@Nonnull
		protected ErrorReporter getErrorReporter() {
			return this.errorReporter;
		}

		@Nonnull
		protected Database getDatabase() {
			return this.databaseProvider.get();
		}

		@Nonnull
		protected Configuration getConfiguration() {
			return this.configuration;
		}

		@Nonnull
		protected Logger getLogger() {
			return this.logger;
		}
	}

	@Nonnull
	public Boolean isBackgroundTaskStarted() {
		synchronized (getBackgroundTaskLock()) {
			return this.backgroundTaskStarted;
		}
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected SystemService getSystemService() {
		return this.systemServiceProvider.get();
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return this.errorReporter;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected Long getBackgroundTaskIntervalInSeconds() {
		return BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected Long getBackgroundTaskInitialDelayInSeconds() {
		return BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Object getBackgroundTaskLock() {
		return this.backgroundTaskLock;
	}

	@Nonnull
	protected Provider<BackgroundSyncTask> getBackgroundSyncTaskProvider() {
		return this.backgroundSyncTaskProvider;
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getBackgroundTaskExecutorService() {
		return Optional.ofNullable(this.backgroundTaskExecutorService);
	}
}