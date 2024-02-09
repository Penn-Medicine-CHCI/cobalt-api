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

package com.cobaltplatform.api.integration.hl7.model;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/ORC
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7CommonOrder extends Hl7Object {
	@Nullable
	private String orderControl; // ORC.1 - Order Control
	@Nullable
	private String placerOrderNumber; // ORC.2 - Placer Order Number
	@Nullable
	private String fillerOrderNumber; // ORC.3 - Filler Order Number
	@Nullable
	private String placerGroupNumber; // ORC.4 - Placer Group Number
	@Nullable
	private String orderStatus; // ORC.5 - Order Status
	@Nullable
	private String responseFlag; // ORC.6 - Response Flag
	@Nullable
	private String quantityTiming; // ORC.7 - Quantity/Timing
	@Nullable
	private String parentOrder; // ORC.8 - Parent Order
	@Nullable
	private String dateTimeOfTransaction; // ORC.9 - Date/Time of Transaction
	@Nullable
	private String enteredBy; // ORC.10 - Entered By
	@Nullable
	private String verifiedBy; // ORC.11 - Verified By
	@Nullable
	private String orderingProvider; // ORC.12 - Ordering Provider
	@Nullable
	private String enterersLocation; // ORC.13 - Enterer's Location
	@Nullable
	private String callBackPhoneNumber; // ORC.14 - Call Back Phone Number
	@Nullable
	private String orderEffectiveDateTime; // ORC.15 - Order Effective Date/Time
	@Nullable
	private String orderControlCodeReason; // ORC.16 - Order Control Code Reason
	@Nullable
	private String enteringOrganization; // ORC.17 - Entering Organization
	@Nullable
	private String enteringDevice; // ORC.18 - Entering Device
	@Nullable
	private String actionBy; // ORC.19 - Action By

	@Nullable
	public String getOrderControl() {
		return this.orderControl;
	}

	public void setOrderControl(@Nullable String orderControl) {
		this.orderControl = orderControl;
	}

	@Nullable
	public String getPlacerOrderNumber() {
		return this.placerOrderNumber;
	}

	public void setPlacerOrderNumber(@Nullable String placerOrderNumber) {
		this.placerOrderNumber = placerOrderNumber;
	}

	@Nullable
	public String getFillerOrderNumber() {
		return this.fillerOrderNumber;
	}

	public void setFillerOrderNumber(@Nullable String fillerOrderNumber) {
		this.fillerOrderNumber = fillerOrderNumber;
	}

	@Nullable
	public String getPlacerGroupNumber() {
		return this.placerGroupNumber;
	}

	public void setPlacerGroupNumber(@Nullable String placerGroupNumber) {
		this.placerGroupNumber = placerGroupNumber;
	}

	@Nullable
	public String getOrderStatus() {
		return this.orderStatus;
	}

	public void setOrderStatus(@Nullable String orderStatus) {
		this.orderStatus = orderStatus;
	}

	@Nullable
	public String getResponseFlag() {
		return this.responseFlag;
	}

	public void setResponseFlag(@Nullable String responseFlag) {
		this.responseFlag = responseFlag;
	}

	@Nullable
	public String getQuantityTiming() {
		return this.quantityTiming;
	}

	public void setQuantityTiming(@Nullable String quantityTiming) {
		this.quantityTiming = quantityTiming;
	}

	@Nullable
	public String getParentOrder() {
		return this.parentOrder;
	}

	public void setParentOrder(@Nullable String parentOrder) {
		this.parentOrder = parentOrder;
	}

	@Nullable
	public String getDateTimeOfTransaction() {
		return this.dateTimeOfTransaction;
	}

	public void setDateTimeOfTransaction(@Nullable String dateTimeOfTransaction) {
		this.dateTimeOfTransaction = dateTimeOfTransaction;
	}

	@Nullable
	public String getEnteredBy() {
		return this.enteredBy;
	}

	public void setEnteredBy(@Nullable String enteredBy) {
		this.enteredBy = enteredBy;
	}

	@Nullable
	public String getVerifiedBy() {
		return this.verifiedBy;
	}

	public void setVerifiedBy(@Nullable String verifiedBy) {
		this.verifiedBy = verifiedBy;
	}

	@Nullable
	public String getOrderingProvider() {
		return this.orderingProvider;
	}

	public void setOrderingProvider(@Nullable String orderingProvider) {
		this.orderingProvider = orderingProvider;
	}

	@Nullable
	public String getEnterersLocation() {
		return this.enterersLocation;
	}

	public void setEnterersLocation(@Nullable String enterersLocation) {
		this.enterersLocation = enterersLocation;
	}

	@Nullable
	public String getCallBackPhoneNumber() {
		return this.callBackPhoneNumber;
	}

	public void setCallBackPhoneNumber(@Nullable String callBackPhoneNumber) {
		this.callBackPhoneNumber = callBackPhoneNumber;
	}

	@Nullable
	public String getOrderEffectiveDateTime() {
		return this.orderEffectiveDateTime;
	}

	public void setOrderEffectiveDateTime(@Nullable String orderEffectiveDateTime) {
		this.orderEffectiveDateTime = orderEffectiveDateTime;
	}

	@Nullable
	public String getOrderControlCodeReason() {
		return this.orderControlCodeReason;
	}

	public void setOrderControlCodeReason(@Nullable String orderControlCodeReason) {
		this.orderControlCodeReason = orderControlCodeReason;
	}

	@Nullable
	public String getEnteringOrganization() {
		return this.enteringOrganization;
	}

	public void setEnteringOrganization(@Nullable String enteringOrganization) {
		this.enteringOrganization = enteringOrganization;
	}

	@Nullable
	public String getEnteringDevice() {
		return this.enteringDevice;
	}

	public void setEnteringDevice(@Nullable String enteringDevice) {
		this.enteringDevice = enteringDevice;
	}

	@Nullable
	public String getActionBy() {
		return this.actionBy;
	}

	public void setActionBy(@Nullable String actionBy) {
		this.actionBy = actionBy;
	}
}