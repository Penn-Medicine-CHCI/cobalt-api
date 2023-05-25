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

package com.cobaltplatform.api.model.service;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AccountCapabilityFlags {
	private boolean canEditIcTriages;
	private boolean canViewIcReports;
	private boolean canEditIcSafetyPlanning;
	private boolean canImportIcPatientOrders;

	public boolean isCanEditIcTriages() {
		return this.canEditIcTriages;
	}

	public void setCanEditIcTriages(boolean canEditIcTriages) {
		this.canEditIcTriages = canEditIcTriages;
	}

	public boolean isCanViewIcReports() {
		return this.canViewIcReports;
	}

	public void setCanViewIcReports(boolean canViewIcReports) {
		this.canViewIcReports = canViewIcReports;
	}

	public boolean isCanEditIcSafetyPlanning() {
		return this.canEditIcSafetyPlanning;
	}

	public void setCanEditIcSafetyPlanning(boolean canEditIcSafetyPlanning) {
		this.canEditIcSafetyPlanning = canEditIcSafetyPlanning;
	}

	public boolean isCanImportIcPatientOrders() {
		return this.canImportIcPatientOrders;
	}

	public void setCanImportIcPatientOrders(boolean canImportIcPatientOrders) {
		this.canImportIcPatientOrders = canImportIcPatientOrders;
	}
}
