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

package com.cobaltplatform.api.integration.hl7.model.segment;

import ca.uhn.hl7v2.model.v251.segment.OBX;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/OBX
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7ObservationResultSegment extends Hl7Object {
	// TODO

	@Nonnull
	public static Boolean isPresent(@Nullable OBX obx) {
		if (obx == null)
			return false;

		// TODO

		return true;
	}

	public Hl7ObservationResultSegment() {
		// Nothing to do
	}

	public Hl7ObservationResultSegment(@Nullable OBX obx) {
		if (obx != null) {
			// TODO
		}
	}
}