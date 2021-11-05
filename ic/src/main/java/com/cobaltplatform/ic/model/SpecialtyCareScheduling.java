package com.cobaltplatform.ic.model;

import com.cobaltplatform.ic.backend.model.db.DSpecialtyCareScheduling;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * @author Transmogrify, LLC.
 */
public class SpecialtyCareScheduling {
	private String agency;
	private LocalDate date;
	private LocalTime time;
	private boolean attendanceConfirmed;
	private String notes;

	public static SpecialtyCareScheduling fromModel(DSpecialtyCareScheduling model) {
		if(model == null)
			return null;

		SpecialtyCareScheduling specialtyCareScheduling = new SpecialtyCareScheduling();
		specialtyCareScheduling.setAgency(model.getAgency());
		specialtyCareScheduling.setAttendanceConfirmed(model.isAttendanceConfirmed());
		specialtyCareScheduling.setDate(model.getDate());
		specialtyCareScheduling.setTime(model.getTime());
		specialtyCareScheduling.setNotes(model.getNotes());

		return specialtyCareScheduling;
	}

	public String getAgency() {
		return agency;
	}

	public void setAgency(String agency) {
		this.agency = agency;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalTime getTime() {
		return time;
	}

	public void setTime(LocalTime time) {
		this.time = time;
	}

	public boolean isAttendanceConfirmed() {
		return attendanceConfirmed;
	}

	public void setAttendanceConfirmed(boolean attendanceConfirmed) {
		this.attendanceConfirmed = attendanceConfirmed;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
