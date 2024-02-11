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
import ca.uhn.hl7v2.model.v251.group.ORM_O01_ORDER_DETAIL;
import com.cobaltplatform.api.integration.hl7.UncheckedHl7ParsingException;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7ContactDataSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7DiagnosisSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7NotesAndCommentsSegment;

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
public class Hl7OrderDetailSection extends Hl7Object {
	@Nullable
	private Hl7OrderDetailSegmentSection orderDetailSegment;
	@Nullable
	private List<Hl7NotesAndCommentsSegment> notesAndComments;
	@Nullable
	private Hl7ContactDataSegment contactData;
	@Nullable
	private Hl7DiagnosisSegment diagnosis;
	@Nullable
	private List<Hl7ObservationSection> observation;

	@Nonnull
	public static Boolean isPresent(@Nullable ORM_O01_ORDER_DETAIL orderDetail) {
		if (orderDetail == null)
			return false;

		try {
			return Hl7OrderDetailSegmentSection.isPresent(orderDetail)
					|| (orderDetail.getNTEAll() != null && orderDetail.getNTEAll().size() > 0)
					|| Hl7ContactDataSegment.isPresent(orderDetail.getCTD())
					|| Hl7DiagnosisSegment.isPresent(orderDetail.getDG1())
					|| (orderDetail.getOBSERVATIONAll() != null && orderDetail.getOBSERVATIONAll().size() > 0);
		} catch (HL7Exception e) {
			throw new UncheckedHl7ParsingException(e);
		}
	}

	public Hl7OrderDetailSection() {
		// Nothing to do
	}

	public Hl7OrderDetailSection(@Nullable ORM_O01_ORDER_DETAIL orderDetail) {
		try {
			if (orderDetail != null) {
				if (Hl7OrderDetailSegmentSection.isPresent(orderDetail))
					this.orderDetailSegment = new Hl7OrderDetailSegmentSection(orderDetail);

				if (orderDetail.getNTEAll() != null && orderDetail.getNTEAll().size() > 0)
					this.notesAndComments = orderDetail.getNTEAll().stream()
							.map(nte -> Hl7NotesAndCommentsSegment.isPresent(nte) ? new Hl7NotesAndCommentsSegment(nte) : null)
							.filter(notesAndComments -> notesAndComments != null)
							.collect(Collectors.toList());

				if (Hl7ContactDataSegment.isPresent(orderDetail.getCTD()))
					this.contactData = new Hl7ContactDataSegment(orderDetail.getCTD());

				if (Hl7DiagnosisSegment.isPresent(orderDetail.getDG1()))
					this.diagnosis = new Hl7DiagnosisSegment(orderDetail.getDG1());

				if (orderDetail.getOBSERVATIONAll() != null && orderDetail.getOBSERVATIONAll().size() > 0)
					this.observation = orderDetail.getOBSERVATIONAll().stream()
							.map(ormObservation -> Hl7ObservationSection.isPresent(ormObservation) ? new Hl7ObservationSection(ormObservation) : null)
							.filter(observation -> observation != null)
							.collect(Collectors.toList());
			}
		} catch (HL7Exception e) {
			throw new UncheckedHl7ParsingException(e);
		}
	}

	@Nullable
	public Hl7OrderDetailSegmentSection getOrderDetailSegment() {
		return this.orderDetailSegment;
	}

	public void setOrderDetailSegment(@Nullable Hl7OrderDetailSegmentSection orderDetailSegment) {
		this.orderDetailSegment = orderDetailSegment;
	}

	@Nullable
	public List<Hl7NotesAndCommentsSegment> getNotesAndComments() {
		return this.notesAndComments;
	}

	public void setNotesAndComments(@Nullable List<Hl7NotesAndCommentsSegment> notesAndComments) {
		this.notesAndComments = notesAndComments;
	}

	@Nullable
	public Hl7ContactDataSegment getContactData() {
		return this.contactData;
	}

	public void setContactData(@Nullable Hl7ContactDataSegment contactData) {
		this.contactData = contactData;
	}

	@Nullable
	public Hl7DiagnosisSegment getDiagnosis() {
		return this.diagnosis;
	}

	public void setDiagnosis(@Nullable Hl7DiagnosisSegment diagnosis) {
		this.diagnosis = diagnosis;
	}

	@Nullable
	public List<Hl7ObservationSection> getObservation() {
		return this.observation;
	}

	public void setObservation(@Nullable List<Hl7ObservationSection> observation) {
		this.observation = observation;
	}
}