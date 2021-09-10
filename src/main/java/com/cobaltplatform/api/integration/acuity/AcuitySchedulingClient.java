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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public interface AcuitySchedulingClient {
	@Nonnull
	default Instant parseAcuityTime(@Nonnull String acuityTime) {
		requireNonNull(acuityTime);

		try {
			// e.g. 2020-04-16T15:00:00-0400
			// It's not quite ISO_OFFSET_DATE_TIME so we can't use DateTimeFormatter.ISO_OFFSET_DATE_TIME!
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(acuityTime).toInstant();
		} catch (ParseException e) {
			throw new RuntimeException(format("Unable to parse Acuity timestamp value '%s'", acuityTime));
		}
	}

	@Nonnull
	List<AcuityCalendar> findCalendars();

	@Nonnull
	Optional<AcuityCalendar> findCalendarById(@Nullable Long calendarId);

	@Nonnull
	List<AcuityAppointmentType> findAppointmentTypes();

	@Nonnull
	Optional<AcuityAppointmentType> findAppointmentTypeById(@Nullable Long appointmentTypeId);

	@Nonnull
	List<AcuityDate> findAvailabilityDates(@Nonnull Long calendarId,
																				 @Nonnull Long appointmentTypeId,
																				 @Nonnull YearMonth yearMonth,
																				 @Nonnull ZoneId timeZone);

	@Nonnull
	List<AcuityTime> findAvailabilityTimes(@Nonnull Long calendarId,
																				 @Nonnull Long appointmentTypeId,
																				 @Nonnull LocalDate localDate,
																				 @Nonnull ZoneId timeZone);

	@Nonnull
	List<AcuityClass> findAvailabilityClasses(@Nonnull YearMonth yearMonth,
																						@Nonnull ZoneId timeZone);

	@Nonnull
	List<AcuityForm> findForms();

	@Nonnull
	AcuityAppointment createAppointment(@Nonnull AcuityAppointmentCreateRequest request);

	@Nonnull
	AcuityAppointment cancelAppointment(@Nonnull Long appointmentId);

	@Nonnull
	List<AcuityAppointment> findAppointments();

	@Nonnull
	Optional<AcuityAppointment> findAppointmentById(@Nullable Long appointmentId);

	@Nonnull
	AcuityCheckTime availabilityCheckTime(@Nonnull AcuityAvailabilityCheckTimeRequest request);

	@Nonnull
	SortedMap<String, Long> getCallFrequencyHistogram();

	@Nonnull
	Boolean verifyWebhookRequestCameFromAcuity(@Nonnull String acuitySignature,
																						 @Nonnull String requestBody);
}
