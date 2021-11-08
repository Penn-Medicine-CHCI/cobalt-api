package com.cobaltplatform.ic.backend;

import com.cobaltplatform.ic.backend.config.IcConfig;
import com.cobaltplatform.ic.backend.service.BusinessDayUtil;
import com.cobaltplatform.ic.backend.service.CobaltService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.cobaltplatform.ic.backend.exception.CobaltException;
import com.cobaltplatform.ic.backend.model.cobalt.SendSmsMessageRequest;
import com.cobaltplatform.ic.backend.model.db.DPatientMessage;
import com.cobaltplatform.ic.backend.model.response.AggregateMessageDto;
import com.cobaltplatform.ic.backend.model.serialize.DSTU3PatientSerializer;
import com.cobaltplatform.ic.backend.model.serialize.FhirR4Deserializer;
import com.cobaltplatform.ic.backend.model.serialize.FhirR4Serializer;
import com.cobaltplatform.ic.model.DispositionFlag;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.Transaction;
import io.ebean.config.DatabaseConfig;
import io.javalin.plugin.json.JavalinJackson;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {
	static Database DB;
	static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);
	private static final CobaltService cobaltService = CobaltService.getSharedInstance();
	static BusinessDayUtil hoursUtil = new BusinessDayUtil();
	static String linkToIc = IcConfig.getBaseUrl();

	private static final int BASE_PERIOD_VALUE = 1;
	private static final String BASE_PERIOD_SPAN = "MINUTES";

	private static final int REMINDER_PERIOD_VALUE = 24;
	private static final String REMINDER_PERIOD_SPAN = "HOURS";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		Locale.setDefault(Locale.US);
		Config.configureJackson();

		scheduler.scheduleWithFixedDelay(new RunAllTasks(), 0, 10, TimeUnit.SECONDS);
	}

	static class RunAllTasks implements Runnable {
		public void run() {
			try {
				if (!hoursUtil.isInstantWithinBusinessHours(Instant.now(), BusinessDayUtil.Department.COBALT)) {
					return;
				}
				// Run all functions
				remindPatientsAboutNotYetStartedAssessments();
				remindPatientsStartedButNotFinishedAssessment();
				notifyDigitalPatientsForLoginFirstTime();
				notifyPhonePatientsForLoginFirstTime();
				notifyScheduleAppointmentReminder();
			} catch (Exception e) {
				// swallow errors, errors may be common in complex SQL queries
				logger.error("Exception in scheduler run", e);
			}
		}
	}

	/**
	 * Finds all patients with a disposition last updated over 24 hours ago with flag NOT_YET_STARTED and assessment status is NOT_STARTED
	 * Then checks to see if they have been reminded less than three times yet
	 * If not, it texts them a reminder
	 *
	 * Assessments are only created once a patient has logged in
	 */
	private static void remindPatientsAboutNotYetStartedAssessments() {
		var timePeriod = String.format("%d %s", REMINDER_PERIOD_VALUE, REMINDER_PERIOD_SPAN);
		var sql = "SELECT patient_id, preferred_phone_number, Concat(preferred_first_name, ' ', preferred_last_name) AS full_name, max_attempts, latest_dt, disposition_id FROM( SELECT patient_disposition.id AS disposition_id, patient_disposition.patient_id AS patient_id, COALESCE(Max(attempt), 0) AS max_attempts, COALESCE(Max(filtered_messages.created_dt), Now()) AS latest_dt FROM patient_disposition LEFT JOIN ( SELECT * FROM messages WHERE had_flag = 11) filtered_messages ON patient_disposition.id = filtered_messages.disposition_id WHERE patient_disposition.id IN ( SELECT patient_disposition.id FROM patient_disposition JOIN assessment ON assessment.disposition_id = patient_disposition.id WHERE flag = 11 AND assessment.status = 'NOT_STARTED' AND patient_disposition.updated_dt <= Now() - CAST(:time_period AS interval)) GROUP BY patient_disposition.id, had_flag HAVING max(attempt) IS NULL OR ( max(attempt) < 3 AND max(filtered_messages.created_dt) < now() - CAST(:time_period AS interval) )) t JOIN patient ON t.patient_id = patient.id;";
		List<AggregateMessageDto> patientMessageAggregates = DB.findDto(AggregateMessageDto.class, sql)
				.setParameter("time_period", timePeriod)
				.findList();

		var messageBody = String.format("Your assessment is now available - click here: %s to complete it so we can connect you to the care you discussed recently.", linkToIc);
		sendAndSaveSMS(patientMessageAggregates, messageBody, DispositionFlag.NOT_YET_SCREENED);

		if(patientMessageAggregates.size() > 0)
			logger.debug("remindAfterHoursAttempts: Sent SMS to {} people", patientMessageAggregates.size());
	}

	/**
	 * Finds all patients that have an in progress assessment that has not been updated in some time
	 * This message will go out 3 times.
	 */
	private static void remindPatientsStartedButNotFinishedAssessment() {
		var timePeriod = String.format("%d %s", REMINDER_PERIOD_VALUE, REMINDER_PERIOD_SPAN);
		var sql = "SELECT patient_id, preferred_phone_number, Concat(preferred_first_name, ' ', preferred_last_name) AS full_name, max_attempts, latest_dt, disposition_id FROM( SELECT patient_disposition.id AS disposition_id, patient_disposition.patient_id AS patient_id, COALESCE(Max(attempt), 0) AS max_attempts, COALESCE(Max(filtered_messages.created_dt), Now()) AS latest_dt FROM patient_disposition LEFT JOIN ( SELECT * FROM messages WHERE had_flag = 20) filtered_messages ON patient_disposition.id = filtered_messages.disposition_id WHERE patient_disposition.id IN ( SELECT assessment.disposition_id FROM assessment WHERE status = 'IN_PROGRESS' AND updated_dt <= Now() - CAST(:time_period AS interval)) GROUP BY patient_disposition.id HAVING max(attempt) IS NULL OR ( max(attempt) < 3 AND max(filtered_messages.created_dt) < now() - CAST(:time_period AS interval) )) t JOIN patient ON t.patient_id = patient.id;";
		List<AggregateMessageDto> patientMessageAggregates = DB.findDto(AggregateMessageDto.class, sql)
				.setParameter("time_period", timePeriod)
				.findList();

		var messageBody = String.format("Your assessment is incomplete. Click here: %s to finish so we can get you connected to the right care.", linkToIc);
		sendAndSaveSMS(patientMessageAggregates, messageBody, DispositionFlag.ASSESSMENT_STARTED_NO_DISPOSITION_YET);

		if(patientMessageAggregates.size() > 0)
			logger.debug("remindPatientsStartedButNotFinishedAssessment: Sent SMS to {} people", patientMessageAggregates.size());
	}

	/**
	 * Finds all patients that have a disposition with flag `NOT_YET_STARTED` that does not yet have an assessment, and was referred for a digital assessment
	 * Assessments are created when a patient logs in for the first time
	 * This message will only go out once.
	 */
	private static void notifyDigitalPatientsForLoginFirstTime() {
		var timePeriod = String.format("%d %s", BASE_PERIOD_VALUE, BASE_PERIOD_SPAN);
		var sql = "SELECT patient_id, preferred_phone_number, Concat(preferred_first_name, ' ', preferred_last_name) AS full_name, 0 AS max_attempts, Now() AS latest_dt, disposition_id FROM( SELECT t.patient_id, t.disposition_id FROM ( SELECT patient_id, disposition_id FROM ( SELECT patient_disposition.id AS disposition_id, patient_disposition.patient_id AS patient_id, patient_disposition.flag AS flag, assessment.id AS assessment_id, patient_disposition.updated_dt AS updated_dt FROM patient_disposition LEFT JOIN assessment ON patient_disposition.id = assessment.disposition_id where patient_disposition.is_digital) t WHERE flag = 11 AND updated_dt <= Now() - CAST(:time_period AS interval) AND assessment_id IS NULL) t LEFT JOIN messages ON messages.disposition_id = t.disposition_id WHERE attempt IS NULL) t2 JOIN patient ON patient.id = t2.patient_id;";
		List<AggregateMessageDto> patientMessageAggregates = DB.findDto(AggregateMessageDto.class, sql)
				.setParameter("time_period", timePeriod)
				.findList();

		var messageBody = String.format("Your Primary Care Provider would like you to click here: %s to complete a 10 min. assessment so we can connect you to the care you discussed recently.", linkToIc);
		sendAndSaveSMS(patientMessageAggregates, messageBody, DispositionFlag.PATIENT_CREATED_NOT_YET_LOGGED_IN);

		if(patientMessageAggregates.size() > 0)
			logger.debug("notifyPatientsForLoginFirstTime: Sent SMS to {} people", patientMessageAggregates.size());
	}

	/**
	 * Finds all patients that have a disposition with flag `NOT_YET_STARTED` that does not yet have an assessment, and was referred for a phone assessment
	 * Assessments are created when a patient logs in for the first time
	 * This message will only go out once.
	 */
	private static void notifyPhonePatientsForLoginFirstTime() {
		var timePeriod = String.format("%d %s", BASE_PERIOD_VALUE, BASE_PERIOD_SPAN);
		var sql = "SELECT patient_id, preferred_phone_number, Concat(preferred_first_name, ' ', preferred_last_name) AS full_name, 0 AS max_attempts, Now() AS latest_dt, disposition_id FROM( SELECT t.patient_id, t.disposition_id FROM ( SELECT patient_id, disposition_id FROM ( SELECT patient_disposition.id AS disposition_id, patient_disposition.patient_id AS patient_id, patient_disposition.flag AS flag, assessment.id AS assessment_id, patient_disposition.updated_dt AS updated_dt FROM patient_disposition LEFT JOIN assessment ON patient_disposition.id = assessment.disposition_id WHERE NOT patient_disposition.is_digital) t WHERE flag = 11 AND updated_dt <= Now() - CAST(:time_period AS interval) AND assessment_id IS NULL) t LEFT JOIN messages ON messages.disposition_id = t.disposition_id WHERE attempt IS NULL) t2 JOIN patient ON patient.id = t2.patient_id;";
		List<AggregateMessageDto> patientMessageAggregates = DB.findDto(AggregateMessageDto.class, sql)
				.setParameter("time_period", timePeriod)
				.findList();

		var messageBody = "Your Primary Care Provider would like you to call 215-615-4222 today to complete a 10 min assessment so we can connect you to the care you discussed recently.";
		sendAndSaveSMS(patientMessageAggregates, messageBody, DispositionFlag.PATIENT_CREATED_NOT_YET_LOGGED_IN);

		if(patientMessageAggregates.size() > 0)
			logger.debug("notifyPatientsForLoginFirstTime: Sent SMS to {} people", patientMessageAggregates.size());
	}

	/**
	 * Reminder to schedule your first appointment with an IC LCSW (3 reminders over the course of 1 week, if you don't schedule immediately after finishing assessment and getting triaged into IC)
	 * looking for completed assessment and flag 12 (AWAITING_IC_SCHEDULING)
	 *
	 * goes out at most 3 times
	 */
	private static void notifyScheduleAppointmentReminder() {
		var timePeriod = String.format("%d %s", REMINDER_PERIOD_VALUE, REMINDER_PERIOD_SPAN);
		var sql = "SELECT patient_id, preferred_phone_number, Concat(preferred_first_name, ' ', preferred_last_name) AS full_name, max_attempts, latest_dt, disposition_id FROM( SELECT patient_disposition.id AS disposition_id, patient_disposition.patient_id AS patient_id, COALESCE(Max(attempt), 0) AS max_attempts, COALESCE(Max(filtered_messages.created_dt), Now()) AS latest_dt FROM patient_disposition LEFT JOIN ( SELECT * FROM messages WHERE had_flag = 12) filtered_messages ON patient_disposition.id = filtered_messages.disposition_id WHERE patient_disposition.id IN ( SELECT patient_disposition.id FROM patient_disposition JOIN assessment ON assessment.disposition_id = patient_disposition.id WHERE patient_disposition.flag = 12 AND assessment.status = 'COMPLETED' AND patient_disposition.updated_dt <= Now() - CAST(:time_period AS interval)) GROUP BY patient_disposition.id, had_flag HAVING max(attempt) IS NULL OR ( max(attempt) < 3 AND max(filtered_messages.created_dt) < now() - CAST(:time_period AS interval) )) t JOIN patient ON t.patient_id = patient.id;";
		List<AggregateMessageDto> patientMessageAggregates = DB.findDto(AggregateMessageDto.class, sql)
				.setParameter("time_period", timePeriod)
				.findList();

		var messageBody = String.format("Based on your recent assessment responses we recommend you click here: %s to schedule with a provider.", linkToIc);
		sendAndSaveSMS(patientMessageAggregates, messageBody, DispositionFlag.AWAITING_IC_SCHEDULING);

		if(patientMessageAggregates.size() > 0)
			logger.debug("notifyScheduleAppointmentReminder: Sent SMS to {} people", patientMessageAggregates.size());
	}

	private static void sendAndSaveSMS(List<AggregateMessageDto> patientMessageAggregates, String messageBody, DispositionFlag hadFlag) {
		try (Transaction transaction = DB.beginTransaction()) {
			transaction.setBatchMode(true);

			var formattedBody = "This is Example Institution - " + messageBody;
			patientMessageAggregates.forEach(q -> {
				var phone = formatPhoneNumber(q.getPreferredPhoneNumber());
				boolean succeeded;

				logger.debug("Sending SMS to {}: {}", phone, formattedBody);

				try {
					cobaltService.sendSmsMessage(new SendSmsMessageRequest(phone, formattedBody));
					succeeded = true;
				} catch (CobaltException e) {
					succeeded = false;
					logger.warn("Sending Message failed", e);
				}
				new DPatientMessage()
						.setPatientId(q.getPatientId())
						.setHadFlag(hadFlag)
						.setDispositionId(q.getDispositionId())
						.setBody(formattedBody)
						.setSucceeded(succeeded)
						.setAttempt((short) (q.getMaxAttempts() + 1))
						.setAddress(phone)
						.save();
			});

			transaction.commit();
		}
	}

	private static String formatPhoneNumber(String phone) {
		if (phone.startsWith("+1")) {
			return phone.replace("-", "");
		}
		return String.format("+1%s", phone.replace("-", ""));
	}

	protected static class Config {
		public static void configureJackson() {
			SimpleModule fhirModule = new SimpleModule();
			fhirModule.addSerializer(Questionnaire.class, new FhirR4Serializer());
			fhirModule.addDeserializer(Questionnaire.class, new FhirR4Deserializer<>(Questionnaire.class));
			fhirModule.addSerializer(QuestionnaireResponse.class, new FhirR4Serializer());
			fhirModule.addDeserializer(QuestionnaireResponse.class, new FhirR4Deserializer<>(QuestionnaireResponse.class));

			fhirModule.addSerializer(Patient.class, new DSTU3PatientSerializer());
			DatabaseConfig config = new DatabaseConfig();

			// read the ebean.properties and load
			// those settings into this DatabaseConfig object
			config.loadFromProperties();

			ObjectMapper dbObjectMapper = new ObjectMapper();

			dbObjectMapper.registerModule(fhirModule);

			dbObjectMapper.registerModule((new JavaTimeModule())).configure(
					SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false);
			dbObjectMapper.registerModule(new JodaModule()).configure(
					SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false);
			config.setObjectMapper(dbObjectMapper);

			DB = DatabaseFactory.create(config);

			JavalinJackson.getObjectMapper().registerModule(fhirModule);
			JavalinJackson.getObjectMapper().registerModule((new JavaTimeModule())).configure(
					SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false);
			JavalinJackson.getObjectMapper().registerModule(new JodaModule()).configure(
					SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false);
		}
	}
}
