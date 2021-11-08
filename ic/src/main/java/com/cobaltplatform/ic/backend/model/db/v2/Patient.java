package com.cobaltplatform.ic.backend.model.db.v2;

import org.postgresql.jdbc.PgArray;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Patient {
	@Nullable
	private UUID id;
	@Nullable
	private PgArray loggedInDt; // really character varying[], e.g. "{2021-05-07T16:29:54.579Z,2021-05-11T12:51:16.464Z,2021-05-14T13:35:25.946Z}"
	@Nullable
	private String fhirId;
	@Nullable
	private String fhirProvider;
	@Nullable
	private String uid;
	@Deprecated
	@Nullable
	private PgArray goals; // really character varying[], unused currently
	@Nullable
	private String preferredFirstName;
	@Nullable
	private String preferredLastName;
	@Nullable
	private String preferredEmail;
	@Nullable
	private String preferredPhoneNumber;
	@Nullable
	private String preferredGender;
	@Nullable
	private UUID cobaltAccountId;
	@Nullable
	private Boolean preferredEmailHasBeenUpdated;
	@Nullable
	private Boolean preferredPhoneHasBeenUpdated;
	@Nullable
	private Instant createdDt;
	@Nullable
	private Instant updatedDt;
	@Nullable
	private Boolean deleted;

	@Nullable
	public UUID getId() {
		return id;
	}

	public void setId(@Nullable UUID id) {
		this.id = id;
	}

	@Nullable
	public PgArray getLoggedInDt() {
		return loggedInDt;
	}

	public void setLoggedInDt(@Nullable PgArray loggedInDt) {
		this.loggedInDt = loggedInDt;
	}

	@Nullable
	public String getFhirId() {
		return fhirId;
	}

	public void setFhirId(@Nullable String fhirId) {
		this.fhirId = fhirId;
	}

	@Nullable
	public String getFhirProvider() {
		return fhirProvider;
	}

	public void setFhirProvider(@Nullable String fhirProvider) {
		this.fhirProvider = fhirProvider;
	}

	@Nullable
	public String getUid() {
		return uid;
	}

	public void setUid(@Nullable String uid) {
		this.uid = uid;
	}

	@Nullable
	@Deprecated
	public PgArray getGoals() {
		return goals;
	}

	@Deprecated
	public void setGoals(@Nullable PgArray goals) {
		this.goals = goals;
	}

	@Nullable
	public String getPreferredFirstName() {
		return preferredFirstName;
	}

	public void setPreferredFirstName(@Nullable String preferredFirstName) {
		this.preferredFirstName = preferredFirstName;
	}

	@Nullable
	public String getPreferredLastName() {
		return preferredLastName;
	}

	public void setPreferredLastName(@Nullable String preferredLastName) {
		this.preferredLastName = preferredLastName;
	}

	@Nullable
	public String getPreferredEmail() {
		return preferredEmail;
	}

	public void setPreferredEmail(@Nullable String preferredEmail) {
		this.preferredEmail = preferredEmail;
	}

	@Nullable
	public String getPreferredPhoneNumber() {
		return preferredPhoneNumber;
	}

	public void setPreferredPhoneNumber(@Nullable String preferredPhoneNumber) {
		this.preferredPhoneNumber = preferredPhoneNumber;
	}

	@Nullable
	public String getPreferredGender() {
		return preferredGender;
	}

	public void setPreferredGender(@Nullable String preferredGender) {
		this.preferredGender = preferredGender;
	}

	@Nullable
	public UUID getCobaltAccountId() {
		return cobaltAccountId;
	}

	public void setCobaltAccountId(@Nullable UUID cobaltAccountId) {
		this.cobaltAccountId = cobaltAccountId;
	}

	@Nullable
	public Boolean getPreferredEmailHasBeenUpdated() {
		return preferredEmailHasBeenUpdated;
	}

	public void setPreferredEmailHasBeenUpdated(@Nullable Boolean preferredEmailHasBeenUpdated) {
		this.preferredEmailHasBeenUpdated = preferredEmailHasBeenUpdated;
	}

	@Nullable
	public Boolean getPreferredPhoneHasBeenUpdated() {
		return preferredPhoneHasBeenUpdated;
	}

	public void setPreferredPhoneHasBeenUpdated(@Nullable Boolean preferredPhoneHasBeenUpdated) {
		this.preferredPhoneHasBeenUpdated = preferredPhoneHasBeenUpdated;
	}

	@Nullable
	public Instant getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(@Nullable Instant createdDt) {
		this.createdDt = createdDt;
	}

	@Nullable
	public Instant getUpdatedDt() {
		return updatedDt;
	}

	public void setUpdatedDt(@Nullable Instant updatedDt) {
		this.updatedDt = updatedDt;
	}

	@Nullable
	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(@Nullable Boolean deleted) {
		this.deleted = deleted;
	}
}
