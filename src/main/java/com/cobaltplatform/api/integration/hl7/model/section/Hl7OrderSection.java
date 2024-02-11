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

import ca.uhn.hl7v2.model.v251.group.ORM_O01_ORDER;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7BillingSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7ClinicalTrialIdentificationSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7CommonOrderSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7FinancialTransactionSegment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/TriggerEvents/ORM_O01
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7OrderSection extends Hl7Object {
	@Nullable
	private Hl7CommonOrderSegment commonOrder;
	@Nullable
	private Hl7OrderDetailSection orderDetail;
	@Nullable
	private List<Hl7FinancialTransactionSegment> financialTransaction;
	@Nullable
	private List<Hl7ClinicalTrialIdentificationSegment> clinicalTrialIdentification;
	@Nullable
	private Hl7BillingSegment billing;

	@Nonnull
	public static Boolean isPresent(@Nullable ORM_O01_ORDER order) {
		if(order == null)
			return false;

		return Hl7CommonOrderSegment.isPresent(order.getORC())
				;
	}

	@Nullable
	public Hl7CommonOrderSegment getCommonOrder() {
		return this.commonOrder;
	}

	public void setCommonOrder(@Nullable Hl7CommonOrderSegment commonOrder) {
		this.commonOrder = commonOrder;
	}

	@Nullable
	public Hl7OrderDetailSection getOrderDetail() {
		return this.orderDetail;
	}

	public void setOrderDetail(@Nullable Hl7OrderDetailSection orderDetail) {
		this.orderDetail = orderDetail;
	}

	@Nullable
	public List<Hl7FinancialTransactionSegment> getFinancialTransaction() {
		return this.financialTransaction;
	}

	public void setFinancialTransaction(@Nullable List<Hl7FinancialTransactionSegment> financialTransaction) {
		this.financialTransaction = financialTransaction;
	}

	@Nullable
	public List<Hl7ClinicalTrialIdentificationSegment> getClinicalTrialIdentification() {
		return this.clinicalTrialIdentification;
	}

	public void setClinicalTrialIdentification(@Nullable List<Hl7ClinicalTrialIdentificationSegment> clinicalTrialIdentification) {
		this.clinicalTrialIdentification = clinicalTrialIdentification;
	}

	@Nullable
	public Hl7BillingSegment getBilling() {
		return this.billing;
	}

	public void setBilling(@Nullable Hl7BillingSegment billing) {
		this.billing = billing;
	}
}