/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.util.ValidationException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A validation failure caused solely by the patient already having an open order.
 * Additional validation failures must be reported as ordinary ValidationExceptions.
 */
@NotThreadSafe
public class PatientOrderAlreadyOpenException extends ValidationException {
	public PatientOrderAlreadyOpenException(@Nonnull String message) {
		super(message);
	}
}
