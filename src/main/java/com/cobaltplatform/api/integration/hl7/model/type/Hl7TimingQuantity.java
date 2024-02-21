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

package com.cobaltplatform.api.integration.hl7.model.type;

import ca.uhn.hl7v2.model.v251.datatype.TQ;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/TQ
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7TimingQuantity extends Hl7Object {
	@Nullable
	private Hl7CompositeQuantityWithUnits quantity; // ORC.7.1 - Quantity
	@Nullable
	private Hl7RepeatInterval interval; // ORC.7.2 - Interval
	@Nullable
	private String duration; // ORC.7.3 - Duration
	@Nullable
	private Hl7TimeStamp startDateTime; // ORC.7.4 - Start Date/Time
	@Nullable
	private Hl7TimeStamp endDateTime; // ORC.7.5 - End Date/Time
	@Nullable
	private String priority; // ORC.7.6 - Priority
	@Nullable
	private String condition; // ORC.7.7 - Condition
	@Nullable
	private String text; // ORC.7.8 - Text
	@Nullable
	private String conjunction; // ORC.7.9 - Conjunction
	@Nullable
	private Hl7OrderSequenceDefinition orderSequencing; // ORC.7.10 - Order Sequencing
	@Nullable
	private Hl7CodedElement occurrenceDuration; // ORC.7.11 - Occurrence Duration
	@Nullable
	private Double totalOccurrences; // ORC.7.12 - Total Occurrences

	@Nonnull
	public static Boolean isPresent(@Nullable TQ tq) {
		if (tq == null)
			return false;

		return Hl7CompositeQuantityWithUnits.isPresent(tq.getQuantity())
				|| Hl7RepeatInterval.isPresent(tq.getInterval())
				|| trimToNull(tq.getDuration().getValueOrEmpty()) != null
				|| Hl7TimeStamp.isPresent(tq.getStartDateTime())
				|| Hl7TimeStamp.isPresent(tq.getEndDateTime())
				|| trimToNull(tq.getPriority().getValueOrEmpty()) != null
				|| trimToNull(tq.getCondition().getValueOrEmpty()) != null
				|| trimToNull(tq.getText().getValueOrEmpty()) != null
				|| trimToNull(tq.getConjunction().getValueOrEmpty()) != null
				|| Hl7OrderSequenceDefinition.isPresent(tq.getOrderSequencing())
				|| Hl7CodedElement.isPresent(tq.getOccurrenceDuration())
				|| trimToNull(tq.getTotalOccurrences().getValue()) != null;
	}

	public Hl7TimingQuantity() {
		// Nothing to do
	}

	public Hl7TimingQuantity(@Nullable TQ tq) {
		if (tq != null) {
			this.quantity = Hl7CompositeQuantityWithUnits.isPresent(tq.getQuantity()) ? new Hl7CompositeQuantityWithUnits(tq.getQuantity()) : null;
			this.interval = Hl7RepeatInterval.isPresent(tq.getInterval()) ? new Hl7RepeatInterval(tq.getInterval()) : null;
			this.duration = trimToNull(tq.getDuration().getValueOrEmpty());
			this.startDateTime = Hl7TimeStamp.isPresent(tq.getStartDateTime()) ? new Hl7TimeStamp(tq.getStartDateTime()) : null;
			this.endDateTime = Hl7TimeStamp.isPresent(tq.getEndDateTime()) ? new Hl7TimeStamp(tq.getEndDateTime()) : null;
			this.duration = trimToNull(tq.getPriority().getValueOrEmpty());
			this.condition = trimToNull(tq.getCondition().getValueOrEmpty());
			this.text = trimToNull(tq.getText().getValueOrEmpty());
			this.conjunction = trimToNull(tq.getConjunction().getValueOrEmpty());
			this.orderSequencing = Hl7OrderSequenceDefinition.isPresent(tq.getOrderSequencing()) ? new Hl7OrderSequenceDefinition(tq.getOrderSequencing()) : null;
			this.occurrenceDuration = Hl7CodedElement.isPresent(tq.getOccurrenceDuration()) ? new Hl7CodedElement(tq.getTq11_OccurrenceDuration()) : null;

			String totalOccurrencesAsString = trimToNull(tq.getTotalOccurrences().getValue());

			if (totalOccurrencesAsString != null)
				this.totalOccurrences = Double.parseDouble(totalOccurrencesAsString);
		}
	}

	@Nullable
	public Hl7CompositeQuantityWithUnits getQuantity() {
		return this.quantity;
	}

	public void setQuantity(@Nullable Hl7CompositeQuantityWithUnits quantity) {
		this.quantity = quantity;
	}

	@Nullable
	public Hl7RepeatInterval getInterval() {
		return this.interval;
	}

	public void setInterval(@Nullable Hl7RepeatInterval interval) {
		this.interval = interval;
	}

	@Nullable
	public String getDuration() {
		return this.duration;
	}

	public void setDuration(@Nullable String duration) {
		this.duration = duration;
	}

	@Nullable
	public Hl7TimeStamp getStartDateTime() {
		return this.startDateTime;
	}

	public void setStartDateTime(@Nullable Hl7TimeStamp startDateTime) {
		this.startDateTime = startDateTime;
	}

	@Nullable
	public Hl7TimeStamp getEndDateTime() {
		return this.endDateTime;
	}

	public void setEndDateTime(@Nullable Hl7TimeStamp endDateTime) {
		this.endDateTime = endDateTime;
	}

	@Nullable
	public String getPriority() {
		return this.priority;
	}

	public void setPriority(@Nullable String priority) {
		this.priority = priority;
	}

	@Nullable
	public String getCondition() {
		return this.condition;
	}

	public void setCondition(@Nullable String condition) {
		this.condition = condition;
	}

	@Nullable
	public String getText() {
		return this.text;
	}

	public void setText(@Nullable String text) {
		this.text = text;
	}

	@Nullable
	public String getConjunction() {
		return this.conjunction;
	}

	public void setConjunction(@Nullable String conjunction) {
		this.conjunction = conjunction;
	}

	@Nullable
	public Hl7CodedElement getOccurrenceDuration() {
		return this.occurrenceDuration;
	}

	public void setOccurrenceDuration(@Nullable Hl7CodedElement occurrenceDuration) {
		this.occurrenceDuration = occurrenceDuration;
	}

	@Nullable
	public Double getTotalOccurrences() {
		return this.totalOccurrences;
	}

	public void setTotalOccurrences(@Nullable Double totalOccurrences) {
		this.totalOccurrences = totalOccurrences;
	}
}