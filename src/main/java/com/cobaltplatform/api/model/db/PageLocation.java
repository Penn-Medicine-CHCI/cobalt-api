package com.cobaltplatform.api.model.db;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;

/**
 * Directory categories, independent of scheduled site placements.
 */
@NotThreadSafe
public class PageLocation {
	@Nullable
	private PageLocationId pageLocationId;
	@Nullable
	private String description;
	@Nullable
	private String directoryRelativeUrl;
	@Nullable
	private Integer displayOrder;

	@JsonAdapter(PageLocationIdTypeAdapter.class)
	public enum PageLocationId {
		FEATURED_TOPIC,
		COMMUNITY,
		UNCATEGORIZED
	}

	// Gson's default enum adapter silently turns unknown IDs into null. An unknown
	// category must be rejected rather than treated as an omitted settings field.
	public static final class PageLocationIdTypeAdapter extends TypeAdapter<PageLocationId> {
		@Override
		public PageLocationId read(JsonReader reader) throws IOException {
			if (reader.peek() != JsonToken.STRING)
				throw new JsonParseException("Page location must be a supported category ID.");
			String value = reader.nextString();
			try {
				return PageLocationId.valueOf(value);
			} catch (IllegalArgumentException e) {
				throw new JsonParseException("Unsupported page location ID.", e);
			}
		}

		@Override
		public void write(JsonWriter writer, PageLocationId value) throws IOException {
			writer.value(value.name());
		}
	}

	@Nullable
	public PageLocationId getPageLocationId() {
		return this.pageLocationId;
	}

	public void setPageLocationId(@Nullable PageLocationId pageLocationId) {
		this.pageLocationId = pageLocationId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getDirectoryRelativeUrl() {
		return this.directoryRelativeUrl;
	}

	public void setDirectoryRelativeUrl(@Nullable String directoryRelativeUrl) {
		this.directoryRelativeUrl = directoryRelativeUrl;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}
