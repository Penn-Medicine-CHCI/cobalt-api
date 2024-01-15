package com.cobaltplatform.api.util;

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.request.CreateLogicalAvailabilityRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientIntakeQuestionRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningQuestionRequest;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.FontSize.FontSizeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.LogicalAvailabilityType.LogicalAvailabilityTypeId;
import com.cobaltplatform.api.model.db.QuestionContentHint.QuestionContentHintId;
import com.cobaltplatform.api.model.db.RecurrenceType.RecurrenceTypeId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.SourceSystem.SourceSystemId;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.AvailabilityService;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ProviderGenerator {
	@Nonnull
	private final Locale locale;
	@Nonnull
	private final ZoneId timeZone;

	public static void main(String[] args) {
		new ProviderGenerator(Locale.forLanguageTag("en-US"), ZoneId.of("America/New_York"))
				.generate(50);
	}

	public ProviderGenerator(@Nonnull Locale locale,
													 @Nonnull ZoneId timeZone) {
		requireNonNull(locale);
		requireNonNull(timeZone);

		this.locale = locale;
		this.timeZone = timeZone;
	}

	public void generate(@Nonnull Integer numberOfProviders) {
		requireNonNull(numberOfProviders);

		IntegrationTestExecutor.runTransactionallyAndCommit((app) -> {
			String currentTimestampAsString = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", getLocale()).withZone(getTimeZone()).format(Instant.now());

			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AvailabilityService availabilityService = app.getInjector().getInstance(AvailabilityService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			LocalDateTime now = LocalDateTime.now(getTimeZone());

			for (int i = 0; i < numberOfProviders; ++i) {
				System.out.printf("Creating provider %d of %d...\n", i + 1, numberOfProviders);

				UUID providerId = UUID.randomUUID();
				String uniqueIdentifier = format("%s-%d", currentTimestampAsString, i + 1);
				String firstName = "Provider";
				String lastName = uniqueIdentifier;
				String fullName = format("%s %s", firstName, lastName);
				String emailAddress = format("provider-%s@cobaltinnovations.org", uniqueIdentifier);

				// Create account for the provider
				UUID accountId = accountService.createAccount(new CreateAccountRequest() {{
					setEmailAddress(emailAddress);
					setRoleId(RoleId.PROVIDER);
					setSourceSystemId(SourceSystemId.COBALT);
					setInstitutionId(InstitutionId.COBALT);
					setAccountSourceId(AccountSourceId.EMAIL_PASSWORD);
					setFirstName(firstName);
					setLastName(lastName);
					setDisplayName(fullName);
					setPassword("Test1234!");
				}});

				// Create the provider
				database.execute("INSERT INTO provider (provider_id, institution_id, name, title, email_address, " + "locale, time_zone, scheduling_system_id) VALUES (?,?,?,?,?,?,?,?)", providerId, InstitutionId.COBALT, fullName, "Test Provider", emailAddress, getLocale(), getTimeZone(), SchedulingSystemId.COBALT);

				// Associate the account to the provider
				database.execute("UPDATE account SET provider_id=? WHERE account_id=?", providerId, accountId);

				// Create appointment types
				UUID npvAppointmentTypeId = appointmentService.createAppointmentType(new CreateAppointmentTypeRequest() {{
					setProviderId(providerId);
					setName("Test NPV");
					setDescription("Test NPV Description");
					setVisitTypeId(VisitTypeId.INITIAL);
					setDurationInMinutes(60L);
					setHexColor("#FFFFFF");
					setSchedulingSystemId(SchedulingSystemId.COBALT);
					setPatientIntakeQuestions(List.of(new CreatePatientIntakeQuestionRequest() {{
						setQuestion("What is your student ID?");
						setQuestionContentHintId(QuestionContentHintId.STUDENT_ID);
						setFontSizeId(FontSizeId.DEFAULT);
					}}));
					setScreeningQuestions(List.of(new CreateScreeningQuestionRequest() {{
						setQuestion("Is this your first visit with this provider?");
						setFontSizeId(FontSizeId.DEFAULT);
					}}));
				}});

				UUID rpvAppointmentTypeId = appointmentService.createAppointmentType(new CreateAppointmentTypeRequest() {{
					setProviderId(providerId);
					setName("Test RPV");
					setDescription("Test RPV Description");
					setVisitTypeId(VisitTypeId.FOLLOWUP);
					setDurationInMinutes(30L);
					setHexColor("#FFFFFF");
					setSchedulingSystemId(SchedulingSystemId.COBALT);
					setPatientIntakeQuestions(Collections.emptyList());
					setScreeningQuestions(List.of(new CreateScreeningQuestionRequest() {{
						setQuestion("Have you previously booked with this provider?");
						setFontSizeId(FontSizeId.DEFAULT);
					}}));
				}});

				// Create recurring M-F 9-5 availability
				availabilityService.createLogicalAvailability(new CreateLogicalAvailabilityRequest() {{
					setProviderId(providerId);
					setAccountId(accountId);
					setLogicalAvailabilityTypeId(LogicalAvailabilityTypeId.OPEN);
					setRecurrenceTypeId(RecurrenceTypeId.DAILY);
					setRecurMonday(true);
					setRecurTuesday(true);
					setRecurWednesday(true);
					setRecurThursday(true);
					setRecurFriday(true);
					setAppointmentTypeIds(List.of(npvAppointmentTypeId, rpvAppointmentTypeId));
					setStartDateTime(LocalDateTime.of(now.toLocalDate(), LocalTime.of(9, 00)));
					setEndTime(LocalTime.of(17, 00));
				}});

				// Create recurring evening availability a couple nights a week for RPVs only
				availabilityService.createLogicalAvailability(new CreateLogicalAvailabilityRequest() {{
					setProviderId(providerId);
					setAccountId(accountId);
					setLogicalAvailabilityTypeId(LogicalAvailabilityTypeId.OPEN);
					setRecurrenceTypeId(RecurrenceTypeId.DAILY);
					setRecurTuesday(true);
					setRecurWednesday(true);
					setAppointmentTypeIds(List.of(rpvAppointmentTypeId));
					setStartDateTime(LocalDateTime.of(now.toLocalDate(), LocalTime.of(18, 00)));
					setEndTime(LocalTime.of(20, 00));
				}});

				// Block off Friday afternoons
				availabilityService.createLogicalAvailability(new CreateLogicalAvailabilityRequest() {{
					setProviderId(providerId);
					setAccountId(accountId);
					setLogicalAvailabilityTypeId(LogicalAvailabilityTypeId.BLOCK);
					setRecurrenceTypeId(RecurrenceTypeId.DAILY);
					setRecurFriday(true);
					setStartDateTime(LocalDateTime.of(now.toLocalDate(), LocalTime.of(15, 00)));
					setEndTime(LocalTime.of(17, 00));
				}});

				database.execute("INSERT INTO provider_support_role (provider_id, support_role_id) VALUES(?,?)", providerId, SupportRoleId.CLINICIAN);
			}
		});
	}

	@Nonnull
	public Locale getLocale() {
		return locale;
	}

	@Nonnull
	public ZoneId getTimeZone() {
		return timeZone;
	}
}
