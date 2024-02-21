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

package com.cobaltplatform.api.integration.hl7.model.type;

import ca.uhn.hl7v2.model.v251.datatype.MSG;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/HL7v2.5.1/DataTypes/MSG
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7MessageType extends Hl7Object {
	@Nullable
	private String messageCode;
	@Nullable
	private String triggerEvent;
	@Nullable
	private String messageStructure;

	@Nonnull
	public static Boolean isPresent(@Nullable MSG msg) {
		if (msg == null)
			return false;

		return trimToNull(msg.getMessageCode().getValueOrEmpty()) != null
				|| trimToNull(msg.getTriggerEvent().getValueOrEmpty()) != null
				|| trimToNull(msg.getMessageStructure().getValueOrEmpty()) != null;
	}

	public Hl7MessageType() {
		// Nothing to do
	}

	public Hl7MessageType(@Nullable MSG msg) {
		if (msg != null) {
			this.messageCode = trimToNull(msg.getMessageCode().getValueOrEmpty());
			this.triggerEvent = trimToNull(msg.getTriggerEvent().getValueOrEmpty());
			this.messageStructure = trimToNull(msg.getMessageStructure().getValueOrEmpty());
		}
	}

	@Nullable
	public String getMessageCode() {
		return this.messageCode;
	}

	public void setMessageCode(@Nullable String messageCode) {
		this.messageCode = messageCode;
	}

	@Nullable
	public String getTriggerEvent() {
		return this.triggerEvent;
	}

	public void setTriggerEvent(@Nullable String triggerEvent) {
		this.triggerEvent = triggerEvent;
	}

	@Nullable
	public String getMessageStructure() {
		return this.messageStructure;
	}

	public void setMessageStructure(@Nullable String messageStructure) {
		this.messageStructure = messageStructure;
	}
}