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

package com.cobaltplatform.api.integration.way2health;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class Way2HealthGsonSupport {
	@Nonnull
	private static final Gson SHARED_GSON;

	static {
		SHARED_GSON = new GsonBuilder()
				.setPrettyPrinting()
				.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.registerTypeAdapter(LocalDate.class, new LocalDateConverter())
				.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter())
				.create();
	}

	@Nonnull
	public static Gson sharedGson() {
		return SHARED_GSON;
	}

	/**
	 * LocalDateTime: yyyy-MM-dd HH:mm:ss (e.g. 2020-12-14 11:34:56)
	 */
	@ThreadSafe
	public static class LocalDateTimeConverter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
		@Nonnull
		private static final DateTimeFormatter DATE_TIME_FORMATTER;

		static {
			DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.US);
		}

		@Override
		@Nullable
		public LocalDateTime deserialize(@Nullable JsonElement json,
																		 @Nonnull Type typeOfJson,
																		 @Nonnull JsonDeserializationContext context) throws JsonParseException {
			requireNonNull(typeOfJson);
			requireNonNull(context);

			if (json == null)
				return null;

			JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

			if (jsonPrimitive == null)
				return null;

			String localDateTimeAsString = jsonPrimitive.getAsString();

			if (localDateTimeAsString == null)
				return null;

			return LocalDateTime.parse(localDateTimeAsString, getDateTimeFormatter());
		}

		@Override
		@Nullable
		public JsonElement serialize(@Nullable LocalDateTime localDateTime,
																 @Nonnull Type typeOfLocalDateTime,
																 @Nonnull JsonSerializationContext context) {
			requireNonNull(typeOfLocalDateTime);
			requireNonNull(context);

			if (localDateTime == null)
				return null;

			return new JsonPrimitive(getDateTimeFormatter().format(localDateTime));
		}

		@Nonnull
		protected DateTimeFormatter getDateTimeFormatter() {
			return DATE_TIME_FORMATTER;
		}
	}

	/**
	 * LocalDateTime: yyyy-MM-dd (e.g. 2020-12-14)
	 */
	@ThreadSafe
	public static class LocalDateConverter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
		@Nonnull
		private static final DateTimeFormatter DATE_TIME_FORMATTER;

		static {
			DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.US);
		}

		@Override
		@Nullable
		public LocalDate deserialize(@Nullable JsonElement json,
																 @Nonnull Type typeOfJson,
																 @Nonnull JsonDeserializationContext context) throws JsonParseException {
			requireNonNull(typeOfJson);
			requireNonNull(context);

			if (json == null)
				return null;

			JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

			if (jsonPrimitive == null)
				return null;

			String localDateAsString = jsonPrimitive.getAsString();

			if (localDateAsString == null)
				return null;

			return LocalDate.parse(localDateAsString, getDateTimeFormatter());
		}

		@Override
		@Nullable
		public JsonElement serialize(@Nullable LocalDate localDate,
																 @Nonnull Type typeOfLocalDate,
																 @Nonnull JsonSerializationContext context) {
			requireNonNull(typeOfLocalDate);
			requireNonNull(context);

			if (localDate == null)
				return null;

			return new JsonPrimitive(getDateTimeFormatter().format(localDate));
		}

		@Nonnull
		protected DateTimeFormatter getDateTimeFormatter() {
			return DATE_TIME_FORMATTER;
		}
	}
}
