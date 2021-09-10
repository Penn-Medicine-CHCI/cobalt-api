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

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class StringUtility {
	private StringUtility() {
		// Non-instantiable
	}

	/**
	 * Thanks to org/apache/commons/lang3/StringUtils.java.
	 */
	@Nonnull
	public static String stripAccents(@Nonnull String input) {
		requireNonNull(input);

		final Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
		final StringBuilder decomposed = new StringBuilder(java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD));
		convertRemainingAccentCharacters(decomposed);
		// Note that this doesn't correctly remove ligatures...
		return pattern.matcher(decomposed).replaceAll(StringUtils.EMPTY);
	}

	/**
	 * Thanks to org/apache/commons/lang3/StringUtils.java.
	 */
	private static void convertRemainingAccentCharacters(@Nonnull StringBuilder decomposed) {
		requireNonNull(decomposed);

		for (int i = 0; i < decomposed.length(); i++) {
			if (decomposed.charAt(i) == '\u0141') {
				decomposed.deleteCharAt(i);
				decomposed.insert(i, 'L');
			} else if (decomposed.charAt(i) == '\u0142') {
				decomposed.deleteCharAt(i);
				decomposed.insert(i, 'l');
			}
		}
	}
}