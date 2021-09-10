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

package com.cobaltplatform.api.web.response;

import com.soklet.web.response.BinaryResponse;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class ResponseGenerator {
	private ResponseGenerator() {
		// Prevents instantiation
	}

	@Nonnull
	public static BinaryResponse utf8Response(@Nonnull String utf8Text,
																						@Nonnull String contentType) {
		requireNonNull(utf8Text);
		requireNonNull(contentType);

		try (InputStream inputStream = IOUtils.toInputStream(utf8Text, StandardCharsets.UTF_8)) {
			return new BinaryResponse(format("%s; charset=utf-8", contentType), inputStream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
