package com.cobaltplatform.ic.model;

import com.cobaltplatform.ic.backend.model.db.DCobaltAccount;
import com.cobaltplatform.ic.backend.model.db.DDispositionNote;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class DispositionNote {
	@Nonnull
	private static final DateTimeFormatter DATE_TIME_FORMATTER;

	@Nullable
	private UUID dispositionNoteId;
	@Nullable
	private UUID accountId;
	@Nullable
	private String authorDescription;
	@Nullable
	private String note;
	@Nullable
	private Instant createdDt;
	@Nullable
	private String createdDtDescription;

	static {
		// TODO: This is a hack until we merge IC into employee Cobalt.  We hardcode user's locale and timezone...
		DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
				.withZone(ZoneId.of("America/New_York"))
				.withLocale(Locale.US);
	}

	@Nonnull
	public static Optional<DispositionNote> from(@Nullable DDispositionNote dispositionNote) {
		if (dispositionNote == null)
			return Optional.empty();

		DCobaltAccount cobaltAccount = dispositionNote.getCobaltAccount();

		DispositionNote newDispositionNote = new DispositionNote();
		newDispositionNote.setDispositionNoteId(dispositionNote.getDispositionNoteId());
		newDispositionNote.setAccountId(cobaltAccount.getAccountId());
		newDispositionNote.setAuthorDescription(cobaltAccount.getDisplayName());
		newDispositionNote.setNote(dispositionNote.getNote());
		newDispositionNote.setCreatedDt(cobaltAccount.getCreatedDt());
		newDispositionNote.setCreatedDtDescription(getDateTimeFormatter().format(newDispositionNote.getCreatedDt()));

		return Optional.of(newDispositionNote);
	}

	@Nonnull
	private static DateTimeFormatter getDateTimeFormatter() {
		return DATE_TIME_FORMATTER;
	}

	@Nullable
	public UUID getDispositionNoteId() {
		return dispositionNoteId;
	}

	public void setDispositionNoteId(@Nullable UUID dispositionNoteId) {
		this.dispositionNoteId = dispositionNoteId;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getAuthorDescription() {
		return authorDescription;
	}

	public void setAuthorDescription(@Nullable String authorDescription) {
		this.authorDescription = authorDescription;
	}

	@Nullable
	public String getNote() {
		return note;
	}

	public void setNote(@Nullable String note) {
		this.note = note;
	}

	@Nullable
	public Instant getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(@Nullable Instant createdDt) {
		this.createdDt = createdDt;
	}

	@Nullable
	public String getCreatedDtDescription() {
		return createdDtDescription;
	}

	public void setCreatedDtDescription(@Nullable String createdDtDescription) {
		this.createdDtDescription = createdDtDescription;
	}
}
