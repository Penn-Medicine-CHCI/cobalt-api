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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.service.ScreeningQuestionContextId;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
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
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify LLC.
 */
@ThreadSafe
public class JsonMapper {
	@Nonnull
	private final Supplier<Gson> gsonSupplier;

	public JsonMapper() {
		this(null, null, null, null);
	}

	protected JsonMapper(@Nullable Provider<CurrentContext> currentContextProvider,
											 @Nullable MappingStrategy mappingStrategy,
											 @Nullable MappingFormat mappingFormat,
											 @Nullable MappingNullability mappingNullability) {
		mappingStrategy = mappingStrategy == null ? MappingStrategy.CAMEL_CASE : mappingStrategy;
		mappingFormat = mappingFormat == null ? MappingFormat.PRETTY_PRINTED : mappingFormat;
		mappingNullability = mappingNullability == null ? MappingNullability.INCLUDE_NULLS : mappingNullability;

		GsonBuilder gsonBuilder = new GsonBuilder().disableHtmlEscaping();

		if (mappingNullability == MappingNullability.INCLUDE_NULLS)
			gsonBuilder.serializeNulls();

		if (mappingStrategy == MappingStrategy.UNDERSCORES)
			gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);

		if (mappingFormat == MappingFormat.PRETTY_PRINTED)
			gsonBuilder.setPrettyPrinting();

		registerBuiltinTypeAdapters(gsonBuilder);

