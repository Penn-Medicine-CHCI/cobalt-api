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
import com.cobaltplatform.api.messaging.push.PushMessage;
import com.cobaltplatform.api.messaging.push.PushMessageTemplate;
import com.cobaltplatform.api.model.api.request.CreateAccountCheckInActionFileUploadRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateFileUploadRequest;
import com.cobaltplatform.api.model.api.request.CreateScheduledMessageRequest;
import com.cobaltplatform.api.model.api.request.CreateStudyAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateStudyFileUploadRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountStudyPreferences;
import com.cobaltplatform.api.model.api.request.UpdateCheckInAction;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountCheckIn;
import com.cobaltplatform.api.model.db.AccountCheckInAction;
import com.cobaltplatform.api.model.db.AccountCheckInActionFileUpload;
import com.cobaltplatform.api.model.db.AccountSource;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.AccountStudy;
import com.cobaltplatform.api.model.db.CheckInActionStatus.CheckInActionStatusId;
import com.cobaltplatform.api.model.db.CheckInStatus.CheckInStatusId;
import com.cobaltplatform.api.model.db.CheckInStatusGroup.CheckInStatusGroupId;
import com.cobaltplatform.api.model.db.CheckInType.CheckInTypeId;
import com.cobaltplatform.api.model.db.ClientDevicePushToken;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.Study;
import com.cobaltplatform.api.model.db.StudyBeiweConfig;
import com.cobaltplatform.api.model.db.StudyCheckIn;
import com.cobaltplatform.api.model.db.StudyCheckInAction;
import com.cobaltplatform.api.model.db.StudyCheckInReminder;
import com.cobaltplatform.api.model.db.StudyFileUpload;
import com.cobaltplatform.api.model.service.FileUploadResult;
import com.cobaltplatform.api.model.service.StudyAccount;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.ValidationUtility.isValidUUID;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class StudyService implements AutoCloseable {
	@Nonnull
	private static final DateTimeFormatter STUDY_FILE_UPLOAD_TIMESTAMP_FORMATTER;

	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<SystemService> systemServiceProvider;
	@Nonnull
	private final Provider<StudyServiceNotificationTask> studyServiceNotificationTaskProvider;
	@Nonnull
	private final Object backgroundTaskLock;
	@Nonnull
	private Boolean backgroundTaskStarted;
	@Nullable
	private ScheduledExecutorService backgroundTaskExecutorService;

	@Nonnull
	private static final Long BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;
	@Nonnull
	private final Provider<MessageService> messageServiceProvider;

	static {
		STUDY_FILE_UPLOAD_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US).withZone(ZoneId.of("UTC"));
		BACKGROUND_TASK_INTERVAL_IN_SECONDS = 60L;
		BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS = 10L;
	}

	@Inject
	public StudyService(@Nonnull DatabaseProvider databaseProvider,
											@Nonnull Strings strings,
											@Nonnull Authenticator authenticator,
											@Nonnull Provider<AccountService> accountServiceProvider,
											@Nonnull Provider<SystemService> systemServiceProvider,
											@Nonnull Provider<StudyServiceNotificationTask> studyServiceNotificationTaskProvider,
											@Nonnull Provider<MessageService> messageServiceProvider) {
		requireNonNull(databaseProvider);
		requireNonNull(strings);
		requireNonNull(authenticator);
		requireNonNull(accountServiceProvider);
		requireNonNull(systemServiceProvider);
		requireNonNull(messageServiceProvider);

		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.authenticator = authenticator;
		this.accountServiceProvider = accountServiceProvider;
		this.systemServiceProvider = systemServiceProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.studyServiceNotificationTaskProvider = studyServiceNotificationTaskProvider;
		this.backgroundTaskLock = new Object();
		this.backgroundTaskStarted = false;
		this.messageServiceProvider = messageServiceProvider;
	}

	@Nonnull
	public List<AccountCheckIn> findAccountCheckInsForAccountAndStudy(@Nonnull Account account,
																																		@Nonnull UUID studyId,
																																		@Nonnull Optional<CheckInStatusGroupId> checkInStatusGroupId) {
		requireNonNull(account);
		requireNonNull(studyId);

		List<Object> sqlParams = new ArrayList<>();
		StringBuilder query = new StringBuilder("""
				SELECT *
					FROM v_account_check_in 
					WHERE account_id = ? AND study_id = ?
				""");
		sqlParams.add(account.getAccountId());
		sqlParams.add(studyId);

		if (checkInStatusGroupId.isPresent()) {
			query.append("AND check_in_status_group_id = ? ");
			sqlParams.add(checkInStatusGroupId.get().toString());
		}

		return getDatabase().queryForList(query.toString(), AccountCheckIn.class, sqlParams.toArray());
	}

	@Nonnull
	public List<Study> findStudiesByInstitutionId(@Nonnull InstitutionId institutionId) {
		return getDatabase().queryForList("""
				SELECT * 
				FROM study 
				WHERE institution_id = ? AND deleted=false""", Study.class, institutionId);

	}

	@Nonnull
	public List<AccountCheckInAction> findAccountCheckInActionsFoAccountAndCheckIn(@Nonnull UUID accountId,
																																								 @Nonnull UUID accountCheckInId,
																																								 @Nonnull Optional<CheckInActionStatusId> checkInActionStatusId) {
		requireNonNull(accountId);
		requireNonNull(accountCheckInId);

		List<Object> sqlParams = new ArrayList<>();
		StringBuilder query = new StringBuilder("""
				SELECT * 
				FROM v_account_check_in_action
				WHERE account_id = ? AND account_check_in_id = ?
				""");
		sqlParams.add(accountId);
		sqlParams.add(accountCheckInId);

		if (checkInActionStatusId.isPresent()) {
			query.append("AND check_in_action_status_id = ? ");
			sqlParams.add(checkInActionStatusId.get().toString());
		}

		return getDatabase().queryForList(query.toString(), AccountCheckInAction.class, sqlParams.toArray());
	}

	@Nonnull
	public Boolean accountCheckInStarted(@Nonnull UUID accountId,
																			 @Nonnull UUID accountCheckInId) {
		requireNonNull(accountId);
		requireNonNull(accountCheckInId);


		return getDatabase().queryForObject("""
				SELECT COUNT(*) > 0
				FROM v_account_check_in_action
				WHERE account_id = ? AND account_check_in_id = ?
				AND check_in_action_status_id != ?
				""", Boolean.class, accountId, accountCheckInId, CheckInActionStatusId.INCOMPLETE).get();
	}

	@Nonnull
	public Optional<AccountCheckInAction> findAccountCheckInActionFoAccountAndCheckIn(@Nonnull UUID accountId,
																																										@Nonnull UUID accountCheckInId) {
		requireNonNull(accountId);
		requireNonNull(accountCheckInId);

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_account_check_in_action
				WHERE account_id = ? AND account_check_in_action_id = ?
				""", AccountCheckInAction.class, accountId, accountCheckInId);
	}

	@Nonnull
	public Optional<Study> findStudyByIdentifier(@Nullable Object studyIdentifier) {
		if (studyIdentifier == null)
			return Optional.empty();

		if (studyIdentifier instanceof UUID)
			return findStudyById((UUID) studyIdentifier);

		if (studyIdentifier instanceof String) {
			String studyIdentifierAsString = (String) studyIdentifier;

			if (isValidUUID(studyIdentifierAsString))
				return findStudyById(UUID.fromString(studyIdentifierAsString));

			return findStudyByInstitutionIdAndUrlName(studyIdentifierAsString);
		}

		return Optional.empty();
	}

	@Nonnull
	public Optional<Study> findStudyById(@Nullable UUID studyId) {
		if (studyId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM study
				WHERE study_id=?
				""", Study.class, studyId);
	}

	@Nonnull
	public Optional<Study> findStudyByInstitutionIdAndUrlName(@Nullable String urlName) {
		if (urlName == null)
			return Optional.empty();

		urlName = urlName.trim();

		return getDatabase().queryForObject("""
				SELECT *
				FROM study
				WHERE url_name=?
				""", Study.class, urlName);
	}

	@Nonnull
	public List<Study> findStudiesByPatientOrderId(@Nullable UUID patientOrderId) {
		if (patientOrderId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT s.*
				FROM study s, patient_order_study pos
				WHERE s.study_id=pos.study_id
				AND pos.patient_order_id=?
				ORDER BY pos.display_order
				""", Study.class, patientOrderId);
	}

	@Nonnull
	public List<StudyAccount> generateAccountsForStudies(@Nonnull CreateStudyAccountRequest request,
																											 @Nonnull Account account) {
		requireNonNull(request);
		requireNonNull(account);

		List<UUID> studyIds = request.getStudyIds();
		Integer participantCount = request.getParticipantCount();

		ValidationException validationException = new ValidationException();

		if (studyIds == null || studyIds.size() == 0)
			validationException.add(new FieldError("studyIds", getStrings().get("At least one study id is required.")));
		if (participantCount == null)
			validationException.add(new FieldError("participantCount", getStrings().get("Participant count is required.")));

		if (validationException.hasErrors())
			throw validationException;

		List<StudyAccount> studyAccounts = generateAccountsForStudy(participantCount, account);

		for (StudyAccount studyAccount : studyAccounts)
			for (UUID studyId : studyIds) {
				addAccountToStudy(studyAccount.getAccountId(), studyId);
			}

		return studyAccounts;
	}

	@Nonnull
	public List<StudyAccount> generateAccountsForStudy(@Nonnull Integer participantCount,
																										 @Nonnull Account account) {
		requireNonNull(participantCount);
		requireNonNull(account);

		ValidationException validationException = new ValidationException();
		List<StudyAccount> studyAccounts = new ArrayList<>();

		if (!account.getRoleId().equals(RoleId.ADMINISTRATOR))
			validationException.add(new FieldError("accountId", getStrings().get("Only administrators can create Study accounts.")));

		if (validationException.hasErrors())
			throw validationException;

		for (int i = 0; i < participantCount; ++i) {
			String accountUsername = new Random().ints(10, 97, 122)
					.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
					.toString();
			String accountPassword = new Random().ints(10, 97, 122)
					.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
					.toString();

			getLogger().debug(format("Adding %s with password %s", accountUsername, accountPassword));
			UUID accountId = getAccountService().createAccount(new CreateAccountRequest() {{
				setAccountSourceId(AccountSourceId.USERNAME);
				setRoleId(RoleId.PATIENT);
				setInstitutionId(account.getInstitutionId());
				setUsername(accountUsername);
				setDisplayName(accountUsername);
				setPassword(getAuthenticator().hashPassword(accountPassword));
				setPasswordResetRequired(true);
			}});
			studyAccounts.add(new StudyAccount() {{
				setAccountId(accountId);
				setUsername(accountUsername);
				setPassword(accountPassword);
			}});
		}

		return studyAccounts;
	}

	@Nonnull
	public Optional<StudyCheckInAction> findStudyCheckInActionById(@Nonnull UUID studyCheckInActionId) {
		requireNonNull(studyCheckInActionId);

		return getDatabase().queryForObject("""
				SELECT *
				FROM study_check_in_action
				WHERE study_check_in_action_id = ?""", StudyCheckInAction.class, studyCheckInActionId);
	}

	@Nonnull
	public void addAccountToStudy(@Nonnull UUID accountId,
																@Nonnull UUID studyId) {
		requireNonNull(accountId);
		requireNonNull(studyId);

		Account account = getAccountService().findAccountById(accountId).get();
		ValidationException validationException = new ValidationException();
		List<StudyCheckIn> studyCheckIns = getDatabase().queryForList("""
				SELECT * 
				FROM study_check_in 
				WHERE study_id = ? 
				ORDER BY check_in_number ASC""", StudyCheckIn.class, studyId);
		LocalDateTime currentDate = LocalDateTime.now(account.getTimeZone());
		LocalDateTime checkInStartDateTime;
		LocalDateTime checkInEndDateTime;
		Integer minutesInDay = 1440;
		UUID accountStudyId = UUID.randomUUID();
		Optional<Study> study = findStudyById(studyId);

		Boolean accountAlreadyInStudy = getDatabase().queryForObject("""
				SELECT COUNT(*) > 0
				FROM v_account_study
				WHERE account_id = ?
				AND study_id = ?
				""", Boolean.class, account.getAccountId(), studyId).get();

		if (accountAlreadyInStudy)
			validationException.add(new FieldError("account", getStrings().get("Account is already in this study.")));

		if (!study.isPresent())
			validationException.add(new FieldError("study", getStrings().get("This study does not exist.")));
		else if (!study.get().getInstitutionId().equals(account.getInstitutionId()))
			validationException.add(new FieldError("study", getStrings().get("You cannot join this study.")));

		if (validationException.hasErrors())
			throw validationException;

		// This is the format expected by Beiwe.
		// We might have this be specifiable in the future for other types of studies
		UUID encryptionKeypairId = getSystemService().createEncryptionKeypair("RSA", 2048);

		getDatabase().execute("""
					INSERT INTO account_study
					  (account_study_id, account_id, study_id, encryption_keypair_id)
					VALUES
					  (?, ?, ?, ?)
				""", accountStudyId, account.getAccountId(), studyId, encryptionKeypairId);

		int checkInCount = 0;
		for (StudyCheckIn studyCheckIn : studyCheckIns) {
			if (study.get().getMinutesBetweenCheckIns() >= minutesInDay) {
				//TODO: Validate that the minutes are a whole number of days
				Integer daysToAdd = checkInCount == 0 ? 0 : (study.get().getMinutesBetweenCheckIns() * checkInCount) / minutesInDay;
				LocalDate checkInStartDate = currentDate.toLocalDate().plusDays(daysToAdd);
				checkInStartDateTime = LocalDateTime.of(checkInStartDate, LocalTime.of(0, 0, 0));
			} else {
				checkInStartDateTime = currentDate.plus(study.get().getMinutesBetweenCheckIns() * checkInCount, ChronoUnit.MINUTES);
			}
			checkInEndDateTime = checkInStartDateTime.plusMinutes(study.get().getMinutesBetweenCheckIns());

			UUID accountCheckInId = UUID.randomUUID();
			getDatabase().execute("""
									INSERT INTO account_check_in 
									  (account_check_in_id, account_study_id, study_check_in_id, check_in_start_date_time, check_in_end_date_time)
									VALUES
									  (?, ?, ?, ?, ?)      				
					""", accountCheckInId, accountStudyId, studyCheckIn.getStudyCheckInId(), checkInStartDateTime, checkInEndDateTime);

			getDatabase().execute("""
					INSERT INTO account_check_in_action
					  (account_check_in_action_id, study_check_in_action_id, account_check_in_id, check_in_action_status_id)
					SELECT uuid_generate_v4(), study_check_in_action_id, ?, ?
					FROM study_check_in_action
					WHERE study_check_in_id = ?
					""", accountCheckInId, CheckInActionStatusId.INCOMPLETE, studyCheckIn.getStudyCheckInId());
			checkInCount++;
		}
	}

	@Nonnull
	private void updateCheckInStatusId(@Nonnull UUID accountCheckInId,
																		 @Nonnull CheckInStatusId checkInStatusId) {
		getDatabase().execute("""
				UPDATE account_check_in 
				SET check_in_status_id = ? 
				WHERE account_check_in_id = ?""", checkInStatusId, accountCheckInId);
	}

	@Nonnull
	public void rescheduleAccountCheckIn(@Nonnull Account account, @Nonnull UUID studyId) {
		//Adjust the start and end times for all future check-ins based on the current check-in. If there is no
		//current check-in then take the "next" check-in and make that active. If it's been longer than the
		//study.minutesBetweenCheckIns then start a check-in immediately
		ValidationException validationException = new ValidationException();
		//Find all of the check-ins
		List<AccountCheckIn> accountCheckIns = findAccountCheckInsForAccountAndStudy(account, studyId, Optional.empty());
		int checkInCount = 0;
		Integer minutesInADay = 1440;
		LocalDateTime checkInStartDateTime;
		LocalDateTime checkInEndDateTime;
		LocalDateTime currentDateTime = LocalDateTime.now(account.getTimeZone());
		LocalDateTime newStartDateTime = currentDateTime;
		Optional<Study> study = findStudyById(studyId);
		Optional<AccountStudy> accountStudy = findAccountStudyByAccountIdAndStudyId(account.getAccountId(), studyId);
		Boolean resetCheckIns = false;
		Boolean reschedulingCheckIns = false;

		if (!study.isPresent())
			validationException.add(new FieldError("studyId", getStrings().get("Not a valid Study ID.")));

		if (!accountStudy.isPresent())
			validationException.add(new FieldError("accountId", getStrings().get("Account not part of this study.")));

		if (validationException.hasErrors())
			throw validationException;

		getLogger().debug("Rescheduling check-ins");
		for (AccountCheckIn accountCheckIn : accountCheckIns) {
			if (accountStudy.get().getStudyStarted() && accountCheckActive(account, accountCheckIn) && accountCheckIn.getCheckInNumber() == 1) {
				getLogger().debug(format("Breaking because check-in %s is active.", accountCheckIn.getCheckInNumber()));
				break;
			} else if (accountCheckIn.getCheckInNumber() == 1 && !accountCheckIn.getCheckInStatusId().equals(CheckInStatusId.COMPLETE)) {
				//This is the first check-in and it has not been completed so check to see if it's been started
				getLogger().debug("Hit first check-in and it's not complete, check if it's been started");
				Boolean checkInStarted = accountCheckInStarted(account.getAccountId(), accountCheckIn.getAccountCheckInId());
				if (checkInStarted) {
					getLogger().debug("First check-in is started but not complete so expiring and continuing on.");
					updateCheckInStatusId(accountCheckIn.getAccountCheckInId(), CheckInStatusId.EXPIRED);
					checkInCount = 1;
					continue;
				} else if (accountStudy.get().getStudyStarted() && accountCheckExpired(account, accountCheckIn)) {
					getLogger().debug(format("Check-in %s has expired so continuing to next check-in", accountCheckIn.getCheckInNumber()));
					if (!accountCheckIn.getCheckInStatusId().equals(CheckInStatusId.EXPIRED)) {
						getLogger().debug(format("Check-in %s was not set to expired so expiring", accountCheckIn.getCheckInNumber()));
						updateCheckInStatusId(accountCheckIn.getAccountCheckInId(), CheckInStatusId.EXPIRED);
					}
					continue;
				} else {
					getLogger().debug("First check-in is NOT started and not complete so setting start time to now and continuing on.");
					newStartDateTime = currentDateTime;
					reschedulingCheckIns = true;
				}
			} else if (accountCheckIn.getCheckInStatusId().equals(CheckInStatusId.COMPLETE)) {
				getLogger().debug(format("Check-in %s is complete so continuing to next check-in", accountCheckIn.getCheckInNumber()));
				newStartDateTime = accountCheckIn.getCompletedDate();
				checkInCount = 1;

				if (accountCheckIn.getCheckInNumber() == accountCheckIns.size())
					resetCheckIns = true;
				continue;
			} else if (accountCheckExpired(account, accountCheckIn) && !reschedulingCheckIns) {
				getLogger().debug(format("Check-in %s has expired so continuing to next check-in", accountCheckIn.getCheckInNumber()));
				newStartDateTime = currentDateTime;
				checkInCount = 1;
				if (!accountCheckIn.getCheckInStatusId().equals(CheckInStatusId.EXPIRED)) {
					getLogger().debug(format("Check-in %s was not set to expired so expiring", accountCheckIn.getCheckInNumber()));
					updateCheckInStatusId(accountCheckIn.getAccountCheckInId(), CheckInStatusId.EXPIRED);
				}

				if (accountCheckIn.getCheckInNumber() == accountCheckIns.size())
					resetCheckIns = true;
				continue;
			}

			// If the study has not started then reset start/end times for check ins
			if (!accountStudy.get().getStudyStarted()) {
				if (study.get().getMinutesBetweenCheckIns() >= minutesInADay) {
					//TODO: Validate that the minutes are a whole number of days
					Integer daysToAdd = checkInCount == 0 ? 0 : (study.get().getMinutesBetweenCheckIns() * checkInCount) / minutesInADay;
					LocalDate checkInStartDate = newStartDateTime.toLocalDate().plusDays(daysToAdd);
					checkInStartDateTime = LocalDateTime.of(checkInStartDate, LocalTime.of(0, 0, 0));
				} else {
					Integer minutesToAdd = checkInCount == 0 ? 0 : study.get().getMinutesBetweenCheckIns() * checkInCount;
					checkInStartDateTime = newStartDateTime.plus(minutesToAdd, ChronoUnit.MINUTES);
					getLogger().debug(format("Adding %s minutes to %s and setting next check-in to %s", minutesToAdd, newStartDateTime, checkInStartDateTime));
				}
				checkInEndDateTime = checkInStartDateTime.plusMinutes(study.get().getMinutesBetweenCheckIns()).minusSeconds(1);

				getDatabase().execute("""
						UPDATE account_check_in
						SET check_in_start_date_time = ?, check_in_end_date_time = ?
						WHERE account_check_in_id = ?
						""", checkInStartDateTime, checkInEndDateTime, accountCheckIn.getAccountCheckInId());
			}

			checkInCount++;
		}

		if (resetCheckIns && study.get().getResetAfterFinalCheckIn()) {
			getLogger().debug("Final check-in reached, resetting check-ins");
			resetCheckIns(account, studyId);
		}
	}


	@Nonnull
	private void resetCheckIns(@Nonnull Account account, @Nonnull UUID studyId) {
		requireNonNull(account);
		requireNonNull(studyId);
		getLogger().debug(format("Setting account_study to deleted for account_id %s and study_id %s", account.getAccountId(), studyId));
		getDatabase().execute("""
				UPDATE account_study 
				SET deleted=true 
				WHERE account_id=? AND study_id =? 
				AND deleted=false""", account.getAccountId(), studyId);

		addAccountToStudy(account.getAccountId(), studyId);
	}

	@Nonnull
	public boolean accountCheckActive(Account account, AccountCheckIn accountCheckIn) {
		return !accountCheckIn.getCheckInStatusId().equals(CheckInStatusId.COMPLETE) &&
				(LocalDateTime.now(account.getTimeZone()).isAfter(accountCheckIn.getCheckInStartDateTime())
						&& LocalDateTime.now(account.getTimeZone()).isBefore(accountCheckIn.getCheckInEndDateTime()));
	}

	@Nonnull
	public boolean accountCheckExpired(Account account, AccountCheckIn accountCheckIn) {
		return !accountCheckIn.getCheckInStatusId().equals(CheckInStatusId.COMPLETE) && LocalDateTime.now(account.getTimeZone()).isAfter(accountCheckIn.getCheckInEndDateTime());
	}

	@Nonnull
	public Optional<AccountCheckInAction> findAccountCheckInActionById(@Nullable UUID accountCheckInActionId) {
		if (accountCheckInActionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_account_check_in_action
				WHERE account_check_in_action_id = ?
				""", AccountCheckInAction.class, accountCheckInActionId);
	}

	@Nonnull
	public Optional<AccountCheckIn> findAccountCheckInById(@Nullable UUID accountCheckInId) {
		if (accountCheckInId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_account_check_in
				WHERE account_check_in_id = ?
				""", AccountCheckIn.class, accountCheckInId);
	}

	@Nonnull
	public Optional<AccountCheckIn> findAccountCheckInByActionId(@Nullable UUID accountCheckInActionId) {
		if (accountCheckInActionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT ac.*, sci.check_in_number
				FROM account_check_in ac, account_check_in_action aci, study_check_in sci
				WHERE ac.account_check_in_id = aci.account_check_in_id
				AND ac.study_check_in_id = sci.study_check_in_id
				AND aci.account_check_in_action_id = ?
				""", AccountCheckIn.class, accountCheckInActionId);
	}

	@Nonnull
	private Boolean checkInComplete(@Nonnull UUID accountCheckInId) {
		return getDatabase().queryForObject("""
				SELECT COUNT(*) = 0
				FROM account_check_in_action
				WHERE account_check_in_id = ?
				AND check_in_action_status_id != ?
				""", Boolean.class, accountCheckInId, CheckInActionStatusId.COMPLETE).get();
	}

	@Nonnull
	public void updateAccountCheckInAction(@Nonnull Account account,
																				 @Nonnull UpdateCheckInAction request) {
		requireNonNull(account);
		requireNonNull(request);

		ValidationException validationException = new ValidationException();
		Optional<AccountCheckInAction> accountCheckInAction = findAccountCheckInActionById(request.getAccountCheckInActionId());
		CheckInActionStatusId checkInActionStatusId = request.getCheckInActionStatusId();
		LocalDateTime currentLocalDateTime = LocalDateTime.now(account.getTimeZone());

		if (!accountCheckInAction.isPresent())
			validationException.add(new FieldError("accountCheckInAction", getStrings().get("Account check-in action not found.")));

		Optional<AccountCheckIn> accountCheckIn = null;
		if (accountCheckInAction.isPresent()) {
			accountCheckIn = findAccountCheckInById(accountCheckInAction.get().getAccountCheckInId());
			if (!accountCheckIn.isPresent()) {
				validationException.add(new FieldError("accountCheckIn", getStrings().get("Account check-in not found.")));
			} else if (accountCheckIn.get().getCheckInStatusId().equals(CheckInStatusId.COMPLETE) || accountCheckInAction.get().getCheckInActionStatusId() == CheckInActionStatusId.COMPLETE) {
				String checkInActionDescription = getStrings().get("action");

				if (accountCheckIn.get().getCheckInTypeId() == CheckInTypeId.SCREENING)
					checkInActionDescription = getStrings().get("assessment");
				else if (accountCheckIn.get().getCheckInTypeId() == CheckInTypeId.VIDEO)
					checkInActionDescription = getStrings().get("recording");

				validationException.add(new FieldError("accountCheckIn", getStrings().get("This check-in's {{checkInActionDescription}} has already been completed. You won't be able to do another one until your next check-in time.",
						Map.of("checkInActionDescription", checkInActionDescription))));
			} else if (accountCheckInAction.get().getCheckInActionStatusId().compareTo(CheckInActionStatusId.IN_PROGRESS) != 0) {
				if (accountCheckIn.get().getCheckInStartDateTime().isAfter(currentLocalDateTime) || accountCheckIn.get().getCheckInEndDateTime().isBefore(currentLocalDateTime))
					validationException.add(new FieldError("accountCheckIn", getStrings().get("Check-in is not permitted at this time.")));
			}
		}

		if (checkInActionStatusId == null)
			validationException.add(new FieldError("checkInStatusId", getStrings().get("checkInStatusId is required.")));

		if (validationException.hasErrors())
			throw validationException;

		//TODO: validate account owns the check-in and validate status changes, should not be able to go from COMPLETE -> INCOMPLETE
		getDatabase().execute("""
				UPDATE account_check_in_action 
				SET check_in_action_status_id = ?
				WHERE account_check_in_action_id=?
				""", checkInActionStatusId, request.getAccountCheckInActionId());

		if (request.getCheckInActionStatusId().equals(CheckInActionStatusId.COMPLETE)) {
			// If this check-in has a followup notification to be sent and we have not already sent one, schedule that now
			if (accountCheckInAction.get().getSendFollowupNotification() && !followupNotificationSentForAccountCheckInAction(request.getAccountCheckInActionId())) {
				List<ClientDevicePushToken> clientDevicePushTokens = getAccountService().findClientDevicePushTokensForAccountId(account.getAccountId());
				Map<String, Object> standardMessageContext = new HashMap<>();
				StudyCheckInAction studyCheckInAction = findStudyCheckInActionById(accountCheckInAction.get().getStudyCheckInActionId()).get();

				standardMessageContext.put("condition", format("Check-In %s", accountCheckIn.get().getCheckInNumber()));
				standardMessageContext.put("title", studyCheckInAction.getFollowupNotificationMessageTitle());
				standardMessageContext.put("body", studyCheckInAction.getFollowupNotificationMessageBody());

				getLogger().debug(format("Scheduling a micro-intervention for accountCheckInActionId %s", accountCheckInAction.get().getAccountCheckInActionId()));
				for (ClientDevicePushToken clientDevicePushToken : clientDevicePushTokens) {
					PushMessage pushMessage = new PushMessage.Builder(account.getInstitutionId(), PushMessageTemplate.FREEFORM,
							clientDevicePushToken.getClientDevicePushTokenTypeId(), clientDevicePushToken.getPushToken(), Locale.US)
							.messageContext(standardMessageContext).build();
					UUID scheduledMessageId = getMessageService().createScheduledMessage(new CreateScheduledMessageRequest<>() {{
						setMessage(pushMessage);
						setTimeZone(account.getTimeZone());
						setScheduledAt(LocalDateTime.now(account.getTimeZone()).plusMinutes(accountCheckInAction.get().getFollowupNotificationMinutes()));
					}});

					getDatabase().execute("""
							INSERT INTO account_check_in_action_scheduled_message
							  (account_check_in_action_id, scheduled_message_id)
							VALUES
							  (?,?)""", accountCheckInAction.get().getAccountCheckInActionId(), scheduledMessageId);
				}
			}
			// If a check-in action is being completed, check if all the actions are complete
			// and set the check-in to complete if they are.
			if (checkInComplete(accountCheckInAction.get().getAccountCheckInId())) {
				LocalDateTime completedDateTime = LocalDateTime.now(account.getTimeZone());
				getDatabase().execute("""
						UPDATE account_check_in 
						SET check_in_status_id = ?, completed_date = ?
						WHERE account_check_in_id = ?
						""", CheckInStatusId.COMPLETE.toString(), completedDateTime, accountCheckInAction.get().getAccountCheckInId());
			}
		}

		if (!accountCheckIn.get().getCheckInStatusId().equals(CheckInStatusId.IN_PROGRESS))
			updateCheckInStatusId(accountCheckIn.get().getAccountCheckInId(), CheckInStatusId.IN_PROGRESS);
	}

	@Nonnull
	private Boolean followupNotificationSentForAccountCheckInAction(@Nonnull UUID accountaCheckInActionId) {
		return getDatabase().queryForObject("""
				SELECT COUNT(*) > 0
				FROM account_check_in_action_scheduled_message
				WHERE account_check_in_action_id = ?
				""", Boolean.class, accountaCheckInActionId).get();
	}

	@Nonnull
	public Optional<AccountStudy> findAccountStudyById(@Nullable UUID accountStudyId) {
		if (accountStudyId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				  SELECT *
				  FROM v_account_study
				  WHERE account_study_id=?
				""", AccountStudy.class, accountStudyId);
	}

	@Nonnull
	public Optional<AccountStudy> findAccountStudyByAccountIdAndStudyId(@Nullable UUID accountId,
																																			@Nullable UUID studyId) {
		if (accountId == null || studyId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				  SELECT *
				  FROM v_account_study
				  WHERE account_id=?
				  AND study_id=?
				""", AccountStudy.class, accountId, studyId);
	}

	@Nonnull
	public Optional<AccountStudy> findAccountStudyByUsernameAndStudyId(@Nullable String username,
																																		 @Nullable UUID studyId) {
		if (username == null || studyId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				  SELECT *
				  FROM v_account_study
				  WHERE username=?
				  AND study_id=?
				""", AccountStudy.class, username, studyId);
	}

	@Nonnull
	public Optional<StudyBeiweConfig> findStudyBeiweConfigByStudyId(@Nullable UUID studyId) {
		if (studyId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
					SELECT *
					FROM study_beiwe_config
					WHERE study_id=?
				""", StudyBeiweConfig.class, studyId);
	}

	@Nonnull
	public Set<AccountSourceId> findPermittedAccountSourceIdsByStudyId(@Nullable UUID studyId) {
		if (studyId == null)
			return Set.of();

		Study study = findStudyById(studyId).orElse(null);

		if (study == null)
			return Set.of();

		// See if there are explicitly-permitted account sources for this study
		List<AccountSource> permittedAccountSources = getDatabase().queryForList("""
					SELECT asrc.*
					FROM account_source asrc, study_account_source sas
					WHERE asrc.account_source_id=sas.account_source_id
					AND sas.study_id=?
				""", AccountSource.class, studyId);

		// Didn't find any explicitly permitted for this study?  Then all of them are permitted
		if (permittedAccountSources.size() == 0) {
			permittedAccountSources = getDatabase().queryForList("""
						SELECT *
						FROM institution_account_source
						WHERE institution_id=?
					""", AccountSource.class, study.getInstitutionId());
		}

		return permittedAccountSources.stream()
				.map(accountSource -> accountSource.getAccountSourceId())
				.collect(Collectors.toSet());
	}

	@Nonnull
	public Optional<Study> findStudyByStudyCheckInActionId(@Nullable UUID studyCheckInActionId) {
		if (studyCheckInActionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				  SELECT s.*
				  FROM study s, study_check_in sci, study_check_in_action scia
				  WHERE scia.study_check_in_action_id=?
				  AND scia.study_check_in_id=sci.study_check_in_id
				  AND sci.study_id=s.study_id
				""", Study.class, studyCheckInActionId);
	}

	@Nonnull
	public FileUploadResult createAccountCheckInActionFileUpload(@Nonnull CreateAccountCheckInActionFileUploadRequest request) {
		requireNonNull(request);

		AccountCheckInAction accountCheckInAction = null;
		ValidationException validationException = new ValidationException();

		if (request.getAccountId() == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (request.getAccountCheckInActionId() == null) {
			validationException.add(new FieldError("accountCheckInActionId", getStrings().get("Account check-in action ID is required.")));
		} else {
			accountCheckInAction = findAccountCheckInActionById(request.getAccountCheckInActionId()).orElse(null);

			if (accountCheckInAction == null)
				validationException.add(new FieldError("accountCheckInActionId", getStrings().get("Account check-in action ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		// Make a separate instance so we don't mutate the request passed into this method
		CreateFileUploadRequest fileUploadRequest = new CreateFileUploadRequest();
		fileUploadRequest.setAccountId(request.getAccountId());
		fileUploadRequest.setFileUploadTypeId(request.getFileUploadTypeId());
		fileUploadRequest.setContentType(request.getContentType());
		fileUploadRequest.setFilename(request.getFilename());
		fileUploadRequest.setFilesize(request.getFilesize());
		fileUploadRequest.setPublicRead(false);
		fileUploadRequest.setStorageKeyPrefix(format("account-check-in-actions/%s/%s%s", accountCheckInAction.getAccountCheckInActionId(),
				request.getAccountId(), STUDY_FILE_UPLOAD_TIMESTAMP_FORMATTER.format(Instant.now())));
		fileUploadRequest.setMetadata(Map.of(
				"account-check-in-action-id", request.getAccountCheckInActionId().toString(),
				"account-id", request.getAccountId().toString()
		));

		FileUploadResult fileUploadResult = getSystemService().createFileUpload(fileUploadRequest);

		getDatabase().execute("""
				INSERT INTO account_check_in_action_file_upload (
				  account_check_in_action_id,
				  file_upload_id
				) VALUES (?,?)
				""", accountCheckInAction.getAccountCheckInActionId(), fileUploadResult.getFileUploadId());

		return fileUploadResult;
	}

	@Nonnull
	public FileUploadResult createStudyFileUpload(@Nonnull CreateStudyFileUploadRequest request) {
		requireNonNull(request);

		Study study = null;
		ValidationException validationException = new ValidationException();

		if (request.getAccountId() == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (request.getStudyId() == null) {
			validationException.add(new FieldError("studyId", getStrings().get("Study ID is required.")));
		} else {
			study = findStudyById(request.getStudyId()).orElse(null);

			if (study == null)
				validationException.add(new FieldError("studyId", getStrings().get("Study ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		// Make a separate instance so we don't mutate the request passed into this method
		CreateFileUploadRequest fileUploadRequest = new CreateFileUploadRequest();
		fileUploadRequest.setAccountId(request.getAccountId());
		fileUploadRequest.setFileUploadTypeId(request.getFileUploadTypeId());
		fileUploadRequest.setContentType(request.getContentType());
		fileUploadRequest.setFilename(request.getFilename());
		fileUploadRequest.setFilesize(request.getFilesize());
		fileUploadRequest.setPublicRead(false);
		fileUploadRequest.setStorageKeyPrefix(format("studies/%s/%s/%s", study.getStudyId(), request.getAccountId(),
				STUDY_FILE_UPLOAD_TIMESTAMP_FORMATTER.format(Instant.now())));
		fileUploadRequest.setMetadata(Map.of(
				"study-id", request.getStudyId().toString(),
				"account-id", request.getAccountId().toString()
		));

		FileUploadResult fileUploadResult = getSystemService().createFileUpload(fileUploadRequest);

		getDatabase().execute("""
				INSERT INTO study_file_upload (
				  study_id,
				  file_upload_id
				) VALUES (?,?)
				""", study.getStudyId(), fileUploadResult.getFileUploadId());

		return fileUploadResult;
	}

	@Nonnull
	public List<Study> findStudiesForAccountId(@Nullable UUID accountId) {
		requireNonNull(accountId);

		return getDatabase().queryForList("""
				SELECT s.*
				FROM study s, v_account_study a
				WHERE s.study_id = a.study_id
				AND a.account_id = ?
				""", Study.class, accountId);
	}

	@Nonnull
	public List<StudyFileUpload> findStudyFileUploadsByAccountStudyId(@Nullable UUID accountStudyId) {
		if (accountStudyId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM v_study_file_upload
				WHERE account_study_id=?
				ORDER BY file_upload_created
				""", StudyFileUpload.class, accountStudyId);
	}

	@Nonnull
	public Optional<StudyFileUpload> findStudyFileUploadByStudyIdAndFileUploadId(@Nullable UUID studyId,
																																							 @Nullable UUID fileUploadId) {
		if (studyId == null || fileUploadId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_study_file_upload
				WHERE study_id=?
				AND file_upload_id=?
				""", StudyFileUpload.class, studyId, fileUploadId);
	}

	@Nonnull
	public List<AccountCheckInActionFileUpload> findAccountCheckInActionFileUploadsByAccountStudyId(@Nullable UUID accountStudyId) {
		if (accountStudyId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM v_account_check_in_action_file_upload
				WHERE account_study_id=?
				ORDER BY file_upload_created
				""", AccountCheckInActionFileUpload.class, accountStudyId);
	}

	@Nonnull
	public Optional<AccountCheckInActionFileUpload> findAccountCheckInActionFileUploadByAccountCheckInActionIdAndFileUploadId(@Nullable UUID accountCheckInActionId,
																																																														@Nullable UUID fileUploadId) {
		if (accountCheckInActionId == null || fileUploadId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_account_check_in_action_file_upload
				WHERE account_check_in_action_id=?
				AND file_upload_id=?
				""", AccountCheckInActionFileUpload.class, accountCheckInActionId, fileUploadId);
	}

	@Nonnull
	public void startStudyForAccount(@Nullable UUID accountId,
																	 @Nullable UUID studyId) {

		getDatabase().execute("""
				UPDATE account_study 
				SET study_started = true
				WHERE account_id = ? 
				AND study_id=? """, accountId, studyId);
	}

	@Nonnull
	public void updateAccountStudyPreferences(@Nonnull UpdateAccountStudyPreferences request) {
		ValidationException validationException = new ValidationException();

		getLogger().debug(format("looking for %s, %s, %s", request.getUsername(), AccountSourceId.USERNAME, request.getInstitutionId()));
		Optional<Account> account = getAccountService()
				.findAccountByUsernameAndAccountSourceId(request.getUsername(), AccountSourceId.USERNAME, request.getInstitutionId());

		if (!account.isPresent())
			validationException.add(new FieldError("username", getStrings().get("Could not find username.")));

		if (request.getRecordingPreferenceId() == null)
			validationException.add(new FieldError("recordingPreferenceId", getStrings().get("A valid recording preference is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE account_study 
				SET recording_preference_id = ? WHERE account_id = ? 
				AND study_id = ? """, request.getRecordingPreferenceId().toString(), account.get().getAccountId(), request.getStudyId());

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

			getLogger().trace("Starting Study Service background task...");

			this.backgroundTaskExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("study-service-background-task").build());
			this.backgroundTaskStarted = true;
			getBackgroundTaskExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getStudyServiceNotificationTask().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to complete Study Service background task - will retry in %s seconds", String.valueOf(getBackgroundTaskIntervalInSeconds())), e);
					}
				}
			}, getBackgroundTaskInitialDelayInSeconds(), getBackgroundTaskIntervalInSeconds(), TimeUnit.SECONDS);

			getLogger().trace("Study Service background task started.");

			return true;
		}
	}

	@Nonnull
	public Boolean stopBackgroundTask() {
		synchronized (getBackgroundTaskLock()) {
			if (!isBackgroundTaskStarted())
				return false;

			getLogger().trace("Stopping Study Service background task...");

			getBackgroundTaskExecutorService().get().shutdownNow();
			this.backgroundTaskExecutorService = null;
			this.backgroundTaskStarted = false;

			getLogger().trace("Study Service background task stopped.");

			return true;
		}
	}

	@ThreadSafe
	protected static class StudyServiceNotificationTask implements Runnable {
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
		@Nonnull
		private final AccountService accountService;

		@Nonnull
		private final MessageService messageService;

		@Inject
		public StudyServiceNotificationTask(@Nonnull CurrentContextExecutor currentContextExecutor,
																				@Nonnull ErrorReporter errorReporter,
																				@Nonnull DatabaseProvider databaseProvider,
																				@Nonnull Configuration configuration,
																				@Nonnull AccountService accountService,
																				@Nonnull MessageService messageService) {
			requireNonNull(currentContextExecutor);
			requireNonNull(errorReporter);
			requireNonNull(databaseProvider);
			requireNonNull(configuration);
			requireNonNull(accountService);
			requireNonNull(messageService);

			this.currentContextExecutor = currentContextExecutor;
			this.errorReporter = errorReporter;
			this.databaseProvider = databaseProvider;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
			this.accountService = accountService;
			this.messageService = messageService;
		}

		@Override
		public void run() {
			CurrentContext currentContext = new CurrentContext.Builder(InstitutionId.COBALT,
					getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

			getCurrentContextExecutor().execute(currentContext, () -> {
				try {
					scheduleCheckInNotificationReminder();
				} catch (Exception e) {
					getLogger().error("Unable to schedule study check-in reminders", e);
					getErrorReporter().report(e);
				}
			});
		}

		protected void scheduleCheckInNotificationReminder() {
			// TODO: support non fixed window studies
			// List of all studies that have check in reminders turned on and the number of minutes
			// at which to send a notification after the check in has started but has not been completed
			List<StudyCheckInReminder> studyCheckInReminders = getDatabase().queryForList("""
					SELECT *
					FROM v_study_check_in_reminder sc
					WHERE send_check_in_reminder_notification = true
					AND check_in_windows_fixed = true""", StudyCheckInReminder.class);

			// Get a list of all "Active" study check ins that have not been notified
			for (StudyCheckInReminder studyCheckInReminder : studyCheckInReminders) {
				getLogger().debug(format("Sending check in reminders for Study check in reminder id %s", studyCheckInReminder.getStudyCheckInReminderId()));
				List<AccountCheckIn> accountCheckIns =
						getDatabase().queryForList("""
												SELECT va.*
												FROM v_account_check_in va
												WHERE va.study_id = ?
												AND va.study_started = TRUE
												AND va.check_in_status_id != ?
												AND EXTRACT(EPOCH FROM NOW() at time zone va.time_zone - va.check_in_start_date_time) / 60 >  ?
												AND now() at time zone va.time_zone BETWEEN check_in_start_date_time AND check_in_end_date_time
												AND NOT EXISTS
												(SELECT 'X'
												FROM account_check_in_reminder ac
												WHERE ac.account_check_in_id = va.account_check_in_id
												AND ac.study_check_in_reminder_id = ?)
										""", AccountCheckIn.class, studyCheckInReminder.getStudyId(), CheckInStatusId.COMPLETE,
								studyCheckInReminder.getCheckInReminderNotificationMinutes(), studyCheckInReminder.getStudyCheckInReminderId());

				for (AccountCheckIn accountCheckIn : accountCheckIns) {
					Map<String, Object> standardMessageContext = new HashMap<>();
					standardMessageContext.put("condition", format("accountId %s", accountCheckIn.getAccountId()));
					standardMessageContext.put("title", studyCheckInReminder.getCheckInReminderNotificationMessageTitle());
					standardMessageContext.put("body", studyCheckInReminder.getCheckInReminderNotificationMessageBody());

					List<ClientDevicePushToken> clientDevicePushTokens = accountService.findClientDevicePushTokensForAccountId(accountCheckIn.getAccountId());

					for (ClientDevicePushToken clientDevicePushToken : clientDevicePushTokens) {
						getLogger().debug(format("Scheduling account check-in reminder for accountId %s", accountCheckIn.getAccountId()));

						PushMessage pushMessage = new PushMessage.Builder(accountCheckIn.getInstitutionId(), PushMessageTemplate.FREEFORM,
								clientDevicePushToken.getClientDevicePushTokenTypeId(), clientDevicePushToken.getPushToken(), Locale.US)
								.messageContext(standardMessageContext).build();
						UUID scheduledMessageId = messageService.createScheduledMessage(new CreateScheduledMessageRequest<>() {{
							setMessage(pushMessage);
							setTimeZone(accountCheckIn.getTimeZone());
							setScheduledAt(LocalDateTime.now(accountCheckIn.getTimeZone()));
						}});

						getDatabase().execute("""
											INSERT INTO account_check_in_reminder
										  (account_check_in_id, study_check_in_reminder_id, scheduled_message_id)
										VALUES
										  (?,?,?)""",
								accountCheckIn.getAccountCheckInId(), studyCheckInReminder.getStudyCheckInReminderId(), scheduledMessageId);
					}

				}
			}
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
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return this.authenticator;
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
	protected StudyServiceNotificationTask getStudyServiceNotificationTask() {
		return this.studyServiceNotificationTaskProvider.get();
	}

	@Nonnull
	protected Object getBackgroundTaskLock() {
		return this.backgroundTaskLock;
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getBackgroundTaskExecutorService() {
		return Optional.ofNullable(this.backgroundTaskExecutorService);
	}

	@Nonnull
	protected MessageService getMessageService() {
		return this.messageServiceProvider.get();
	}
}
