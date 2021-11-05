package com.cobaltplatform.ic.backend.exception;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CobaltException extends Exception {
	public CobaltException(String message) {
		super(message);
	}

	public CobaltException(String message, Throwable cause) {
		super(message, cause);
	}

	public CobaltException(Throwable cause) {
		super(cause);
	}
}
