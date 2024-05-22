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
	private boolean canServiceIcOrders;
	private boolean canEditIcTriages;
	private boolean canViewIcReports;
	private boolean canEditIcSafetyPlanning;
	private boolean canImportIcPatientOrders;
	private boolean canAdministerIcDepartments;
	private boolean canAdministerGroupSessions;
	private boolean canAdministerContent;
	private boolean canViewAnalytics;
	private boolean canViewProviderReports;
	private boolean canViewProviderReportUnusedAvailability;
	private boolean canViewProviderReportAppointmentCancelations;
	private boolean canViewProviderReportAppointments;
	private boolean canViewProviderReportAppointmentsEap;

	public boolean isCanServiceIcOrders() {
		return this.canServiceIcOrders;
	}

	public void setCanServiceIcOrders(boolean canServiceIcOrders) {
		this.canServiceIcOrders = canServiceIcOrders;
	}

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

	public boolean isCanAdministerIcDepartments() {
		return this.canAdministerIcDepartments;
	}

	public void setCanAdministerIcDepartments(boolean canAdministerIcDepartments) {
		this.canAdministerIcDepartments = canAdministerIcDepartments;
	}

	public boolean isCanAdministerGroupSessions() {
		return this.canAdministerGroupSessions;
	}

	public void setCanAdministerGroupSessions(boolean canAdministerGroupSessions) {
		this.canAdministerGroupSessions = canAdministerGroupSessions;
	}

	public boolean isCanAdministerContent() {
		return this.canAdministerContent;
	}

	public void setCanAdministerContent(boolean canAdministerContent) {
		this.canAdministerContent = canAdministerContent;
	}

	public boolean isCanViewProviderReports() {
		return this.canViewProviderReports;
	}

	public void setCanViewProviderReports(boolean canViewProviderReports) {
		this.canViewProviderReports = canViewProviderReports;
	}

	public boolean isCanViewAnalytics() {
		return this.canViewAnalytics;
	}

	public void setCanViewAnalytics(boolean canViewAnalytics) {
		this.canViewAnalytics = canViewAnalytics;
	}

	public boolean isCanViewProviderReportUnusedAvailability() {
		return this.canViewProviderReportUnusedAvailability;
	}

	public void setCanViewProviderReportUnusedAvailability(boolean canViewProviderReportUnusedAvailability) {
		this.canViewProviderReportUnusedAvailability = canViewProviderReportUnusedAvailability;
	}

	public boolean isCanViewProviderReportAppointmentCancelations() {
		return this.canViewProviderReportAppointmentCancelations;
	}

	public void setCanViewProviderReportAppointmentCancelations(boolean canViewProviderReportAppointmentCancelations) {
		this.canViewProviderReportAppointmentCancelations = canViewProviderReportAppointmentCancelations;
	}

	public boolean isCanViewProviderReportAppointments() {
		return this.canViewProviderReportAppointments;
	}

	public void setCanViewProviderReportAppointments(boolean canViewProviderReportAppointments) {
		this.canViewProviderReportAppointments = canViewProviderReportAppointments;
	}

	public boolean isCanViewProviderReportAppointmentsEap() {
		return this.canViewProviderReportAppointmentsEap;
	}

	public void setCanViewProviderReportAppointmentsEap(boolean canViewProviderReportAppointmentsEap) {
		this.canViewProviderReportAppointmentsEap = canViewProviderReportAppointmentsEap;
	}
}
