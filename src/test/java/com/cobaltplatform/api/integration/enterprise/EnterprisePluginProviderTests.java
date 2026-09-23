/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package com.cobaltplatform.api.integration.enterprise;

import com.cobaltplatform.api.UnitTest;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.annotation.concurrent.ThreadSafe;

import static org.junit.Assert.assertEquals;

@ThreadSafe
@Category(UnitTest.class)
public class EnterprisePluginProviderTests {
	@Test
	public void discoversPluginsThroughIntermediateBaseClasses() {
		assertEquals(CobaltIcEaseEnterprisePlugin.class,
				EnterprisePluginProvider.createEnterprisePluginClassesByInstitutionId().get(InstitutionId.COBALT_IC_EASE));
	}
}
