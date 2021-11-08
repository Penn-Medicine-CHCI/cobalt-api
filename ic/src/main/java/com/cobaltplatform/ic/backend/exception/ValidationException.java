package com.cobaltplatform.ic.backend.exception;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ValidationException extends RuntimeException {
	@Nonnull
	private final List<String> globalErrors = new ArrayList<>();
	@Nonnull
	private final List<FieldError> fieldErrors = new ArrayList<>();
	@Nonnull
	private final Map<String, Object> metadata = new HashMap<>();

	public ValidationException() {
		super();
	}

	public ValidationException(@Nonnull String globalError) {
		this();
		requireNonNull(globalError);
		globalErrors.add(globalError);
	}

	public ValidationException(@Nonnull FieldError fieldError) {
		this();
		requireNonNull(fieldError);
		this.fieldErrors.add(fieldError);
	}

	public ValidationException(@Nonnull List<FieldError> fieldErrors) {
		this();
		requireNonNull(fieldErrors);
		this.fieldErrors.addAll(fieldErrors);
	}

	protected ValidationException(@Nonnull List<String> globalErrors, @Nonnull List<FieldError> fieldErrors) {
		this();
		requireNonNull(globalErrors);
		requireNonNull(fieldErrors);
		this.globalErrors.addAll(globalErrors);
		this.fieldErrors.addAll(fieldErrors);
	}

	@Override
	public String toString() {
		List<String> components = new ArrayList<>(3);

		if (globalErrors.size() > 0)
			components.add(format("globalErrors=%s", globalErrors));

		if (fieldErrors.size() > 0)
			components.add(format("fieldErrors=%s", fieldErrors));

		if (metadata.size() > 0)
			components.add(format("metadata=%s", metadata));

		return format("%s{%s}", getClass().getSimpleName(), components.stream().collect(Collectors.joining(", ")));
	}

	@Override
	public String getMessage() {
		List<String> components = new ArrayList<>(3);

		if (globalErrors.size() > 0)
			components.add(format("Global Errors: %s ", globalErrors));

		if (fieldErrors.size() > 0)
			components.add(format("Field Errors: %s", fieldErrors));

		if (metadata.size() > 0)
			components.add(format("Metadata: %s", metadata));

		return components.stream().collect(Collectors.joining(", "));
	}

	public void add(@Nonnull String globalError) {
		requireNonNull(globalError);

		if (!globalErrors.contains(globalError))
			globalErrors.add(globalError);
	}

	public void add(@Nonnull FieldError fieldError) {
		requireNonNull(fieldError);

		if (!fieldErrors.contains(fieldError))
			fieldErrors.add(fieldError);
	}

	public void setMetadata(@Nonnull Map<String, Object> metadata) {
		requireNonNull(metadata);

		this.metadata.clear();
		this.metadata.putAll(metadata);
	}

	public void add(@Nonnull ValidationException validationException) {
		requireNonNull(validationException);

		validationException.getGlobalErrors().forEach(this::add);
		validationException.getFieldErrors().forEach(this::add);
		setMetadata(validationException.getMetadata());
	}

	public boolean hasErrors() {
		return getGlobalErrors().size() > 0 || getFieldErrors().size() > 0;
	}

	@Nonnull
	public List<String> getGlobalErrors() {
		return unmodifiableList(this.globalErrors);
	}

	@Nonnull
	public List<FieldError> getFieldErrors() {
		return unmodifiableList(this.fieldErrors);
	}

	@Nonnull
	public Map<String, Object> getMetadata() {
		return Collections.unmodifiableMap(metadata);
	}

	@Immutable
	public static class FieldError {
		@Nonnull
		private final String field;
		@Nonnull
		private final String error;

		public FieldError(@Nonnull String field, @Nonnull String error) {
			requireNonNull(field);
			requireNonNull(error);

			this.field = field;
			this.error = error;
		}

		@Override
		public String toString() {
			return format("%s{field=%s, error=%s}", getClass().getSimpleName(), getField(), getError());
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other)
				return true;

			if (other == null || !getClass().equals(other.getClass()))
				return false;

			FieldError otherFieldError = (FieldError) other;

			return Objects.equals(getField(), otherFieldError.getField())
					&& Objects.equals(getError(), otherFieldError.getError());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getField(), getError());
		}

		@Nonnull
		public String getField() {
			return this.field;
		}

		@Nonnull
		public String getError() {
			return this.error;
		}
	}
}