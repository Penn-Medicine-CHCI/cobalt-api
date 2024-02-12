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

import ca.uhn.hl7v2.model.v251.datatype.CP;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigDecimal;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/CP
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7CompositePrice extends Hl7Object {
	@Nullable
	private Hl7Money price; // CP.1 - Price
	@Nullable
	private String priceType; // CP.2 - Price Type
	@Nullable
	private BigDecimal fromValue; // CP.3 - From Value
	@Nullable
	private BigDecimal toValue; // CP.4 - To Value
	@Nullable
	private Hl7CodedElement rangeUnits; // CP.5 - Range Units
	@Nullable
	private String rangeType; // CP.6 - Range Type

	@Nonnull
	public static Boolean isPresent(@Nullable CP cp) {
		if (cp == null)
			return false;

		return Hl7Money.isPresent(cp.getPrice());
	}

	public Hl7CompositePrice() {
		// Nothing to do
	}

	public Hl7CompositePrice(@Nullable CP cp) {
		if (cp != null) {
			if (Hl7Money.isPresent(cp.getPrice()))
				this.price = new Hl7Money(cp.getPrice());

			this.priceType = trimToNull(cp.getPriceType().getValueOrEmpty());

			String fromValueAsString = trimToNull(cp.getFromValue().getValue());
			if (fromValueAsString != null)
				this.fromValue = new BigDecimal(fromValueAsString);

			String toValueAsString = trimToNull(cp.getToValue().getValue());
			if (toValueAsString != null)
				this.toValue = new BigDecimal(toValueAsString);

			if (Hl7CodedElement.isPresent(cp.getRangeUnits()))
				this.rangeUnits = new Hl7CodedElement(cp.getRangeUnits());

			this.rangeType = trimToNull(cp.getRangeType().getValueOrEmpty());
		}
	}

	@Nullable
	public Hl7Money getPrice() {
		return this.price;
	}

	public void setPrice(@Nullable Hl7Money price) {
		this.price = price;
	}

	@Nullable
	public String getPriceType() {
		return this.priceType;
	}

	public void setPriceType(@Nullable String priceType) {
		this.priceType = priceType;
	}

	@Nullable
	public BigDecimal getFromValue() {
		return this.fromValue;
	}

	public void setFromValue(@Nullable BigDecimal fromValue) {
		this.fromValue = fromValue;
	}

	@Nullable
	public BigDecimal getToValue() {
		return this.toValue;
	}

	public void setToValue(@Nullable BigDecimal toValue) {
		this.toValue = toValue;
	}

	@Nullable
	public Hl7CodedElement getRangeUnits() {
		return this.rangeUnits;
	}

	public void setRangeUnits(@Nullable Hl7CodedElement rangeUnits) {
		this.rangeUnits = rangeUnits;
	}

	@Nullable
	public String getRangeType() {
		return this.rangeType;
	}

	public void setRangeType(@Nullable String rangeType) {
		this.rangeType = rangeType;
	}
}