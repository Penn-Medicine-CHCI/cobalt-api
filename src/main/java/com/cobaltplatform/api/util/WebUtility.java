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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.net.URLDecoder.decode;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public final class WebUtility {
	@Nonnull
	private static final Pattern DISALLOWED_CHARACTERS_PATTERN;
	@Nonnull
	private static final Pattern WHITESPACE_PATTERN;
	@Nonnull
	private static final Pattern MULTILPLE_WHITESPACE_PATTERN;
	@Nonnull
	private static final Pattern MULTILPLE_HYPHEN_PATTERN;

	static {
		DISALLOWED_CHARACTERS_PATTERN = Pattern.compile("[^(\\p{Alnum}-_)]", Pattern.UNICODE_CHARACTER_CLASS);
		WHITESPACE_PATTERN = Pattern.compile("\\s", Pattern.UNICODE_CHARACTER_CLASS);
		MULTILPLE_WHITESPACE_PATTERN = Pattern.compile("\\s{2,}", Pattern.UNICODE_CHARACTER_CLASS);
		MULTILPLE_HYPHEN_PATTERN = Pattern.compile("-{2,}", Pattern.UNICODE_CHARACTER_CLASS);
	}

	private WebUtility() {
		// Non-instantiable
	}

	@Nonnull
	public static String httpServletRequestUrl(@Nonnull HttpServletRequest httpServletRequest) {
		requireNonNull(httpServletRequest);
		return httpServletRequest.getQueryString() == null ? httpServletRequest.getPathInfo()
				: format("%s?%s", httpServletRequest.getPathInfo(), httpServletRequest.getQueryString());
	}

	@Nonnull
	public static String httpServletRequestDescription(@Nonnull HttpServletRequest httpServletRequest) {
		requireNonNull(httpServletRequest);
		String url = httpServletRequestUrl(httpServletRequest);
		return format("%s %s", httpServletRequest.getMethod(), url);
	}

	/**
	 * Tries query parameter value, then header, then cookie.
	 */
	@Nonnull
	public static Optional<String> extractValueFromRequest(@Nonnull HttpServletRequest httpServletRequest,
																												 @Nonnull String name) {
		requireNonNull(httpServletRequest);
		requireNonNull(name);

		name = trimToNull(name);

		if (name == null)
			return Optional.empty();

		// Try query parameter
		String value = trimToNull(httpServletRequest.getParameter(name));

		if (value != null)
			return Optional.of(value);

		// Try header
		value = trimToNull(httpServletRequest.getHeader(name));

		if (value != null)
			return Optional.of(value);

		// Try cookie
		Cookie[] cookies = httpServletRequest.getCookies();

		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (name.equals(cookie.getName())) {
					value = trimToNull(cookie.getValue());

					if (value != null)
						return Optional.of(value);
				}
			}
		}

		return Optional.empty();
	}

	@Nonnull
	public static String urlSafeRepresentation(@Nonnull String urlComponent, @Nonnull Locale locale) {
		requireNonNull(urlComponent);
		requireNonNull(locale);

		urlComponent = trimToNull(urlComponent);

		if (urlComponent == null)
			throw new IllegalArgumentException("URL component cannot be blank");

		urlComponent = urlComponent.toLowerCase(locale);

		// Condense multiple spaces into a single spaces
		urlComponent = MULTILPLE_WHITESPACE_PATTERN.matcher(urlComponent).replaceAll(" ");

		// Replace whitespace with "-"
		urlComponent = WHITESPACE_PATTERN.matcher(urlComponent).replaceAll("-");

		// Replace a few special characters
		urlComponent = urlComponent.replace("_", "-").replace("(", "").replace(")", "");

		// Get rid of any other characters
		urlComponent = DISALLOWED_CHARACTERS_PATTERN.matcher(urlComponent).replaceAll("");

		// Condense multiple hyphens into a single hyphen
		urlComponent = MULTILPLE_HYPHEN_PATTERN.matcher(urlComponent).replaceAll("-");

		// Remove leading and trailing hyphens, if present
		if (urlComponent.startsWith("-"))
			urlComponent = urlComponent.substring(1);

		if (urlComponent.endsWith("-"))
			urlComponent = urlComponent.substring(0, urlComponent.length() - 1);

		urlComponent = StringUtility.stripAccents(urlComponent);

		return urlComponent;
	}

	/**
	 * Performs an improved version of the URL encoding provided by
	 * {@code URLEncoder#encode}. Thanks to
	 * http://stackoverflow.com/questions/14321873/java-url-encoding-urlencoder-
	 * vs-uri
	 *
	 * @param urlComponent The value to URL-encode
	 * @return An encoded representation of {@code urlComponent}
	 */
	@Nonnull
	public static String urlEncode(@Nonnull String urlComponent) {
		requireNonNull(urlComponent);

		try {
			return URLEncoder.encode(urlComponent, "UTF-8").replaceAll("\\+", "%20").replaceAll("\\%21", "!")
					.replaceAll("\\%27", "'").replaceAll("\\%28", "(").replaceAll("\\%29", ")")
					.replaceAll("\\%7E", "~");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("This platform does not support UTF-8");
		}
	}

	@Nonnull
	public static String urlEncode(Map<?, ?> map) {
		requireNonNull(map);

		StringBuilder sb = new StringBuilder();

		for (Entry<?, ?> entry : map.entrySet()) {
			if (sb.length() > 0)
				sb.append("&");

			sb.append(format("%s=%s",
					urlEncode(entry.getKey().toString()),
					urlEncode(entry.getValue() == null ? "" : entry.getValue().toString())
			));
		}

		return sb.toString();
	}

	/**
	 * Converts a URL-encoded string to a plaintext string.
	 * <p>
	 * Assumes {@code input} is in {@code UTF-8} format.
	 *
	 * @param input The URL-encoded string to decode.
	 * @return The plaintext version of the URL-encoded string.
	 */
	@Nonnull
	public static String urlDecode(String input) {
		if (input == null)
			return null;
		try {
			return input == null ? null : decode(input, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Cannot perform URL decoding", e);
		}
	}

	@Nonnull
	public static List<FileItem> parseMultipartRequest(@Nonnull HttpServletRequest httpServletRequest) {
		requireNonNull(httpServletRequest);

		DiskFileItemFactory factory = new DiskFileItemFactory();

		ServletContext servletContext = httpServletRequest.getServletContext();
		File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
		factory.setRepository(repository);

		ServletFileUpload upload = new ServletFileUpload(factory);

		try {
			return upload.parseRequest(httpServletRequest);
		} catch (FileUploadException e) {
			throw new RuntimeException("Unable to parse multipart request", e);
		}
	}
}