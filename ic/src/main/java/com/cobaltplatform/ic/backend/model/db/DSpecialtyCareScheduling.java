package com.cobaltplatform.ic.backend.model.db;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "specialty_care_scheduling", schema = "ic")
public class DSpecialtyCareScheduling extends Model {
	@Id
	private UUID id;
	@WhenCreated
	private DateTime createdDt;
	@WhenModified
	private DateTime updatedDt;
	@OneToOne
	@Column(nullable = false)
	private DPatientDisposition disposition;
	@Column(nullable = false)
	private String agency;
	@Column(nullable = false)
	private LocalDate date;
	private LocalTime time;
	@Column(nullable = false)
	private boolean attendanceConfirmed;
	@Column(length = 10_000)
	private String notes;

	public UUID getId() {
		return id;
	}

	public DSpecialtyCareScheduling setId(final UUID id) {
		this.id = id;
		return this;
	}

	public DateTime getCreatedDt() {
		return createdDt;
	}

	public DSpecialtyCareScheduling setCreatedDt(final DateTime createdDt) {
		this.createdDt = createdDt;
		return this;
	}

	public DateTime getUpdatedDt() {
		return updatedDt;
	}

	public DSpecialtyCareScheduling setUpdatedDt(final DateTime updatedDt) {
		this.updatedDt = updatedDt;
		return this;
	}

	public DPatientDisposition getDisposition() {
		return disposition;
	}

	public void setDisposition(DPatientDisposition disposition) {
		this.disposition = disposition;
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


