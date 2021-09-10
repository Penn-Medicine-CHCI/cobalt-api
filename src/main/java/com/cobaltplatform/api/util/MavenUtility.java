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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.FileReader;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class MavenUtility {
	private MavenUtility() {
		// Non-instantiable
	}

	@Nonnull
	public static String getPomVersion() {
		MavenXpp3Reader reader = new MavenXpp3Reader();

		try (FileReader fileReader = new FileReader("pom.xml")) {
			Model model = reader.read(fileReader);
			return model.getVersion();
		} catch (Exception e) {
			throw new RuntimeException("Unable to extract version from POM", e);
		}
	}
}
