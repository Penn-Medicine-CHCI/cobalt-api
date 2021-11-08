package com.cobaltplatform.ic.backend.model.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class DispositionNoteCreateRequest {
	@Nullable
	private UUID dispositionId;
	@Nullable
	private String note;
	@Nullable
	private UUID accountId;

	@Nullable
	public UUID getDispositionId() {
		return dispositionId;
	}

	public void setDispositionId(@Nullable UUID dispositionId) {
		this.dispositionId = dispositionId;
	}

	@Nullable
	public String getNote() {
		return note;
	}

	public void setNote(@Nullable String note) {
		this.note = note;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}
}
