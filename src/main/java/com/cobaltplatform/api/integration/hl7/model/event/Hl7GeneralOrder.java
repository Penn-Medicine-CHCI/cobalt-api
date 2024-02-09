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

package com.cobaltplatform.api.integration.hl7.model.event;

import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7CommonOrder;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7MessageHeader;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7NotesAndComments;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/TriggerEvents/ORM_O01
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7GeneralOrder extends Hl7Object {
	@Nullable
	private Hl7MessageHeader messageHeader;
	@Nullable
	private List<Hl7NotesAndComments> notesAndComments;
	@Nullable
	private Hl7CommonOrder commonOrder;

	@Nullable
	public Hl7MessageHeader getMessageHeader() {
		return this.messageHeader;
	}

	public void setMessageHeader(@Nullable Hl7MessageHeader messageHeader) {
		this.messageHeader = messageHeader;
	}

	@Nullable
	public List<Hl7NotesAndComments> getNotesAndComments() {
		return this.notesAndComments;
	}

	public void setNotesAndComments(@Nullable List<Hl7NotesAndComments> notesAndComments) {
		this.notesAndComments = notesAndComments;
	}

	@Nullable
	public Hl7CommonOrder getCommonOrder() {
		return this.commonOrder;
	}

	public void setCommonOrder(@Nullable Hl7CommonOrder commonOrder) {
		this.commonOrder = commonOrder;
	}
}