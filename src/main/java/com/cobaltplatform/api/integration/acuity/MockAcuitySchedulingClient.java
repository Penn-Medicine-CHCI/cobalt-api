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

package com.cobaltplatform.api.integration.acuity;

import com.cobaltplatform.api.integration.acuity.model.AcuityAppointment;
import com.cobaltplatform.api.integration.acuity.model.AcuityAppointmentType;
import com.cobaltplatform.api.integration.acuity.model.AcuityCalendar;
import com.cobaltplatform.api.integration.acuity.model.AcuityCheckTime;
import com.cobaltplatform.api.integration.acuity.model.AcuityClass;
import com.cobaltplatform.api.integration.acuity.model.AcuityDate;
import com.cobaltplatform.api.integration.acuity.model.AcuityForm;
import com.cobaltplatform.api.integration.acuity.model.AcuityTime;
import com.cobaltplatform.api.integration.acuity.model.request.AcuityAppointmentCreateRequest;
import com.cobaltplatform.api.integration.acuity.model.request.AcuityAvailabilityCheckTimeRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class MockAcuitySchedulingClient implements AcuitySchedulingClient {
	@Nonnull
	@Override
	public List<AcuityCalendar> findCalendars() {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public Optional<AcuityCalendar> findCalendarById(@Nullable Long calendarId) {
		return Optional.empty();
	}

	@Nonnull
	@Override
	public List<AcuityAppointmentType> findAppointmentTypes() {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public Optional<AcuityAppointmentType> findAppointmentTypeById(@Nullable Long appointmentTypeId) {
		return Optional.empty();
	}

	@Nonnull
	@Override
	public List<AcuityDate> findAvailabilityDates(@Nonnull Long calendarId, @Nonnull Long appointmentTypeId, @Nonnull YearMonth yearMonth, @Nonnull ZoneId timeZone) {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public List<AcuityTime> findAvailabilityTimes(@Nonnull Long calendarId, @Nonnull Long appointmentTypeId, @Nonnull LocalDate localDate, @Nonnull ZoneId timeZone) {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public List<AcuityClass> findAvailabilityClasses(@Nonnull YearMonth yearMonth, @Nonnull ZoneId timeZone) {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public List<AcuityForm> findForms() {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public AcuityAppointment createAppointment(@Nonnull AcuityAppointmentCreateRequest request) {
		return new AcuityAppointment();
	}

	@Nonnull
	@Override
	public AcuityAppointment cancelAppointment(@Nonnull Long appointmentId) {
		return new AcuityAppointment();
	}

	@Nonnull
	@Override
	public List<AcuityAppointment> findAppointments() {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public Optional<AcuityAppointment> findAppointmentById(@Nullable Long appointmentId) {
		return Optional.empty();
	}

	@Nonnull
	@Override
	public AcuityCheckTime availabilityCheckTime(@Nonnull AcuityAvailabilityCheckTimeRequest request) {
		return new AcuityCheckTime();
	}

	@Nonnull
	@Override
	public SortedMap<String, Long> getCallFrequencyHistogram() {
		return Collections.emptySortedMap();
	}

	@Nonnull
	@Override
	public Boolean verifyWebhookRequestCameFromAcuity(@Nonnull String acuitySignature,
																										@Nonnull String requestBody) {
		return true;
	}
}
