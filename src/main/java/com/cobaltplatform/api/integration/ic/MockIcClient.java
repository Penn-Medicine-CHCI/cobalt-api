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

package com.cobaltplatform.api.integration.ic;

import com.cobaltplatform.api.integration.ic.model.IcAppointmentCanceledRequest;
import com.cobaltplatform.api.integration.ic.model.IcAppointmentCreatedRequest;
import com.cobaltplatform.api.integration.ic.model.IcEpicPatient;
import com.cobaltplatform.api.util.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class MockIcClient implements IcClient {
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Logger logger;

	public MockIcClient() {
		this.jsonMapper = new JsonMapper();
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void notifyOfAppointmentCreation(@Nonnull IcAppointmentCreatedRequest request) throws IcException {
		requireNonNull(request);
		getLogger().debug("Pretending to notify IC that appointment ID {} was created for account ID {}", request.getAppointmentId(), request.getAccountId());
	}

	@Override
	public void notifyOfAppointmentCancelation(@Nonnull IcAppointmentCanceledRequest request) throws IcException {
		requireNonNull(request);
		getLogger().debug("Pretending to notify IC that appointment ID {} was canceled for account ID {}", request.getAppointmentId(), request.getAccountId());
	}

	@Override
	@Nonnull
	public IcEpicPatient parseEpicPatientPayload(@Nonnull String rawEpicPatientPayload) throws IcException {
		requireNonNull(rawEpicPatientPayload);

		try {
			return getJsonMapper().fromJson(rawEpicPatientPayload, IcEpicPatient.class);
		} catch (Exception e) {
			throw new IcException("Unable to parse PIC patient JSON", e);
		}
	}

	@Nonnull
	@Override
	public Boolean verifyIcSigningToken(@Nonnull String icSigningToken) {
		return true;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
