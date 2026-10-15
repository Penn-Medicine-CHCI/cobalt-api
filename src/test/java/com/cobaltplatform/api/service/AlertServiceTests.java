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

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.model.api.request.CreateAlertDismissalRequest;
import com.cobaltplatform.api.model.db.AlertDismissal;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

public class AlertServiceTests {
	@Test
	public void repeatedDismissalReturnsOriginalIdAndPreservesExistingRow() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AlertService alertService = app.getInjector().getInstance(AlertService.class);
			UUID accountId = createAccount(database);
			UUID alertId = createAlert(database);
			CreateAlertDismissalRequest request = requestFor(accountId, alertId);

			UUID originalId = alertService.createAlertDismissal(request);
			AlertDismissal original = findDismissal(database, accountId, alertId);
			Assert.assertEquals(originalId, original.getAlertDismissalId());
			Assert.assertNotNull(original.getCreated());
			Assert.assertNotNull(original.getLastUpdated());

			Assert.assertEquals(originalId, alertService.createAlertDismissal(request));
			List<AlertDismissal> dismissals = database.queryForList("""
					SELECT * FROM alert_dismissal WHERE account_id=? AND alert_id=?
					""", AlertDismissal.class, accountId, alertId);
			Assert.assertEquals(1, dismissals.size());
			AlertDismissal repeated = dismissals.get(0);
			Assert.assertEquals(originalId, repeated.getAlertDismissalId());
			Assert.assertEquals(accountId, repeated.getAccountId());
			Assert.assertEquals(alertId, repeated.getAlertId());
			Assert.assertEquals(original.getCreated(), repeated.getCreated());
			Assert.assertEquals(original.getLastUpdated(), repeated.getLastUpdated());
		});
	}

	@Test
	public void differentAccountsAndAlertsReceiveIndependentDismissals() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AlertService alertService = app.getInjector().getInstance(AlertService.class);
			UUID firstAccountId = createAccount(database);
			UUID secondAccountId = createAccount(database);
			UUID firstAlertId = createAlert(database);
			UUID secondAlertId = createAlert(database);

			UUID firstId = alertService.createAlertDismissal(requestFor(firstAccountId, firstAlertId));
			UUID otherAccountId = alertService.createAlertDismissal(requestFor(secondAccountId, firstAlertId));
			UUID otherAlertId = alertService.createAlertDismissal(requestFor(firstAccountId, secondAlertId));

			Assert.assertNotEquals(firstId, otherAccountId);
			Assert.assertNotEquals(firstId, otherAlertId);
			Assert.assertNotEquals(otherAccountId, otherAlertId);
			Assert.assertEquals(firstId, findDismissal(database, firstAccountId, firstAlertId).getAlertDismissalId());
			Assert.assertEquals(otherAccountId, findDismissal(database, secondAccountId, firstAlertId).getAlertDismissalId());
			Assert.assertEquals(otherAlertId, findDismissal(database, firstAccountId, secondAlertId).getAlertDismissalId());
			Assert.assertEquals(Long.valueOf(3), database.queryForObject("""
					SELECT COUNT(*) FROM alert_dismissal WHERE alert_id IN (?,?)
					""", Long.class, firstAlertId, secondAlertId).orElseThrow());
		});
	}

	private static UUID createAccount(Database database) {
		UUID accountId = UUID.randomUUID();
		database.execute("""
				INSERT INTO account (account_id, institution_id, role_id, account_source_id)
				VALUES (?, 'COBALT', 'PATIENT', 'EMAIL_PASSWORD')
				""", accountId);
		return accountId;
	}

	private static UUID createAlert(Database database) {
		UUID alertId = UUID.randomUUID();
		database.execute("""
				INSERT INTO alert (alert_id, alert_type_id, title, message)
				VALUES (?, 'INFORMATION', 'Dismissal test', 'Dismiss this alert')
				""", alertId);
		return alertId;
	}

	private static CreateAlertDismissalRequest requestFor(UUID accountId, UUID alertId) {
		CreateAlertDismissalRequest request = new CreateAlertDismissalRequest();
		request.setAccountId(accountId);
		request.setAlertId(alertId);
		return request;
	}

	private static AlertDismissal findDismissal(Database database, UUID accountId, UUID alertId) {
		return database.queryForObject("""
				SELECT * FROM alert_dismissal WHERE account_id=? AND alert_id=?
				""", AlertDismissal.class, accountId, alertId).orElseThrow();
	}
}
