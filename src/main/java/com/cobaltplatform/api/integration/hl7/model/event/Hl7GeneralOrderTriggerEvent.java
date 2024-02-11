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

package com.cobaltplatform.api.integration.hl7.model.event;

import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.section.Hl7OrderSection;
import com.cobaltplatform.api.integration.hl7.model.section.Hl7PatientSection;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7MessageHeaderSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7NotesAndCommentsSegment;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/TriggerEvents/ORM_O01
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7GeneralOrderTriggerEvent extends Hl7Object {
	@Nullable
	private Hl7MessageHeaderSegment messageHeader;
	@Nullable
	private List<Hl7NotesAndCommentsSegment> notesAndComments;
	@Nullable
	private Hl7PatientSection patient;
	@Nullable
	private List<Hl7OrderSection> orders;

	@Nullable
	public Hl7MessageHeaderSegment getMessageHeader() {
		return this.messageHeader;
	}

	public void setMessageHeader(@Nullable Hl7MessageHeaderSegment messageHeader) {
		this.messageHeader = messageHeader;
	}

	@Nullable
	public List<Hl7NotesAndCommentsSegment> getNotesAndComments() {
		return this.notesAndComments;
	}

	public void setNotesAndComments(@Nullable List<Hl7NotesAndCommentsSegment> notesAndComments) {
		this.notesAndComments = notesAndComments;
	}

	@Nullable
	public Hl7PatientSection getPatient() {
		return this.patient;
	}

	public void setPatient(@Nullable Hl7PatientSection patient) {
		this.patient = patient;
	}

	@Nullable
	public List<Hl7OrderSection> getOrders() {
		return this.orders;
	}

	public void setOrders(@Nullable List<Hl7OrderSection> orders) {
		this.orders = orders;
	}
}