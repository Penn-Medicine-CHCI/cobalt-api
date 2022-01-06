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
import com.cobaltplatform.api.integration.way2health.Way2HealthClient;
import com.cobaltplatform.api.integration.way2health.model.entity.Incident;
import com.cobaltplatform.api.integration.way2health.model.request.GetIncidentRequest;
import com.cobaltplatform.api.integration.way2health.model.request.GetIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.request.UpdateIncidentRequest;
import com.cobaltplatform.api.integration.way2health.model.response.ObjectResponse;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Way2HealthIncident;
import com.cobaltplatform.api.model.service.AdvisoryLock;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class Way2HealthService implements AutoCloseable {
	@Nonnull
	private static final Long BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;

	@Nonnull
	private final Database database;
	@Nonnull
	private final Way2HealthClient way2HealthClient;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Provider<BackgroundSyncTask> backgroundSyncTaskProvider;
	@Nonnull
	private final Object backgroundTaskLock;
	@Nonnull
	private final Logger logger;

	@Nonnull
	private Boolean backgroundTaskStarted;
	@Nullable
	private ScheduledExecutorService backgroundTaskExecutorService;

	static {
		BACKGROUND_TASK_INTERVAL_IN_SECONDS = 60L;
		BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS = 10L;
	}

	@Inject
	public Way2HealthService(@Nonnull Way2HealthClient way2HealthClient,
													 @Nonnull Database database,
													 @Nonnull Strings strings,
													 @Nonnull Provider<BackgroundSyncTask> backgroundSyncTaskProvider) {
		requireNonNull(way2HealthClient);
		requireNonNull(database);
		requireNonNull(strings);
		requireNonNull(backgroundSyncTaskProvider);

		this.way2HealthClient = way2HealthClient;
		this.database = database;
		this.strings = strings;
		this.backgroundTaskLock = new Object();
		this.backgroundTaskStarted = false;
		this.backgroundSyncTaskProvider = backgroundSyncTaskProvider;
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

			getLogger().trace("Starting group session background task...");

			this.backgroundTaskExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("way2health-background-task").build());
			this.backgroundTaskStarted = true;

			getBackgroundTaskExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getBackgroundSyncTaskProvider().get().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to complete group session background task - will retry in %s seconds", String.valueOf(getBackgroundTaskIntervalInSeconds())), e);
					}
				}
			}, getBackgroundTaskInitialDelayInSeconds(), getBackgroundTaskIntervalInSeconds(), TimeUnit.SECONDS);

			getLogger().trace("Group session background task started.");

			return true;
		}
	}

	@Nonnull
	public Boolean stopBackgroundTask() {
		synchronized (getBackgroundTaskLock()) {
			if (!isBackgroundTaskStarted())
				return false;

			getLogger().trace("Stopping group session background task...");

			getBackgroundTaskExecutorService().get().shutdownNow();
			this.backgroundTaskExecutorService = null;
			this.backgroundTaskStarted = false;

			getLogger().trace("Group session background task stopped.");

			return true;
		}
	}

	@ThreadSafe
	protected static class BackgroundSyncTask implements Runnable {
		@Nonnull
		private final SystemService systemService;
		@Nonnull
		private final InstitutionService institutionService;
		@Nonnull
		private final Way2HealthClient way2HealthClient;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final EmailMessageManager emailMessageManager;
		@Nonnull
		private final ErrorReporter errorReporter;
		@Nonnull
		private final Database database;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public BackgroundSyncTask(@Nonnull SystemService systemService,
															@Nonnull InstitutionService institutionService,
															@Nonnull Way2HealthClient way2HealthClient,
															@Nonnull CurrentContextExecutor currentContextExecutor,
															@Nonnull EmailMessageManager emailMessageManager,
															@Nonnull ErrorReporter errorReporter,
															@Nonnull Database database,
															@Nonnull Configuration configuration) {
			requireNonNull(systemService);
			requireNonNull(institutionService);
			requireNonNull(way2HealthClient);
			requireNonNull(currentContextExecutor);
			requireNonNull(emailMessageManager);
			requireNonNull(errorReporter);
			requireNonNull(database);
			requireNonNull(configuration);

			this.systemService = systemService;
			this.institutionService = institutionService;
			this.way2HealthClient = way2HealthClient;
			this.currentContextExecutor = currentContextExecutor;
			this.emailMessageManager = emailMessageManager;
			this.errorReporter = errorReporter;
			this.database = database;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			// Use advisory lock to ensure we don't have multiple nodes working on Way2Health processing at one time
			getSystemService().performAdvisoryLockOperationIfAvailable(AdvisoryLock.WAY2HEALTH_INCIDENT_SYNCING, () -> {
				// Find any institutions that are marked as supporting W2H incident tracking
				List<Institution> institutions = getInstitutionService().findInstitutionsMatchingMetadata(new HashMap<String, Object>() {{
					put("way2HealthIncidentTrackingEnabled", true);
				}});

				CurrentContext currentContext = new CurrentContext.Builder(getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

				getCurrentContextExecutor().execute(currentContext, () -> {
					for (Institution institution : institutions) {
						String institutionDescription = institution.getInstitutionId().name();

						Map<String, Object> metadata = institution.getMetadataAsMap();
						Number studyId = (Number) metadata.get("way2HealthIncidentTrackingStudyId");
						String type = (String) metadata.get("way2HealthIncidentTrackingType");
						List<String> emailAddressesToNotify = (List<String>) metadata.get("way2HealthIncidentTrackingEmailAddressesToNotify");

						if (studyId == null || type == null || emailAddressesToNotify == null || emailAddressesToNotify.size() == 0) {
							getErrorReporter().report(format("%s is not configured correctly for Way2Health, not tracking incidents. " +
									"Values were {studyId=%s, type=%, emailAddressesToNotify=%s}", institutionDescription, studyId, type, emailAddressesToNotify));
							continue;
						}

						List<ObjectResponse<Incident>> incidentResponses = new ArrayList<>();

						try {
							// Get all incidents (implicitly walking over all available pages) so we can process them
							List<Incident> incidents = getWay2HealthClient().getAllIncidents(new GetIncidentsRequest() {{
								setStatus("New");
								setStudyId(studyId.longValue());
								setType(type);
								setOrderBy("desc(created_at)");
							}});

							// For each incident, pull in its details so we have the full picture for future reference
							for (Incident incident : incidents) {
								ObjectResponse<Incident> incidentResponse = getWay2HealthClient().getIncident(new GetIncidentRequest() {{
									setIncidentId(incident.getId());
									setInclude(List.of("comments", "participant", "reporter", "tags", "attachments"));
								}});

								incidentResponses.add(incidentResponse);
							}
						} catch (Exception e) {
							getLogger().error(format("Unable to pull incident data from Way2Health for %s", institutionDescription), e);
							getErrorReporter().report(e);
						}

						if (incidentResponses.size() > 0) {
							getLogger().debug("There are {} incident[s] to process for {}.", incidentResponses.size(), institutionDescription);

							for (ObjectResponse<Incident> incidentResponse : incidentResponses) {
								Incident incident = incidentResponse.getData();

								// See if we have already processed this incident...
								Way2HealthIncident way2HealthIncident = getDatabase().queryForObject(
										"SELECT * FROM way2health_incident WHERE incident_id=?", Way2HealthIncident.class, incident.getId()).orElse(null);

								// If we have already processed this incident, there must be some problem,
								// e.g. the status update PATCH call to W2H failed.  We don't want to reprocess in that case
								if (way2HealthIncident != null) {
									getErrorReporter().report(format("Issue detected with Way2Health incident ID %s. " +
											"It exists in our DB but W2H still considers it 'new'. We are not going to re-send notifications for it.", incident.getId()));
								} else {
									getDatabase().transaction(() -> {
										// Track that we have seen this incident
										getDatabase().execute("INSERT INTO way2health_incident (institution_id, incident_id, study_id, raw_json) " +
														"VALUES (?,?,?,CAST (? AS JSONB))",
												institution.getInstitutionId(), incident.getId(), incident.getStudyId(), incidentResponse.getRawResponseBody());

										// Once we commit successfully, fire off emails letting people know there is an issue
										getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
											// Send emails to interested parties to let them know
											for (String emailAddressToNotify : emailAddressesToNotify) {
												// TODO: wire up real email information
												getEmailMessageManager().enqueueMessage(new EmailMessage.Builder(EmailMessageTemplate.FREEFORM, institution.getLocale())
														.toAddresses(List.of(emailAddressToNotify))
														.messageContext(new HashMap<String, Object>() {{
															// Message is supposed to include relevant information, but in case it's null, have a fallback
															String body = trimToNull(incident.getMessage());

															if (body == null)
																body = format("Please check Way2Health incident ID %s for crisis information.", incident.getId());

															put("subject", "Way2Health crisis notification");
															put("body", body);
														}})
														.build());
											}

											try {
												// Next, let Way2Health know that we have processed the record so it doesn't get reprocessed
												getWay2HealthClient().updateIncident(new UpdateIncidentRequest() {
													{
														setIncidentId(incident.getId());
														setPatchOperations(List.of(
																new PatchOperation() {{
																	setOp("add");
																	setPath("/comments");
																	setValue("Imported to Cobalt");
																}},
																new PatchOperation() {{
																	setOp("replace");
																	setPath("/status");
																	setValue("Resolved");
																}}
														));
													}
												});
											} catch (Exception e) {
												getLogger().error(format("Unable to update incident ID %s in Way2Health for %s", incident.getId(), institutionDescription), e);
												getErrorReporter().report(e);
											}
										});
									});
								}
							}
						} else {
							getLogger().trace("No incidents to process for {}", institutionDescription);
						}
					}
				});
			});
		}

		@Nonnull
		protected SystemService getSystemService() {
			return systemService;
		}

		@Nonnull
		protected InstitutionService getInstitutionService() {
			return institutionService;
		}

		@Nonnull
		protected Way2HealthClient getWay2HealthClient() {
			return way2HealthClient;
		}

		@Nonnull
		protected CurrentContextExecutor getCurrentContextExecutor() {
			return currentContextExecutor;
		}

		@Nonnull
		protected EmailMessageManager getEmailMessageManager() {
			return emailMessageManager;
		}

		@Nonnull
		protected ErrorReporter getErrorReporter() {
			return errorReporter;
		}

		@Nonnull
		protected Database getDatabase() {
			return database;
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

	@Nonnull
	public Boolean isBackgroundTaskStarted() {
		synchronized (getBackgroundTaskLock()) {
			return backgroundTaskStarted;
		}
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
	protected Way2HealthClient getWay2HealthClient() {
		return way2HealthClient;
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Provider<BackgroundSyncTask> getBackgroundSyncTaskProvider() {
		return backgroundSyncTaskProvider;
	}

	@Nonnull
	protected Object getBackgroundTaskLock() {
		return backgroundTaskLock;
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getBackgroundTaskExecutorService() {
		return Optional.ofNullable(backgroundTaskExecutorService);
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
