package com.cobaltplatform.ic.model;

import com.cobaltplatform.ic.backend.config.IcConfig;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.cobaltplatform.ic.backend.model.cobalt.CobaltClaims;
import com.cobaltplatform.ic.backend.model.response.PublicKeyResponse;
import io.javalin.core.security.Role;
import io.javalin.plugin.json.JavalinJackson;
import io.jsonwebtoken.Jwts;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public enum IcRole implements Role {
	ANYONE("ANYONE"),
	PATIENT("PATIENT"),
	MHIC("MHIC"),
	PROVIDER("PROVIDER"), // Employee Cobalt role
	ADMINISTRATOR("ADMINISTRATOR"), // Employee Cobalt role
	SUPER_ADMINISTRATOR("SUPER_ADMINISTRATOR"), // Employee Cobalt role
	BHS("BHS"), // Employee Cobalt role
	;

	private final String name;


	IcRole(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	private static final OkHttpClient httpClient = new OkHttpClient();
	private static final Logger logger = LoggerFactory.getLogger(IcRole.class);

	private static final PublicKey publicKey;

	static {
		PublicKey temp = null;
		try {
			var getRequest = new Request.Builder().get().url(IcConfig.getCobaltBackendBaseUrl() + "/system/public-key")
					.addHeader(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString())
					.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
					.build();
			Response publicKeyResponse = httpClient.newCall(getRequest).execute();
			Validate.isTrue(publicKeyResponse.isSuccessful());
			var responseByteStream = requireNonNull(publicKeyResponse.body()).string();
			PublicKeyResponse cobaltKey = JavalinJackson.getObjectMapper().readValue(responseByteStream, PublicKeyResponse.class);
			temp = KeyFactory.getInstance(cobaltKey.getAlgorithm()).generatePublic(new X509EncodedKeySpec(cobaltKey.getPublicKey()));
		} catch (Exception e) {
			logger.error("Could not get public key", e);
		}
		publicKey = temp;
	}

	public static PublicKey getCobaltPublicKey() {
		return publicKey;
	}

	@Nonnull
	public static Optional<CobaltClaims> getCobaltClaimsFromJWT(@Nullable String accessToken) {
		accessToken = StringUtils.trimToNull(accessToken);

		if(accessToken == null)
		return Optional.empty();

		var parser = Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build();

		try {
			var claims = parser.parseClaimsJws(String.valueOf(accessToken)).getBody();
			UUID accountId = UUID.fromString(claims.getSubject());
			IcRole icRole = IcRole.fromName(claims.get("roleId", String.class));
			return Optional.of(new CobaltClaims(accountId, icRole));
		} catch (Exception e) {
			logger.warn("Failed to get role from JWT", e);
			return Optional.empty();
		}
	}

	public static IcRole fromName(String value) throws NoSuchElementException {
		return Arrays
				.stream(IcRole.values())
				.filter(v -> v.getName().equals(value))
				.findFirst()
				.orElseThrow(NoSuchElementException::new);
	}
}
