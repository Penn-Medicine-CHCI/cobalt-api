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

import ca.uhn.hl7v2.model.v251.datatype.MOC;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/MOC
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7MoneyAndCode extends Hl7Object {
	@Nullable
	private Hl7Money monetaryAmount; // MOC.1 - Monetary Amount
	@Nullable
	private Hl7CodedElement chargeCode; // MOC.2 - Charge Code

	@Nonnull
	public static Boolean isPresent(@Nullable MOC moc) {
		if (moc == null)
			return false;

		return Hl7Money.isPresent(moc.getMonetaryAmount())
				|| Hl7CodedElement.isPresent(moc.getChargeCode());
	}

	public Hl7MoneyAndCode() {
		// Nothing to do
	}

	public Hl7MoneyAndCode(@Nullable MOC moc) {
		if (moc != null) {
			if (Hl7Money.isPresent(moc.getMonetaryAmount()))
				this.monetaryAmount = new Hl7Money(moc.getMonetaryAmount());

			if (Hl7CodedElement.isPresent(moc.getChargeCode()))
				this.chargeCode = new Hl7CodedElement(moc.getChargeCode());
		}
	}

	@Nullable
	public Hl7Money getMonetaryAmount() {
		return this.monetaryAmount;
	}

	public void setMonetaryAmount(@Nullable Hl7Money monetaryAmount) {
		this.monetaryAmount = monetaryAmount;
	}

	@Nullable
	public Hl7CodedElement getChargeCode() {
		return this.chargeCode;
	}

	public void setChargeCode(@Nullable Hl7CodedElement chargeCode) {
		this.chargeCode = chargeCode;
	}
}