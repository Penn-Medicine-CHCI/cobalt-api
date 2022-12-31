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

import com.google.common.io.BaseEncoding;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class CryptoUtility {
	private CryptoUtility() {
		// Non-instantiable
	}

	@Nonnull
	public static String generateNonce() {
		// Include timestamp for sanity checking
		return format("%s-%s", Instant.now().toEpochMilli(), UUID.randomUUID());
	}

	/**
	 * @param algorithm For example, {@code HmacSHA512}
	 */
	@Nonnull
	public static String generateSecretKeyInBase64(@Nonnull String algorithm) {
		requireNonNull(algorithm);

		SecretKey secretKey;

		try {
			secretKey = KeyGenerator.getInstance(algorithm).generateKey();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Unable to generate secret key", e);
		}

		return Base64.getEncoder().encodeToString(secretKey.getEncoded());
	}

	/**
	 * @param secretKeyInBase64 A base64-encoded representation of secret key bytes
	 * @param algorithm         For example, {@code HmacSHA512}
	 */
	@Nonnull
	public static SecretKey loadSecretKeyInBase64(@Nonnull String secretKeyInBase64,
																								@Nonnull String algorithm) {
		requireNonNull(secretKeyInBase64);
		requireNonNull(algorithm);

		byte[] decodedKey = Base64.getDecoder().decode(secretKeyInBase64);
		return new SecretKeySpec(decodedKey, 0, decodedKey.length, algorithm);
	}

	@Nonnull
	public static KeyPair generateKeyPair() {
		return generateKeyPair(null);
	}

	@Nonnull
	public static KeyPair generateKeyPair(@Nullable String algorithm) {
		if (algorithm == null)
			algorithm = "RSA";

		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			keyPairGenerator.initialize(4096, secureRandom);
			return keyPairGenerator.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	@Nonnull
	public static X509Certificate toX509Certificate(@Nonnull String x509CertificateAsString) {
		requireNonNull(x509CertificateAsString);

		x509CertificateAsString = x509CertificateAsString.trim();

		try (InputStream is = new ByteArrayInputStream(x509CertificateAsString.getBytes(StandardCharsets.UTF_8))) {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			return (X509Certificate) certificateFactory.generateCertificate(is);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (CertificateException e) {
			throw new RuntimeException(e);
		}
	}

	@Nonnull
	public static PrivateKey toPrivateKey(@Nonnull String privateKeyAsString) {
		requireNonNull(privateKeyAsString);

		// Remove header/footer
		privateKeyAsString = privateKeyAsString.replace("-----BEGIN PRIVATE KEY-----", "");
		privateKeyAsString = privateKeyAsString.replace("-----END PRIVATE KEY-----", "");

		// Remove all whitespace
		privateKeyAsString = privateKeyAsString.replaceAll("\\s", "").trim();

		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyAsString));

		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePrivate(privateKeySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

	@Nonnull
	public static byte[] sha1Thumbprint(@Nonnull X509Certificate x509Certificate) {
		requireNonNull(x509Certificate);

		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
			messageDigest.update(x509Certificate.getEncoded());
			return messageDigest.digest();
		} catch (CertificateException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Nonnull
	public static String sha1ThumbprintHexRepresentation(@Nonnull X509Certificate x509Certificate) {
		requireNonNull(x509Certificate);
		// Output should match this openssl command (not including ':' characters)
		// % openssl x509 -in cobalt.epic.nonprod.crt -noout -fingerprint
		// SHA1 Fingerprint=F8:99:26:90:36:B9:B4:D8:2A:DA:EE:DA:34:91:2F:EC:C2:93:11:65
		return HexFormat.of().formatHex(sha1Thumbprint(x509Certificate));
	}

	@Nonnull
	public static String sha1ThumbprintBase64UrlRepresentation(@Nonnull X509Certificate x509Certificate) {
		requireNonNull(x509Certificate);
		return BaseEncoding.base64Url().encode(sha1Thumbprint(x509Certificate));
	}

	@Nonnull
	public static String base64Representation(@Nonnull X509Certificate x509Certificate) {
		requireNonNull(x509Certificate);

		try {
			return Base64.getEncoder().encodeToString(x509Certificate.getEncoded());
		} catch (CertificateEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Nonnull
	public static String base64Representation(@Nonnull PublicKey publicKey) {
		requireNonNull(publicKey);
		return Base64.getEncoder().encodeToString(publicKey.getEncoded());
	}

	@Nonnull
	public static String base64Representation(@Nonnull PrivateKey privateKey) {
		requireNonNull(privateKey);
		return Base64.getEncoder().encodeToString(privateKey.getEncoded());
	}

	@Nonnull
	public static String exponentBase64UrlRepresentation(@Nonnull PublicKey publicKey) {
		requireNonNull(publicKey);
		return Base64.getUrlEncoder().encodeToString(toRSAPublicKey(publicKey).getPublicExponent().toByteArray());
	}

	@Nonnull
	public static String modulusBase64UrlRepresentation(@Nonnull PublicKey publicKey) {
		requireNonNull(publicKey);
		return Base64.getUrlEncoder().encodeToString(toRSAPublicKey(publicKey).getModulus().toByteArray());
	}

	@Nonnull
	private static RSAPublicKey toRSAPublicKey(@Nonnull PublicKey publicKey) {
		requireNonNull(publicKey);

		if (!(publicKey instanceof RSAPublicKey))
			throw new IllegalArgumentException(format("Public key is not an instance of %s", RSAPublicKey.class.getSimpleName()));

		return (RSAPublicKey) publicKey;
	}

	@Immutable
	public static class PublicKeyExponentModulus {
		@Nonnull
		private final PublicKey publicKey;
		@Nonnull
		private final BigInteger exponent;
		@Nonnull
		private final BigInteger modulus;
		@Nonnull
		private final String exponentInBase64;
		@Nonnull
		private final String modulusInBase64;

		public PublicKeyExponentModulus(@Nonnull X509Certificate x509Certificate) {
			this(requireNonNull(x509Certificate).getPublicKey());
		}

		public PublicKeyExponentModulus(@Nonnull PublicKey publicKey) {
			requireNonNull(publicKey);

			if (!(publicKey instanceof RSAPublicKey))
				throw new IllegalArgumentException(format("Public key is not an instance of %s", RSAPublicKey.class.getSimpleName()));

			RSAPublicKey rsaPublicKey = (RSAPublicKey) (publicKey);

			this.publicKey = publicKey;
			this.exponent = rsaPublicKey.getPublicExponent();
			this.modulus = rsaPublicKey.getModulus();
			this.exponentInBase64 = Base64.getUrlEncoder().encodeToString(this.exponent.toByteArray());
			this.modulusInBase64 = Base64.getUrlEncoder().encodeToString(this.modulus.toByteArray());
		}

		@Nonnull
		public PublicKey getPublicKey() {
			return this.publicKey;
		}

		@Nonnull
		public BigInteger getExponent() {
			return this.exponent;
		}

		@Nonnull
		public BigInteger getModulus() {
			return this.modulus;
		}

		@Nonnull
		public String getExponentInBase64() {
			return this.exponentInBase64;
		}

		@Nonnull
		public String getModulusInBase64() {
			return this.modulusInBase64;
		}
	}
}