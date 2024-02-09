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

import ca.uhn.hl7v2.model.v251.datatype.CQ;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/CQ
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7CompositeQuantity extends Hl7Object {
	@Nullable
	private Double quantity;
	@Nullable
	private Hl7CodedElement units;

	@Nonnull
	public static Boolean isPresent(@Nullable CQ cq) {
		if (cq == null)
			return false;

		return trimToNull(cq.getQuantity().getValue()) != null
				|| Hl7CodedElement.isPresent(cq.getUnits());
	}

	public Hl7CompositeQuantity() {
		// Nothing to do
	}

	public Hl7CompositeQuantity(@Nullable CQ cq) {
		if (cq != null) {
			String quantityAsString = trimToNull(cq.getQuantity().getValue());
			this.quantity = quantityAsString == null ? null : Double.parseDouble(quantityAsString);
			this.units = Hl7CodedElement.isPresent(cq.getUnits()) ? new Hl7CodedElement(cq.getUnits()) : null;
		}
	}

	@Nullable
	public Double getQuantity() {
		return this.quantity;
	}

	public void setQuantity(@Nullable Double quantity) {
		this.quantity = quantity;
	}

	@Nullable
	public Hl7CodedElement getUnits() {
		return this.units;
	}

	public void setUnits(@Nullable Hl7CodedElement units) {
		this.units = units;
	}
}