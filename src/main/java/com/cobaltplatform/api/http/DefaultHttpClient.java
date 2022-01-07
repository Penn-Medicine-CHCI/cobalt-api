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

package com.cobaltplatform.api.http;

import com.cobaltplatform.api.util.WebUtility;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify LLC.
 */
@ThreadSafe
public class DefaultHttpClient implements HttpClient {
	@Nonnull
	private static final Set<HttpMethod> UNSUPPORTED_HTTP_METHODS;
	@Nonnull
	private static final RequestBody EMPTY_REQUEST_BODY;
	@Nonnull
	private static final AtomicLong SEQUENTIAL_IDENTIFIER;

	@Nonnull
	private final OkHttpClient okHttpClient;
	@Nonnull
	private final Logger httpLogger;
	@Nonnull
	private final Logger httpHeaderLogger;
	@Nonnull
	private final Logger httpRequestBodyLogger;
	@Nonnull
	private final Logger httpResponseBodyLogger;
	@Nonnull
	private final Logger httpTimingLogger;

	static {
		UNSUPPORTED_HTTP_METHODS = Collections.unmodifiableSet(new HashSet<HttpMethod>() {{
			add(HttpMethod.HEAD);
			add(HttpMethod.OPTIONS);
		}});

		EMPTY_REQUEST_BODY = RequestBody.create(new byte[]{});
		SEQUENTIAL_IDENTIFIER = new AtomicLong();
	}

	public DefaultHttpClient() {
		this(DefaultHttpClient.class.getPackage().getName());
	}

	public DefaultHttpClient(@Nonnull String loggingBaseName) {
		this(loggingBaseName, false);
	}

	public DefaultHttpClient(@Nonnull String loggingBaseName,
													 @Nonnull Boolean permitUnsafeCerts) {
		requireNonNull(loggingBaseName);
		requireNonNull(permitUnsafeCerts);

		OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
				.connectTimeout(30, TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS);

		if (permitUnsafeCerts) {
			X509TrustManager[] trustManagers = new X509TrustManager[]{new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				@Override
				public void checkServerTrusted(final X509Certificate[] chain,
																			 final String authType) throws CertificateException {
					// No-op
				}

				@Override
				public void checkClientTrusted(final X509Certificate[] chain,
																			 final String authType) throws CertificateException {
					// No-op
				}
			}};

			try {
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, trustManagers, new java.security.SecureRandom());
				okHttpClientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManagers[0]);

