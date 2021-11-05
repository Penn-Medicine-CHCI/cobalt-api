package com.cobaltplatform.ic.backend.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import com.cobaltplatform.ic.backend.config.IcConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.cobaltplatform.ic.backend.exception.CobaltException;
import com.cobaltplatform.ic.backend.model.cobalt.CreateMpmPatientRequest;
import com.cobaltplatform.ic.backend.model.cobalt.CreateOrderReportPatientRequest;
import com.cobaltplatform.ic.backend.model.cobalt.CreatePatientResponse;
import com.cobaltplatform.ic.backend.model.cobalt.FindAccountResponse;
import com.cobaltplatform.ic.backend.model.cobalt.SendCallMessageRequest;
import com.cobaltplatform.ic.backend.model.cobalt.SendSmsMessageRequest;
import com.cobaltplatform.ic.model.IcRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
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
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class CobaltService {
	@Nonnull
	private static final String DEFAULT_SIGNING_TOKEN_SUBJECT;
	@Nonnull
	private static final String SIGNING_TOKEN_HEADER_NAME;
	@Nonnull
	private static final Long SIGNING_TOKEN_EXPIRATION_IN_SECONDS;
	@Nonnull
	private static final CobaltService SHARED_INSTANCE;
	@Nonnull
	private static final Gson GSON;
	@Nonnull
	private static final OkHttpClient OK_HTTP_CLIENT;
	@Nonnull
	private static final FhirContext FHIR_CONTEXT;

	@Nonnull
	private final String cobaltBackendBaseUrl;
	@Nonnull
	private final PrivateKey privateKey;
	@Nonnull
	private final Logger logger;

	static {
		DEFAULT_SIGNING_TOKEN_SUBJECT = "IC_SYSTEM";
		SIGNING_TOKEN_HEADER_NAME = "X-IC-Signing-Token";
		SIGNING_TOKEN_EXPIRATION_IN_SECONDS = 60 * 10L;
		GSON = createGson();

		// Safe to cache - threadsafe/expensive to create: https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-base/ca/uhn/fhir/context/FhirContext.html
		FHIR_CONTEXT = FhirContext.forCached(FhirVersionEnum.DSTU3);

		OK_HTTP_CLIENT = new OkHttpClient.Builder()
				.connectTimeout(10, TimeUnit.SECONDS)
				.writeTimeout(20, TimeUnit.SECONDS)
				.readTimeout(20, TimeUnit.SECONDS)
				.build();

		SHARED_INSTANCE = new CobaltService(IcConfig.getCobaltBackendBaseUrl(), IcConfig.getKeyPair().getPrivate());
	}

	public CobaltService(@Nonnull String cobaltBackendBaseUrl,
											 @Nonnull PrivateKey privateKey) {
		requireNonNull(cobaltBackendBaseUrl);
		requireNonNull(privateKey);

		this.cobaltBackendBaseUrl = cobaltBackendBaseUrl;
		this.privateKey = privateKey;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Boolean verifyCobaltSigningToken(@Nullable String cobaltSigningToken) {
		requireNonNull(cobaltSigningToken);

		cobaltSigningToken = trimToEmpty(cobaltSigningToken);

		try {
			Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(IcRole.getCobaltPublicKey()).build().parseClaimsJws(cobaltSigningToken);
			Date expirationAsDate = claims.getBody().getExpiration();

			if (expirationAsDate == null) {
				getLogger().warn(format("Cobalt signing token is missing an expiry: %s", cobaltSigningToken));
			} else {
				Instant expiration = expirationAsDate.toInstant();

				if (Instant.now().isAfter(expiration)) {
					getLogger().warn(format("Cobalt signing token has expired: %s", cobaltSigningToken));
				} else {
					return true;
				}
			}
		} catch (UnsupportedJwtException e) {
			getLogger().warn(format("Cobalt signing token is unsupported: %s", cobaltSigningToken), e);
		} catch (ExpiredJwtException e) {
			getLogger().warn(format("Cobalt signing token has expired: %s", cobaltSigningToken), e);
		} catch (Exception e) {
			getLogger().warn(format("Cobalt signing token could not be processed: %s", cobaltSigningToken), e);
		}

		return false;
	}

	@Nonnull
	public Optional<FindAccountResponse> findAccount(@Nonnull UUID cobaltAccountId) throws CobaltException {
		requireNonNull(cobaltAccountId);

		return callCobalt(HttpMethod.GET, format("/accounts/ic/%s", cobaltAccountId), null, FindAccountResponse.class);
	}

	@Nonnull
	public CreatePatientResponse createPatient(@Nonnull CreateMpmPatientRequest request) throws CobaltException {
		requireNonNull(request);

		String patientJson = getFhirContext().newJsonParser().encodeResourceToString(request.getPatient());

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("patient", getGson().fromJson(patientJson, Map.class));

		return callCobalt(HttpMethod.POST,"/accounts/ic/mpm", requestBody, CreatePatientResponse.class).get();
	}

	@Nonnull
	public CreatePatientResponse createPatient(@Nonnull CreateOrderReportPatientRequest request) throws CobaltException {
		requireNonNull(request);
		return callCobalt(HttpMethod.POST,"/accounts/ic/order-report", request, CreatePatientResponse.class).get();
	}

	public void sendSmsMessage(@Nonnull SendSmsMessageRequest request) throws CobaltException {
		requireNonNull(request);
		sendSmsMessages(List.of(request));
	}

	public void sendSmsMessages(@Nonnull List<SendSmsMessageRequest> requests) throws CobaltException {
		requireNonNull(requests);

		Map<String, Object> json = new HashMap<>();
		json.put("smsMessages", requests);

		callCobalt(HttpMethod.POST,"/messages/sms/send", json);
	}

	public void sendCallMessage(@Nonnull SendCallMessageRequest request) throws CobaltException {
		requireNonNull(request);
		sendCallMessages(List.of(request));
	}

	public void sendCallMessages(@Nonnull List<SendCallMessageRequest> requests) throws CobaltException {
		requireNonNull(requests);

		Map<String, Object> json = new HashMap<>();
		json.put("callMessages", requests);

		callCobalt(HttpMethod.POST, "/messages/call/send", json);
	}

	protected enum HttpMethod {
		GET,
		POST
	}

	@Nonnull
	protected <T> Optional<T> callCobalt(@Nonnull HttpMethod httpMethod,
																			 @Nonnull String relativeUrl) throws CobaltException {
		requireNonNull(relativeUrl);
		return callCobalt(httpMethod, relativeUrl, null, null);
	}

	@Nonnull
	protected <T> Optional<T> callCobalt(@Nonnull HttpMethod httpMethod,
																			 @Nonnull String relativeUrl,
																			 @Nullable Object requestBody) throws CobaltException {
		requireNonNull(relativeUrl);
		return callCobalt(httpMethod, relativeUrl, requestBody, null);
	}

	@Nonnull
	protected <T> Optional<T> callCobalt(@Nonnull HttpMethod httpMethod,
																			 @Nonnull String relativeUrl,
																			 @Nullable Object requestBody,
																			 @Nullable Class<T> responseClass) throws CobaltException {
		requireNonNull(httpMethod);
		requireNonNull(relativeUrl);

		MediaType mediaType = MediaType.get("application/json; charset=utf-8");

		Request.Builder requestBuilder = new Request.Builder()
				.header(getSigningTokenHeaderName(), generateSigningToken())
				.url(format("%s%s", getCobaltBackendBaseUrl(), relativeUrl));

		if(httpMethod == HttpMethod.GET)
			requestBuilder.get();
		else if(httpMethod == HttpMethod.POST)
			requestBuilder.post(requestBody == null ? null : RequestBody.create(getGson().toJson(requestBody), mediaType));
		else
			throw new UnsupportedOperationException(format("We don't support %s.%s yet", HttpMethod.class.getSimpleName(), httpMethod.name()));

		Request httpRequest = requestBuilder.build();

		try {
			try (Response httpResponse = getOkHttpClient().newCall(httpRequest).execute()) {
				String responseBody = httpResponse.body().string();

				if (httpResponse.code() >= 400)
					throw new CobaltException(format("Cobalt responded with HTTP status %d and response body:\n%s", httpResponse.code(), responseBody));

				if (responseClass == null)
					return Optional.empty();

				return Optional.of(getGson().fromJson(responseBody, responseClass));
			}
		} catch (IOException e) {
			throw new CobaltException(e);
		}
	}

	@Nonnull
	protected String generateSigningToken() {
		Instant now = Instant.now();
		Instant expiration = now.plus(getSigningTokenExpirationInSeconds(), ChronoUnit.SECONDS);

		return Jwts.builder()
				.setSubject(getDefaultSigningTokenSubject())
				.setIssuedAt(Date.from(now))
				.setNotBefore(Date.from(now))
				.setExpiration(Date.from(expiration))
				.addClaims(new HashMap<String, Object>() {{
					put("nonce", generateNonce());
				}})
				.signWith(getPrivateKey())
				.compact();
	}

	@Nonnull
	private static Gson createGson() {
		return new GsonBuilder()
				.registerTypeAdapter(Instant.class, new JsonDeserializer<Instant>() {
					@Override
					@Nullable
					public Instant deserialize(@Nullable JsonElement json,
																		 @Nonnull Type type,
																		 @Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
						requireNonNull(type);
						requireNonNull(jsonDeserializationContext);

						if (json == null)
							return null;

						JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

						if (jsonPrimitive.isNumber())
							return Instant.ofEpochMilli(json.getAsLong());

						if (jsonPrimitive.isString())
							return Instant.parse(json.getAsString());

						throw new IllegalArgumentException(format("Unable to convert JSON value '%s' to %s", json, type));
					}
				}).registerTypeAdapter(Instant.class, new JsonSerializer<Instant>() {
					@Override
					@Nullable
					public JsonElement serialize(@Nullable Instant instant,
																			 @Nonnull Type type,
																			 @Nonnull JsonSerializationContext jsonSerializationContext) {
						requireNonNull(type);
						requireNonNull(jsonSerializationContext);

						return instant == null ? null : new JsonPrimitive(instant.toString());
					}
				}).create();
	}

	@Nonnull
	protected String generateNonce() {
		return format("%s-%s", Instant.now().toEpochMilli(), UUID.randomUUID());
	}

	@Nonnull
	public static CobaltService getSharedInstance() {
		return SHARED_INSTANCE;
	}

	@Nonnull
	protected String getSigningTokenHeaderName() {
		return SIGNING_TOKEN_HEADER_NAME;
	}

	@Nonnull
	protected String getDefaultSigningTokenSubject() {
		return DEFAULT_SIGNING_TOKEN_SUBJECT;
	}

	@Nonnull
	protected Long getSigningTokenExpirationInSeconds() {
		return SIGNING_TOKEN_EXPIRATION_IN_SECONDS;
	}

	@Nonnull
	protected Gson getGson() {
		return GSON;
	}

	@Nonnull
	protected OkHttpClient getOkHttpClient() {
		return OK_HTTP_CLIENT;
	}

	@Nonnull
	protected FhirContext getFhirContext() {
		return FHIR_CONTEXT;
	}

	@Nonnull
	protected String getCobaltBackendBaseUrl() {
		return cobaltBackendBaseUrl;
	}

	@Nonnull
	protected PrivateKey getPrivateKey() {
		return privateKey;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
