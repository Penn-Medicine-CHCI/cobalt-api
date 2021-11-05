package com.cobaltplatform.ic.backend.model.cobalt;

import com.cobaltplatform.ic.model.CobaltAccount;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class FindAccountResponse {
	@Nullable
	private CobaltAccount account;

	@Nullable
	public CobaltAccount getAccount() {
		return account;
	}

	public void setAccount(@Nullable CobaltAccount account) {
		this.account = account;
	}
}
