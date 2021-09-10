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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

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
	public static SecretKey loadSecretKeyInBase64(@Nonnull String secretKeyInBase64, @Nonnull String algorithm) {
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

	public enum KeyFormat {
		BASE64,
		BASE64_WITH_HEADER_AND_FOOTER
	}

	@Nonnull
	public static String stringRepresentation(@Nonnull PublicKey publicKey,
																						@Nonnull KeyFormat keyFormat) {
		requireNonNull(publicKey);
		requireNonNull(keyFormat);

		if (keyFormat == KeyFormat.BASE64_WITH_HEADER_AND_FOOTER) {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("-----BEGIN RSA PUBLIC KEY-----\n");
			stringBuilder.append(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
			stringBuilder.append("\n-----END RSA PUBLIC KEY-----\n");

			return stringBuilder.toString();
		} else if (keyFormat == KeyFormat.BASE64) {
			return Base64.getEncoder().encodeToString(publicKey.getEncoded());
		}

		throw new IllegalStateException(format("Unexpected value %s.%s", KeyFormat.class.getSimpleName(), keyFormat.name()));
	}

	@Nonnull
	public static String stringRepresentation(@Nonnull PrivateKey privateKey,
																						@Nonnull KeyFormat keyFormat) {
		requireNonNull(privateKey);
		requireNonNull(keyFormat);

		if (keyFormat == KeyFormat.BASE64_WITH_HEADER_AND_FOOTER) {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("-----BEGIN RSA PRIVATE KEY-----\n");
			// Private key format: PKCS#8
			stringBuilder.append(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
			stringBuilder.append("\n-----END RSA PRIVATE KEY-----\n");

			return stringBuilder.toString();
		} else if (keyFormat == KeyFormat.BASE64) {
			return Base64.getEncoder().encodeToString(privateKey.getEncoded());
		}

		throw new IllegalStateException(format("Unexpected value %s.%s", KeyFormat.class.getSimpleName(), keyFormat.name()));
	}

	@Nonnull
	public static KeyPair keyPairFromStringRepresentation(@Nonnull String publicKeyAsString,
																												@Nonnull String privateKeyAsString,
																												@Nonnull PublicKeyFormat publicKeyFormat) {
		requireNonNull(publicKeyAsString);
		requireNonNull(privateKeyAsString);
		requireNonNull(publicKeyFormat);

		publicKeyAsString = trimToEmpty(publicKeyAsString);
		privateKeyAsString = trimToEmpty(privateKeyAsString
				.replace("-----BEGIN RSA PRIVATE KEY-----", "")
				.replace("-----END RSA PRIVATE KEY-----", "")
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", ""));

		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey publicKey;

			if (publicKeyFormat == PublicKeyFormat.SSH) {
				publicKey = convertPublicKeyFromSshRsaToX509(publicKeyAsString);
			} else if (publicKeyFormat == PublicKeyFormat.X509) {
				try (InputStream inputStream = new ByteArrayInputStream(publicKeyAsString.getBytes(StandardCharsets.UTF_8))) {
					Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(inputStream);
					publicKey = certificate.getPublicKey();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} else {
				throw new IllegalArgumentException(format("Not sure how to handle %s.%s", PublicKeyFormat.class.getSimpleName(), publicKeyFormat.name()));
			}

			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decodeBase64(privateKeyAsString));
			PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

			return new KeyPair(publicKey, privateKey);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | CertificateException e) {
			throw new IllegalStateException(e);
		}
	}

	@Nonnull
	public static PublicKey publicKeyFromStringRepresentation(@Nonnull String publicKeyAsString) {
		requireNonNull(publicKeyAsString);

		publicKeyAsString = trimToNull(publicKeyAsString);

		if (publicKeyAsString == null)
			throw new IllegalArgumentException("Public key is blank");

		publicKeyAsString = publicKeyAsString
				.replace("-----BEGIN RSA PUBLIC KEY-----", "")
				.replace("-----END RSA PUBLIC KEY-----", "");

		// Remove all whitespace
		publicKeyAsString = publicKeyAsString.replaceAll("\\s", "");

		byte[] keyBytes = Base64.getDecoder().decode(publicKeyAsString);

		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);

		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePublic(spec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new IllegalStateException(e);
		}
	}

	public enum PublicKeyFormat {
		X509,
		SSH
	}

	/**
	 * // SSH-RSA key format
	 * //
	 * //        00 00 00 07             The length in bytes of the next field
	 * //        73 73 68 2d 72 73 61    The key type (ASCII encoding of "ssh-rsa")
	 * //        00 00 00 03             The length in bytes of the public exponent
	 * //        01 00 01                The public exponent (usually 65537, as here)
	 * //        00 00 01 01             The length in bytes of the modulus (here, 257)
	 * //        00 c3 a3...             The modulus
	 * <p>
	 * See https://stackoverflow.com/a/54600720
	 */
	@Nonnull
	public static PublicKey convertPublicKeyFromSshRsaToX509(@Nonnull String sshRsaPublicKey) {
		requireNonNull(sshRsaPublicKey);

		sshRsaPublicKey = sshRsaPublicKey.trim();

		final byte[] INITIAL_PREFIX = new byte[]{0x00, 0x00, 0x00, 0x07, 0x73, 0x73, 0x68, 0x2d, 0x72, 0x73, 0x61};
		final Pattern SSH_RSA_PATTERN = Pattern.compile("ssh-rsa[\\s]+([A-Za-z0-9/+]+=*)[\\s]+.*");

		Matcher matcher = SSH_RSA_PATTERN.matcher(sshRsaPublicKey);

		if (!matcher.matches())
			throw new IllegalArgumentException("Key format is invalid for SSH RSA.");

		String keyStr = matcher.group(1);

		ByteArrayInputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(keyStr));

		byte[] prefix = new byte[INITIAL_PREFIX.length];

		try {
			if (INITIAL_PREFIX.length != is.read(prefix) || !Objects.deepEquals(INITIAL_PREFIX, prefix))
				throw new IllegalArgumentException("Initial [ssh-rsa] key prefix missed.");

			BigInteger exponent = getValue(is);
			BigInteger modulus = getValue(is);

			return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to read SSH RSA certificate from string", e);
		}
	}

	@Nonnull
	private static BigInteger getValue(@Nonnull InputStream is) throws IOException {
		requireNonNull(is);

		final int VALUE_LENGTH = 4;

		byte[] lenBuff = new byte[VALUE_LENGTH];
		if (VALUE_LENGTH != is.read(lenBuff))
			throw new IllegalArgumentException("Unable to read value length.");

		int len = ByteBuffer.wrap(lenBuff).getInt();
		byte[] valueArray = new byte[len];

		if (len != is.read(valueArray))
			throw new IllegalArgumentException("Unable to read value.");

		return new BigInteger(valueArray);
	}
}