				okHttpClientBuilder.hostnameVerifier(new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				});
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		this.okHttpClient = okHttpClientBuilder.build();

		this.httpLogger = LoggerFactory.getLogger(format("%s.HTTP", loggingBaseName));
		this.httpHeaderLogger = LoggerFactory.getLogger(format("%s.HTTP_HEADER", loggingBaseName));
		this.httpRequestBodyLogger = LoggerFactory.getLogger(format("%s.HTTP_REQUEST_BODY", loggingBaseName));
		this.httpResponseBodyLogger = LoggerFactory.getLogger(format("%s.HTTP_RESPONSE_BODY", loggingBaseName));
		this.httpTimingLogger = LoggerFactory.getLogger(format("%s.HTTP_TIMING", loggingBaseName));
	}

	@Override
	@Nonnull
	public HttpResponse execute(@Nonnull HttpRequest httpRequest,
															@Nullable HttpRequestOption... httpRequestOptions) throws IOException {
		Set<HttpRequestOption> httpRequestOptionsAsSet = httpRequestOptions == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(httpRequestOptions));
		HttpMethod httpMethod = httpRequest.getHttpMethod();

		if (UNSUPPORTED_HTTP_METHODS.contains(httpMethod))
			throw new UnsupportedOperationException(format("Sorry, this client does not support the %s method.",
					httpMethod.name()));

		String url = httpRequest.getUrl();

		if (httpRequest.getQueryParameters().size() > 0) {
			StringBuilder queryString = new StringBuilder();

			for (Entry<String, Object> entry : httpRequest.getQueryParameters().entrySet()) {
				String name = entry.getKey();
				String value = entry.getValue() == null ? null : trimToNull(entry.getValue().toString());

				if (queryString.length() == 0)
					queryString.append("?");
				else
					queryString.append("&");

				queryString.append(WebUtility.urlEncode(name));
				queryString.append("=");

				if (value != null)
					queryString.append(WebUtility.urlEncode(value));
			}

			url = url + queryString;
		}

		Request.Builder requestBuilder = new Request.Builder().url(url);

		if (httpRequest.getBody().isPresent()) {
			MediaType mediaType = MediaType.parse(httpRequest.getContentType().get());
			RequestBody requestBody = RequestBody.create(httpRequest.getBody().get(), mediaType);

			if (httpRequest.getHttpMethod() == HttpMethod.POST)
				requestBuilder.post(requestBody);
			else if (httpRequest.getHttpMethod() == HttpMethod.PUT)
				requestBuilder.put(requestBody);
			else if (httpRequest.getHttpMethod() == HttpMethod.PATCH)
				requestBuilder.patch(requestBody);
			else if (httpRequest.getHttpMethod() == HttpMethod.DELETE)
				requestBuilder.delete(requestBody);
			else
				throw new IllegalStateException(format("Unexpected method encountered: %s", httpMethod.name()));
		} else {
			if (httpRequest.getHttpMethod() == HttpMethod.GET)
				requestBuilder.get();
			else if (httpRequest.getHttpMethod() == HttpMethod.POST)
				requestBuilder.post(EMPTY_REQUEST_BODY);
			else if (httpRequest.getHttpMethod() == HttpMethod.PUT)
				requestBuilder.put(EMPTY_REQUEST_BODY);
			else if (httpRequest.getHttpMethod() == HttpMethod.PATCH)
				requestBuilder.patch(EMPTY_REQUEST_BODY);
			else if (httpRequest.getHttpMethod() == HttpMethod.DELETE)
				requestBuilder.delete();
		}

		for (Entry<String, Object> header : httpRequest.getHeaders().entrySet()) {
			String name = header.getKey();
			String value = header.getValue() == null ? null : trimToNull(header.getValue().toString());
			requestBuilder.header(name, value);
		}

		Request request = requestBuilder.build();
		Long requestIdentifier = SEQUENTIAL_IDENTIFIER.incrementAndGet();

		getHttpLogger().debug("[{}]: Executing {} {}", requestIdentifier, httpRequest.getHttpMethod().name(), url);

		if (httpRequest.getHeaders().size() > 0)
			getHttpHeaderLogger().debug("[{}]: Request headers: {}", requestIdentifier, httpRequest.getHeaders());

		if (getHttpRequestBodyLogger().isDebugEnabled()
				&& httpRequest.getBody().isPresent()
				&& !httpRequestOptionsAsSet.contains(HttpRequestOption.SUPPRESS_REQUEST_BODY_LOGGING)) {
			String requestBodyString = new String(httpRequest.getBody().get(), UTF_8);
			getHttpRequestBodyLogger().debug("[{}]: Request body:\n{}", requestIdentifier, requestBodyString);
		}

		long time = System.nanoTime();

		Response response = getOkHttpClient().newCall(request).execute();

		time = System.nanoTime() - time;
		getHttpTimingLogger().debug(format("[%s]: Request completed in %.1fms.", requestIdentifier, time / (double) 1000000));

		int responseCode = response.code();
		byte[] responseBodyBytes = response.body().bytes();

		getHttpLogger().debug("[{}]: Response status was {}.", requestIdentifier, responseCode);

		if (!httpRequestOptionsAsSet.contains(HttpRequestOption.SUPPRESS_RESPONSE_BODY_LOGGING)) {
			String responseBodyString = responseBodyBytes == null ? null : new String(responseBodyBytes, UTF_8);

			if (responseBodyString != null && responseBodyString.length() > 0)
				getHttpResponseBodyLogger().debug("[{}]: Response body:\n{}", requestIdentifier, responseBodyString);
		}

		return new HttpResponse(responseCode, responseBodyBytes);
	}

	@Nonnull
	protected OkHttpClient getOkHttpClient() {
		return okHttpClient;
	}

	@Nonnull
	protected Logger getHttpLogger() {
		return httpLogger;
	}

	@Nonnull
	protected Logger getHttpHeaderLogger() {
		return httpHeaderLogger;
	}

	@Nonnull
	protected Logger getHttpRequestBodyLogger() {
		return httpRequestBodyLogger;
	}

	@Nonnull
	protected Logger getHttpResponseBodyLogger() {
		return httpResponseBodyLogger;
	}

	@Nonnull
	protected Logger getHttpTimingLogger() {
		return httpTimingLogger;
	}
}