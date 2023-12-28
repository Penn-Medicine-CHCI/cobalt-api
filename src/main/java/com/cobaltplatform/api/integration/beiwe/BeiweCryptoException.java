/*
 * Copyright 2023 Cobalt Innovations, Inc.
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

package com.cobaltplatform.api.integration.beiwe;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Cobalt Innovations, Inc.
 */
@ThreadSafe
public class BeiweCryptoException extends RuntimeException {
	public BeiweCryptoException(@Nullable String message) {
		super(message);
	}

	public BeiweCryptoException(@Nullable Throwable cause) {
		super(cause);
	}

	public BeiweCryptoException(@Nullable String message,
															@Nullable Throwable cause) {
		super(message, cause);
	}
}
