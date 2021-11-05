package com.cobaltplatform.ic.backend.model.cobalt;

import com.cobaltplatform.ic.model.CobaltAccount;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreatePatientResponse {
	@Nullable
	private String accessToken;
	@Nullable
	private CobaltAccount account;

	@Nullable
	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(@Nullable String accessToken) {
		this.accessToken = accessToken;
	}

	@Nullable
	public CobaltAccount getAccount() {
		return account;
	}

	public void setAccount(@Nullable CobaltAccount account) {
		this.account = account;
	}
}
