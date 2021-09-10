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

import com.soklet.json.JSONArray;
import com.soklet.json.JSONException;
import com.soklet.json.JSONObject;
import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * @author Transmogrify LLC.
 */
public class HttpLoggingInterceptor implements Interceptor {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private final Logger logger;

	public HttpLoggingInterceptor() {
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public Response intercept(Interceptor.Chain chain) throws IOException {
		final String tag = format("%s:%s ", HttpLoggingInterceptor.class.getSimpleName(), UUID.randomUUID().toString().substring(0, 4));

		Request request = chain.request();

		boolean logHeaders = true;

		RequestBody requestBody = request.body();
		boolean hasRequestBody = requestBody != null;

		Connection connection = chain.connection();
		Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
		String requestStartMessage = "--> " + request.method() + ' ' + request.url() + ' ' + protocol;

		logger.debug(tag + requestStartMessage);

		if (hasRequestBody) {
			// Request body headers are only present when installed as a network interceptor. Force
			// them to be included (when available) so there values are known.
			if (requestBody.contentType() != null) {
				logger.debug( format(tag + "Content-Type: %s", requestBody.contentType()));
			}
			if (requestBody.contentLength() != -1) {
				logger.debug( format(tag + "Content-Length: %s", requestBody.contentLength()));
			}
		}

		Headers headers = request.headers();
		StringBuilder headerBuilder = new StringBuilder();
		for (int i = 0, count = headers.size(); i < count; i++) {
			String name = headers.name(i);
			// Skip headers from the request body as they are explicitly logged above.
			if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
				headerBuilder.append(format("(%s : %s)", name, headers.value(i)));
			}
		}
		logger.debug(format(tag +"Headers: %s", headerBuilder.toString()));

		if (!hasRequestBody) {
			logger.debug(tag + tag + "--> END %s", request.method());
		} else if (bodyEncoded(request.headers())) {
			
			logger.debug(tag + "--> END %s (encoded body omitted)", request.method());
		} else {
			Buffer buffer = new Buffer();
			requestBody.writeTo(buffer);

			Charset charset = UTF8;
			MediaType contentType = requestBody.contentType();
			if (contentType != null) {
				charset = contentType.charset(UTF8);
			}
			
			logger.debug(tag + "");

			String body = extractBody(buffer, charset);
			
			logger.debug(tag + body);

			
			logger.debug(format(tag + "--> END %s (%s-byte body)", request.method(), requestBody.contentLength()));
		}

		long startNs = System.nanoTime();
		Response response = chain.proceed(request);
		long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

		ResponseBody responseBody = response.body();
		long contentLength = responseBody.contentLength();
		String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
		

		logger.debug(tag + "<-- " + response.code() + ' ' + response.message() + ' ' + response.request().url() + " (" + tookMs + "ms" + ("") + ')');

		headerBuilder = new StringBuilder();
		for (int i = 0, count = headers.size(); i < count; i++) {
			headerBuilder.append(format("(%s: %s)", headers.name(i), headers.value(i)));
		}
		
		logger.debug(format(tag + "Headers: %s", headerBuilder.toString()));

		if (!HttpHeaders.hasBody(response)) {
			
			logger.debug(tag + "<-- END HTTP");
		} else if (bodyEncoded(response.headers())) {
			
			logger.debug(tag + "<-- END HTTP (encoded body omitted)");
		} else {
			BufferedSource source = responseBody.source();
			source.request(Long.MAX_VALUE); // Buffer the entire body.
			Buffer buffer = source.buffer();

			Charset charset = UTF8;
			MediaType contentType = responseBody.contentType();
			if (contentType != null) {
				try {
					charset = contentType.charset(UTF8);
				} catch (UnsupportedCharsetException e) {
					
					logger.debug(tag + "");
					
					logger.warn(tag + "Couldn't decode the response body; charset is likely malformed.");
					
					logger.debug(tag + "<-- END HTTP");

					return response;
				}
			}

			if (contentLength != 0) {
				
				logger.debug(tag + "");

				String body = extractBody(buffer.clone(), charset);

				
				logger.debug(tag + body);
			}
			
			logger.debug(format(tag + "<-- END HTTP (%s-byte body)", buffer.size()));
		}

		return response;

	}

	private boolean bodyEncoded(Headers headers) {
		String contentEncoding = headers.get("Content-Encoding");
		return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
	}

	private String extractBody(Buffer buffer, Charset charset) {

		String body = buffer.readString(charset);
		try {
			body = new JSONObject(body).toString(2);
		} catch (JSONException e) {
			try {
				JSONArray array = new JSONArray(body);
				body = format("%s items: %s", array.length(), array.toString(2));
			} catch (JSONException e2) {
				if (body.length() >= 4 && body.substring(1, 4).equals("PNG")) {
					body = "PNG IMAGE, skipping log...";
				} else {
					if (body.length() > 1000) {
						body = "[NON JSON BODY]".concat(body).substring(0, 1000);
					} else {
						body = "[NON JSON BODY]".concat(body);
					}
				}
			}
		}
		return body;
	}
}
