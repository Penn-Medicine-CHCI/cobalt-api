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

import com.cobaltplatform.api.integration.beiwe.BeiweActiveDataAnalyzer.FFProbeResult.FFProbeStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
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

		String outputCsvFileEnvVarValue = requiredEnvironmentVariableValue("OUTPUT_FILE");
		Path outputFile = Path.of(outputCsvFileEnvVarValue);

		if (Files.isDirectory(outputFile))
			throw new IllegalArgumentException(format("Output file is a directory at %s", outputFile.toAbsolutePath()));

		List<ActiveDataCsvRow> rows = new ArrayList<>();

		Files.list(dataDirectory)
				.filter(dataDirectoryFile -> Files.isDirectory(dataDirectoryFile))
				.sorted()
				.forEach(usernameDirectory -> {
					try {
						Path activeDirectory = usernameDirectory.resolve("active");
						AtomicBoolean activeFileDetected = new AtomicBoolean(false);

						if (Files.isDirectory(activeDirectory)) {
							Files.list(usernameDirectory.resolve("active"))
									.filter(file -> !file.getFileName().toString().equals(".DS_Store"))
									.sorted()
									.forEach(activeDataFile -> {
										activeFileDetected.set(true);

										try {
											FFProbeResult ffProbeResult = extractFFProbeResultFromFile(ffprobeFile, activeDataFile);

											FFProbeStream audioStream = null;
											FFProbeStream videoStream = null;

											for (FFProbeStream stream : ffProbeResult.getStreams()) {
												if ("audio".equals(stream.getCodecType()))
													audioStream = stream;
												else if ("video".equals(stream.getCodecType()))
													videoStream = stream;
												else
													System.err.println(format("Unexpected codec type '%s' for %s: %s", stream.getCodecType(), activeDataFile.toAbsolutePath(), stream));
											}

											if (audioStream == null && videoStream == null)
												throw new IllegalStateException(format("Unable to find any video/audio streams for %s", activeDataFile.toAbsolutePath()));

											ActiveDataCsvRow row = new ActiveDataCsvRow();
											row.setUsername(usernameDirectory.getFileName().toString());
											row.setRecordingType(videoStream != null ? "VIDEO" : "AUDIO");
											row.setDuration(videoStream != null ? videoStream.getDuration() : audioStream.getDuration());
											row.setFilesize(String.valueOf(activeDataFile.toFile().length()));
											row.setVideoWidth(videoStream != null ? videoStream.getWidth() : null);
											row.setVideoHeight(videoStream != null ? videoStream.getHeight() : null);
											row.setVideoCodecName(videoStream != null ? videoStream.getCodecName() : null);
											row.setVideoCodecLongName(videoStream != null ? videoStream.getCodecLongName() : null);
											row.setAudioSampleRate(audioStream != null ? audioStream.getSampleRate() : null);
											row.setAudioCodecName(audioStream != null ? audioStream.getCodecName() : null);
											row.setAudioCodecLongName(audioStream != null ? audioStream.getCodecLongName() : null);

											rows.add(row);
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

		final List<String> CSV_HEADERS = List.of(
				"Username",
				"Recording Type",
				"Duration (Seconds)",
				"Filesize (Bytes)",
				"Video Width (Pixels)",
				"Video Height (Pixels)",
				"Video Codec Name",
				"Video Codec Long Name",
				"Audio Sample Rate",
				"Audio Codec Name",
				"Audio Codec Long Name"
		);

		try (Writer writer = new FileWriter(outputFile.toFile(), StandardCharsets.UTF_8);
				 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(CSV_HEADERS.toArray(new String[0])))) {
			for (ActiveDataCsvRow row : rows) {
				List<String> recordElements = new ArrayList<>();

				recordElements.add(row.getUsername());
				recordElements.add(row.getRecordingType());
				recordElements.add(row.getDuration());
				recordElements.add(row.getFilesize());
				recordElements.add(row.getVideoWidth());
				recordElements.add(row.getVideoHeight());
				recordElements.add(row.getVideoCodecName());
				recordElements.add(row.getVideoCodecLongName());
				recordElements.add(row.getAudioSampleRate());
				recordElements.add(row.getAudioCodecName());
				recordElements.add(row.getAudioCodecLongName());

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@NotThreadSafe
	public static class ActiveDataCsvRow {
		@Nullable
		private String username;
		@Nullable
		private String recordingType;
		@Nullable
		private String duration;
		@Nullable
		private String filesize;
		@Nullable
		private String videoWidth;
		@Nullable
		private String videoHeight;
		@Nullable
		private String videoCodecName;
		@Nullable
		private String videoCodecLongName;
		@Nullable
		private String audioSampleRate;
		@Nullable
		private String audioCodecName;
		@Nullable
		private String audioCodecLongName;

		@Nullable
		public String getUsername() {
			return this.username;
		}

		public void setUsername(@Nullable String username) {
			this.username = username;
		}

		@Nullable
		public String getRecordingType() {
			return this.recordingType;
		}

		public void setRecordingType(@Nullable String recordingType) {
			this.recordingType = recordingType;
		}

		@Nullable
		public String getDuration() {
			return this.duration;
		}

		public void setDuration(@Nullable String duration) {
			this.duration = duration;
		}

		@Nullable
		public String getFilesize() {
			return this.filesize;
		}

		public void setFilesize(@Nullable String filesize) {
			this.filesize = filesize;
		}

		@Nullable
		public String getVideoWidth() {
			return this.videoWidth;
		}

		public void setVideoWidth(@Nullable String videoWidth) {
			this.videoWidth = videoWidth;
		}

		@Nullable
		public String getVideoHeight() {
			return this.videoHeight;
		}

		public void setVideoHeight(@Nullable String videoHeight) {
			this.videoHeight = videoHeight;
		}

		@Nullable
		public String getVideoCodecName() {
			return this.videoCodecName;
		}

		public void setVideoCodecName(@Nullable String videoCodecName) {
			this.videoCodecName = videoCodecName;
		}

		@Nullable
		public String getVideoCodecLongName() {
			return this.videoCodecLongName;
		}

		public void setVideoCodecLongName(@Nullable String videoCodecLongName) {
			this.videoCodecLongName = videoCodecLongName;
		}

		@Nullable
		public String getAudioSampleRate() {
			return this.audioSampleRate;
		}

		public void setAudioSampleRate(@Nullable String audioSampleRate) {
			this.audioSampleRate = audioSampleRate;
		}

		@Nullable
		public String getAudioCodecName() {
			return this.audioCodecName;
		}

		public void setAudioCodecName(@Nullable String audioCodecName) {
			this.audioCodecName = audioCodecName;
		}

		@Nullable
		public String getAudioCodecLongName() {
			return this.audioCodecLongName;
		}

		public void setAudioCodecLongName(@Nullable String audioCodecLongName) {
			this.audioCodecLongName = audioCodecLongName;
		}
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
