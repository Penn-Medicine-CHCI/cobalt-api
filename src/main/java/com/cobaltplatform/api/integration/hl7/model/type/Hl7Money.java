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

import ca.uhn.hl7v2.model.v251.datatype.MO;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigDecimal;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/MO
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7Money extends Hl7Object {
	@Nullable
	private BigDecimal quantity; // MO.1 - Quantity
	@Nullable
	private String denomination; // MO.2 - Denomination

	@Nonnull
	public static Boolean isPresent(@Nullable MO mo) {
		if (mo == null)
			return false;

		return trimToNull(mo.getQuantity().getValue()) != null
				|| trimToNull(mo.getDenomination().getValueOrEmpty()) != null;
	}

	public Hl7Money() {
		// Nothing to do
	}

	public Hl7Money(@Nullable MO mo) {
		if (mo != null) {
			String quantityAsString = trimToNull(mo.getQuantity().getValue());
			if (quantityAsString != null)
				this.quantity = new BigDecimal(quantityAsString);

			this.denomination = trimToNull(mo.getDenomination().getValueOrEmpty());
		}
	}

	@Nullable
	public BigDecimal getQuantity() {
		return this.quantity;
	}

	public void setQuantity(@Nullable BigDecimal quantity) {
		this.quantity = quantity;
	}

	@Nullable
	public String getDenomination() {
		return this.denomination;
	}

	public void setDenomination(@Nullable String denomination) {
		this.denomination = denomination;
	}
}