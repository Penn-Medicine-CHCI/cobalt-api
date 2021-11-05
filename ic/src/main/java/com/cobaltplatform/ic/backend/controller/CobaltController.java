package com.cobaltplatform.ic.backend.controller;

import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import com.cobaltplatform.ic.backend.service.CobaltService;
import com.cobaltplatform.ic.backend.service.DispositionService;
import com.cobaltplatform.ic.model.DispositionFlag;
import com.google.gson.Gson;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@ThreadSafe
public class CobaltController {
	@Nonnull
	private static final String COBALT_SIGNING_TOKEN_HEADER_NAME;
	@Nonnull
	private static final Gson GSON;
	@Nonnull
	private static final Logger LOGGER;

	static {
		COBALT_SIGNING_TOKEN_HEADER_NAME = "X-Cobalt-Signing-Token";
		GSON = new Gson();
		LOGGER = LoggerFactory.getLogger(SystemController.class);
	}

	@Nonnull
	public static Handler appointmentCreated = ctx -> {
		requireNonNull(ctx);

		handleCobaltRequest(ctx, AppointmentCreatedRequest.class, (appointmentCreatedRequest -> {
			UUID cobaltAccountId = appointmentCreatedRequest.getAccountId();
			DPatientDisposition disposition = DispositionService.getLatestDispositionForCobaltAccountId(cobaltAccountId).orElse(null);

			if (disposition == null) {
				LOGGER.warn("No disposition found for cobalt account ID {}, continuing on...", cobaltAccountId);
			} else {
				if (disposition.getFlag() == DispositionFlag.AWAITING_IC_SCHEDULING || disposition.getFlag() == DispositionFlag.OPTIONAL_REFERRAL) {
					LOGGER.warn("Disposition flag for cobalt account ID {} is {}, transitioning to {} due to appointment creation...", cobaltAccountId, DispositionFlag.AWAITING_IC_SCHEDULING.name(), DispositionFlag.AWAITING_FIRST_IC_APPOINTMENT);
					DispositionService.updateDispositionFlag(disposition.getId(), DispositionFlag.AWAITING_FIRST_IC_APPOINTMENT);
				} else {
					LOGGER.warn("Disposition flag for cobalt account ID {} is {}, no action needs to be taken.", cobaltAccountId, disposition.getFlag().name());
				}
			}
		}));
	};

	@Nonnull
	public static Handler appointmentCanceled = ctx -> {
		requireNonNull(ctx);

		handleCobaltRequest(ctx, AppointmentCanceledRequest.class, (appointmentCanceledRequest -> {
			UUID cobaltAccountId = appointmentCanceledRequest.getAccountId();
			DPatientDisposition disposition = DispositionService.getLatestDispositionForCobaltAccountId(cobaltAccountId).orElse(null);

			if (disposition == null) {
				LOGGER.warn("No disposition found for cobalt account ID {}, continuing on...", cobaltAccountId);
			} else {
				if (disposition.getFlag() == DispositionFlag.AWAITING_FIRST_IC_APPOINTMENT) {
					DispositionFlag resetFlag = disposition.getDiagnosis().getFlag();
					LOGGER.warn("Disposition flag for cobalt account ID {} is {}, transitioning to {} due to appointment cancelation...", cobaltAccountId, DispositionFlag.AWAITING_FIRST_IC_APPOINTMENT.name(), resetFlag);
					DispositionService.updateDispositionFlag(disposition.getId(), resetFlag);
				} else {
					LOGGER.warn("Disposition flag for cobalt account ID {} is {}, no action needs to be taken.", cobaltAccountId, disposition.getFlag().name());
				}
			}
		}));
	};

	@Nonnull
	private static <T> void handleCobaltRequest(@Nonnull Context context,
																							@Nonnull Class<T> requestBodyClass,
																							@Nonnull Consumer<T> handler) {
		requireNonNull(context);
		requireNonNull(requestBodyClass);
		requireNonNull(handler);

		String requestBody = context.body();
		T requestBodyInstance;

		String cobaltSigningToken = context.header(COBALT_SIGNING_TOKEN_HEADER_NAME);

		if (!CobaltService.getSharedInstance().verifyCobaltSigningToken(cobaltSigningToken)) {
			LOGGER.error("Unable to verify Cobalt signing token '{}' from header '{}'", cobaltSigningToken, COBALT_SIGNING_TOKEN_HEADER_NAME);
			context.status(HttpStatus.SC_UNAUTHORIZED);
			return;
		}

		try {
			requestBodyInstance = GSON.fromJson(requestBody, requestBodyClass);
		} catch (Exception e) {
			LOGGER.error(format("Unable to parse Cobalt request body: %s", requestBody), e);
			context.status(HttpStatus.SC_UNPROCESSABLE_ENTITY);
			return;
		}

		try {
			handler.accept(requestBodyInstance);
		} catch (Exception e) {
			LOGGER.error("Unable to handle Cobalt request", e);
			context.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		context.status(HttpStatus.SC_OK);
	}

	@NotThreadSafe
	public static class AppointmentCreatedRequest {
		@Nullable
		private UUID accountId;
		@Nullable
		private UUID appointmentId;

		@Nullable
		public UUID getAccountId() {
			return accountId;
		}

		public void setAccountId(@Nullable UUID accountId) {
			this.accountId = accountId;
		}

		@Nullable
		public UUID getAppointmentId() {
			return appointmentId;
		}

		public void setAppointmentId(@Nullable UUID appointmentId) {
			this.appointmentId = appointmentId;
		}
	}

	@NotThreadSafe
	public static class AppointmentCanceledRequest {
		@Nullable
		private UUID accountId;
		@Nullable
		private UUID appointmentId;

		@Nullable
		public UUID getAccountId() {
			return accountId;
		}

		public void setAccountId(@Nullable UUID accountId) {
			this.accountId = accountId;
		}

		@Nullable
		public UUID getAppointmentId() {
			return appointmentId;
		}

		public void setAppointmentId(@Nullable UUID appointmentId) {
			this.appointmentId = appointmentId;
		}
	}
}
