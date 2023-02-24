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

import com.cobaltplatform.api.integration.way2health.model.entity.Attachment;
import com.cobaltplatform.api.integration.way2health.model.entity.Attachment.Details;
import com.cobaltplatform.api.integration.way2health.model.response.PagedResponse.Meta.Pagination;
import com.cobaltplatform.api.integration.way2health.model.response.PagedResponse.Meta.Pagination.Links;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

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
				.disableHtmlEscaping()
				.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.registerTypeAdapter(LocalDate.class, new LocalDateConverter())
				.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter())
				.registerTypeAdapter(Instant.class, new InstantConverter())
				.registerTypeAdapter(Pagination.class, new PaginationDeserializer())
				.registerTypeAdapter(Links.class, new LinksDeserializer())
				.registerTypeAdapter(Attachment.class, new AttachmentDeserializer())
				.create();
	}

	@Nonnull
	public static Gson sharedGson() {
		return SHARED_GSON;
	}

	@ThreadSafe
	public static class AttachmentDeserializer implements JsonDeserializer<Attachment> {
		@Override
		public Attachment deserialize(@Nullable JsonElement jsonElement,
																	@Nonnull Type typeOfJson,
																	@Nonnull JsonDeserializationContext context) throws JsonParseException {
			requireNonNull(typeOfJson);
			requireNonNull(context);

			JsonObject jsonObject = jsonElement.getAsJsonObject();

			Attachment attachment = new Attachment();
			attachment.setAttachmentId(extractLongFieldValue(jsonObject, "attachmentId").orElse(null));
			attachment.setObjectId(extractLongFieldValue(jsonObject, "objectId").orElse(null));
			attachment.setObjectType(extractStringFieldValue(jsonObject, "objectType").orElse(null));

			// Details is supposed to be an object.
			// W2H made this field nullable on 2023-02-23.
			// However, likely a consequence of PHP JSON handling, on the W2H side the null details field is instead sent as
			// the empty array [] (therefore this field has 2 possible types).
			// To work around - if this field is anything but an object, deserialize as null
			JsonElement detailsElement = jsonObject.get("details");

			if (detailsElement.isJsonObject())
				attachment.setDetails(context.deserialize(detailsElement, Details.class));

			return attachment;
		}
	}

	@ThreadSafe
	public static class LinksDeserializer implements JsonDeserializer<Links> {
		@Override
		public Links deserialize(@Nullable JsonElement jsonElement,
														 @Nonnull Type typeOfJson,
														 @Nonnull JsonDeserializationContext context) throws JsonParseException {
			requireNonNull(typeOfJson);
			requireNonNull(context);

			JsonObject jsonObject = jsonElement.getAsJsonObject();

			// We sometimes see this odd format, so we need to parse specially:
			//
			// {
			//   "next":"https:\/\/app.waytohealth.org\/api\/v2\/incidents?per_page=1&order_by=desc(created_at)&study_id=715&page=2",
			//   "0":{
			//     "base_request":"https:\/\/app.waytohealth.org\/api\/v2\/incidents?per_page=1&order_by=desc(created_at)&study_id=715"
			//   }
			// }
			//
			// which normally should be:
			//
			// {
			//   "next":"https:\/\/app.waytohealth.org\/api\/v2\/incidents?per_page=1&order_by=desc(created_at)&study_id=715&page=2",
			//   "base_request":"https:\/\/app.waytohealth.org\/api\/v2\/incidents?per_page=1&order_by=desc(created_at)&study_id=715"
			// }

			String next = extractStringFieldValue(jsonObject, "next").orElse(null);
			String previous = extractStringFieldValue(jsonObject, "previous").orElse(null);
			String baseRequest = extractStringFieldValue(jsonObject, "base_request").orElse(null);

			JsonObject zeroObject = jsonObject.getAsJsonObject("0");

			if (zeroObject != null && !zeroObject.isJsonNull()) {
				String alternateBaseRequest = extractStringFieldValue(zeroObject, "base_request").orElse(null);

				if (alternateBaseRequest != null)
					baseRequest = alternateBaseRequest;
			}

			Links links = new Links();
			links.setNext(next);
			links.setPrevious(previous);
			links.setBaseRequest(baseRequest);

			return links;
		}
	}

	@ThreadSafe
	public static class PaginationDeserializer implements JsonDeserializer<Pagination> {
		@Override
		public Pagination deserialize(@Nullable JsonElement jsonElement,
																	@Nonnull Type typeOfJson,
																	@Nonnull JsonDeserializationContext context) throws JsonParseException {
			requireNonNull(typeOfJson);
			requireNonNull(context);

			// We sometimes see this odd format, so we need to parse specially:
			//
			// "links":[]
			//
			// which normally should be:
			//
			// "links":{}s

			JsonObject jsonObject = jsonElement.getAsJsonObject();

			Pagination pagination = new Pagination();
			pagination.setTotal(extractIntegerFieldValue(jsonObject, "total").get());
			pagination.setCount(extractIntegerFieldValue(jsonObject, "count").get());
			pagination.setPerPage(extractIntegerFieldValue(jsonObject, "per_page").get());
			pagination.setCurrentPage(extractIntegerFieldValue(jsonObject, "current_page").get());
			pagination.setTotalPages(extractIntegerFieldValue(jsonObject, "total_pages").get());

			Links links = new Links();

			JsonElement linksElement = jsonObject.get("links");

			if (linksElement.isJsonArray()) {
				JsonArray jsonArray = linksElement.getAsJsonArray();
				links = (sharedGson().fromJson(jsonArray.get(0), Links.class));
			} else if (linksElement.isJsonObject()) {
				links = sharedGson().fromJson(linksElement, Links.class);
			}

			pagination.setLinks(links);

			return pagination;
		}
	}

	/**
	 * LocalDateTime: yyyy-MM-dd H:mm:ss (e.g. 2020-12-14 11:34:56)
	 */
	@ThreadSafe
	public static class LocalDateTimeConverter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
		@Nonnull
		private static final DateTimeFormatter DATE_TIME_FORMATTER;

		static {
			DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss").withLocale(Locale.US);
		}

		@Override
		@Nullable
		public LocalDateTime deserialize(@Nullable JsonElement jsonElement,
																		 @Nonnull Type typeOfJson,
																		 @Nonnull JsonDeserializationContext context) throws JsonParseException {
			requireNonNull(typeOfJson);
			requireNonNull(context);

			if (jsonElement == null)
				return null;

			JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();

			if (jsonPrimitive == null)
				return null;

			String localDateTimeAsString = trimToNull(jsonPrimitive.getAsString());

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
		public LocalDate deserialize(@Nullable JsonElement jsonElement,
																 @Nonnull Type typeOfJson,
																 @Nonnull JsonDeserializationContext context) throws JsonParseException {
			requireNonNull(typeOfJson);
			requireNonNull(context);

			if (jsonElement == null)
				return null;

			JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();

			if (jsonPrimitive == null)
				return null;

			String localDateAsString = trimToNull(jsonPrimitive.getAsString());

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

	/**
	 * Instant: seconds since epoch as a *string* (e.g. "1452269461")
	 * Also, there is also a more standard ISO 8601 "2021-12-22T21:37:29Z" format in some places...
	 */
	@ThreadSafe
	public static class InstantConverter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
		@Override
		@Nullable
		public Instant deserialize(@Nullable JsonElement jsonElement,
															 @Nonnull Type typeOfJson,
															 @Nonnull JsonDeserializationContext context) throws JsonParseException {
			requireNonNull(typeOfJson);
			requireNonNull(context);

			if (jsonElement == null)
				return null;

			JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();

			if (jsonPrimitive == null)
				return null;

			String instantAsString = trimToNull(jsonPrimitive.getAsString());

			if (instantAsString == null)
				return null;

			try {
				// Handle ISO 8601 "2021-12-22T21:37:29Z" case
				return DateTimeFormatter.ISO_INSTANT.parse(instantAsString, Instant::from);
			} catch (Exception ignored) {
				// There are multiple Instant representations in Way2Health, this must not be the right one...
			}

			return Instant.ofEpochSecond(Long.valueOf(instantAsString));
		}

		@Override
		@Nullable
		public JsonElement serialize(@Nullable Instant instant,
																 @Nonnull Type typeOfInstant,
																 @Nonnull JsonSerializationContext context) {
			requireNonNull(typeOfInstant);
			requireNonNull(context);

			if (instant == null)
				return null;

			// Unclear which of the two formats is "right" in Way2Health-land...we use the more standard ISO 8601 version here.
			// Alternative would be: String.valueOf(instant.getEpochSecond())
			return new JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(instant));
		}
	}

	@Nonnull
	private static Optional<Integer> extractIntegerFieldValue(@Nonnull JsonObject jsonObject,
																														@Nonnull String fieldName) {
		requireNonNull(jsonObject);
		requireNonNull(fieldName);

		if (!jsonObject.has(fieldName))
			return Optional.empty();

		JsonElement jsonElement = jsonObject.get(fieldName);

		if (jsonElement == null || jsonElement.isJsonNull())
			return Optional.empty();

		return Optional.of(jsonElement.getAsInt());
	}

	@Nonnull
	private static Optional<Long> extractLongFieldValue(@Nonnull JsonObject jsonObject,
																											@Nonnull String fieldName) {
		requireNonNull(jsonObject);
		requireNonNull(fieldName);

		if (!jsonObject.has(fieldName))
			return Optional.empty();

		JsonElement jsonElement = jsonObject.get(fieldName);

		if (jsonElement == null || jsonElement.isJsonNull())
			return Optional.empty();

		return Optional.of(jsonElement.getAsLong());
	}

	@Nonnull
	private static Optional<String> extractStringFieldValue(@Nonnull JsonObject jsonObject,
																													@Nonnull String fieldName) {
		requireNonNull(jsonObject);
		requireNonNull(fieldName);

		if (!jsonObject.has(fieldName))
			return Optional.empty();

		JsonElement jsonElement = jsonObject.get(fieldName);

		if (jsonElement == null || jsonElement.isJsonNull())
			return Optional.empty();

		return Optional.of(trimToNull(jsonElement.getAsString()));
	}
}
