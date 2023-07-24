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

package com.cobaltplatform.api.util;

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

	public ValidationException(@Nonnull String globalError,
														 @Nonnull Map<String, Object> metadata) {
		this();
		requireNonNull(globalError);
		requireNonNull(metadata);

		globalErrors.add(globalError);
		setMetadata(metadata);
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

	public ValidationException(@Nonnull List<String> globalErrors,
														 @Nonnull List<FieldError> fieldErrors) {
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