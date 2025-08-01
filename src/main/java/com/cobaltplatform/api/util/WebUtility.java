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
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Iterator;
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

	@Nonnull
	public static Optional<String> normalizedHostnameForUrl(@Nullable String url) {
		url = StringUtils.trimToEmpty(url).toLowerCase(Locale.US);

		if (url.length() == 0)
			return Optional.empty();

		// Save off for error messaging (if necessary)
		String originalUrl = url;

		// Discard protocol
		if (url.startsWith("https://"))
			url = url.substring("https://".length());
		else if (url.startsWith("http://"))
			url = url.substring("http://".length());

		// Discard query string
		int queryStringSeparator = url.indexOf("?");

		if (queryStringSeparator != -1)
			url = url.substring(0, queryStringSeparator);

		// Discard trailing slashes
		while (url.length() > 0 && url.endsWith("/"))
			url = url.substring(0, url.length() - 1);

		// Discard trailing port number
		int portNumberSeparator = url.indexOf(":");

		if (portNumberSeparator != -1)
			url = url.substring(0, portNumberSeparator);

		if (url.length() == 0)
			return Optional.empty();

		return Optional.of(url);
	}

	@Nonnull
	public static String elideSensitiveDataInUrl(@Nonnull String url) {
		requireNonNull(url);

		url = trimToNull(url);

		if (url == null || (!url.startsWith("http://") && !url.startsWith("https://")))
			return url;

		// Turns
		// https://example.cobalt.care/auth?accessToken=eyJhbG...abc
		// into
		// https://example.cobalt.care/auth?accessToken=(elided)
		int authAccessTokenIndex = url.indexOf("/auth?accessToken=");

		if (authAccessTokenIndex != -1)
			url = url.substring(0, authAccessTokenIndex) + "/auth?accessToken=(elided)";

		return url;
	}

	@Nonnull
	public static Boolean urlNameContainsIllegalCharacters(@Nonnull String urlName) {
		requireNonNull(urlName);
		// Alphanumerics and hyphens only
		return !urlName.matches("[-\\pL\\pN]+");
	}

	@Nonnull
	public static Optional<String> normalizeUrlName(@Nullable String urlName) {
		urlName = trimToNull(urlName);

		if (urlName == null)
			return Optional.empty();

		return Optional.ofNullable(urlName.toLowerCase(Locale.ENGLISH)
				// All groups of whitespace characters are converted to a single '-'
				.replaceAll("\\p{Zs}+", "-")
				// Anything that's not alphanumeric or a hyphen is discarded
				.replaceAll("[^-\\pL\\pN]", ""));
	}

	/**
	 * Returns a new URL string formed by adding the given query parameters to the inputUrl.
	 * Existing parameters (if any) are preserved, and new ones are URL‑encoded and appended.
	 *
	 * @param inputUrl        the absolute URL to which parameters should be added
	 * @param queryParameters the map of query‑parameter names and values
	 * @return a new URL string with the parameters added
	 * @throws IllegalArgumentException if inputUrl is not a valid URI or encoding fails
	 */
	@Nonnull
	public static String appendQueryParameters(@Nonnull String inputUrl,
																						 @Nullable Map<String, String> queryParameters) {
		requireNonNull(inputUrl);

		if (queryParameters == null || queryParameters.isEmpty())
			return inputUrl;

		try {
			URI uri = new URI(inputUrl);
			String existingQuery = uri.getQuery();

			// Build up the new query string
			StringBuilder newQuery = new StringBuilder();

			if (existingQuery != null && !existingQuery.isEmpty())
				newQuery.append(existingQuery).append('&');

			Iterator<Entry<String, String>> iter = queryParameters.entrySet().iterator();

			while (iter.hasNext()) {
				Map.Entry<String, String> entry = iter.next();
				newQuery
						.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
						.append('=')
						.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
				if (iter.hasNext()) {
					newQuery.append('&');
				}
			}

			// Reconstruct the URI with the updated query (preserving fragment, if any)
			URI updated = new URI(
					uri.getScheme(),
					uri.getAuthority(),
					uri.getPath(),
					newQuery.toString(),
					uri.getFragment()
			);

			return updated.toString();
		} catch (URISyntaxException | UnsupportedEncodingException e) {
			throw new IllegalArgumentException(format("Failed to append query parameters to URL: %s", inputUrl), e);
		}
	}
}