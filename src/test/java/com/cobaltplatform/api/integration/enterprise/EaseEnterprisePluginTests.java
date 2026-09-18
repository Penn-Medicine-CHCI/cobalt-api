/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package com.cobaltplatform.api.integration.enterprise;

import com.cobaltplatform.api.UnitTest;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.annotation.concurrent.ThreadSafe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ThreadSafe
@Category(UnitTest.class)
public class EaseEnterprisePluginTests {
	@Test
	public void autoProvisioningIsNonproductionPatientOnly() {
		assertTrue(EaseEnterprisePlugin.shouldAutoProvisionTestPatient(false, UserExperienceTypeId.PATIENT,
				true, "ease-test@example.com", "password"));
		assertFalse(EaseEnterprisePlugin.shouldAutoProvisionTestPatient(false, UserExperienceTypeId.STAFF,
				true, "ease-test@example.com", "password"));
		assertFalse(EaseEnterprisePlugin.shouldAutoProvisionTestPatient(true, UserExperienceTypeId.PATIENT,
				true, "ease-test@example.com", "password"));
	}

	@Test
	public void autoProvisionedAccountIsMarkedAsTestPatient() {
		CreateAccountRequest request = EaseEnterprisePlugin.createTestPatientAccountRequest(
				InstitutionId.COBALT_IC_EASE, "ease-test@example.com", "password-hash");

		assertEquals(RoleId.PATIENT, request.getRoleId());
		assertEquals(InstitutionId.COBALT_IC_EASE, request.getInstitutionId());
		assertEquals(AccountSourceId.EMAIL_PASSWORD, request.getAccountSourceId());
		assertEquals("ease-test@example.com", request.getEmailAddress());
		assertEquals("password-hash", request.getPassword());
		assertEquals(Boolean.TRUE, request.getTestAccount());
	}
}
