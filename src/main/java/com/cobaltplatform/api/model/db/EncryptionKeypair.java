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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.PrivateKeyFormat.PrivateKeyFormatId;
import com.cobaltplatform.api.model.db.PublicKeyFormat.PublicKeyFormatId;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class EncryptionKeypair {
	@Nullable
	private UUID encryptionKeypairId;
	@Nullable
	private PublicKeyFormatId publicKeyFormatId;
	@Nullable
	private PrivateKeyFormatId privateKeyFormatId;
	@Nullable
	@DatabaseColumn("public_key")
	private String publicKeyAsString;
	@Nullable
	@DatabaseColumn("private_key")
	private String privateKeyAsString;
	@Nullable
	private Integer keySize;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getEncryptionKeypairId() {
		return this.encryptionKeypairId;
	}

	public void setEncryptionKeypairId(@Nullable UUID encryptionKeypairId) {
		this.encryptionKeypairId = encryptionKeypairId;
	}

	@Nullable
	public PublicKeyFormatId getPublicKeyFormatId() {
		return this.publicKeyFormatId;
	}

	public void setPublicKeyFormatId(@Nullable PublicKeyFormatId publicKeyFormatId) {
		this.publicKeyFormatId = publicKeyFormatId;
	}

	@Nullable
	public PrivateKeyFormatId getPrivateKeyFormatId() {
		return this.privateKeyFormatId;
	}

	public void setPrivateKeyFormatId(@Nullable PrivateKeyFormatId privateKeyFormatId) {
		this.privateKeyFormatId = privateKeyFormatId;
	}

	@Nullable
	public String getPublicKeyAsString() {
		return this.publicKeyAsString;
	}

	public void setPublicKeyAsString(@Nullable String publicKeyAsString) {
		this.publicKeyAsString = publicKeyAsString;
	}

	@Nullable
	public String getPrivateKeyAsString() {
		return this.privateKeyAsString;
	}

	public void setPrivateKeyAsString(@Nullable String privateKeyAsString) {
		this.privateKeyAsString = privateKeyAsString;
	}

	@Nullable
	public Integer getKeySize() {
		return this.keySize;
	}

	public void setKeySize(@Nullable Integer keySize) {
		this.keySize = keySize;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}