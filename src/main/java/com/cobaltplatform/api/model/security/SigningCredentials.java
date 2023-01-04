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
package com.cobaltplatform.api.model.security;

import com.cobaltplatform.api.util.CryptoUtility;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class SigningCredentials {
	@Nonnull
	private final X509Certificate x509Certificate;
	@Nonnull
	private final PrivateKey privateKey;

	public SigningCredentials(@Nonnull X509Certificate x509Certificate,
														@Nonnull PrivateKey privateKey) {
		requireNonNull(x509Certificate);
		requireNonNull(privateKey);

		this.x509Certificate = x509Certificate;
		this.privateKey = privateKey;
	}

	public SigningCredentials(@Nonnull String x509CertificateAsString,
														@Nonnull String privateKeyAsString) {
		requireNonNull(x509CertificateAsString);
		requireNonNull(privateKeyAsString);

		this.x509Certificate = CryptoUtility.toX509Certificate(x509CertificateAsString);
		this.privateKey = CryptoUtility.toPrivateKey(privateKeyAsString);
	}

	@Nonnull
	public X509Certificate getX509Certificate() {
		return this.x509Certificate;
	}

	@Nonnull
	public PrivateKey getPrivateKey() {
		return this.privateKey;
	}
}
