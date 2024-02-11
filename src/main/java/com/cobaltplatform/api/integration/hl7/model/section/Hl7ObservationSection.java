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

package com.cobaltplatform.api.integration.hl7.model.section;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v251.group.ORM_O01_OBSERVATION;
import com.cobaltplatform.api.integration.hl7.UncheckedHl7ParsingException;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7NotesAndCommentsSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7ObservationResultSegment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/TriggerEvents/ORM_O01
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7ObservationSection extends Hl7Object {
	@Nullable
	private Hl7ObservationResultSegment observationResultSegment;
	@Nullable
	private List<Hl7NotesAndCommentsSegment> notesAndComments;

	@Nonnull
	public static Boolean isPresent(@Nullable ORM_O01_OBSERVATION observation) {
		if (observation == null)
			return false;

		try {
			return Hl7ObservationResultSegment.isPresent(observation.getOBX())
					|| observation.getNTEAll() != null && observation.getNTEAll().size() > 0;
		} catch (HL7Exception e) {
			throw new UncheckedHl7ParsingException(e);
		}
	}

	public Hl7ObservationSection() {
		// Nothing to do
	}

	public Hl7ObservationSection(@Nullable ORM_O01_OBSERVATION observation) {
		try {
			if (Hl7ObservationResultSegment.isPresent(observation.getOBX()))
				this.observationResultSegment = new Hl7ObservationResultSegment(observation.getOBX());

			if (observation.getNTEAll() != null && observation.getNTEAll().size() > 0)
				this.notesAndComments = observation.getNTEAll().stream()
						.map(nte -> Hl7NotesAndCommentsSegment.isPresent(nte) ? new Hl7NotesAndCommentsSegment(nte) : null)
						.filter(notesAndComments -> notesAndComments != null)
						.collect(Collectors.toList());
		} catch (HL7Exception e) {
			throw new UncheckedHl7ParsingException(e);
		}
	}

	@Nullable
	public Hl7ObservationResultSegment getObservationResultSegment() {
		return this.observationResultSegment;
	}

	public void setObservationResultSegment(@Nullable Hl7ObservationResultSegment observationResultSegment) {
		this.observationResultSegment = observationResultSegment;
	}

	@Nullable
	public List<Hl7NotesAndCommentsSegment> getNotesAndComments() {
		return this.notesAndComments;
	}

	public void setNotesAndComments(@Nullable List<Hl7NotesAndCommentsSegment> notesAndComments) {
		this.notesAndComments = notesAndComments;
	}
}