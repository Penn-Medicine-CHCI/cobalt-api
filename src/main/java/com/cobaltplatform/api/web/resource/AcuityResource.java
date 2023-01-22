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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.integration.acuity.AcuitySchedulingCache;
import com.cobaltplatform.api.integration.acuity.AcuitySchedulingClient;
import com.cobaltplatform.api.integration.acuity.AcuitySyncManager;
import com.cobaltplatform.api.integration.acuity.model.AcuityAppointment;
import com.cobaltplatform.api.model.api.request.CancelAppointmentRequest;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.ProviderService;
import com.soklet.web.annotation.FormParameter;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.RequestHeader;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ForkJoinPool;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class AcuityResource {
	@Nonnull
	private final ProviderService providerService;
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final AcuitySyncManager acuitySyncManager;
	@Nonnull
	private final AcuitySchedulingClient acuitySchedulingClient;
	@Nonnull
	private final AcuitySchedulingCache acuitySchedulingCache;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Logger logger;

	@Inject
	public AcuityResource(@Nonnull ProviderService providerService,
												@Nonnull AppointmentService appointmentService,
												@Nonnull AcuitySyncManager acuitySyncManager,
												@Nonnull AcuitySchedulingClient acuitySchedulingClient,
												@Nonnull AcuitySchedulingCache acuitySchedulingCache,
												@Nonnull Configuration configuration) {
		requireNonNull(providerService);
		requireNonNull(appointmentService);
		requireNonNull(acuitySyncManager);
		requireNonNull(acuitySchedulingClient);
		requireNonNull(configuration);

		this.providerService = providerService;
		this.appointmentService = appointmentService;
		this.acuitySyncManager = acuitySyncManager;
		this.acuitySchedulingClient = acuitySchedulingClient;
		this.acuitySchedulingCache = acuitySchedulingCache;
		this.configuration = configuration;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	// Acuity sends a application/x-www-form-urlencoded POST to us
	@POST("/acuity/webhook/appointments/scheduled")
	public void appointmentScheduledWebhook(@Nonnull @RequestHeader("X-Acuity-Signature") String acuitySignature,
																					@Nonnull @FormParameter("id") Long appointmentId,
																					@Nonnull @FormParameter("calendarID") Long calendarId,
																					@Nonnull @FormParameter("appointmentTypeID") Long appointmentTypeId,
																					@Nonnull @RequestBody String requestBody) {
		requireNonNull(acuitySignature);
		requireNonNull(appointmentId);
		requireNonNull(calendarId);
		requireNonNull(appointmentTypeId);
		requireNonNull(requestBody);

		getLogger().info("Received Webhook from Acuity. Signature is '{}' request body is '{}'", acuitySignature, requestBody);

		// Example:
		// Received Webhook from Acuity. Signature is '2mPyApxVhCkjle6r4xnQfjT291GcxCuDML4UwYZ8PU4='
		// request body is 'action=changed&id=371434327&calendarID=3800231&appointmentTypeID=13757193'

		if (!getAcuitySchedulingClient().verifyWebhookRequestCameFromAcuity(acuitySignature, requestBody)) {
			getLogger().warn("Acuity signature doesn't match request body signature - rejecting webhook!");
			throw new AuthenticationException();
		}

		AcuityAppointment appointment = getAcuitySchedulingClient().findAppointmentById(appointmentId).get();
		Instant appointmentTimestamp = getAcuitySchedulingClient().parseAcuityTime(appointment.getDatetime());
		ZoneId appointmentTimeZone = ZoneId.of(appointment.getTimezone());
		LocalDate appointmentDate = appointmentTimestamp.atZone(appointmentTimeZone).toLocalDate();

		getLogger().debug("Handling 'scheduled'...");
		getAcuitySchedulingCache().invalidateAvailability(appointmentDate, appointmentTimeZone);

		Appointment localAppointment = getAppointmentService().findAppointmentByAcuityAppointmentId(appointmentId).orElse(null);

		if (localAppointment != null && localAppointment.getProviderId() != null) {
			ForkJoinPool.commonPool().execute(() -> {
				getAcuitySyncManager().syncProviderAvailability(localAppointment.getProviderId(), appointmentDate);
			});
		}
	}

	// Acuity sends a application/x-www-form-urlencoded POST to us
	@POST("/acuity/webhook/appointments/rescheduled")
	public void appointmentRescheduledWebhook(@Nonnull @RequestHeader("X-Acuity-Signature") String acuitySignature,
																						@Nonnull @FormParameter("id") Long appointmentId,
																						@Nonnull @FormParameter("calendarID") Long calendarId,
																						@Nonnull @FormParameter("appointmentTypeID") Long appointmentTypeId,
																						@Nonnull @RequestBody String requestBody) {
		requireNonNull(acuitySignature);
		requireNonNull(appointmentId);
		requireNonNull(calendarId);
		requireNonNull(appointmentTypeId);
		requireNonNull(requestBody);

		getLogger().info("Received Webhook from Acuity. Signature is '{}' request body is '{}'", acuitySignature, requestBody);

		// Example:
		// Received Webhook from Acuity. Signature is '2mPyApxVhCkjle6r4xnQfjT291GcxCuDML4UwYZ8PU4='
		// request body is 'action=changed&id=371434327&calendarID=3800231&appointmentTypeID=13757193'

		if (!getAcuitySchedulingClient().verifyWebhookRequestCameFromAcuity(acuitySignature, requestBody)) {
			getLogger().warn("Acuity signature doesn't match request body signature - rejecting webhook!");
			throw new AuthenticationException();
		}

		AcuityAppointment appointment = getAcuitySchedulingClient().findAppointmentById(appointmentId).get();
		Instant appointmentTimestamp = getAcuitySchedulingClient().parseAcuityTime(appointment.getDatetime());
		ZoneId appointmentTimeZone = ZoneId.of(appointment.getTimezone());
		LocalDate appointmentDate = appointmentTimestamp.atZone(appointmentTimeZone).toLocalDate();
		Appointment localAppointment = getAppointmentService().findAppointmentByAcuityAppointmentId(appointmentId).orElse(null);

		getLogger().debug("Handling 'rescheduled'...");

		if (localAppointment != null) {
			LocalDate localAppointmentDate = localAppointment.getStartTime().toLocalDate();

			getAppointmentService().rescheduleAppointmentFromWebhook(appointment, localAppointment);

			// Invalidate old date if different from new one
			if (!localAppointmentDate.equals(appointmentDate) || !localAppointment.getTimeZone().equals(appointmentTimeZone)) {
				getAcuitySchedulingCache().invalidateAvailability(localAppointmentDate, localAppointment.getTimeZone());

				if (localAppointment != null && localAppointment.getProviderId() != null) {
					ForkJoinPool.commonPool().execute(() -> {
						getAcuitySyncManager().syncProviderAvailability(localAppointment.getProviderId(), localAppointmentDate);
					});
				}
			}
		}

		getAcuitySchedulingCache().invalidateAvailability(appointmentDate, appointmentTimeZone);

		if (localAppointment != null && localAppointment.getProviderId() != null) {
			ForkJoinPool.commonPool().execute(() -> {
				getAcuitySyncManager().syncProviderAvailability(localAppointment.getProviderId(), appointmentDate);
			});
		}
	}

	// Acuity sends a application/x-www-form-urlencoded POST to us
	@POST("/acuity/webhook/appointments/canceled")
	public void appointmentCanceledWebhook(@Nonnull @RequestHeader("X-Acuity-Signature") String acuitySignature,
																				 @Nonnull @FormParameter("id") Long appointmentId,
																				 @Nonnull @FormParameter("calendarID") Long calendarId,
																				 @Nonnull @FormParameter("appointmentTypeID") Long appointmentTypeId,
																				 @Nonnull @RequestBody String requestBody) {
		requireNonNull(acuitySignature);
		requireNonNull(appointmentId);
		requireNonNull(calendarId);
		requireNonNull(appointmentTypeId);
		requireNonNull(requestBody);

		getLogger().info("Received Webhook from Acuity. Signature is '{}' request body is '{}'", acuitySignature, requestBody);

		// Example:
		// Received Webhook from Acuity. Signature is '2mPyApxVhCkjle6r4xnQfjT291GcxCuDML4UwYZ8PU4='
		// request body is 'action=changed&id=371434327&calendarID=3800231&appointmentTypeID=13757193'

		if (!getAcuitySchedulingClient().verifyWebhookRequestCameFromAcuity(acuitySignature, requestBody)) {
			getLogger().warn("Acuity signature doesn't match request body signature - rejecting webhook!");
			throw new AuthenticationException();
		}

		AcuityAppointment appointment = getAcuitySchedulingClient().findAppointmentById(appointmentId).get();
		Instant appointmentTimestamp = getAcuitySchedulingClient().parseAcuityTime(appointment.getDatetime());
		ZoneId appointmentTimeZone = ZoneId.of(appointment.getTimezone());
		LocalDate appointmentDate = appointmentTimestamp.atZone(appointmentTimeZone).toLocalDate();
		Appointment localAppointment = getAppointmentService().findAppointmentByAcuityAppointmentId(appointmentId).orElse(null);

		getLogger().debug("Handling 'canceled'...");

		if (localAppointment != null) {
			getLogger().debug("There is a local appointment, canceling it...");
			getAppointmentService().cancelAppointment(new CancelAppointmentRequest() {{
				setAppointmentId(localAppointment.getAppointmentId());
				setCanceledByWebhook(true);
				setCanceledForReschedule(false);
			}});
		} else {
			getLogger().debug("No appointment to cancel in local database.");
		}

		getAcuitySchedulingCache().invalidateAvailability(appointmentDate, appointmentTimeZone);

		if (localAppointment != null && localAppointment.getProviderId() != null) {
			ForkJoinPool.commonPool().execute(() -> {
				getAcuitySyncManager().syncProviderAvailability(localAppointment.getProviderId(), appointmentDate);
			});
		}
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerService;
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentService;
	}

	@Nonnull
	protected AcuitySyncManager getAcuitySyncManager() {
		return acuitySyncManager;
	}

	@Nonnull
	protected AcuitySchedulingClient getAcuitySchedulingClient() {
		return acuitySchedulingClient;
	}

	@Nonnull
	protected AcuitySchedulingCache getAcuitySchedulingCache() {
		return acuitySchedulingCache;
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
