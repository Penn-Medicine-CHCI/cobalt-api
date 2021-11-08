package com.cobaltplatform.ic.backend.util;

import org.junit.jupiter.api.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.security.KeyPair;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class KeyManagerTest {
	@Test
	public void keyPairStringMarshaling() {
		KeyPair keyPair = KeyManager.generateKeyPair("RSA");
		String publicKeyAsString = KeyManager.stringRepresentation(keyPair.getPublic());
		String privateKeyAsString = KeyManager.stringRepresentation(keyPair.getPrivate());

		KeyPair newKeyPair = KeyManager.keyPairFromStringRepresentation(publicKeyAsString, privateKeyAsString);
		assert keyPair.getPublic().equals(newKeyPair.getPublic());
		assert keyPair.getPrivate().equals(newKeyPair.getPrivate());

		// Uncomment to dump out a new keypair for use in (say) a new environment
		// System.out.println("Public: " + publicKeyAsString);
		// System.out.println("Private: " + privateKeyAsString);
	}
}
