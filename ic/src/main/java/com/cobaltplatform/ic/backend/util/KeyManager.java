package com.cobaltplatform.ic.backend.util;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class KeyManager {
	private KeyManager() {
		// Not instantiable
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
	public static String stringRepresentation(@Nonnull PublicKey publicKey) {
		requireNonNull(publicKey);
		return Base64.getEncoder().encodeToString(publicKey.getEncoded());
	}

	@Nonnull
	public static String stringRepresentation(@Nonnull PrivateKey privateKey) {
		requireNonNull(privateKey);
		return Base64.getEncoder().encodeToString(privateKey.getEncoded());
	}

	@Nonnull
	public static PrivateKey privateKeyFromPem(@Nonnull String pemKey) {
		pemKey = trimToNull(pemKey);

		requireNonNull(pemKey);

		PrivateKey pk;

		try {
			PemReader pr = new PemReader(new StringReader(pemKey));
			PemObject po = pr.readPemObject();
			PEMParser pem = new PEMParser(new StringReader(pemKey));

			if (po.getType().equals("PRIVATE KEY")) {
				pk = new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) pem.readObject());
			} else {
				PEMKeyPair kp = (PEMKeyPair) pem.readObject();
				pk = new JcaPEMKeyConverter().getPrivateKey(kp.getPrivateKeyInfo());
			}
		} catch (Exception e) {
			throw new RuntimeException(format("Invalid PEM '%s'", pemKey), e);
		}

		if (pk == null)
			throw new RuntimeException(format("Unable to extract private key from PEM '%s'", pemKey));

		return pk;
	}

	@Nonnull
	public static KeyPair keyPairFromStringRepresentation(@Nonnull String publicKeyAsString,
																												@Nonnull String privateKeyAsString) {
		requireNonNull(publicKeyAsString);
		requireNonNull(privateKeyAsString);

		publicKeyAsString = normalizePublicKey(publicKeyAsString);
		privateKeyAsString = normalizePrivateKey(privateKeyAsString);

		if (publicKeyAsString.isBlank())
			throw new IllegalArgumentException("Public key is blank");

		if (privateKeyAsString.isBlank())
			throw new IllegalArgumentException("Private key is blank");

		KeyFactory keyFactory;
		PublicKey publicKey;
		PrivateKey privateKey;

		try {
			keyFactory = KeyFactory.getInstance("RSA");
		} catch (Exception e) {
			throw new IllegalStateException("Unable to load RSA", e);
		}

		try {
			publicKey = publicKeyFromStringRepresentation(publicKeyAsString);
		} catch (Exception e) {
			throw new RuntimeException(format("Invalid public key '%s'", publicKeyAsString), e);
		}

		try {
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decodeBase64(privateKeyAsString));
			privateKey = keyFactory.generatePrivate(privateKeySpec);
		} catch (Exception e) {
			throw new RuntimeException(format("Invalid private key '%s'", privateKeyAsString), e);
		}

		return new KeyPair(publicKey, privateKey);
	}


	@Nonnull
	public static PublicKey publicKeyFromStringRepresentation(@Nonnull String publicKeyAsString) {
		requireNonNull(publicKeyAsString);

		publicKeyAsString = normalizePublicKey(publicKeyAsString);

		if (publicKeyAsString == null)
			throw new IllegalArgumentException("Public key is blank");

		byte[] keyBytes = Base64.getDecoder().decode(publicKeyAsString);

		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);

		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePublic(spec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new IllegalStateException(e);
		}
	}

	@Nonnull
	public static PrivateKey privateKeyFromStringRepresentation(@Nonnull String privateKeyAsString) {
		requireNonNull(privateKeyAsString);

		privateKeyAsString = normalizePrivateKey(privateKeyAsString);

		if (privateKeyAsString == null)
			throw new IllegalArgumentException("Private key is blank");

		byte[] keyBytes = Base64.getDecoder().decode(privateKeyAsString);

		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);

		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePrivate(spec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new IllegalStateException(e);
		}
	}
	@Nonnull
	private static final String normalizePublicKey(@Nonnull String publicKeyAsString) {
		requireNonNull(publicKeyAsString);

		publicKeyAsString = publicKeyAsString
				.replace("-----BEGIN RSA PUBLIC KEY-----", "")
				.replace("-----END RSA PUBLIC KEY-----", "");

		// Remove all whitespace
		publicKeyAsString = publicKeyAsString.replaceAll("\\s", "");

		return publicKeyAsString;
	}

	@Nonnull
	private static final String normalizePrivateKey(@Nonnull String privateKeyAsString) {
		requireNonNull(privateKeyAsString);

		privateKeyAsString = trimToEmpty(privateKeyAsString
				.replace("-----BEGIN RSA PRIVATE KEY-----", "")
				.replace("-----END RSA PRIVATE KEY-----", "")
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", ""));

		// Remove all whitespace
		privateKeyAsString = privateKeyAsString.replaceAll("\\s", "");

		return privateKeyAsString;
	}
}
