/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package com.cobaltplatform.api.service;

import com.cobaltplatform.api.UnitTest;
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin.PatientOrderCrisisMode;
import com.cobaltplatform.api.service.ScreeningService.PatientOrderCrisisActions;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.annotation.concurrent.ThreadSafe;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ThreadSafe
@Category(UnitTest.class)
public class ScreeningServiceCrisisTests {
	@Test
	public void emailOnlyFirstPatientCrisisFlagsWithoutInteractionOrStandardResponse() {
		PatientOrderCrisisActions actions = ScreeningService.determinePatientOrderCrisisActions(
				PatientOrderCrisisMode.EMAIL_ONLY, false, true, true);

		assertTrue(actions.markScreeningSession());
		assertTrue(actions.markPatientOrderForSafetyPlanning());
		assertTrue(actions.sendCrisisEmails());
		assertFalse(actions.createInteraction());
		assertFalse(actions.performStandardResponse());
	}

	@Test
	public void repeatedEmailOnlyCrisisDoesNotNotifyTwice() {
		PatientOrderCrisisActions actions = ScreeningService.determinePatientOrderCrisisActions(
				PatientOrderCrisisMode.EMAIL_ONLY, true, false, true);

		assertFalse(actions.markScreeningSession());
		assertFalse(actions.markPatientOrderForSafetyPlanning());
		assertFalse(actions.sendCrisisEmails());
		assertFalse(actions.createInteraction());
		assertFalse(actions.performStandardResponse());
	}

	@Test
	public void standardFirstPatientCrisisPreservesInteractionAndResponderWorkflow() {
		PatientOrderCrisisActions actions = ScreeningService.determinePatientOrderCrisisActions(
				PatientOrderCrisisMode.STANDARD, false, true, true);

		assertTrue(actions.markScreeningSession());
		assertTrue(actions.markPatientOrderForSafetyPlanning());
		assertTrue(actions.createInteraction());
		assertTrue(actions.performStandardResponse());
		assertFalse(actions.sendCrisisEmails());
	}

	@Test
	public void staffAdministeredOrderIsFlaggedWithoutResponderNotification() {
		PatientOrderCrisisActions actions = ScreeningService.determinePatientOrderCrisisActions(
				PatientOrderCrisisMode.EMAIL_ONLY, false, true, false);

		assertTrue(actions.markScreeningSession());
		assertTrue(actions.markPatientOrderForSafetyPlanning());
		assertFalse(actions.sendCrisisEmails());
	}
}
