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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import java.security.spec.X509EncodedKeySpec;
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
		return generateKeyPair(null, null);
	}

	@Nonnull
	public static KeyPair generateKeyPair(@Nullable String algorithm,
																				@Nullable Integer keySize) {
		if (algorithm == null)
			algorithm = "RSA";

		if (keySize == null)
			keySize = 4096;

		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			keyPairGenerator.initialize(keySize, secureRandom);
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
	public static PublicKey toPublicKey(@Nonnull String publicKeyAsString) {
		requireNonNull(publicKeyAsString);

		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyAsString));
			return keyFactory.generatePublic(x509EncodedKeySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
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
		return Base64.getUrlEncoder().withoutPadding().encodeToString(sha1Thumbprint(x509Certificate));
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
		return Base64.getUrlEncoder().withoutPadding().encodeToString(toBytesUnsigned(toRSAPublicKey(publicKey).getPublicExponent()));
	}

	@Nonnull
	public static String modulusBase64UrlRepresentation(@Nonnull PublicKey publicKey) {
		requireNonNull(publicKey);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(toBytesUnsigned(toRSAPublicKey(publicKey).getModulus()));
	}

	@Nonnull
	private static RSAPublicKey toRSAPublicKey(@Nonnull PublicKey publicKey) {
		requireNonNull(publicKey);

		if (!(publicKey instanceof RSAPublicKey))
			throw new IllegalArgumentException(format("Public key is not an instance of %s", RSAPublicKey.class.getSimpleName()));

		return (RSAPublicKey) publicKey;
	}

	@Nonnull
	private static byte[] toBytesUnsigned(@Nonnull BigInteger bigInteger) {
		requireNonNull(bigInteger);

		// Copied from Apache Commons Codec 1.8

		int bitlen = bigInteger.bitLength();

		// round bitlen
		bitlen = ((bitlen + 7) >> 3) << 3;
		final byte[] bigBytes = bigInteger.toByteArray();

		if (((bigInteger.bitLength() % 8) != 0) && (((bigInteger.bitLength() / 8) + 1) == (bitlen / 8))) {
			return bigBytes;
		}

		// set up params for copying everything but sign bit
		int startSrc = 0;
		int len = bigBytes.length;

		// if bigInt is exactly byte-aligned, just skip signbit in copy
		if ((bigInteger.bitLength() % 8) == 0) {
			startSrc = 1;
			len--;
		}

		final int startDst = bitlen / 8 - len; // to pad w/ nulls as per spec
		final byte[] resizedBytes = new byte[bitlen / 8];
		System.arraycopy(bigBytes, startSrc, resizedBytes, startDst, len);
		return resizedBytes;
	}
}