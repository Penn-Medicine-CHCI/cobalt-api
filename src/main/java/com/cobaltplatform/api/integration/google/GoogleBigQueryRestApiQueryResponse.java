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

package com.cobaltplatform.api.integration.google;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.lang.reflect.Type;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * See https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/query
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
class GoogleBigQueryRestApiQueryResponse {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapter(GoogleBigQueryRestApiQueryResponse.Row.RowField.class, new JsonDeserializer<Row.RowField>() {
					@Override
					@Nullable
					public GoogleBigQueryRestApiQueryResponse.Row.RowField deserialize(@Nullable JsonElement json,
																																						 @Nonnull Type type,
																																						 @Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
						requireNonNull(type);
						requireNonNull(jsonDeserializationContext);

						if (json == null)
							return null;

						GoogleBigQueryRestApiQueryResponse.Row.RowField rowField = new GoogleBigQueryRestApiQueryResponse.Row.RowField();

						JsonObject jsonObject = json.getAsJsonObject();

						// "v" can be either a string or a RowField or a List<RowField>.
						JsonElement v = jsonObject.get("v");

						if (v != null && !v.isJsonNull()) {
							if (v.isJsonPrimitive()) {
								rowField.setValue(v.getAsString());
							} else if (v.isJsonArray()) {
								rowField.setFields(jsonDeserializationContext.deserialize(v, new TypeToken<List<Row.RowField>>() {
								}.getType()));
							} else if (v.isJsonObject()) {
								rowField.setField(jsonDeserializationContext.deserialize(v, GoogleBigQueryRestApiQueryResponse.Row.RowField.class));
							} else {
								throw new IllegalArgumentException("Unexpected JSON: " + jsonObject);
							}
						}

						// "f" can be a List<RowField>
						JsonElement f = jsonObject.get("f");

						if (f != null && !f.isJsonNull()) {
							if (f.isJsonArray()) {
								rowField.setFields(jsonDeserializationContext.deserialize(f, new TypeToken<List<Row.RowField>>() {
								}.getType()));
							} else {
								throw new IllegalArgumentException("Unexpected JSON: " + jsonObject);
							}
						}

						return rowField;
					}
				})
				.create();
	}

	@Nullable
	private String kind;
	@Nullable
	private TableSchema schema;
	@Nullable
	private JobReference jobReference;
	@Nullable
	private String totalRows;
	@Nullable
	private String pageToken;
	@Nullable
	private List<Row> rows;
	@Nullable
	private String totalBytesProcessed;
	@Nullable
	private Boolean jobComplete;
	@Nullable
	private List<ErrorProto> errors;
	@Nullable
	private Boolean cacheHit;
	@Nullable
	private String numDmlAffectedRows;
	@Nullable
	private SessionInfo sessionInfo;
	@Nullable
	private DmlStats dmlStats;

	@Nonnull
	public static GoogleBigQueryRestApiQueryResponse fromJson(@Nonnull String json) {
		return GSON.fromJson(json, GoogleBigQueryRestApiQueryResponse.class);
	}

	@Override
	public String toString() {
		return GSON.toJson(this);
	}

	@NotThreadSafe
	public static class Row {
		@Nullable
		@SerializedName("f")
		private List<RowField> fields;

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@NotThreadSafe
		public static class RowField {
			// Could be either "value" or "field" or "fields" - never both
			@Nullable
			private String value;
			@Nullable
			private RowField field;
			@Nullable
			private List<RowField> fields;

			@Override
			public String toString() {
				return GSON.toJson(this);
			}

			@Nullable
			public String getValue() {
				return this.value;
			}

			public void setValue(@Nullable String value) {
				this.value = value;
			}

			@Nullable
			public RowField getField() {
				return this.field;
			}

			public void setField(@Nullable RowField field) {
				this.field = field;
			}

			@Nullable
			public List<RowField> getFields() {
				return this.fields;
			}

			public void setFields(@Nullable List<RowField> fields) {
				this.fields = fields;
			}
		}

		@Nullable
		public List<RowField> getFields() {
			return this.fields;
		}

		public void setFields(@Nullable List<RowField> fields) {
			this.fields = fields;
		}
	}

	@NotThreadSafe
	public static class TableSchema {
		@Nullable
		private List<TableFieldSchema> fields;

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@NotThreadSafe
		public static class TableFieldSchema {
			@Nullable
			private String name;
			@Nullable
			private String type;
			@Nullable
			private String mode;
			@Nullable
			private List<TableFieldSchema> fields;
			@Nullable
			private String description;
			@Nullable
			private PolicyTags policyTags;
			@Nullable
			private String maxLength;
			@Nullable
			private String precision;
			@Nullable
			private String scale;
			@Nullable
			private RoundingMode roundingMode;
			@Nullable
			private String collation;
			@Nullable
			private String defaultValueExpression;

			@Override
			public String toString() {
				return GSON.toJson(this);
			}

			public enum RoundingMode {
				ROUNDING_MODE_UNSPECIFIED,
				ROUND_HALF_AWAY_FROM_ZERO,
				ROUND_HALF_EVEN
			}

			@NotThreadSafe
			public static class PolicyTags {
				@Nullable
				private List<String> names;

				@Override
				public String toString() {
					return GSON.toJson(this);
				}

				@Nullable
				public List<String> getNames() {
					return this.names;
				}

				public void setNames(@Nullable List<String> names) {
					this.names = names;
				}
			}

			@Nullable
			public String getName() {
				return this.name;
			}

			public void setName(@Nullable String name) {
				this.name = name;
			}

			@Nullable
			public String getType() {
				return this.type;
			}

			public void setType(@Nullable String type) {
				this.type = type;
			}

			@Nullable
			public String getMode() {
				return this.mode;
			}

			public void setMode(@Nullable String mode) {
				this.mode = mode;
			}

			@Nullable
			public List<TableFieldSchema> getFields() {
				return this.fields;
			}

			public void setFields(@Nullable List<TableFieldSchema> fields) {
				this.fields = fields;
			}

			@Nullable
			public String getDescription() {
				return this.description;
			}

			public void setDescription(@Nullable String description) {
				this.description = description;
			}

			@Nullable
			public PolicyTags getPolicyTags() {
				return this.policyTags;
			}

			public void setPolicyTags(@Nullable PolicyTags policyTags) {
				this.policyTags = policyTags;
			}

			@Nullable
			public String getMaxLength() {
				return this.maxLength;
			}

			public void setMaxLength(@Nullable String maxLength) {
				this.maxLength = maxLength;
			}

			@Nullable
			public String getPrecision() {
				return this.precision;
			}

			public void setPrecision(@Nullable String precision) {
				this.precision = precision;
			}

			@Nullable
			public String getScale() {
				return this.scale;
			}

			public void setScale(@Nullable String scale) {
				this.scale = scale;
			}

			@Nullable
			public RoundingMode getRoundingMode() {
				return this.roundingMode;
			}

			public void setRoundingMode(@Nullable RoundingMode roundingMode) {
				this.roundingMode = roundingMode;
			}

			@Nullable
			public String getCollation() {
				return this.collation;
			}

			public void setCollation(@Nullable String collation) {
				this.collation = collation;
			}

			@Nullable
			public String getDefaultValueExpression() {
				return this.defaultValueExpression;
			}

			public void setDefaultValueExpression(@Nullable String defaultValueExpression) {
				this.defaultValueExpression = defaultValueExpression;
			}
		}

		@Nullable
		public List<TableFieldSchema> getFields() {
			return this.fields;
		}

		public void setFields(@Nullable List<TableFieldSchema> fields) {
			this.fields = fields;
		}
	}

	@NotThreadSafe
	public static class JobReference {
		@Nullable
		private String projectId;
		@Nullable
		private String jobId;
		@Nullable
		private String location;

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@Nullable
		public String getProjectId() {
			return this.projectId;
		}

		public void setProjectId(@Nullable String projectId) {
			this.projectId = projectId;
		}

		@Nullable
		public String getJobId() {
			return this.jobId;
		}

		public void setJobId(@Nullable String jobId) {
			this.jobId = jobId;
		}

		@Nullable
		public String getLocation() {
			return this.location;
		}

		public void setLocation(@Nullable String location) {
			this.location = location;
		}
	}

	@NotThreadSafe
	public static class ErrorProto {
		@Nullable
		private String reason;
		@Nullable
		private String location;
		@Nullable
		private String debugInfo;
		@Nullable
		private String message;

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@Nullable
		public String getReason() {
			return this.reason;
		}

		public void setReason(@Nullable String reason) {
			this.reason = reason;
		}

		@Nullable
		public String getLocation() {
			return this.location;
		}

		public void setLocation(@Nullable String location) {
			this.location = location;
		}

		@Nullable
		public String getDebugInfo() {
			return this.debugInfo;
		}

		public void setDebugInfo(@Nullable String debugInfo) {
			this.debugInfo = debugInfo;
		}

		@Nullable
		public String getMessage() {
			return this.message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}
	}

	@NotThreadSafe
	public static class SessionInfo {
		@Nullable
		private String sessionId;

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@Nullable
		public String getSessionId() {
			return this.sessionId;
		}

		public void setSessionId(@Nullable String sessionId) {
			this.sessionId = sessionId;
		}
	}

	@NotThreadSafe
	public static class DmlStats {
		@Nullable
		private String insertedRowCount;
		@Nullable
		private String deletedRowCount;
		@Nullable
		private String updatedRowCount;

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@Nullable
		public String getInsertedRowCount() {
			return this.insertedRowCount;
		}

		public void setInsertedRowCount(@Nullable String insertedRowCount) {
			this.insertedRowCount = insertedRowCount;
		}

		@Nullable
		public String getDeletedRowCount() {
			return this.deletedRowCount;
		}

		public void setDeletedRowCount(@Nullable String deletedRowCount) {
			this.deletedRowCount = deletedRowCount;
		}

		@Nullable
		public String getUpdatedRowCount() {
			return this.updatedRowCount;
		}

		public void setUpdatedRowCount(@Nullable String updatedRowCount) {
			this.updatedRowCount = updatedRowCount;
		}
	}

	@Nullable
	public String getKind() {
		return this.kind;
	}

	public void setKind(@Nullable String kind) {
		this.kind = kind;
	}

	@Nullable
	public TableSchema getSchema() {
		return this.schema;
	}

	public void setSchema(@Nullable TableSchema schema) {
		this.schema = schema;
	}

	@Nullable
	public JobReference getJobReference() {
		return this.jobReference;
	}

	public void setJobReference(@Nullable JobReference jobReference) {
		this.jobReference = jobReference;
	}

	@Nullable
	public String getTotalRows() {
		return this.totalRows;
	}

	public void setTotalRows(@Nullable String totalRows) {
		this.totalRows = totalRows;
	}

	@Nullable
	public String getPageToken() {
		return this.pageToken;
	}

	public void setPageToken(@Nullable String pageToken) {
		this.pageToken = pageToken;
	}

	@Nullable
	public List<Row> getRows() {
		return this.rows;
	}

	public void setRows(@Nullable List<Row> rows) {
		this.rows = rows;
	}

	@Nullable
	public String getTotalBytesProcessed() {
		return this.totalBytesProcessed;
	}

	public void setTotalBytesProcessed(@Nullable String totalBytesProcessed) {
		this.totalBytesProcessed = totalBytesProcessed;
	}

	@Nullable
	public Boolean getJobComplete() {
		return this.jobComplete;
	}

	public void setJobComplete(@Nullable Boolean jobComplete) {
		this.jobComplete = jobComplete;
	}

	@Nullable
	public List<ErrorProto> getErrors() {
		return this.errors;
	}

	public void setErrors(@Nullable List<ErrorProto> errors) {
		this.errors = errors;
	}

	@Nullable
	public Boolean getCacheHit() {
		return this.cacheHit;
	}

	public void setCacheHit(@Nullable Boolean cacheHit) {
		this.cacheHit = cacheHit;
	}

	@Nullable
	public String getNumDmlAffectedRows() {
		return this.numDmlAffectedRows;
	}

	public void setNumDmlAffectedRows(@Nullable String numDmlAffectedRows) {
		this.numDmlAffectedRows = numDmlAffectedRows;
	}

	@Nullable
	public SessionInfo getSessionInfo() {
		return this.sessionInfo;
	}

	public void setSessionInfo(@Nullable SessionInfo sessionInfo) {
		this.sessionInfo = sessionInfo;
	}

	@Nullable
	public DmlStats getDmlStats() {
		return this.dmlStats;
	}

	public void setDmlStats(@Nullable DmlStats dmlStats) {
		this.dmlStats = dmlStats;
	}
}