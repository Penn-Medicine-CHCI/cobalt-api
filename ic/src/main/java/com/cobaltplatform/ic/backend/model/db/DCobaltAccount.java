package com.cobaltplatform.ic.backend.model.db;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cobalt_account", schema = "ic")
public class DCobaltAccount extends Model {
	@Id
	private UUID accountId;
	@Column(unique = true)
	private UUID providerId;
	@Column(nullable = false)
	private String roleId;
	@Column
	private String firstName;
	@Column
	private String lastName;
	@Column
	private String displayName;
	@Column
	private String emailAddress;
	@OneToMany(mappedBy = "cobaltAccount")
	private List<DDispositionNote> dispositionNotes;
	@WhenCreated
	private Instant createdDt;
	@WhenModified
	private Instant updatedDt;

	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(UUID accountId) {
		this.accountId = accountId;
	}

	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(UUID providerId) {
		this.providerId = providerId;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public List<DDispositionNote> getDispositionNotes() {
		return dispositionNotes;
	}

	public void setDispositionNotes(List<DDispositionNote> dispositionNotes) {
		this.dispositionNotes = dispositionNotes;
	}

	public Instant getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(Instant createdDt) {
		this.createdDt = createdDt;
	}

	public Instant getUpdatedDt() {
		return updatedDt;
	}

	public void setUpdatedDt(Instant updatedDt) {
		this.updatedDt = updatedDt;
	}
}
