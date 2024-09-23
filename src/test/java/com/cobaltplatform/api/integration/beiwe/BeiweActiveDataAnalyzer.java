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

package com.cobaltplatform.api.integration.beiwe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class BeiweActiveDataAnalyzer {
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	public static void main(String[] args) throws Exception {
		initializeLogback(Paths.get("config/local/logback.xml"));

		String ffprobeAbsolutePathEnvVarValue = requiredEnvironmentVariableValue("FFPROBE_PATH");
		Path ffprobeFile = Path.of(ffprobeAbsolutePathEnvVarValue);

		if (!Files.isRegularFile(ffprobeFile))
			throw new IllegalArgumentException(format("Unable to find ffprobe at %s", ffprobeFile.toAbsolutePath()));

		String dataDirectoryEnvVarValue = requiredEnvironmentVariableValue("DATA_DIRECTORY");
		Path dataDirectory = Path.of(dataDirectoryEnvVarValue);

		if (!Files.isDirectory(dataDirectory))
			throw new IllegalArgumentException(format("Unable to find data directory at %s", dataDirectory.toAbsolutePath()));

		Files.list(dataDirectory)
				.filter(dataDirectoryFile -> Files.isDirectory(dataDirectoryFile))
				.forEach(usernameDirectory -> {
					try {
						Path activeDirectory = usernameDirectory.resolve("active");
						AtomicBoolean activeFileDetected = new AtomicBoolean(false);

						if (Files.isDirectory(activeDirectory)) {
							Files.list(usernameDirectory.resolve("active"))
									.filter(file -> !file.getFileName().toString().equals(".DS_Store"))
									.forEach(activeDataFile -> {
										activeFileDetected.set(true);

										try {
											FFProbeResult ffProbeResult = extractFFProbeResultFromFile(ffprobeFile, activeDataFile);
											System.out.println(ffProbeResult);
										} catch (IOException e) {
											throw new UncheckedIOException(e);
										}
									});
						}

						if (!activeFileDetected.get())
							System.out.println(format("No active files detected for %s", usernameDirectory.getFileName()));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
	}

	@NotThreadSafe
	public static class FFProbeResult {
		@Nullable
		private List<FFProbeStream> streams;

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
		}

		@Nullable
		public List<FFProbeStream> getStreams() {
			return this.streams;
		}

		public void setStreams(@Nullable List<FFProbeStream> streams) {
			this.streams = streams;
		}

		@NotThreadSafe
		public static class FFProbeStream {
			@Nullable
			private Integer index;
			@Nullable
			@SerializedName("codec_name")
			private String codecName;
			@Nullable
			@SerializedName("codec_long_name")
			private String codecLongName;
			@Nullable
			@SerializedName("codec_type")
			private String codecType;
			@Nullable
			private String duration;
			@Nullable
			private String width; // video
			@Nullable
			private String height; // video
			@Nullable
			@SerializedName("sample_rate")
			private String sampleRate; // audio

			@Override
			public String toString() {
				return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
			}

			@Nullable
			public Integer getIndex() {
				return this.index;
			}

			public void setIndex(@Nullable Integer index) {
				this.index = index;
			}

			@Nullable
			public String getCodecName() {
				return this.codecName;
			}

			public void setCodecName(@Nullable String codecName) {
				this.codecName = codecName;
			}

			@Nullable
			public String getCodecLongName() {
				return this.codecLongName;
			}

			public void setCodecLongName(@Nullable String codecLongName) {
				this.codecLongName = codecLongName;
			}

			@Nullable
			public String getCodecType() {
				return this.codecType;
			}

			public void setCodecType(@Nullable String codecType) {
				this.codecType = codecType;
			}

			@Nullable
			public String getDuration() {
				return this.duration;
			}

			public void setDuration(@Nullable String duration) {
				this.duration = duration;
			}

			@Nullable
			public String getWidth() {
				return this.width;
			}

			public void setWidth(@Nullable String width) {
				this.width = width;
			}

			@Nullable
			public String getHeight() {
				return this.height;
			}

			public void setHeight(@Nullable String height) {
				this.height = height;
			}

			@Nullable
			public String getSampleRate() {
				return this.sampleRate;
			}

			public void setSampleRate(@Nullable String sampleRate) {
				this.sampleRate = sampleRate;
			}
		}
	}

	@Nonnull
	private static FFProbeResult extractFFProbeResultFromFile(@Nonnull Path ffprobeFile,
																														@Nonnull Path targetFile) throws IOException {
		requireNonNull(ffprobeFile);
		requireNonNull(targetFile);

		Process process = Runtime.getRuntime().exec(new String[]{
				ffprobeFile.toAbsolutePath().toString(),
				"-loglevel",
				"error",
				"-show_streams",
				"-print_format",
				"json",
				targetFile.toAbsolutePath().toString()
		});

		List<String> jsonLines = new ArrayList<>();

		try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;

			while ((line = input.readLine()) != null)
				jsonLines.add(line);
		}

		String json = jsonLines.stream().collect(Collectors.joining("\n"));
		return GSON.fromJson(json, FFProbeResult.class);
	}

	@Nonnull
	private static String requiredEnvironmentVariableValue(@Nonnull String environmentVariableName) {
		requireNonNull(environmentVariableName);
		String environmentVariableValue = trimToNull(System.getenv(environmentVariableName));

		if (environmentVariableValue == null)
			throw new IllegalArgumentException(format("Environment variable %s must be set", environmentVariableName));

		return environmentVariableValue;
	}
}
