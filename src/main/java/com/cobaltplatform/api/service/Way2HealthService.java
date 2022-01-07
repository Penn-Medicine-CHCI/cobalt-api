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
import com.cobaltplatform.api.integration.way2health.MockWay2HealthClient;
import com.cobaltplatform.api.integration.way2health.Way2HealthClient;
import com.cobaltplatform.api.integration.way2health.model.entity.Incident;
import com.cobaltplatform.api.integration.way2health.model.entity.Participant;
import com.cobaltplatform.api.integration.way2health.model.request.GetIncidentRequest;
import com.cobaltplatform.api.integration.way2health.model.request.GetIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.request.UpdateIncidentRequest;
import com.cobaltplatform.api.integration.way2health.model.response.ObjectResponse;
import com.cobaltplatform.api.model.api.request.CreateInteractionInstanceRequest;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.StandardMetadata.Way2HealthIncidentTrackingConfig;
import com.cobaltplatform.api.model.db.Way2HealthIncident;
import com.cobaltplatform.api.model.service.AdvisoryLock;
import com.cobaltplatform.api.util.ValidationException;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
	private final Configuration configuration;
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
													 @Nonnull Configuration configuration,
													 @Nonnull Strings strings,
													 @Nonnull Provider<BackgroundSyncTask> backgroundSyncTaskProvider) {
		requireNonNull(way2HealthClient);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(strings);
		requireNonNull(backgroundSyncTaskProvider);

		this.way2HealthClient = way2HealthClient;
		this.database = database;
		this.configuration = configuration;
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

	public void resetIncidents() {
		boolean resettingPermitted = !getConfiguration().getShouldUseRealWay2Health()
				&& !getConfiguration().isProduction()
				&& getWay2HealthClient() instanceof MockWay2HealthClient;

		if (!resettingPermitted)
			throw new ValidationException(getStrings().get("Resetting Way2Health incidents is not permitted."));

		getDatabase().execute("UPDATE way2health_incident SET deleted=TRUE");
		((MockWay2HealthClient) getWay2HealthClient()).reset();

		// Immediately run the background sync task
		new Thread(getBackgroundSyncTaskProvider().get()).start();
	}

	@ThreadSafe
	protected static class BackgroundSyncTask implements Runnable {
		@Nonnull
		private final SystemService systemService;
		@Nonnull
		private final InstitutionService institutionService;
		@Nonnull
		private final InteractionService interactionService;
		@Nonnull
		private final Way2HealthClient way2HealthClient;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final ErrorReporter errorReporter;
		@Nonnull
		private final Database database;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Strings strings;
		@Nonnull
		private final Logger logger;

		@Inject
		public BackgroundSyncTask(@Nonnull SystemService systemService,
															@Nonnull InstitutionService institutionService,
															@Nonnull InteractionService interactionService,
															@Nonnull Way2HealthClient way2HealthClient,
															@Nonnull CurrentContextExecutor currentContextExecutor,
															@Nonnull ErrorReporter errorReporter,
															@Nonnull Database database,
															@Nonnull Configuration configuration,
															@Nonnull Strings strings) {
			requireNonNull(systemService);
			requireNonNull(institutionService);
			requireNonNull(interactionService);
			requireNonNull(way2HealthClient);
			requireNonNull(currentContextExecutor);
			requireNonNull(errorReporter);
			requireNonNull(database);
			requireNonNull(configuration);
			requireNonNull(strings);

			this.systemService = systemService;
			this.institutionService = institutionService;
			this.interactionService = interactionService;
			this.way2HealthClient = way2HealthClient;
			this.currentContextExecutor = currentContextExecutor;
			this.errorReporter = errorReporter;
			this.database = database;
			this.configuration = configuration;
			this.strings = strings;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			// Use advisory lock to ensure we don't have multiple nodes working on Way2Health processing at one time
			getSystemService().performAdvisoryLockOperationIfAvailable(AdvisoryLock.WAY2HEALTH_INCIDENT_SYNCING, () -> {
				CurrentContext currentContext = new CurrentContext.Builder(getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

				getCurrentContextExecutor().execute(currentContext, () -> {
					for (Institution institution : getInstitutionService().findInstitutions()) {
						Institution.StandardMetadata institutionMetadata = institution.getStandardMetadata();

						// Institutions can have multiple tracking configs for W2H
						for (Way2HealthIncidentTrackingConfig config : institutionMetadata.getWay2HealthIncidentTrackingConfigs()) {
							String institutionDescription = format("%s (study %s, type '%s')", institution.getInstitutionId().name(),
									config.getStudyId(), config.getType());

							if (!config.getEnabled()) {
								getLogger().trace("{} has Way2Health incident tracking disabled, skipping it...", institutionDescription);
								continue;
							}

							if (config.getInteractionId() == null || config.getStudyId() == null || config.getType() == null) {
								getErrorReporter().report(format("%s is not configured correctly for Way2Health, not tracking incidents", institutionDescription));
								continue;
							}

							List<ObjectResponse<Incident>> incidentResponses = new ArrayList<>();

							try {
								// Get all incidents (implicitly walking over all available pages) so we can process them
								List<Incident> incidents = getWay2HealthClient().getAllIncidents(new GetIncidentsRequest() {{
									setStatus("New");
									setStudyId(config.getStudyId());
									setType(config.getType());
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
											"SELECT * FROM way2health_incident WHERE incident_id=? AND deleted=FALSE", Way2HealthIncident.class, incident.getId()).orElse(null);

									// If we have already processed this incident, there must be some problem,
									// e.g. the status update PATCH call to W2H failed.  We don't want to reprocess in that case
									if (way2HealthIncident != null) {
										getErrorReporter().report(format("Issue detected with Way2Health incident ID %s. " +
												"It exists in our DB but W2H still considers it 'new'. We are not going to re-send notifications for it.", incident.getId()));
									} else {
										getDatabase().transaction(() -> {
											// Track that we have seen this incident
											UUID way2HealthIncidentId = UUID.randomUUID();

											getDatabase().execute("INSERT INTO way2health_incident (way2health_incident_id, institution_id, " +
															"incident_id, study_id, raw_json) VALUES (?,?,?,?,CAST (? AS JSONB))", way2HealthIncidentId,
													institution.getInstitutionId(), incident.getId(), incident.getStudyId(), incidentResponse.getRawResponseBody());

											ZoneId timeZone = institution.getTimeZone();
											LocalDateTime now = LocalDateTime.now(timeZone);
											Map<String, Object> interactionInstanceMetadata = new HashMap<>() {{
												put("way2HealthIncidentId", way2HealthIncidentId);
												put("incidentId", incident.getId());
												put("studyId", incident.getStudyId());
												put("message", incident.getMessage());
												put("participantName", valueForParticipantField(incident, (participant) -> participant.getName()));
												put("participantCellPhoneNumber", valueForParticipantField(incident, (participant) -> participant.getCellPhone()));
												put("participantHomePhoneNumber", valueForParticipantField(incident, (participant) -> participant.getHomePhone()));
												put("participantWorkPhoneNumber", valueForParticipantField(incident, (participant) -> participant.getWorkPhone()));
											}};

											// Record an interaction for this incident, which might send off some email messages (for example)
											getInteractionService().createInteractionInstance(new CreateInteractionInstanceRequest() {{
												setMetadata(interactionInstanceMetadata);
												setStartDateTime(now);
												setTimeZone(timeZone);
												setInteractionId(config.getInteractionId());
											}});

											// Once we commit successfully, let Way2Health know that we have successfully processed the incident
											getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
												try {
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
					}
				});
			});
		}

		@Nonnull
		protected String valueForParticipantField(@Nonnull Incident incident,
																							@Nonnull Function<Participant, String> fieldFunction) {
			requireNonNull(incident);
			requireNonNull(fieldFunction);

			final String MISSING_FIELD_VALUE = getStrings().get("[unknown]");

			if (incident.getParticipant() == null)
				return MISSING_FIELD_VALUE;

			String value = trimToNull(fieldFunction.apply(incident.getParticipant()));
			return value == null ? MISSING_FIELD_VALUE : value;
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
		protected InteractionService getInteractionService() {
			return interactionService;
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
		protected Strings getStrings() {
			return strings;
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
	protected Configuration getConfiguration() {
		return configuration;
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