		if (currentContextProvider != null) {
			gsonBuilder.setExclusionStrategies(new CurrentContextExclusionStrategy(currentContextProvider));

			// Exclusion strategies are cached forever by Gson (we don't want this since they need to be dynamic
			// given the current context).
			// So we have a little Gson factory that vends a new instance every time.
			this.gsonSupplier = () -> gsonBuilder.create();
		} else {
			Gson gson = gsonBuilder.create();
			this.gsonSupplier = () -> gson;
		}
	}

	@Nullable
	public String toJson(@Nullable Object object) {
		return toJsonInternal(object);
	}

	@Nullable
	public Map<String, Object> toMap(@Nullable Object object) {
		String json = toJsonInternal(object);

		return getGsonSupplier().get().fromJson(json, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	@Nullable
	public Map<String, Object> toMapFromRawJson(@Nullable String json) {
		return getGsonSupplier().get().fromJson(json, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	@Nullable
	@SuppressWarnings("unchecked")
	protected String toJsonInternal(@Nullable Object object) {
		if (object == null)
			return null;

		Gson gson = getGsonSupplier().get();
		String json = gson.toJson(object);

		// Special case: it's likely that double-brace initialization is used, which GSON does not support.
		// See https://github.com/google/gson/issues/298
		// We can hack around this in most cases by checking if it's a commonly-used collection type and
		// copying to a non-anonymous instance
		if (json == null || "null".equals(json)) {
			if (object instanceof Map)
				json = gson.toJson(new HashMap((Map) object));
			else if (object instanceof List)
				json = gson.toJson(new ArrayList((List) object));
			else if (object instanceof Set)
				json = gson.toJson(new HashSet((Set) object));
			else
				throw new IllegalArgumentException(format("Sorry, it looks like GSON can't handle %s. " +
						"Are you using double-brace initialization? " +
						"It's a limitation of the library: https://github.com/google/gson/issues/298", object.getClass().getName()));
		}

		return json;
	}

	@Nullable
	public <T> T fromJson(@Nullable byte[] json,
												@Nonnull Class<T> targetClass) {
		requireNonNull(targetClass);

		if (json == null || json.length == 0)
			return null;

		return fromJson(new String(json, UTF_8), targetClass);
	}

	@Nullable
	public <T> T fromJson(@Nullable String json,
												@Nonnull Class<T> targetClass) {
		requireNonNull(targetClass);

		if (json == null || json.length() == 0)
			return null;

		return getGsonSupplier().get().fromJson(json, targetClass);
	}

	@Nullable
	public Map<String, Object> fromJson(@Nullable byte[] json) {
		if (json == null || json.length == 0)
			return null;

		return fromJson(new String(json, UTF_8));
	}

	@Nullable
	public <T> T fromJson(@Nullable String json) {
		if (json == null || json.length() == 0)
			return null;

		return getGsonSupplier().get().fromJson(json, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	@Nullable
	public <T> List<T> toList(@Nullable String json,
														@Nonnull Class<T> targetClass) {
		if (json == null || json.length() == 0)
			return null;

		return getGsonSupplier().get().fromJson(json, TypeToken.getParameterized(ArrayList.class, targetClass).getType());
	}

	protected void registerBuiltinTypeAdapters(@Nonnull GsonBuilder gsonBuilder) {
		requireNonNull(gsonBuilder);

		gsonBuilder.registerTypeAdapter(Instant.class, new JsonDeserializer<Instant>() {
			@Override
			@Nullable
			public Instant deserialize(@Nullable JsonElement json, @Nonnull Type type,
																 @Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
				requireNonNull(type);
				requireNonNull(jsonDeserializationContext);

				if (json == null)
					return null;

				JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

				if (jsonPrimitive.isNumber())
					return Instant.ofEpochMilli(json.getAsLong());

				if (jsonPrimitive.isString()) {
					String string = trimToNull(json.getAsString());
					return string == null ? null : Instant.parse(string);
				}

				throw new IllegalArgumentException(format("Unable to convert JSON value '%s' to %s", json, type));
			}
		});

		gsonBuilder.registerTypeAdapter(Instant.class, new JsonSerializer<Instant>() {
			@Override
			@Nullable
			public JsonElement serialize(@Nullable Instant instant, @Nonnull Type type, @Nonnull JsonSerializationContext jsonSerializationContext) {
				requireNonNull(type);
				requireNonNull(jsonSerializationContext);

				return instant == null ? null : new JsonPrimitive(instant.toString());
			}
		});

		gsonBuilder.registerTypeAdapter(Locale.class, new JsonDeserializer<Locale>() {
			@Override
			@Nullable
			public Locale deserialize(@Nullable JsonElement json, @Nonnull Type type,
																@Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
				requireNonNull(type);
				requireNonNull(jsonDeserializationContext);

				if (json == null)
					return null;

				JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

				if (jsonPrimitive.isString()) {
					String string = trimToNull(json.getAsString());
					return string == null ? null : Locale.forLanguageTag(string);
				}

				throw new IllegalArgumentException(format("Unable to convert JSON value '%s' to %s", json, type));
			}
		});

		gsonBuilder.registerTypeAdapter(Locale.class, new JsonSerializer<Locale>() {
			@Override
			@Nullable
			public JsonElement serialize(@Nullable Locale locale, @Nonnull Type type, @Nonnull JsonSerializationContext jsonSerializationContext) {
				requireNonNull(type);
				requireNonNull(jsonSerializationContext);

				return locale == null ? null : new JsonPrimitive(locale.toLanguageTag());
			}
		});

		gsonBuilder.registerTypeAdapter(ZoneId.class, new JsonDeserializer<ZoneId>() {
			@Override
			@Nullable
			public ZoneId deserialize(@Nullable JsonElement json, @Nonnull Type type,
																@Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
				requireNonNull(type);
				requireNonNull(jsonDeserializationContext);

				if (json == null)
					return null;

				JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

				if (jsonPrimitive.isString()) {
					String string = trimToNull(json.getAsString());
					return string == null ? null : ZoneId.of(string);
				}

				throw new IllegalArgumentException(format("Unable to convert JSON value '%s' to %s", json, type));
			}
		});

		gsonBuilder.registerTypeAdapter(ZoneId.class, new JsonSerializer<ZoneId>() {
			@Override
			@Nullable
			public JsonElement serialize(@Nullable ZoneId zoneId, @Nonnull Type type, @Nonnull JsonSerializationContext jsonSerializationContext) {
				requireNonNull(type);
				requireNonNull(jsonSerializationContext);

				return zoneId == null ? null : new JsonPrimitive(zoneId.getId());
			}
		});

		gsonBuilder.registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
			@Override
			@Nullable
			public LocalDate deserialize(@Nullable JsonElement json, @Nonnull Type type,
																	 @Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
				requireNonNull(type);
				requireNonNull(jsonDeserializationContext);

				if (json == null)
					return null;

				JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

				if (jsonPrimitive.isString()) {
					String string = trimToNull(json.getAsString());
					return string == null ? null : LocalDate.parse(string);
				}

				throw new IllegalArgumentException(format("Unable to convert JSON value '%s' to %s", json, type));
			}
		});

		gsonBuilder.registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
			@Override
			@Nullable
			public JsonElement serialize(@Nullable LocalDate localDate, @Nonnull Type type, @Nonnull JsonSerializationContext jsonSerializationContext) {
				requireNonNull(type);
				requireNonNull(jsonSerializationContext);

				return localDate == null ? null : new JsonPrimitive(localDate.toString());
			}
		});

		gsonBuilder.registerTypeAdapter(LocalTime.class, new JsonDeserializer<LocalTime>() {
			@Override
			@Nullable
			public LocalTime deserialize(@Nullable JsonElement json, @Nonnull Type type,
																	 @Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
				requireNonNull(type);
				requireNonNull(jsonDeserializationContext);

				if (json == null)
					return null;

				JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

				if (jsonPrimitive.isString()) {
					String string = trimToNull(json.getAsString());
					return string == null ? null : LocalTime.parse(string);
				}

				throw new IllegalArgumentException(format("Unable to convert JSON value '%s' to %s", json, type));
			}
		});

		gsonBuilder.registerTypeAdapter(LocalTime.class, new JsonSerializer<LocalTime>() {
			@Override
			@Nullable
			public JsonElement serialize(@Nullable LocalTime localTime, @Nonnull Type type, @Nonnull JsonSerializationContext jsonSerializationContext) {
				requireNonNull(type);
				requireNonNull(jsonSerializationContext);

				return localTime == null ? null : new JsonPrimitive(localTime.toString());
			}
		});

		gsonBuilder.registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
			@Override
			@Nullable
			public LocalDateTime deserialize(@Nullable JsonElement json, @Nonnull Type type,
																			 @Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
				requireNonNull(type);
				requireNonNull(jsonDeserializationContext);

				if (json == null)
					return null;

				JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

				if (jsonPrimitive.isString()) {
					String string = trimToNull(json.getAsString());
					return string == null ? null : LocalDateTime.parse(string);
				}

				throw new IllegalArgumentException(format("Unable to convert JSON value '%s' to %s", json, type));
			}
		});

		gsonBuilder.registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
			@Override
			@Nullable
			public JsonElement serialize(@Nullable LocalDateTime localDateTime, @Nonnull Type type, @Nonnull JsonSerializationContext jsonSerializationContext) {
				requireNonNull(type);
				requireNonNull(jsonSerializationContext);

				return localDateTime == null ? null : new JsonPrimitive(localDateTime.toString());
			}
		});

		gsonBuilder.registerTypeAdapter(ScreeningQuestionContextId.class, new JsonDeserializer<ScreeningQuestionContextId>() {
			@Override
			@Nullable
			public ScreeningQuestionContextId deserialize(@Nullable JsonElement json, @Nonnull Type type,
																										@Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
				requireNonNull(type);
				requireNonNull(jsonDeserializationContext);

				if (json == null)
					return null;

				JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

				if (jsonPrimitive.isString()) {
					String string = trimToNull(json.getAsString());
					return string == null ? null : new ScreeningQuestionContextId(string);
				}

				throw new IllegalArgumentException(format("Unable to convert JSON value '%s' to %s", json, type));
			}
		});

		gsonBuilder.registerTypeAdapter(ScreeningQuestionContextId.class, new JsonSerializer<ScreeningQuestionContextId>() {
			@Override
			@Nullable
			public JsonElement serialize(@Nullable ScreeningQuestionContextId screeningQuestionContextId, @Nonnull Type type, @Nonnull JsonSerializationContext jsonSerializationContext) {
				requireNonNull(type);
				requireNonNull(jsonSerializationContext);

				return screeningQuestionContextId == null ? null : new JsonPrimitive(screeningQuestionContextId.getIdentifier());
			}
		});
	}

	@Nonnull
	protected Supplier<Gson> getGsonSupplier() {
		return gsonSupplier;
	}

	@ThreadSafe
	protected static class CurrentContextExclusionStrategy implements ExclusionStrategy {
		@Nullable
		private final Provider<CurrentContext> currentContextProvider;

		public CurrentContextExclusionStrategy(@Nullable Provider<CurrentContext> currentContextProvider) {
			this.currentContextProvider = currentContextProvider;
		}

		@Override
		public boolean shouldSkipField(FieldAttributes fieldAttributes) {
			if (currentContextProvider == null)
				return false;

			// TODO: any role-based checking here

			return false;
		}

		@Override
		public boolean shouldSkipClass(Class<?> aClass) {
			return false;
		}

		@Nonnull
		public Optional<CurrentContext> getCurrentContext() {
			return currentContextProvider == null ? Optional.empty() : Optional.of(currentContextProvider.get());
		}
	}

	@NotThreadSafe
	public static class Builder {
		@Nullable
		private Provider<CurrentContext> currentContextProvider;
		@Nullable
		private MappingStrategy mappingStrategy;
		@Nullable
		private MappingFormat mappingFormat;
		@Nullable
		private MappingNullability mappingNullability;

		@Nonnull
		public Builder currentContextProvider(@Nullable Provider<CurrentContext> currentContextProvider) {
			this.currentContextProvider = currentContextProvider;
			return this;
		}

		@Nonnull
		public Builder mappingStrategy(@Nullable MappingStrategy mappingStrategy) {
			this.mappingStrategy = mappingStrategy;
			return this;
		}

		@Nonnull
		public Builder mappingFormat(@Nullable MappingFormat mappingFormat) {
			this.mappingFormat = mappingFormat;
			return this;
		}

		@Nonnull
		public Builder mappingNullability(@Nullable MappingNullability mappingNullability) {
			this.mappingNullability = mappingNullability;
			return this;
		}

		public JsonMapper build() {
			return new JsonMapper(currentContextProvider, mappingStrategy, mappingFormat, mappingNullability);
		}
	}

	public enum MappingStrategy {
		CAMEL_CASE, UNDERSCORES
	}

	public enum MappingFormat {
		PRETTY_PRINTED, COMPACT
	}

	public enum MappingNullability {
		INCLUDE_NULLS, EXCLUDE_NULLS
	}
}