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

package com.cobaltplatform.api;

import com.soklet.archive.ArchivePath;
import com.soklet.archive.ArchivePaths;
import com.soklet.archive.Archiver;
import com.cobaltplatform.api.util.GitUtility;
import com.cobaltplatform.api.util.MavenUtility;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import static com.soklet.util.LoggingUtils.initializeLogback;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AppArchiver {
	// Note: you probably want to run this from the command line via "archive" script
	public static void main(String[] args) throws Exception {
		initializeLogback(Paths.get("config/local/logback.xml"));

		Path archiveFile = Paths.get("cobalt-api.zip");

		// Copy these directories and files into the archive
		Set<ArchivePath> archivePaths = new HashSet<>() {
			{
				add(ArchivePaths.get(Paths.get("package.json")));

				add(ArchivePaths.get(Paths.get("config")));
				add(ArchivePaths.get(Paths.get("messages")));
				add(ArchivePaths.get(Paths.get("web")));
				add(ArchivePaths.get(Paths.get("target/dependency"), Paths.get("lib")));
				add(ArchivePaths.get(Paths.get("target/classes"), Paths.get("classes")));
				add(ArchivePaths.get(Paths.get("start-api"), Paths.get(".")));

				// Generated below during preprocessing phase
				add(ArchivePaths.get(Paths.get("build-timestamp"), Paths.get(".")));
				add(ArchivePaths.get(Paths.get("application-version"), Paths.get(".")));
				add(ArchivePaths.get(Paths.get("git-commit-hash"), Paths.get(".")));
				add(ArchivePaths.get(Paths.get("git-branch"), Paths.get(".")));
			}
		};

		Archiver archiver = Archiver.forArchiveFile(archiveFile)
				.archivePaths(archivePaths)
				.preProcessOperation((Archiver currentArchiver, Path workingDirectory) -> {
					// Write build timestamp to a file to be included in the archive
					String buildTimestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
					Path buildTimestampFile = workingDirectory.resolve("build-timestamp");
					Files.write(buildTimestampFile, buildTimestamp.getBytes(StandardCharsets.UTF_8));

					// Write application to a file to be included in the archive
					String applicationVersion = MavenUtility.getPomVersion();
					Path applicationVersionFile = workingDirectory.resolve("application-version");
					Files.write(applicationVersionFile, applicationVersion.getBytes(StandardCharsets.UTF_8));

					// Write git commit hash to a file to be included in the archive
					String gitCommitHash = GitUtility.getHeadCommitHash();
					Path gitCommitHashFile = workingDirectory.resolve("git-commit-hash");
					Files.write(gitCommitHashFile, gitCommitHash.getBytes(StandardCharsets.UTF_8));

					// Write git branch to a file to be included in the archive
					String branch = GitUtility.getBranch();
					Path gitBranchFile = workingDirectory.resolve("git-branch");
					Files.write(gitBranchFile, branch.getBytes(StandardCharsets.UTF_8));
				})
				.mavenSupport()
				.build();

		archiver.run();
	}
}
