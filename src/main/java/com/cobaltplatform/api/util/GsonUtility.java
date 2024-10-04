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

import com.cobaltplatform.api.integration.epic.MyChartAccessToken;
import com.cobaltplatform.api.model.service.ScreeningQuestionContextId;
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
import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class GsonUtility {
	private GsonUtility() {
		// Non-instantiable
	}

	public static void applyDefaultTypeAdapters(@Nonnull GsonBuilder gsonBuilder,
																							@Nullable Class<?>... excludedTypeAdapterTypes) {
		requireNonNull(gsonBuilder);

		Set<Class<?>> excludedTypeAdapterTypesAsSet = excludedTypeAdapterTypes == null
				? Collections.emptySet()
				: Arrays.stream(excludedTypeAdapterTypes).collect(Collectors.toSet());

		if (!excludedTypeAdapterTypesAsSet.contains(Instant.class)) {
			gsonBuilder.registerTypeAdapter(Instant.class, new JsonDeserializer<Instant>() {
				@Override
				@Nullable
				public Instant deserialize(@Nullable JsonElement json,
																	 @Nonnull Type type,
																	 @Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
					requireNonNull(type);
					requireNonNull(jsonDeserializationContext);

					if (json == null)
						return null;

					JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

					if (jsonPrimitive.isNumber()) {
						// Instants might be pure long values (millis) or include a fractional component too (micros)
						BigDecimal instantAsBigDecimal = BigDecimal.valueOf(json.getAsDouble());
						BigInteger integerPart = instantAsBigDecimal.toBigInteger();
						BigDecimal fractionalPart = instantAsBigDecimal.subtract(new BigDecimal(integerPart));

						// Start with low resolution (millis)
						Instant instant = Instant.ofEpochMilli(integerPart.longValue());

						// If there is a fractional part (micros), add them on for higher precision
						if (!fractionalPart.equals(BigDecimal.ZERO)) {
							BigInteger fractionalPartAsInteger = fractionalPart.remainder(BigDecimal.ONE).movePointRight(fractionalPart.scale()).toBigInteger();
							instant = instant.plus(fractionalPartAsInteger.longValue(), ChronoUnit.MICROS);
						}

						return instant;
					}

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
				public JsonElement serialize(@Nullable Instant instant,
																		 @Nonnull Type type,
																		 @Nonnull JsonSerializationContext jsonSerializationContext) {
					requireNonNull(type);
					requireNonNull(jsonSerializationContext);

					return instant == null ? null : new JsonPrimitive(instant.toString());
				}
			});
		}

		if (!excludedTypeAdapterTypesAsSet.contains(Locale.class)) {
			gsonBuilder.registerTypeAdapter(Locale.class, new JsonDeserializer<Locale>() {
				@Override
				@Nullable
				public Locale deserialize(@Nullable JsonElement json,
																	@Nonnull Type type,
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
				public JsonElement serialize(@Nullable Locale locale,
																		 @Nonnull Type type,
																		 @Nonnull JsonSerializationContext jsonSerializationContext) {
					requireNonNull(type);
					requireNonNull(jsonSerializationContext);

					return locale == null ? null : new JsonPrimitive(locale.toLanguageTag());
				}
			});
		}

		if (!excludedTypeAdapterTypesAsSet.contains(ZoneId.class)) {
			gsonBuilder.registerTypeAdapter(ZoneId.class, new JsonDeserializer<ZoneId>() {
				@Override
				@Nullable
				public ZoneId deserialize(@Nullable JsonElement json,
																	@Nonnull Type type,
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
				public JsonElement serialize(@Nullable ZoneId zoneId,
																		 @Nonnull Type type,
																		 @Nonnull JsonSerializationContext jsonSerializationContext) {
					requireNonNull(type);
					requireNonNull(jsonSerializationContext);

					return zoneId == null ? null : new JsonPrimitive(zoneId.getId());
				}
			});
		}

		if (!excludedTypeAdapterTypesAsSet.contains(LocalDate.class)) {
			gsonBuilder.registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
				@Override
				@Nullable
				public LocalDate deserialize(@Nullable JsonElement json,
																		 @Nonnull Type type,
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
				public JsonElement serialize(@Nullable LocalDate localDate,
																		 @Nonnull Type type,
																		 @Nonnull JsonSerializationContext jsonSerializationContext) {
					requireNonNull(type);
					requireNonNull(jsonSerializationContext);

					return localDate == null ? null : new JsonPrimitive(localDate.toString());
				}
			});
		}

		if (!excludedTypeAdapterTypesAsSet.contains(LocalTime.class)) {
			gsonBuilder.registerTypeAdapter(LocalTime.class, new JsonDeserializer<LocalTime>() {
				@Override
				@Nullable
				public LocalTime deserialize(@Nullable JsonElement json,
																		 @Nonnull Type type,
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
				public JsonElement serialize(@Nullable LocalTime localTime,
																		 @Nonnull Type type,
																		 @Nonnull JsonSerializationContext jsonSerializationContext) {
					requireNonNull(type);
					requireNonNull(jsonSerializationContext);

					return localTime == null ? null : new JsonPrimitive(localTime.toString());
				}
			});
		}

		if (!excludedTypeAdapterTypesAsSet.contains(LocalDateTime.class)) {
			gsonBuilder.registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
				@Override
				@Nullable
				public LocalDateTime deserialize(@Nullable JsonElement json,
																				 @Nonnull Type type,
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
				public JsonElement serialize(@Nullable LocalDateTime localDateTime,
																		 @Nonnull Type type,
																		 @Nonnull JsonSerializationContext jsonSerializationContext) {
					requireNonNull(type);
					requireNonNull(jsonSerializationContext);

					return localDateTime == null ? null : new JsonPrimitive(localDateTime.toString());
				}
			});
		}

		if (!excludedTypeAdapterTypesAsSet.contains(ScreeningQuestionContextId.class)) {
			gsonBuilder.registerTypeAdapter(ScreeningQuestionContextId.class, new JsonDeserializer<ScreeningQuestionContextId>() {
				@Override
				@Nullable
				public ScreeningQuestionContextId deserialize(@Nullable JsonElement json,
																											@Nonnull Type type,
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
				public JsonElement serialize(@Nullable ScreeningQuestionContextId screeningQuestionContextId,
																		 @Nonnull Type type,
																		 @Nonnull JsonSerializationContext jsonSerializationContext) {
					requireNonNull(type);
					requireNonNull(jsonSerializationContext);

					return screeningQuestionContextId == null ? null : new JsonPrimitive(screeningQuestionContextId.getIdentifier());
				}
			});
		}

		if (!excludedTypeAdapterTypesAsSet.contains(MyChartAccessToken.class)) {
			gsonBuilder.registerTypeAdapter(MyChartAccessToken.class, new JsonDeserializer<MyChartAccessToken>() {
				@Override
				@Nullable
				public MyChartAccessToken deserialize(@Nullable JsonElement json,
																							@Nonnull Type type,
																							@Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
					requireNonNull(type);
					requireNonNull(jsonDeserializationContext);

					if (json == null)
						return null;

					JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

					if (jsonPrimitive.isString()) {
						String string = trimToNull(json.getAsString());
						return string == null ? null : MyChartAccessToken.deserialize(string);
					}

					throw new IllegalArgumentException(format("Unable to convert JSON value '%s' to %s", json, type));
				}
			});

			gsonBuilder.registerTypeAdapter(MyChartAccessToken.class, new JsonSerializer<MyChartAccessToken>() {
				@Override
				@Nullable
				public JsonElement serialize(@Nullable MyChartAccessToken myChartAccessToken,
																		 @Nonnull Type type,
																		 @Nonnull JsonSerializationContext jsonSerializationContext) {
					requireNonNull(type);
					requireNonNull(jsonSerializationContext);

					return myChartAccessToken == null ? null : new JsonPrimitive(myChartAccessToken.serialize());
				}
			});
		}
	}
}