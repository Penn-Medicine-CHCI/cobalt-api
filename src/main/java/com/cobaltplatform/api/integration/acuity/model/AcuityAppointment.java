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

package com.cobaltplatform.api.integration.acuity.model;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AcuityAppointment {
	@Nullable
	private Long id;
	@Nullable
	@SerializedName("appointmentTypeID")
	private Long appointmentTypeId;
	@Nullable
	@SerializedName("classID")
	private Long classId;
	@Nullable
	@SerializedName("calendarID")
	private Long calendarId;
	@Nullable
	@SerializedName("addonIDs")
	private List<String> addonIds;
	@Nullable
	private String firstName;
	@Nullable
	private String lastName;
	@Nullable
	private String phone;
	@Nullable
	private String email;
	@Nullable
	private String date; // e.g. "April 21, 2020"
	@Nullable
	private String time; // e.g. "1:00pm"
	@Nullable
	private String endTime; // e.g. "2:00pm"
	@Nullable
	private String dateCreated; // e.g. "April 6, 2020"
	@Nullable
	private String datetimeCreated; // e.g. "2020-04-06T20:24:52-0500"
	@Nullable
	private String datetime; // e.g. "2020-04-21T13:00:00-0400"
	@Nullable
	private String price;
	@Nullable
	private String priceSold;
	@Nullable
	private String paid;
	@Nullable
	private String amountPaid;
	@Nullable
	private String type;
	@Nullable
	private String category;
	@Nullable
	private String duration;
	@Nullable
	private String calendar;
	@Nullable
	private String certificate;
	@Nullable
	private String confirmationPage;
	@Nullable
	private String location;
	@Nullable
	private String notes;
	@Nullable
	private String timezone;
	@Nullable
	private String calendarTimezone;
	@Nullable
	private Boolean canceled;
	@Nullable
	private Boolean canClientCancel;
	@Nullable
	private Boolean canClientReschedule;
	@Nullable
	private List<AcuityAppointmentForm> forms;
	@Nullable
	private String formsText;
	@Nullable
	private Boolean isVerified;
	@Nullable
	private String scheduledBy;

	@Nullable
	public Long getId() {
		return id;
	}

	public void setId(@Nullable Long id) {
		this.id = id;
	}

	@Nullable
	public Long getAppointmentTypeId() {
		return appointmentTypeId;
	}

	public void setAppointmentTypeId(@Nullable Long appointmentTypeId) {
		this.appointmentTypeId = appointmentTypeId;
	}

	@Nullable
	public Long getClassId() {
		return classId;
	}

	public void setClassId(@Nullable Long classId) {
		this.classId = classId;
	}

	@Nullable
	public Long getCalendarId() {
		return calendarId;
	}

	public void setCalendarId(@Nullable Long calendarId) {
		this.calendarId = calendarId;
	}

	@Nullable
	public List<String> getAddonIds() {
		return addonIds;
	}

	public void setAddonIds(@Nullable List<String> addonIds) {
		this.addonIds = addonIds;
	}

	@Nullable
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(@Nullable String firstName) {
		this.firstName = firstName;
	}

	@Nullable
	public String getLastName() {
		return lastName;
	}

	public void setLastName(@Nullable String lastName) {
		this.lastName = lastName;
	}

	@Nullable
	public String getPhone() {
		return phone;
	}

	public void setPhone(@Nullable String phone) {
		this.phone = phone;
	}

	@Nullable
	public String getEmail() {
		return email;
	}

	public void setEmail(@Nullable String email) {
		this.email = email;
	}

	@Nullable
	public String getDate() {
		return date;
	}

	public void setDate(@Nullable String date) {
		this.date = date;
	}

	@Nullable
	public String getTime() {
		return time;
	}

	public void setTime(@Nullable String time) {
		this.time = time;
	}

	@Nullable
	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(@Nullable String endTime) {
		this.endTime = endTime;
	}

	@Nullable
	public String getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(@Nullable String dateCreated) {
		this.dateCreated = dateCreated;
	}

	@Nullable
	public String getDatetimeCreated() {
		return datetimeCreated;
	}

	public void setDatetimeCreated(@Nullable String datetimeCreated) {
		this.datetimeCreated = datetimeCreated;
	}

	@Nullable
	public String getDatetime() {
		return datetime;
	}

	public void setDatetime(@Nullable String datetime) {
		this.datetime = datetime;
	}

	@Nullable
	public String getPrice() {
		return price;
	}

	public void setPrice(@Nullable String price) {
		this.price = price;
	}

	@Nullable
	public String getPriceSold() {
		return priceSold;
	}

	public void setPriceSold(@Nullable String priceSold) {
		this.priceSold = priceSold;
	}

	@Nullable
	public String getPaid() {
		return paid;
	}

	public void setPaid(@Nullable String paid) {
		this.paid = paid;
	}

	@Nullable
	public String getAmountPaid() {
		return amountPaid;
	}

	public void setAmountPaid(@Nullable String amountPaid) {
		this.amountPaid = amountPaid;
	}

	@Nullable
	public String getType() {
		return type;
	}

	public void setType(@Nullable String type) {
		this.type = type;
	}

	@Nullable
	public String getCategory() {
		return category;
	}

	public void setCategory(@Nullable String category) {
		this.category = category;
	}

	@Nullable
	public String getDuration() {
		return duration;
	}

	public void setDuration(@Nullable String duration) {
		this.duration = duration;
	}

	@Nullable
	public String getCalendar() {
		return calendar;
	}

	public void setCalendar(@Nullable String calendar) {
		this.calendar = calendar;
	}

	@Nullable
	public String getCertificate() {
		return certificate;
	}

	public void setCertificate(@Nullable String certificate) {
		this.certificate = certificate;
	}

	@Nullable
	public String getConfirmationPage() {
		return confirmationPage;
	}

	public void setConfirmationPage(@Nullable String confirmationPage) {
		this.confirmationPage = confirmationPage;
	}

	@Nullable
	public String getLocation() {
		return location;
	}

	public void setLocation(@Nullable String location) {
		this.location = location;
	}

	@Nullable
	public String getNotes() {
		return notes;
	}

	public void setNotes(@Nullable String notes) {
		this.notes = notes;
	}

	@Nullable
	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(@Nullable String timezone) {
		this.timezone = timezone;
	}

	@Nullable
	public String getCalendarTimezone() {
		return calendarTimezone;
	}

	public void setCalendarTimezone(@Nullable String calendarTimezone) {
		this.calendarTimezone = calendarTimezone;
	}

	@Nullable
	public Boolean getCanceled() {
		return canceled;
	}

	public void setCanceled(@Nullable Boolean canceled) {
		this.canceled = canceled;
	}

	@Nullable
	public Boolean getCanClientCancel() {
		return canClientCancel;
	}

	public void setCanClientCancel(@Nullable Boolean canClientCancel) {
		this.canClientCancel = canClientCancel;
	}

	@Nullable
	public Boolean getCanClientReschedule() {
		return canClientReschedule;
	}

	public void setCanClientReschedule(@Nullable Boolean canClientReschedule) {
		this.canClientReschedule = canClientReschedule;
	}

	@Nullable
	public List<AcuityAppointmentForm> getForms() {
		return forms;
	}

	public void setForms(@Nullable List<AcuityAppointmentForm> forms) {
		this.forms = forms;
	}

	@Nullable
	public String getFormsText() {
		return formsText;
	}

	public void setFormsText(@Nullable String formsText) {
		this.formsText = formsText;
	}

	@Nullable
	public Boolean getVerified() {
		return isVerified;
	}

	public void setVerified(@Nullable Boolean verified) {
		isVerified = verified;
	}

	@Nullable
	public String getScheduledBy() {
		return scheduledBy;
	}

	public void setScheduledBy(@Nullable String scheduledBy) {
		this.scheduledBy = scheduledBy;
	}
}
