/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.model.db;

import org.junit.Test;

import static com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId.CANCELED;
import static com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId.CLOSED;
import static com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId.OPEN;
import static org.junit.Assert.assertEquals;

public class CareEncounterStatusTests {
	@Test
	public void detailedStatusesMapToSimpleDisplayLabels() {
		assertEquals("Open", OPEN.getDisplayLabel());
		assertEquals("Closed", CLOSED.getDisplayLabel());
		assertEquals("Closed", CANCELED.getDisplayLabel());
	}
}
